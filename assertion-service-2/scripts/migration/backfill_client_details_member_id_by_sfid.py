#!/usr/bin/env python3
"""
Step 1 of 2: export member-portal members (memberservice.member) to a JSON file that
api-credentials-service's backfill_client_details_member_id_by_sfid.py consumes to
backfill client_details.member_id by Salesforce ID.

This half exists because the api-credentials-service container cannot reach MongoDB.
It is READ-ONLY: nothing in MongoDB is modified.

Output JSON:
    {
      "source": {"mongo_database": "memberservice", "collection": "member"},
      "stats": {"members_total": N, "members_exported": N, "members_skipped": N},
      "members": [{"member_id": "<24-hex>", "salesforce_id": "<18 chars>", "member_name": "..."}]
    }

Next step: copy the file into the api-credentials-service container and run
    python3 /scripts/migration/backfill_client_details_member_id_by_sfid.py --members-file <file>

Related to: PD-12890

Usage:
    python backfill_client_details_member_id_by_sfid.py
    python backfill_client_details_member_id_by_sfid.py --output /tmp/members_by_sfid.json
"""

import argparse
import json
import os
import sys
from pathlib import Path
from typing import List, Tuple

from pymongo.errors import OperationFailure

CURRENT_DIR = Path(__file__).resolve().parent
UTILS_DIR = CURRENT_DIR.parent / "utils"

if str(UTILS_DIR) not in sys.path:
    sys.path.insert(0, str(UTILS_DIR))

from logger_config import setup_logger
from db_connection import MongoDBConnection
from config import Config

logger = setup_logger(__name__, log_file='export-members-by-sfid.log')

MONGO_DATABASE = 'memberservice'
MONGO_COLLECTION = 'member'


def fetch_members(connection: MongoDBConnection) -> Tuple[List[dict], int, int]:
    """Return (exported_members, total_read, skipped) in Mongo natural order."""
    collection = connection.get_collection(MONGO_COLLECTION)
    try:
        raw_members = list(collection.find({}, {'_id': 1, 'salesforce_id': 1, 'client_name': 1}))
    except OperationFailure as e:
        logger.error(f"Failed to query members: {e}")
        raise

    exported: List[dict] = []
    skipped = 0
    for member in raw_members:
        raw_id = member.get('_id')
        sf = member.get('salesforce_id')
        sf = sf.strip() if isinstance(sf, str) else sf
        if not raw_id or not sf:
            logger.warning(
                " Skipping member with missing _id or salesforce_id (_id=%s, salesforce_id=%s)",
                raw_id, sf,
            )
            skipped += 1
            continue
        exported.append({
            'member_id': str(raw_id),
            'salesforce_id': sf,
            'member_name': member.get('client_name'),
        })
    return exported, len(raw_members), skipped


def write_json(payload: dict, output_path: Path) -> None:
    """Write atomically: tmp file then rename, so a partial file is never left behind."""
    output_path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = output_path.with_suffix(output_path.suffix + '.tmp')
    with tmp_path.open('w', encoding='utf-8') as fh:
        json.dump(payload, fh, ensure_ascii=False, indent=2)
        fh.write('\n')
    os.replace(tmp_path, output_path)


def parse_arguments():
    parser = argparse.ArgumentParser(
        description='Export memberservice.member (member_id, salesforce_id, name) to JSON '
                    'for the api-credentials-service client_details.member_id backfill.',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Default output: scripts/migration/logs/members_by_sfid.json
  python backfill_client_details_member_id_by_sfid.py

  # Explicit output path
  python backfill_client_details_member_id_by_sfid.py --output /tmp/members_by_sfid.json

Environment Variables:
  SPRING_DATA_MONGODB_URI  - MongoDB connection string (member portal)
        """
    )
    parser.add_argument(
        '--output',
        default=None,
        help='Path of the JSON file to write. '
             'Default: <script dir>/logs/members_by_sfid.json (overwritten each run)'
    )
    return parser.parse_args()


def main():
    args = parse_arguments()
    config = Config()

    if args.output:
        output_path = Path(args.output).resolve()
    else:
        output_path = CURRENT_DIR / 'logs' / 'members_by_sfid.json'

    logger.info("=" * 80)
    logger.info("Export members for client_details.member_id backfill (step 1 of 2)")
    logger.info("=" * 80)
    logger.info("Mongo database: %s (collection: %s)", MONGO_DATABASE, MONGO_COLLECTION)
    logger.info("Output file: %s", output_path)
    logger.info("Mode: READ-ONLY (MongoDB is not modified)")
    logger.info("=" * 80 + "\n")

    connection = MongoDBConnection(config.mongo_uri, MONGO_DATABASE)
    try:
        if not connection.connect():
            logger.error("Failed to connect to memberservice MongoDB. Exiting.")
            return 1

        members, total, skipped = fetch_members(connection)

        sf15_seen = set()
        duplicates = set()
        for m in members:
            sf15 = m['salesforce_id'][:15]
            if sf15 in sf15_seen:
                duplicates.add(sf15)
            sf15_seen.add(sf15)
        if duplicates:
            logger.warning(
                " %d 15-char salesforce_id prefix(es) shared by multiple members "
                "(the import step keeps the last one): %s",
                len(duplicates), ", ".join(sorted(duplicates)),
            )

        payload = {
            'source': {'mongo_database': MONGO_DATABASE, 'collection': MONGO_COLLECTION},
            'stats': {
                'members_total': total,
                'members_exported': len(members),
                'members_skipped': skipped,
            },
            'members': members,
        }
        write_json(payload, output_path)

        logger.info("Members read: %d, exported: %d, skipped: %d", total, len(members), skipped)
        logger.info("\n JSON written: %s", output_path)
        logger.info(" Next: copy this file into the api-credentials-service container and run")
        logger.info("   python3 /scripts/migration/backfill_client_details_member_id_by_sfid.py "
                    "--members-file <path-in-container> --dry-run")
        return 0

    except KeyboardInterrupt:
        logger.info("\n\n Operation cancelled by user (Ctrl+C)")
        return 1
    except Exception as e:
        logger.error(f"\n Unexpected error: {e}", exc_info=True)
        return 1
    finally:
        connection.disconnect()


if __name__ == '__main__':
    sys.exit(main())
