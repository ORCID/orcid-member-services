#!/usr/bin/env python3
"""
Set a specified member to active.

memberservice.member, setting:
  - active = true
  - activated_date = now (UTC)

The supplied identifier may be either:
  - the internal member `_id` (24-char ObjectId hex), or
  - the member `salesforce_id` (18-char Salesforce ID)

Usage:
    python set_member_active.py --member-id=5ff840862209c40008b0e477
    python set_member_active.py --member-id=0012i00000aQxlxAAC
    python set_member_active.py --member-id=689b3d3a8b6dfd63b72c1234 --dry-run

Environment Variables:
    SPRING_DATA_MONGODB_URI - MongoDB connection string (read from env)
"""

import argparse
import re
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Optional, Tuple

from bson import ObjectId
from bson.errors import InvalidId
from pymongo.collection import Collection
from pymongo.errors import OperationFailure

CURRENT_DIR = Path(__file__).resolve().parent
UTILS_DIR = CURRENT_DIR.parent / "utils"

if str(UTILS_DIR) not in sys.path:
    sys.path.insert(0, str(UTILS_DIR))

from logger_config import setup_logger
from db_connection import MongoDBConnection
from config import Config

logger = setup_logger(__name__, log_file='set-member-active.log')

OBJECT_ID_PATTERN = re.compile(r"^[0-9a-fA-F]{24}$")
SALESFORCE_ID_PATTERN = re.compile(r"^[a-zA-Z0-9]{18}$")


class InvalidMemberIdentifierError(ValueError):
    pass


class MemberActivationError(RuntimeError):
    pass


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Set a specified member to active.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Activate by internal member _id
  python set_member_active.py --member-id=689b3d3a8b6dfd63b72c1234

  # Activate by Salesforce ID
  python set_member_active.py --member-id=0012i00000aQxlxAAC

  # Only report what would change
  python set_member_active.py --member-id=689b3d3a8b6dfd63b72c1234 --dry-run

Environment Variables:
  SPRING_DATA_MONGODB_URI - MongoDB connection string (read from env)
        """,
    )
    parser.add_argument(
        '--member-id',
        required=True,
        help='Member identifier: either 24-char internal _id or 18-char Salesforce ID.',
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Only report what would change; make no changes.',
    )
    return parser.parse_args()


def classify_member_identifier(value: str) -> Tuple[str, object]:
    if OBJECT_ID_PATTERN.match(value):
        try:
            return '_id', ObjectId(value)
        except InvalidId as exc:
            raise InvalidMemberIdentifierError(
                f"Invalid internal member _id: {value!r}"
            ) from exc

    if SALESFORCE_ID_PATTERN.match(value):
        return 'salesforce_id', value

    raise InvalidMemberIdentifierError(
        f"Invalid member identifier: {value!r}. Expected 24-char ObjectId hex "
        "or 18-char Salesforce ID."
    )


class MemberRepository:
    """Read/write access to memberservice.member."""

    def __init__(self, connection_to_db: MongoDBConnection):
        self.collection: Collection = connection_to_db.get_collection('member')

    def find_member(self, field_name: str, field_value: object) -> Optional[Dict]:
        try:
            return self.collection.find_one(
                {field_name: field_value},
                {
                    '_id': 1,
                    'salesforce_id': 1,
                    'client_name': 1,
                    'active': 1,
                    'activated_date': 1,
                    'deactivated_date': 1,
                },
            )
        except OperationFailure as exc:
            logger.error("Failed to query member: %s", exc)
            raise

    def activate_member(self, member_id: ObjectId) -> int:
        try:
            result = self.collection.update_one(
                {'_id': member_id},
                {
                    '$set': {
                        'active': True,
                        'activated_date': datetime.now(timezone.utc),
                    },
                    '$unset': {
                        'deactivated_date': '',
                    },
                },
            )
        except OperationFailure as exc:
            logger.error("Failed to update member: %s", exc)
            raise MemberActivationError(f"Member update failed: {exc}") from exc

        return result.modified_count


def log_member(member: Dict) -> None:
    logger.info(
        "  - _id=%s salesforce_id=%s client_name=%s active=%s activated_date=%s deactivated_date=%s",
        member.get('_id'),
        member.get('salesforce_id'),
        member.get('client_name'),
        member.get('active'),
        member.get('activated_date'),
        member.get('deactivated_date'),
    )


def main() -> int:
    args = parse_arguments()

    try:
        lookup_field, lookup_value = classify_member_identifier(args.member_id)
    except InvalidMemberIdentifierError as exc:
        logger.error(str(exc))
        return 1

    config = Config()
    mongo_uri = config.mongo_uri
    database_memberservice = 'memberservice'

    logger.info("=" * 80)
    logger.info("Set specified member to active")
    logger.info("=" * 80)
    logger.info("Database: %s", database_memberservice)
    logger.info("Collection: member")
    logger.info(f"MongoDB URI: {mongo_uri[:20]}..." if len(mongo_uri) > 20 else f"MongoDB URI: {mongo_uri}")
    logger.info("Lookup: %s=%s", lookup_field, args.member_id)
    logger.info("Mode: %s", "DRY RUN (no writes)" if args.dry_run else "APPLY (after confirmation)")
    logger.info("=" * 80 + "\n")

    connection_memberservice = MongoDBConnection(mongo_uri, database_memberservice)

    try:
        if not connection_memberservice.connect():
            logger.error("Failed to connect to memberservice MongoDB. Exiting.")
            return 1

        member_repo = MemberRepository(connection_memberservice)

        logger.info("\n" + "=" * 80)
        logger.info("PLANNING: locating member and checking whether activation is needed")
        logger.info("(no writes happen in this phase)")
        logger.info("=" * 80)

        member = member_repo.find_member(lookup_field, lookup_value)
        if not member:
            logger.error("No member found for %s=%s", lookup_field, args.member_id)
            return 1

        logger.info("\nMember in scope:")
        log_member(member)

        if member.get('active') is True:
            logger.info("\nMember is already active. Nothing to do.")
            return 0

        logger.info("\n" + "=" * 80)
        logger.info("  WARNING: This will modify the database!")
        logger.info("  Member to activate: %s", member.get('_id'))
        logger.info("  Change to apply: active -> true, activated_date -> now (UTC)")
        logger.info("  deactivated_date will be removed")
        logger.info("=" * 80)

        if args.dry_run:
            logger.info("\nDry run - no changes made. Re-run without --dry-run to apply.")
            return 0

        try:
            response = input("\nDo you want to proceed? (yes/no): ").strip().lower()
            if response not in ('yes', 'y'):
                logger.info("\nOperation cancelled by user")
                return 0
        except (KeyboardInterrupt, EOFError):
            logger.info("\n\nOperation cancelled by user")
            return 1

        logger.info("\n" + "=" * 80)
        logger.info("EXECUTING ACTIVATION")
        logger.info("=" * 80)

        modified = member_repo.activate_member(member['_id'])
        logger.info("member: %d document(s) updated", modified)

        logger.info("\n" + "=" * 80)
        logger.info("VERIFYING ACTIVATION")
        logger.info("=" * 80)

        updated_member = member_repo.find_member('_id', member['_id'])
        if not updated_member or updated_member.get('active') is not True:
            logger.error("Member activation verification failed for _id=%s", member.get('_id'))
            return 1

        logger.info("\nMember is now active:")
        log_member(updated_member)
        return 0

    except (MemberActivationError, OperationFailure) as exc:
        logger.error("Activation failed: %s", exc)
        return 1
    finally:
        connection_memberservice.disconnect()


if __name__ == "__main__":
    sys.exit(main())