#!/usr/bin/env python3
"""
Backfill internal member IDs onto related records.

Builds an in-memory salesforce_id -> member._id map from every member in the
member-service `member` collection, then makes a single pass over each related
collection, writing the matching member_id via batched bulk updates.

This single-pass design avoids per-member queries: each related collection is
scanned a small, constant number of times regardless of how many members
exist, and every write is keyed by `_id` (always indexed). It therefore does
NOT depend on `salesforce_id` being indexed on the related collections
(it is not, on orcid_record / send_notifications_request / jhi_user).

A single member can still be targeted with --source for testing.

Backfilled targets:
  - assertionservice.assertion                   (set member_id where salesforce_id matches)
  - assertionservice.orcid_record                (set tokens[].member_id where tokens[].salesforce_id matches)
  - assertionservice.send_notifications_request  (set member_id where salesforce_id matches)
  - userservice.jhi_user                         (set member_id where salesforce_id matches)

Related to: https://orcid.clickup.com/t/9014437828/PD-5585

Usage:
    python backfill_member_id.py                              # backfill ALL members
    python backfill_member_id.py --source=0012i00000aQxlxAAC  # backfill one member only
"""

import argparse
import re
import sys
from pathlib import Path
from typing import List, Dict, Tuple, Set
from pymongo import UpdateOne
from pymongo.collection import Collection
from pymongo.errors import OperationFailure, BulkWriteError

CURRENT_DIR = Path(__file__).resolve().parent
UTILS_DIR = CURRENT_DIR.parent / "utils"

if str(UTILS_DIR) not in sys.path:
    sys.path.insert(0, str(UTILS_DIR))

# Import shared modules
from logger_config import setup_logger
from db_connection import MongoDBConnection
from config import Config

# Set up logging
logger = setup_logger(__name__, log_file='backfill-member-id.log')

SALESFORCE_ID_PATTERN = re.compile(r"^[a-zA-Z0-9]{18}$")

# Number of bulk operations to accumulate before flushing to MongoDB.
BATCH_SIZE = 1000
# How often (in documents scanned) to emit a progress line.
PROGRESS_EVERY = 100_000


class InvalidSalesforceIdError(ValueError):
    pass


class MemberNotFoundError(LookupError):
    pass


class BackfillError(RuntimeError):
    pass


def validate_salesforce_id(value: str) -> str:
    if not value or not SALESFORCE_ID_PATTERN.match(value):
        raise InvalidSalesforceIdError(
            f"Invalid Salesforce ID: {value!r}. "
            f"Expected exactly 18 alphanumeric characters."
        )
    return value


class MemberRepository:
    """Read-only access to the member-service `member` collection."""

    def __init__(self, connection_to_db: MongoDBConnection):
        self.collection_member = connection_to_db.get_collection('member')

    def build_salesforce_to_member_map(self, source_filter: str = None) -> Dict[str, str]:
        """Return a {salesforce_id: member_id} map.

        Members missing an `_id` (the value backfilled) or a `salesforce_id`
        (the key related records are matched on) are skipped with a warning.
        When `source_filter` is given, only that member is loaded.
        """
        scope = f"salesforce_id={source_filter}" if source_filter else "ALL members"
        logger.info("\n" + "="*80)
        logger.info("Loading members (%s)...", scope)
        logger.info("="*80)

        query = {'salesforce_id': source_filter} if source_filter else {}
        try:
            members = list(self.collection_member.find(
                query, {'_id': 1, 'salesforce_id': 1, 'client_name': 1}
            ))
        except OperationFailure as e:
            logger.error(f"Failed to query members: {e}")
            raise

        if source_filter and not members:
            raise MemberNotFoundError(
                f"No member found for salesforce_id={source_filter}"
            )

        sf_map: Dict[str, str] = {}
        skipped = 0
        duplicates: Set[str] = set()
        for member in members:
            raw_id = member.get('_id')
            salesforce_id = member.get('salesforce_id')
            if not raw_id:
                logger.warning(" Skipping member with no _id (salesforce_id=%s)", salesforce_id)
                skipped += 1
                continue
            if not salesforce_id:
                logger.warning(
                    " Skipping member with no salesforce_id (_id=%s, client_name=%s)",
                    raw_id, member.get('client_name'),
                )
                skipped += 1
                continue
            if salesforce_id in sf_map:
                duplicates.add(salesforce_id)
            # Java entities declare memberId as String, so persist the hex
            # representation of the ObjectId to stay consistent with Spring Data.
            sf_map[salesforce_id] = str(raw_id)

        logger.info("Members loaded: %d total, %d usable, %d skipped",
                    len(members), len(sf_map), skipped)
        if duplicates:
            logger.warning(
                " %d salesforce_id(s) are shared by multiple members; related "
                "records will get the member_id of whichever member was read "
                "last: %s",
                len(duplicates), ", ".join(sorted(duplicates)),
            )
        logger.info("="*80)
        return sf_map


class _BulkBackfiller:
    """Shared bulk-write plumbing for the single-pass backfillers."""

    def __init__(self, collection: Collection, label: str):
        self.collection = collection
        self.label = label

    def _flush(self, batch: List[UpdateOne]) -> int:
        """Write a batch of updates, returning the number of documents modified."""
        if not batch:
            return 0
        try:
            return self.collection.bulk_write(batch, ordered=False).modified_count
        except (BulkWriteError, OperationFailure) as e:
            detail = getattr(e, 'details', e)
            logger.error(f" Bulk write failed on {self.label}: {detail}")
            raise BackfillError(f"{self.label} bulk write failed: {e}") from e


class TopLevelBackfiller(_BulkBackfiller):
    """Backfill member_id on a collection with top-level salesforce_id/member_id."""

    def scan(self, sf_map: Dict[str, str], sf_ids: List[str], apply: bool) -> Tuple[int, int, int]:
        """Single pass over the collection.

        Returns (scanned, needs_update, modified). When `apply` is False no
        writes happen and `modified` is 0.
        """
        logger.info("  Scanning %s%s ...", self.label, " (applying updates)" if apply else "")
        scanned = needs_update = modified = 0
        batch: List[UpdateOne] = []
        cursor = self.collection.find(
            {'salesforce_id': {'$in': sf_ids}},
            {'_id': 1, 'salesforce_id': 1, 'member_id': 1},
            no_cursor_timeout=True,
        )
        try:
            for doc in cursor:
                scanned += 1
                if scanned % PROGRESS_EVERY == 0:
                    logger.info("   ...%s: scanned %d, %d need update", self.label, scanned, needs_update)

                target = sf_map.get(doc.get('salesforce_id'))
                if target is None or doc.get('member_id') == target:
                    continue

                needs_update += 1
                if apply:
                    batch.append(UpdateOne(
                        {'_id': doc['_id']},
                        {'$set': {'member_id': target}},
                    ))
                    if len(batch) >= BATCH_SIZE:
                        modified += self._flush(batch)
                        batch = []

            if apply:
                modified += self._flush(batch)
        finally:
            cursor.close()

        return scanned, needs_update, modified


class OrcidRecordBackfiller(_BulkBackfiller):
    """Backfill tokens[].member_id on the orcid_record collection."""

    def __init__(self, collection: Collection):
        super().__init__(collection, 'orcid_record')

    def scan(self, sf_map: Dict[str, str], sf_ids: List[str], apply: bool) -> Tuple[int, int, int, int]:
        """Single pass over the collection.

        Returns (scanned, docs_needing_update, tokens_needing_update, modified).
        Each affected document has its whole `tokens` array rewritten, changing
        only the member_id of tokens that need it; all other fields are kept.
        """
        logger.info("  Scanning %s%s ...", self.label, " (applying updates)" if apply else "")
        scanned = docs_needing = tokens_needing = modified = 0
        batch: List[UpdateOne] = []
        cursor = self.collection.find(
            {'tokens.salesforce_id': {'$in': sf_ids}},
            {'_id': 1, 'tokens': 1},
            no_cursor_timeout=True,
        )
        try:
            for doc in cursor:
                scanned += 1
                if scanned % PROGRESS_EVERY == 0:
                    logger.info("   ...%s: scanned %d, %d need update", self.label, scanned, docs_needing)

                tokens = doc.get('tokens') or []
                doc_changed = False
                for token in tokens:
                    target = sf_map.get(token.get('salesforce_id'))
                    if target is None or token.get('member_id') == target:
                        continue
                    token['member_id'] = target
                    tokens_needing += 1
                    doc_changed = True

                if not doc_changed:
                    continue

                docs_needing += 1
                if apply:
                    batch.append(UpdateOne(
                        {'_id': doc['_id']},
                        {'$set': {'tokens': tokens}},
                    ))
                    if len(batch) >= BATCH_SIZE:
                        modified += self._flush(batch)
                        batch = []

            if apply:
                modified += self._flush(batch)
        finally:
            cursor.close()

        return scanned, docs_needing, tokens_needing, modified


def parse_arguments():
    parser = argparse.ArgumentParser(
        description='Backfill internal member IDs onto related records',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Backfill every member
  python backfill_member_id.py

  # Backfill a single member only
  python backfill_member_id.py --source=0012i00000aQxlxAAC

Environment Variables:
  SPRING_DATA_MONGODB_URI - MongoDB connection string (read from env)
        """
    )

    parser.add_argument(
        '--source',
        required=False,
        default=None,
        help='Optional Salesforce ID. When given, only that member is '
             'backfilled; otherwise every member is processed.'
    )

    return parser.parse_args()


def main():
    args = parse_arguments()

    source_filter = None
    if args.source:
        try:
            source_filter = validate_salesforce_id(args.source)
        except InvalidSalesforceIdError as e:
            logger.error(str(e))
            return 1

    config = Config()
    mongo_uri = config.mongo_uri

    database_assertionservice = 'assertionservice'
    database_userservice = 'userservice'
    database_memberservice = 'memberservice'

    logger.info("="*80)
    logger.info("Backfill internal member IDs")
    logger.info("="*80)
    logger.info(f"Databases: {database_assertionservice}, {database_userservice}, {database_memberservice}")
    logger.info(f"Collections: assertion, orcid_record, send_notifications_request, jhi_user")
    logger.info(f"MongoDB URI: {mongo_uri[:20]}..." if len(mongo_uri) > 20 else f"MongoDB URI: {mongo_uri}")
    logger.info("Scope: %s", f"single member (salesforce_id={source_filter})" if source_filter else "ALL members")
    logger.info("="*80 + "\n")

    connection_assertionservice = MongoDBConnection(mongo_uri, database_assertionservice)
    connection_userservice = MongoDBConnection(mongo_uri, database_userservice)
    connection_memberservice = MongoDBConnection(mongo_uri, database_memberservice)

    try:
        if not connection_memberservice.connect():
            logger.error("Failed to connect to memberservice MongoDB. Exiting.")
            return 1
        if not connection_assertionservice.connect():
            logger.error("Failed to connect to assertionservice MongoDB. Exiting.")
            return 1
        if not connection_userservice.connect():
            logger.error("Failed to connect to userservice MongoDB. Exiting.")
            return 1

        member_repo = MemberRepository(connection_memberservice)
        sf_map = member_repo.build_salesforce_to_member_map(source_filter)
        if not sf_map:
            logger.info("\n No usable members found. Nothing to do.")
            return 0
        sf_ids = list(sf_map.keys())

        assertion_bf = TopLevelBackfiller(
            connection_assertionservice.get_collection('assertion'), 'assertion')
        notification_bf = TopLevelBackfiller(
            connection_assertionservice.get_collection('send_notifications_request'),
            'send_notifications_request')
        user_bf = TopLevelBackfiller(
            connection_userservice.get_collection('jhi_user'), 'jhi_user')
        orcid_bf = OrcidRecordBackfiller(
            connection_assertionservice.get_collection('orcid_record'))

        # ---- Planning phase: one read-only pass per collection ----
        logger.info("\n" + "="*80)
        logger.info("PLANNING: scanning each collection once to count required changes")
        logger.info("(no writes happen in this phase)")
        logger.info("="*80)

        a_scanned, a_needs, _ = assertion_bf.scan(sf_map, sf_ids, apply=False)
        o_scanned, o_docs_needs, o_tokens_needs, _ = orcid_bf.scan(sf_map, sf_ids, apply=False)
        n_scanned, n_needs, _ = notification_bf.scan(sf_map, sf_ids, apply=False)
        u_scanned, u_needs, _ = user_bf.scan(sf_map, sf_ids, apply=False)

        logger.info("")
        logger.info(" assertion:                   scanned %d, %d need member_id", a_scanned, a_needs)
        logger.info(" orcid_record:                scanned %d, %d need member_id (%d tokens)",
                    o_scanned, o_docs_needs, o_tokens_needs)
        logger.info(" send_notifications_request:  scanned %d, %d need member_id", n_scanned, n_needs)
        logger.info(" jhi_user:                    scanned %d, %d need member_id", u_scanned, u_needs)

        total_needs = a_needs + n_needs + u_needs + o_docs_needs
        if total_needs == 0:
            logger.info(
                "\n member_id is already backfilled on all related records "
                "for the %d member(s) in scope. Nothing to do.",
                len(sf_map),
            )
            return 0

        logger.info("\n" + "="*80)
        logger.info("  WARNING: This will modify the database!")
        logger.info(f"  Members in scope:               {len(sf_map)}")
        logger.info(f"  assertion docs to update:       {a_needs}")
        logger.info(f"  orcid_record docs to update:    {o_docs_needs}  ({o_tokens_needs} tokens)")
        logger.info(f"  send_notifications to update:   {n_needs}")
        logger.info(f"  jhi_user docs to update:        {u_needs}")
        logger.info("  Member documents themselves will NOT be modified")
        logger.info("="*80)

        try:
            response = input("\nDo you want to proceed? (yes/no): ").strip().lower()
            if response not in ('yes', 'y'):
                logger.info("\n Operation cancelled by user")
                return 0
        except (KeyboardInterrupt, EOFError):
            logger.info("\n\n Operation cancelled by user")
            return 1

        # ---- Execution phase: one pass per collection, batched bulk writes ----
        logger.info("\n" + "="*80)
        logger.info("EXECUTING BACKFILL")
        logger.info("="*80)

        if a_needs:
            _, _, a_modified = assertion_bf.scan(sf_map, sf_ids, apply=True)
            logger.info(" assertion: %d documents updated", a_modified)
        if o_docs_needs:
            _, _, _, o_modified = orcid_bf.scan(sf_map, sf_ids, apply=True)
            logger.info(" orcid_record: %d documents updated", o_modified)
        if n_needs:
            _, _, n_modified = notification_bf.scan(sf_map, sf_ids, apply=True)
            logger.info(" send_notifications_request: %d documents updated", n_modified)
        if u_needs:
            _, _, u_modified = user_bf.scan(sf_map, sf_ids, apply=True)
            logger.info(" jhi_user: %d documents updated", u_modified)

        # ---- Verification phase: re-scan, expect nothing left ----
        logger.info("\n" + "="*80)
        logger.info("VERIFYING BACKFILL")
        logger.info("="*80)

        _, a_left, _ = assertion_bf.scan(sf_map, sf_ids, apply=False)
        _, o_left, _, _ = orcid_bf.scan(sf_map, sf_ids, apply=False)
        _, n_left, _ = notification_bf.scan(sf_map, sf_ids, apply=False)
        _, u_left, _ = user_bf.scan(sf_map, sf_ids, apply=False)

        remaining = a_left + o_left + n_left + u_left
        if remaining > 0:
            logger.warning(
                " Verification failed: %d records still missing or mismatched "
                "(assertion=%d, orcid_record=%d, send_notifications_request=%d, jhi_user=%d)",
                remaining, a_left, o_left, n_left, u_left,
            )
            return 1

        logger.info(" Verification passed: all related records carry the expected member_id")
        logger.info("\n" + "="*80)
        logger.info("Script completed successfully")
        logger.info("="*80)
        return 0

    except InvalidSalesforceIdError as e:
        logger.error(f"\nInvalid Salesforce ID: {e}")
        return 1
    except MemberNotFoundError as e:
        logger.error(f"\nMember not found: {e}")
        return 1
    except BackfillError as e:
        logger.error(f"\nBackfill operation failed: {e}")
        return 1
    except KeyboardInterrupt:
        logger.info("\n\n Operation cancelled by user (Ctrl+C)")
        return 1
    except Exception as e:
        logger.error(f"\n Unexpected error: {e}", exc_info=True)
        raise
    finally:
        connection_assertionservice.disconnect()
        connection_memberservice.disconnect()
        connection_userservice.disconnect()


if __name__ == '__main__':
    sys.exit(main())
