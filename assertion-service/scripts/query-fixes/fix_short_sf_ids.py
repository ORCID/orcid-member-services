#!/usr/bin/env python3
"""
Fix short Salesforce IDs in ORCID record tokens.

This script scans the `orcid_record` collection and corrects
15-character Salesforce IDs found inside the `tokens` array
by replacing them with their corresponding 18-character Salesforce IDs.

Only token entries with 15-character Salesforce IDs are updated.

Related to: https://app.clickup.com/t/9014437828/PD-3780

Usage:
    python fix_short_sf_ids.py
"""

import argparse
import sys
from typing import List, Dict, Any
from pymongo.errors import OperationFailure

from logger_config import setup_logger
from db_connection import MongoDBConnection
from config import Config

logger = setup_logger(__name__, log_file='fix-short-sf-ids.log')


class FixShortSfIds:

    def __init__(self, connection: MongoDBConnection, collection_assertion: str, collection_orcid_record: str):
        self.connection = connection
        self.collection_assertion = connection.get_collection(collection_assertion)
        self.collection_orcid_record = connection.get_collection(collection_orcid_record)

    def find_problematic_salesforce_ids(self) -> List[Dict[str, Any]]:
        """
        Find salesforce ids that are 15 characters long.

        Returns:
            List of problematic orcid_record documents
        """
        query = {
            '$expr': {
                '$gt': [
                    {
                        '$size': {
                            '$filter': {
                                'input': '$tokens',
                                'as': 'token',
                                'cond': {
                                    '$and': [
                                        {
                                            '$eq': [
                                                {
                                                    '$type': '$$token.salesforce_id'
                                                }, 'string'
                                            ]
                                        }, {
                                            '$eq': [
                                                {
                                                    '$strLenCP': '$$token.salesforce_id'
                                                }, 15
                                            ]
                                        }
                                    ]
                                }
                            }
                        }
                    }, 0
                ]
            }
        }

        try:
            logger.info("Searching for problematic orcid_records...")
            orcid_records = list(self.collection_orcid_record.find(query))
            logger.info(f"Found {len(orcid_records)} orcid_records to fix")
            return orcid_records
        except OperationFailure as e:
            logger.error(f"Failed to query affiliations: {e}")
            return []
        except Exception as e:
            logger.error(f"Unexpected error during query: {e}")
            return []

    def print_report(self, orcid_records: List[Dict[str, Any]]):
        if not orcid_records:
            logger.info("No problematic orcid records found")
            return

        logger.info("\n" + "="*80)
        logger.info("PROBLEMATIC ORCID_RECORDS REPORT")
        logger.info("="*80)

        for i, rec in enumerate(orcid_records, 1):
            tokens = rec.get("tokens", [])

            for t in tokens:
                if (
                    isinstance(t.get("salesforce_id"), str)
                    and len(t["salesforce_id"]) == 15
                ):
                    logger.info(f" Email: {rec.get('email')}, Token with 15-char Salesforce ID: {t.get("salesforce_id")}")


        logger.info("\n" + "="*80)

    def fix_orcid_records(self, orcid_records: List[Dict[str, Any]]) -> int:
        """
        Fix the sf of problematic orcid_records.

        Returns:
            Number of sf successfully updated
        """
        if not orcid_records:
            logger.info("No orcid records to fix")
            return 0

        logger.info(f"\n Applying fixes to {len(orcid_records)} orcid records...")

        modified_count = 0

        full_salesforce_ids = set(
            self.collection_assertion.distinct(
                "salesforce_id",
                {
                    "$expr": {
                        "$and": [
                            { "$eq": [{ "$type": "$salesforce_id" }, "string"] },
                            { "$eq": [{ "$strLenCP": "$salesforce_id" }, 18] }
                        ]
                    }
                }
            )
        )

        try:

            for orcid_record in orcid_records:
                tokens = orcid_record.get("tokens", [])

                for t in tokens:
                    if (
                        isinstance(t.get("salesforce_id"), str)
                        and len(t["salesforce_id"]) == 15
                    ):
                        salesforce_id = t.get("salesforce_id")

                        for full_salesforce_id in full_salesforce_ids:
                            if full_salesforce_id.startswith(salesforce_id):
                                result = self.collection_orcid_record.update_one(
                                    {"_id": orcid_record["_id"]},
                                    {
                                        "$set": {
                                            "tokens.$[token].salesforce_id": full_salesforce_id
                                        }
                                    },
                                    array_filters=[
                                        {"token.salesforce_id": salesforce_id}
                                    ]
                                )
                                modified_count += result.modified_count
                                logger.info(
                                    f"Updated token: full salesforce id={full_salesforce_id}, short salesforce id={salesforce_id}"
                                )

            logger.info(f" Successfully updated {modified_count} orcid records")

            return modified_count

        except OperationFailure as e:
            logger.error(f" Failed to update orcid records: {e}")
            return 0
        except Exception as e:
            logger.error(f" Unexpected error during update: {e}")
            return 0

    def verify_fixes(self) -> bool:
        logger.info("\n Verifying fixes...")
        remaining = self.find_problematic_salesforce_ids()

        if not remaining:
            logger.info(" Verification passed: No problematic salesforce ids found")
            return True
        else:
            logger.warning(f" Verification failed: {len(remaining)} problematic salesforce ids still exist")
            return False


def parse_arguments():
    parser = argparse.ArgumentParser(
        description='Fix ORCID records salesforce ids',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Interactive mode
  # fix_short_sf_ids.py should be executed first
  python add_missing_ORCID_iD_from_affiliations.py

Environment Variables:
  MONGO_URI or MONGO_DB       - MongoDB connection string
  MONGO_DATABASE or DATABASE  - Database name (default: assertionservice)
  MONGO_COLLECTION or COLLECTION - Collection name (default: assertion)
        """
    )

    parser.add_argument('--mongo-uri', help='MongoDB URI (overrides env)')
    parser.add_argument('--database', help='Database name (overrides env)')
    parser.add_argument('--collection', help='Collection name (overrides env)')

    return parser.parse_args()


def main():
    args = parse_arguments()

    config = Config()

    mongo_uri = args.mongo_uri or config.mongo_uri
    database = args.database or config.mongo_database

    logger.info("="*80)
    logger.info("Add missing ORCID iD and correct SF iD")
    logger.info("="*80)
    logger.info(f"Database: {database}")
    logger.info(f"Collections: assertion, orcid_record")
    logger.info(f"MongoDB URI: {mongo_uri[:20]}..." if len(mongo_uri) > 20 else f"MongoDB URI: {mongo_uri}")
    logger.info("="*80 + "\n")

    connection = MongoDBConnection(mongo_uri, database)
    collection_assertion = 'assertion'
    collection_orcid_record = 'orcid_record'

    try:
        if not connection.connect():
            logger.error("Failed to connect to MongoDB. Exiting.")
            return 1

        fixer = FixShortSfIds(connection, collection_assertion, collection_orcid_record)

        orcid_records = fixer.find_problematic_salesforce_ids()

        fixer.print_report(orcid_records)

        if not orcid_records:
            logger.info("\n No fixes needed. All orcid records are correct.")
            return 0

        logger.info("\n" + "="*80)
        logger.info("  WARNING: This will modify the database!")
        logger.info(f"  {len(orcid_records)} orcid records will be updated")
        logger.info("="*80)

        try:
            response = input("\nDo you want to proceed? (yes/no): ").strip().lower()
            if response not in ['yes', 'y']:
                logger.info("\n Operation cancelled by user")
                return 0
        except (KeyboardInterrupt, EOFError):
            logger.info("\n\n Operation cancelled by user")
            return 1

        updated_count = fixer.fix_orcid_records(orcid_records)

        if updated_count > 0:
            if not fixer.verify_fixes():
                logger.warning("\n Some orcid records may still need attention")
                return 1

        logger.info("\n" + "="*80)
        logger.info("Script completed successfully")
        logger.info("="*80)
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
