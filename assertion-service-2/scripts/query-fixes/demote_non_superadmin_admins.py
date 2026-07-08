#!/usr/bin/env python3
"""
Demote admin users that do not belong to a superadmin-enabled member.

An admin user is only legitimate when the member it belongs to has
`superadmin_enabled == true`. Historically (before server-side validation was
added) admin users could be created against members that are not
superadmin-enabled - for example by ticking the "Admin" checkbox for a
superadmin-enabled member and then switching the organization to a
non-superadmin-enabled one before saving. This script cleans up that bad data.

The check spans two databases:
  - memberservice.member    (source of truth for `superadmin_enabled`)
  - userservice.jhi_user    (holds the `admin` flag to be corrected)

Logic:
  1. Load every superadmin-enabled member from `memberservice.member` and build
     the set of their `_id` (hex) values.
  2. Scan every admin user (`admin == true`) in `userservice.jhi_user`.
  3. An admin user is kept ONLY if it clearly belongs to a superadmin-enabled
     member - its `member_id` matches a superadmin member `_id`.
     Every other admin user (non-superadmin member, orphaned member reference,
     or no member at all) has `admin` set to false.

Related to: https://orcid.clickup.com/t/PD-5857

Usage:
    python demote_non_superadmin_admins.py            # plan, confirm, apply, verify
    python demote_non_superadmin_admins.py --dry-run  # only report what would change

Environment Variables:
    SPRING_DATA_MONGODB_URI - MongoDB connection string (read from env)
"""

import argparse
import sys
from pathlib import Path
from typing import Dict, List, Set, Tuple

from pymongo import UpdateOne
from pymongo.collection import Collection
from pymongo.errors import BulkWriteError, OperationFailure

CURRENT_DIR = Path(__file__).resolve().parent
UTILS_DIR = CURRENT_DIR.parent / "utils"

if str(UTILS_DIR) not in sys.path:
    sys.path.insert(0, str(UTILS_DIR))

# Import shared modules
from logger_config import setup_logger
from db_connection import MongoDBConnection
from config import Config

# Set up logging
logger = setup_logger(__name__, log_file='demote-non-superadmin-admins.log')

# Number of bulk operations to accumulate before flushing to MongoDB.
BATCH_SIZE = 1000
# How often (in documents scanned) to emit a progress line.
PROGRESS_EVERY = 100_000


class DemotionError(RuntimeError):
    pass


class SuperadminMembers:
    """Read-only view of superadmin-enabled members."""

    def __init__(self, connection_to_db: MongoDBConnection):
        self.collection_member = connection_to_db.get_collection('member')
        self.member_ids: Set[str] = set()

    def load(self) -> None:
        """Populate the allowed `_id` (hex) set."""
        logger.info("\n" + "=" * 80)
        logger.info("Loading superadmin-enabled members...")
        logger.info("=" * 80)

        try:
            members = list(
                self.collection_member.find(
                    {'superadmin_enabled': True},
                    {'_id': 1, 'client_name': 1},
                )
            )
        except OperationFailure as e:
            logger.error(f"Failed to query members: {e}")
            raise

        for member in members:
            raw_id = member.get('_id')
            if raw_id is not None:
                # Java stores memberId as the hex string of the ObjectId.
                self.member_ids.add(str(raw_id))

        logger.info(
            "Superadmin-enabled members loaded: %d",
            len(members),
        )
        logger.info("=" * 80)

    def is_allowed_admin(self, user: Dict) -> bool:
        """Return True if this user legitimately belongs to a superadmin member."""
        member_id = user.get('member_id')
        return bool(member_id) and member_id in self.member_ids


class AdminUserDemoter:
    """Find and demote admin users that do not belong to a superadmin member."""

    def __init__(self, collection: Collection, superadmin_members: SuperadminMembers):
        self.collection = collection
        self.superadmin_members = superadmin_members

    def find_users_to_demote(self) -> List[Dict]:
        """Single read-only pass over admin users; return those to demote."""
        logger.info("  Scanning admin users in jhi_user ...")
        scanned = 0
        to_demote: List[Dict] = []
        cursor = self.collection.find(
            {'admin': True},
            {'_id': 1, 'email': 1, 'member_id': 1, 'member_name': 1, 'salesforce_id': 1},
            no_cursor_timeout=True,
        )
        try:
            for user in cursor:
                scanned += 1
                if scanned % PROGRESS_EVERY == 0:
                    logger.info("   ...jhi_user: scanned %d admin users, %d to demote", scanned, len(to_demote))
                if not self.superadmin_members.is_allowed_admin(user):
                    to_demote.append(user)
        finally:
            cursor.close()

        logger.info("  Admin users scanned: %d, to demote: %d", scanned, len(to_demote))
        return to_demote

    def demote(self, users: List[Dict]) -> int:
        """Set admin=false for the given users in batched bulk writes."""
        modified = 0
        batch: List[UpdateOne] = []
        for user in users:
            batch.append(UpdateOne({'_id': user['_id']}, {'$set': {'admin': False}}))
            if len(batch) >= BATCH_SIZE:
                modified += self._flush(batch)
                batch = []
        modified += self._flush(batch)
        return modified

    def _flush(self, batch: List[UpdateOne]) -> int:
        if not batch:
            return 0
        try:
            return self.collection.bulk_write(batch, ordered=False).modified_count
        except (BulkWriteError, OperationFailure) as e:
            detail = getattr(e, 'details', e)
            logger.error(f" Bulk write failed on jhi_user: {detail}")
            raise DemotionError(f"jhi_user bulk write failed: {e}") from e


def _log_affected_users(users: List[Dict]) -> None:
    logger.info("\nUsers that will have admin set to false:")
    for user in users:
        logger.info(
            "  - email=%s member_id=%s member_name=%s salesforce_id=%s",
            user.get('email'),
            user.get('member_id'),
            user.get('member_name'),
            user.get('salesforce_id'),
        )


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Demote admin users that do not belong to a superadmin-enabled member.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Report and, after confirmation, fix the data
  python demote_non_superadmin_admins.py

  # Only report what would change (no writes, no prompt)
  python demote_non_superadmin_admins.py --dry-run

Environment Variables:
  SPRING_DATA_MONGODB_URI - MongoDB connection string (read from env)
        """,
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Only report the admin users that would be demoted; make no changes.',
    )
    return parser.parse_args()


def main() -> int:
    args = parse_arguments()

    config = Config()
    mongo_uri = config.mongo_uri

    database_memberservice = 'memberservice'
    database_userservice = 'userservice'

    logger.info("=" * 80)
    logger.info("Demote admin users not belonging to a superadmin-enabled member")
    logger.info("=" * 80)
    logger.info(f"Databases: {database_memberservice}, {database_userservice}")
    logger.info("Collections: member, jhi_user")
    logger.info(f"MongoDB URI: {mongo_uri[:20]}..." if len(mongo_uri) > 20 else f"MongoDB URI: {mongo_uri}")
    logger.info("Mode: %s", "DRY RUN (no writes)" if args.dry_run else "APPLY (after confirmation)")
    logger.info("=" * 80 + "\n")

    connection_memberservice = MongoDBConnection(mongo_uri, database_memberservice)
    connection_userservice = MongoDBConnection(mongo_uri, database_userservice)

    try:
        if not connection_memberservice.connect():
            logger.error("Failed to connect to memberservice MongoDB. Exiting.")
            return 1
        if not connection_userservice.connect():
            logger.error("Failed to connect to userservice MongoDB. Exiting.")
            return 1

        superadmin_members = SuperadminMembers(connection_memberservice)
        superadmin_members.load()

        demoter = AdminUserDemoter(
            connection_userservice.get_collection('jhi_user'),
            superadmin_members,
        )

        # ---- Planning phase: one read-only pass, no writes ----
        logger.info("\n" + "=" * 80)
        logger.info("PLANNING: scanning admin users to find those to demote")
        logger.info("(no writes happen in this phase)")
        logger.info("=" * 80)

        to_demote = demoter.find_users_to_demote()

        if not to_demote:
            logger.info("\n No admin users need demoting. Nothing to do.")
            return 0

        _log_affected_users(to_demote)

        logger.info("\n" + "=" * 80)
        logger.info("  WARNING: This will modify the database!")
        logger.info(f"  Admin users to demote (admin -> false): {len(to_demote)}")
        logger.info("  Member documents will NOT be modified")
        logger.info("=" * 80)

        if args.dry_run:
            logger.info("\n Dry run - no changes made. Re-run without --dry-run to apply.")
            return 0

        try:
            response = input("\nDo you want to proceed? (yes/no): ").strip().lower()
            if response not in ('yes', 'y'):
                logger.info("\n Operation cancelled by user")
                return 0
        except (KeyboardInterrupt, EOFError):
            logger.info("\n\n Operation cancelled by user")
            return 1

        # ---- Execution phase: batched bulk writes ----
        logger.info("\n" + "=" * 80)
        logger.info("EXECUTING DEMOTION")
        logger.info("=" * 80)

        modified = demoter.demote(to_demote)
        logger.info(" jhi_user: %d admin users demoted", modified)

        # ---- Verification phase: re-scan, expect nothing left ----
        logger.info("\n" + "=" * 80)
        logger.info("VERIFYING DEMOTION")
        logger.info("=" * 80)

        remaining = demoter.find_users_to_demote()
        if remaining:
            logger.error(
                " %d admin user(s) still do not belong to a superadmin-enabled member!",
                len(remaining),
            )
            return 1

        logger.info("\n All admin users now belong to a superadmin-enabled member.")
        return 0

    except (DemotionError, OperationFailure) as e:
        logger.error(f" Demotion failed: {e}")
        return 1
    finally:
        connection_userservice.disconnect()
        connection_memberservice.disconnect()


if __name__ == "__main__":
    sys.exit(main())
