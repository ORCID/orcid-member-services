Security protection e.g USB key #!/usr/bin/env python3
"""
Backfill missing ``tokens[].member_id`` placeholders onto ``orcid_record``.

Usage:
    python backfill_orcid_record_tokens.py                          # all members
    python backfill_orcid_record_tokens.py --member-id=60a...c3     # one member only
    python backfill_orcid_record_tokens.py --source-email=x@y.org   # one email only
"""

import argparse
import re
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Set, Tuple

from pymongo import InsertOne, UpdateOne
from pymongo.collection import Collection
from pymongo.errors import BulkWriteError, OperationFailure

CURRENT_DIR = Path(__file__).resolve().parent
UTILS_DIR = CURRENT_DIR.parent / "utils"

if str(UTILS_DIR) not in sys.path:
    sys.path.insert(0, str(UTILS_DIR))

from logger_config import setup_logger
from db_connection import MongoDBConnection
from config import Config

logger = setup_logger(__name__, log_file="backfill-orcid-record-tokens.log")

EMAIL_PATTERN = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")

BATCH_SIZE = 1000
PROGRESS_EVERY = 100_000


def validate_email(value: str) -> str:
    """Return the lowercased email if it looks valid; raise otherwise."""
    if not value or not EMAIL_PATTERN.match(value):
        raise ValueError(
            f"Invalid email: {value!r}. Expected a well-formed email address."
        )
    return value.lower()


class OrcidRecordTokenBackfiller:
    """Ensure every (email, member_id) in assertion has a matching token."""

    def __init__(self, connection: MongoDBConnection):
        self.collection_assertion: Collection = connection.get_collection("assertion")
        self.collection_orcid_record: Collection = connection.get_collection("orcid_record")

    # ---- planning ---------------------------------------------------------

    def build_needed_map(self, member_id: str = None, source_email: str = None) -> Dict[str, Set[str]]:
        """Return ``{email_lower: {member_id, ...}}`` from the assertion collection.

        One ``(email, memberId)`` pair per distinct assertion grouping, computed
        server-side.  Assertions with a blank member_id or an unparseable email
        are skipped with a count.
        """
        match: Dict[str, object] = {"member_id": {"$nin": [None, ""]}}
        if member_id:
            match["member_id"] = member_id
        if source_email:
            # case-insensitive exact match on the email
            match["email"] = {"$regex": f"^{re.escape(source_email)}$", "$options": "i"}

        logger.info("  Reading distinct (email, member_id) pairs from assertion ...")
        pipeline = [
            {"$match": match},
            {"$group": {"_id": {"email": {"$toLower": "$email"}, "member_id": "$member_id"}}},
        ]

        needed: Dict[str, Set[str]] = {}
        bad_email = 0
        pairs = 0
        try:
            cursor = self.collection_assertion.aggregate(pipeline, allowDiskUse=True)
        except OperationFailure as e:
            logger.error("  Failed to aggregate assertion pairs: %s", e)
            raise

        for doc in cursor:
            key = doc["_id"]
            email = (key.get("email") or "").strip().lower()
            mid = key.get("member_id")
            if not email or not EMAIL_PATTERN.match(email):
                bad_email += 1
                continue
            if not mid:
                continue
            needed.setdefault(email, set()).add(mid)
            pairs += 1

        logger.info(
            "    assertion: %d emails, %d (email, member_id) pairs (%d invalid emails skipped)",
            len(needed), pairs, bad_email,
        )
        return needed

    def plan(self, needed: Dict[str, Set[str]]) -> Tuple[List[UpdateOne], List[InsertOne], int, int]:
        """Single read-only pass over orcid_record.

        Returns ``(updates, inserts, tokens_to_add, records_to_create)``.
        Existing tokens are preserved; only missing placeholders are appended.
        """
        if not needed:
            return [], [], 0, 0

        emails = list(needed.keys())
        logger.info("  Scanning orcid_record for %d candidate emails ...", len(emails))

        updates: List[UpdateOne] = []
        tokens_to_add = 0
        seen: Set[str] = set()
        scanned = 0

        # Restrict the scan to candidate emails so large collections aren't read
        # in full.  $in on the unique email index keeps this efficient.
        cursor = self.collection_orcid_record.find(
            {"email": {"$in": emails}},
            {"_id": 1, "email": 1, "tokens": 1},
            no_cursor_timeout=True,
        )
        try:
            for doc in cursor:
                scanned += 1
                if scanned % PROGRESS_EVERY == 0:
                    logger.info("    ... scanned %d records, %d need tokens", scanned, len(updates))

                email = (doc.get("email") or "").strip().lower()
                if email not in needed:
                    continue
                seen.add(email)

                tokens = doc.get("tokens") or []
                existing_members = {
                    t.get("member_id") for t in tokens if isinstance(t, dict) and t.get("member_id")
                }
                missing = needed[email] - existing_members
                if not missing:
                    continue

                new_tokens = list(tokens) + [{"member_id": mid} for mid in sorted(missing)]
                tokens_to_add += len(missing)
                updates.append(
                    UpdateOne(
                        {"_id": doc["_id"]},
                        {"$set": {"tokens": new_tokens, "modified": datetime.now(timezone.utc)}},
                    )
                )
        finally:
            cursor.close()

        # Emails with assertions but no orcid_record at all -> insert.
        now = datetime.now(timezone.utc)
        inserts: List[InsertOne] = []
        for email in emails:
            if email in seen:
                continue
            tokens = [{"member_id": mid} for mid in sorted(needed[email])]
            inserts.append(
                InsertOne(
                    {"email": email, "tokens": tokens, "created": now, "modified": now}
                )
            )

        return updates, inserts, tokens_to_add, len(inserts)

    # ---- execution --------------------------------------------------------

    def execute(self, ops: List, label: str) -> int:
        """Run a list of bulk ops in batches; return documents written."""
        if not ops:
            return 0
        written = 0
        for start in range(0, len(ops), BATCH_SIZE):
            batch = ops[start:start + BATCH_SIZE]
            written += self._flush(batch, label)
            if start and start % (BATCH_SIZE * 50) == 0:
                logger.info("    ... %s: %d / %d written", label, written, len(ops))
        logger.info("  %s: %d documents written", label, written)
        return written

    def _flush(self, batch: List, label: str) -> int:
        try:
            result = self.collection_orcid_record.bulk_write(batch, ordered=False)
            return result.modified_count + result.inserted_count + result.upserted_count
        except BulkWriteError as bwe:
            detail = getattr(bwe, "details", {}) or {}
            ok = detail.get("nModified", 0) + detail.get("nInserted", 0) + detail.get("nUpserted", 0)
            errs = detail.get("writeErrors", [])
            logger.warning("  %s bulk write: %d ok, %d errors", label, ok, len(errs))
            for err in errs[:5]:
                logger.warning("    %s", err.get("errmsg", str(err)))
            return ok
        except OperationFailure as e:
            logger.error("  %s bulk write failed: %s", label, e)
            raise


def parse_arguments():
    parser = argparse.ArgumentParser(
        description="Backfill missing tokens[].member_id placeholders on orcid_record",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Restore the token invariant for every member
  python backfill_orcid_record_tokens.py

  # Scope to a single member (recommended first run in QA)
  python backfill_orcid_record_tokens.py --member-id=60a1b2c3d4e5f60718293a4b

  # Scope to a single email
  python backfill_orcid_record_tokens.py --source-email=researcher@example.org

Environment Variables:
  SPRING_DATA_MONGODB_URI - MongoDB connection string
        """,
    )
    parser.add_argument(
        "--member-id",
        required=False,
        default=None,
        help="Optional internal member id. When given, only assertions for that "
        "member are considered.",
    )
    parser.add_argument(
        "--source-email",
        required=False,
        default=None,
        help="Optional single email to process (case-insensitive).",
    )
    return parser.parse_args()


def main():
    args = parse_arguments()

    source_email = None
    if args.source_email:
        try:
            source_email = validate_email(args.source_email)
        except ValueError as e:
            logger.error(str(e))
            return 1

    config = Config()
    mongo_uri = config.mongo_uri
    database = "assertionservice"

    logger.info("=" * 80)
    logger.info("Backfill missing orcid_record token placeholders")
    logger.info("=" * 80)
    logger.info("Database:     %s", database)
    logger.info("Collections:  assertion -> orcid_record")
    logger.info("MongoDB URI:  %s...", mongo_uri[:20] if len(mongo_uri) > 20 else mongo_uri)
    logger.info(
        "Scope:        %s%s",
        f"member_id={args.member_id} " if args.member_id else "ALL members ",
        f"/ email={source_email}" if source_email else "",
    )
    logger.info("=" * 80 + "\n")

    connection = MongoDBConnection(mongo_uri, database)

    try:
        if not connection.connect():
            logger.error("Failed to connect to MongoDB. Exiting.")
            return 1

        backfiller = OrcidRecordTokenBackfiller(connection)

        logger.info("\n" + "=" * 80)
        logger.info("PLANNING: discover missing tokens (no writes)")
        logger.info("=" * 80)

        needed = backfiller.build_needed_map(args.member_id, source_email)
        if not needed:
            logger.info("\n No (email, member_id) pairs found in assertion. Nothing to do.")
            return 0

        updates, inserts, tokens_to_add, records_to_create = backfiller.plan(needed)

        if not updates and not inserts:
            logger.info(
                "\n Every (email, member_id) pair already has a matching token. Nothing to do."
            )
            return 0

        logger.info("\n  Summary")
        logger.info("    Existing records needing a token:  %d  (%d tokens to add)", len(updates), tokens_to_add)
        logger.info("    Missing records to create:         %d", records_to_create)

        logger.info("\n" + "=" * 80)
        logger.info("  WARNING: This will modify the database!")
        logger.info("  Collection:                 orcid_record")
        logger.info("  Records updated (add token): %d", len(updates))
        logger.info("  Records inserted (new):      %d", records_to_create)
        logger.info("  Placeholder shape:           { member_id: <id> }  (no token_id)")
        logger.info("  Existing tokens:             left untouched")
        logger.info("=" * 80)

        try:
            response = input("\nDo you want to proceed? (yes/no): ").strip().lower()
            if response not in ("yes", "y"):
                logger.info("\n Operation cancelled by user")
                return 0
        except (KeyboardInterrupt, EOFError):
            logger.info("\n\n Operation cancelled by user")
            return 1

        logger.info("\n" + "=" * 80)
        logger.info("EXECUTING BACKFILL")
        logger.info("=" * 80)

        updated = backfiller.execute(updates, "add-token")
        inserted = backfiller.execute(inserts, "insert-record")

        logger.info("\n" + "=" * 80)
        logger.info("VERIFYING — re-plan, expect nothing left")
        logger.info("=" * 80)

        needed_after = backfiller.build_needed_map(args.member_id, source_email)
        upd_after, ins_after, _, _ = backfiller.plan(needed_after)
        remaining = len(upd_after) + len(ins_after)
        if remaining > 0:
            logger.warning(
                " Verification: %d record(s) still missing a token (expected 0)", remaining
            )
            return 1

        logger.info(" Verification passed: every assertion pair now has a token")
        logger.info("\n" + "=" * 80)
        logger.info(
            "Script completed successfully — %d records updated, %d inserted",
            updated, inserted,
        )
        logger.info("=" * 80)
        return 0

    except KeyboardInterrupt:
        logger.info("\n\n Operation cancelled by user (Ctrl+C)")
        return 1
    except Exception as e:
        logger.error("\n Unexpected error: %s", e, exc_info=True)
        raise
    finally:
        connection.disconnect()


if __name__ == "__main__":
    sys.exit(main())
