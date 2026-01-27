#!/usr/bin/env python3
"""
Script to add any missing ORCID iDs from affiliations.

This script adds any missing ORCID iDs from affiliations that have status 'IN_ORCID' a valid access token

Related to: https://app.clickup.com/t/9014437828/PD-3780

Usage:
    fix_short_sf_ids.py should be executed first
    python add_missing_ORCID_iD_from_affiliations.py
"""

import argparse
import sys
from typing import List, Dict, Any
from pymongo.errors import OperationFailure

# Import shared modules
from logger_config import setup_logger
from db_connection import MongoDBConnection
from config import Config

# Set up logging
logger = setup_logger(__name__, log_file='add-missing-ORCID-iD-from-affiliations.log')


class AddMissingORCIDiDFROMAffiliations:

    def __init__(self, connection: MongoDBConnection, collection_assertion: str, collection_orcid_record: str):
        self.connection = connection
        self.collection_assertion = connection.get_collection(collection_assertion)
        self.collection_orcid_record = connection.get_collection(collection_orcid_record)

    def find_problematic_assertions(self) -> List[Dict[str, Any]]:
        """
        Find assertions without Orcid iD.

        Returns:
            List of problematic assertions documents
        """
        query = {
            'orcid_id': {
                '$exists': False
            },
            'put_code': {
                '$exists': True,
                '$ne': ''
            }
        }

        try:
            logger.info("Searching for problematic assertions...")
            assertions = list(self.collection_assertion.find(query))
            logger.info(f"Found {len(assertions)} assertions to fix")
            return assertions
        except OperationFailure as e:
            logger.error(f"Failed to query affiliations: {e}")
            return []
        except Exception as e:
            logger.error(f"Unexpected error during query: {e}")
            return []

    def print_report(self, assertions: List[Dict[str, Any]]):
        if not assertions:
            logger.info("No problematic assertions found")
            return

        logger.info("\n" + "="*80)
        logger.info("PROBLEMATIC ASSERTIONS REPORT")
        logger.info("="*80)

        for i, rec in enumerate(assertions, 1):
            logger.info(f" _id: {rec.get('_id')}, Email: {rec.get('email')}")

        logger.info("\n" + "="*80)

    def find_assertions(self, assertions: List[Dict[str, Any]]) -> int:
        """
        Fix the assertions without Orcid iD.

        Returns:
            Number of assertions successfully updated
        """
        if not assertions:
            logger.info("No orcid records to fix")
            return 0

        logger.info(f"\n Applying fixes to {len(assertions)} assertions...")

        modified_count = 0

        orcid_records = list(self.collection_orcid_record.find({}))

        try:

            for assertion in assertions:
                assertion_email = assertion.get('email')
                assertion_salesforce_id = assertion.get('salesforce_id')

                for orcid_record in orcid_records:
                    orcid_record_email = orcid_record.get('email')
                    if assertion_email == orcid_record_email:
                        same_salesforce_id = False
                        tokens = orcid_record.get("tokens", [])

                        for t in tokens:
                            salesforce_id = t.get("salesforce_id")

                            if assertion_salesforce_id == salesforce_id:
                                same_salesforce_id = True
                                result = self.collection_assertion.update_one(
                                    {"_id": assertion["_id"]},
                                    {
                                        "$set": {
                                            "orcid_id": orcid_record.get("orcid")
                                        }
                                    }
                                )
                                modified_count += result.modified_count

                                logger.info(
                                    f"Assertion updated id:={assertion["_id"]}, orcid={orcid_record.get("orcid")}"
                                )

                        if not same_salesforce_id:
                            logger.info(
                                f"Same assertion_email={assertion_email} and orcid_record_email={orcid_record_email} but not assertion_salesforce_id={assertion_salesforce_id}"
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
        remaining = self.find_problematic_assertions()

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

        fixer = AddMissingORCIDiDFROMAffiliations(connection, collection_assertion, collection_orcid_record)

        assertions = fixer.find_problematic_assertions()

        fixer.print_report(assertions)

        if not assertions:
            logger.info("\n No fixes needed. All orcid records are correct.")
            return 0

        logger.info("\n" + "="*80)
        logger.info("  WARNING: This will modify the database!")
        logger.info(f"  {len(assertions)} orcid records will be updated")
        logger.info("="*80)

        try:
            response = input("\nDo you want to proceed? (yes/no): ").strip().lower()
            if response not in ['yes', 'y']:
                logger.info("\n Operation cancelled by user")
                return 0
        except (KeyboardInterrupt, EOFError):
            logger.info("\n\n Operation cancelled by user")
            return 1

        updated_count = fixer.find_assertions(assertions)

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
