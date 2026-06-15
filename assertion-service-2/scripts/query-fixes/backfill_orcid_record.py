#!/usr/bin/env python3
"""
Populate the ``orcid_record`` collection with placeholder records for emails
that exist in the ``assertion`` collection but are missing from ``orcid_record``.

Placeholders contain only the email (lowercased) and ``created`` / ``modified``
timestamps — no tokens, no ORCID iD, no notification dates.  They fill the gap
so that every assertion email has a corresponding ``orcid_record`` document
without requiring a full permission-grant flow.

Usage:
    python backfill_orcid_record.py                   # scan and (on confirm) insert
    python backfill_orcid_record.py --source-email=x  # only handle one email
"""

import argparse
import re
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import List, Set

from pymongo import InsertOne
from pymongo.errors import BulkWriteError, OperationFailure

CURRENT_DIR = Path(__file__).resolve().parent
UTILS_DIR = CURRENT_DIR.parent / "utils"

if str(UTILS_DIR) not in sys.path:
    sys.path.insert(0, str(UTILS_DIR))

from logger_config import setup_logger
from db_connection import MongoDBConnection
from config import Config

logger = setup_logger(__name__, log_file="backfill-orcid-record.log")

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


class OrcidRecordBackfiller:
    """Find emails missing from ``orcid_record`` and insert placeholder documents."""

    def __init__(self, connection: MongoDBConnection):
        self.connection = connection
        self.collection_assertion = connection.get_collection("assertion")
        self.collection_snr = connection.get_collection("send_notifications_request")
        self.collection_orcid_record = connection.get_collection("orcid_record")

    def _distinct_emails(self, collection, label: str) -> Set[str]:
        """Return the set of distinct (lower-cased) email strings in *collection*."""
        logger.info("  Reading distinct emails from %s ...", label)
        try:
            raw = collection.distinct("email")
        except OperationFailure as e:
            logger.error("  Failed to query distinct emails in %s: %s", label, e)
            raise

        emails: Set[str] = set()
        bad = 0
        for e in raw:
            if not e:
                continue
            lower = e.strip().lower()
            if EMAIL_PATTERN.match(lower):
                emails.add(lower)
            else:
                bad += 1
        logger.info("    %s: %d unique emails (%d invalid skipped)", label, len(emails), bad)
        return emails

    def discover_missing(self, source_email: str = None) -> List[str]:
        """Return sorted list of emails present in assertion (or source_email)
        that have no matching ``orcid_record`` document."""

        if source_email:
            candidate_emails = {source_email}
            logger.info("  Single-email mode: %s", source_email)
        else:
            logger.info("  Building candidate set from assertion + send_notifications_request ...")
            from_assertion = self._distinct_emails(self.collection_assertion, "assertion")
            from_snr = self._distinct_emails(self.collection_snr, "send_notifications_request")
            candidate_emails = from_assertion | from_snr
            logger.info("  Combined candidate set: %d unique emails", len(candidate_emails))

        if not candidate_emails:
            return []

        # Collect existing orcid_record emails
        logger.info("  Reading existing emails from orcid_record ...")
        try:
            raw = self.collection_orcid_record.distinct("email")
        except OperationFailure as e:
            logger.error("  Failed to query orcid_record emails: %s", e)
            raise

        existing: Set[str] = set()
        for e in raw:
            if e:
                existing.add(e.strip().lower())
        logger.info("    orcid_record: %d existing emails", len(existing))

        missing = sorted(candidate_emails - existing)
        logger.info(
            "  Missing from orcid_record: %d / %d candidate emails",
            len(missing),
            len(candidate_emails),
        )
        return missing

    def insert_placeholders(self, emails: List[str], apply: bool) -> int:
        """Insert a minimal OrcidRecord for each email.  Returns count inserted.

        When ``apply`` is False this is a dry-run that only logs what *would*
        happen; no writes are performed.
        """
        if not emails:
            logger.info("  No emails to insert.")
            return 0

        logger.info(
            "  %s placeholders for %d emails ...",
            "Inserting" if apply else "DRY-RUN — would insert",
            len(emails),
        )

        now = datetime.now(timezone.utc)
        batch: List[InsertOne] = []
        inserted = 0

        for i, email in enumerate(emails):
            if i > 0 and i % PROGRESS_EVERY == 0:
                logger.info("    ... %d / %d processed", i, len(emails))

            doc = {
                "email": email,
                "created": now,
                "modified": now,
            }

            if apply:
                batch.append(InsertOne(doc))
                if len(batch) >= BATCH_SIZE:
                    inserted += self._flush(batch)
                    batch = []

        if apply and batch:
            inserted += self._flush(batch)

        if apply:
            logger.info("  Inserted %d orcid_record documents", inserted)
        else:
            logger.info("  (dry-run — %d would be inserted)", len(emails))

        return inserted

    def _flush(self, batch: List[InsertOne]) -> int:
        """Execute a batch of inserts; return count of documents inserted."""
        if not batch:
            return 0
        try:
            result = self.collection_orcid_record.bulk_write(batch, ordered=False)
            return result.inserted_count
        except BulkWriteError as bwe:
            # Some inserts may have succeeded; log details and continue.
            detail = getattr(bwe, "details", bwe)
            inserted_ok = detail.get("nInserted", 0) if isinstance(detail, dict) else 0
            write_errors = detail.get("writeErrors", []) if isinstance(detail, dict) else []
            logger.warning(
                "  Bulk insert: %d succeeded, %d errors", inserted_ok, len(write_errors)
            )
            for err in write_errors[:5]:  # log first 5 only
                logger.warning("    %s", err.get("errmsg", str(err)))
            return inserted_ok
        except OperationFailure as e:
            logger.error("  Bulk insert failed: %s", e)
            raise


def parse_arguments():
    parser = argparse.ArgumentParser(
        description="Backfill missing orcid_record placeholder documents",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Scan every assertion email and insert missing orcid_record entries
  python backfill_orcid_record.py

  # Handle a single email only
  python backfill_orcid_record.py --source-email=researcher@example.org

Environment Variables:
  SPRING_DATA_MONGODB_URI - MongoDB connection string
        """,
    )

    parser.add_argument(
        "--source-email",
        required=False,
        default=None,
        help="Optional single email to process. When given only that email "
        "is checked / inserted; otherwise every distinct email in assertion "
        "and send_notifications_request is considered.",
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
    logger.info("Backfill missing orcid_record placeholders")
    logger.info("=" * 80)
    logger.info("Database:      %s", database)
    logger.info("Collections:   assertion, send_notifications_request → orcid_record")
    logger.info(
        "MongoDB URI:   %s...",
        mongo_uri[:20] if len(mongo_uri) > 20 else mongo_uri,
    )
    logger.info(
        "Scope:         %s",
        f"single email ({source_email})" if source_email else "ALL assertion emails",
    )
    logger.info("=" * 80 + "\n")

    connection = MongoDBConnection(mongo_uri, database)

    try:
        if not connection.connect():
            logger.error("Failed to connect to MongoDB. Exiting.")
            return 1

        backfiller = OrcidRecordBackfiller(connection)

        logger.info("\n" + "=" * 80)
        logger.info("PLANNING: discover missing emails (no writes)")
        logger.info("=" * 80)

        missing = backfiller.discover_missing(source_email)

        if not missing:
            logger.info(
                "\n All candidate emails already have an orcid_record entry. Nothing to do."
            )
            return 0

        logger.info("\n  Summary: %d email(s) missing an orcid_record", len(missing))
        if len(missing) <= 20:
            for e in missing:
                logger.info("    - %s", e)
        else:
            for e in missing[:10]:
                logger.info("    - %s", e)
            logger.info("    ... and %d more", len(missing) - 10)

        logger.info("\n" + "=" * 80)
        logger.info("  WARNING: This will INSERT new documents!")
        logger.info("  Collection:     orcid_record")
        logger.info("  Documents:      %d new placeholder(s)", len(missing))
        logger.info("  Each contains:  email, created, modified (nothing else)")
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
        logger.info("EXECUTING — inserting placeholder orcid_record documents")
        logger.info("=" * 80)

        inserted = backfiller.insert_placeholders(missing, apply=True)

        logger.info("\n" + "=" * 80)
        logger.info("VERIFYING — re-scan for still-missing emails")
        logger.info("=" * 80)

        still_missing = backfiller.discover_missing(source_email)

        if still_missing:
            logger.warning(
                " Verification: %d email(s) still missing (expected 0)", len(still_missing)
            )
            for e in still_missing[:10]:
                logger.warning("    - %s", e)
            return 1

        logger.info(" Verification passed: 0 missing emails remain")
        logger.info("\n" + "=" * 80)
        logger.info("Script completed successfully — %d placeholder(s) inserted", inserted)
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
