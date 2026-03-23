#!/usr/bin/env python3
"""
Script to add any missing ORCID iDs from affiliations.

This script adds any missing ORCID iDs from affiliations that have status 'IN_ORCID' a valid access token

Related to: https://app.clickup.com/t/9014437828/PD-3780

Usage:
    python add_missing_ORCID_iD_from_affiliations.py
"""

import argparse
import sys
from pathlib import Path
from typing import List, Dict, Any
from pymongo.errors import OperationFailure

CURRENT_DIR = Path(__file__).resolve().parent
UTILS_DIR = CURRENT_DIR.parent / "utils"

if str(UTILS_DIR) not in sys.path:
    sys.path.insert(0, str(UTILS_DIR))

# Import shared modules
from logger_config import setup_logger
from db_connection import MongoDBConnection
from config import Config

# Set up logging
logger = setup_logger(__name__, log_file='add-missing-ORCID-iD-from-affiliations.log')


class AddMissingORCIDiDFROMAffiliations:

    def __init__(self, connection: MongoDBConnection, collection_assertion: str, collection_orcid_record: str, full_report: bool):
        self.connection = connection
        self.collection_assertion = connection.get_collection(collection_assertion)
        self.collection_orcid_record = connection.get_collection(collection_orcid_record)
        self.full_report = full_report

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

    def print_report(self, assertions: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        if not assertions:
            logger.info("No problematic assertions found")
            return []

        logger.info("\n" + "="*80)
        logger.info("PROBLEMATIC ASSERTIONS REPORT")
        logger.info("="*80)

        assertions_to_modify: list[dict[str, str]] = []
        modified_count = 0
        no_match = 0

        try:
            for assertion in assertions:
                assertion_salesforce_id = assertion.get('salesforce_id')
                assertion_email = assertion.get('email')
                if self.full_report:
                    logger.info(
                        f"  Assertion with Salesforce id {assertion_salesforce_id} and email {assertion_email}"
                    )

                orcid_record = self.collection_orcid_record.find_one({
                    "email": assertion_email,
                    "tokens": {
                        "$exists": True,
                        "$ne": [],
                        "$elemMatch": {
                            "salesforce_id": assertion_salesforce_id,
                            "revoked_date": {"$exists": False}
                        }
                    }
                })

                if orcid_record:
                    orcid = orcid_record.get('orcid')
                    assertions_to_modify.append({
                        "assertion_id": assertion.get('_id'),
                        "assertion_email": assertion_email,
                        "orcid": orcid
                    })
                    modified_count += 1
                    logger.info(f"    Orcid {orcid} will be added to assertion with Salesforce id {assertion_salesforce_id} and email {assertion_email}")
                else:
                    no_match += 1
                    if self.full_report:
                        logger.info("    No matching ORCID record found")

            logger.info("\n" + "="*80)
            logger.info(f"  It will modify {modified_count} assertions")
            if no_match > 0:
                logger.info(f"  No match for {no_match} assertions")
            logger.info("\n" + "=" * 80)

            return assertions_to_modify

        except OperationFailure as e:
            logger.error(f"Failed to build report: {e}")
            return []
        except Exception as e:
            logger.error(f"Unexpected error during report generation: {e}")
            return []


    def fix_assertions(self, assertions: List[Dict[str, Any]]) -> int:
        """
        Fix the assertions without Orcid iD.

        Returns:
            Number of assertions successfully updated
        """
        if not assertions:
            logger.info("No assertions to fix")
            return 0

        logger.info(f"\n Applying fixes to {len(assertions)} assertions...")

        modified_count = 0

        try:

            for assertion in assertions:
                orcid = assertion["orcid"]
                result = self.collection_assertion.update_one(
                    {"_id": assertion["assertion_id"]},
                    {
                        "$set": {
                            "orcid_id": orcid
                        }
                    }
                )
                modified_count += result.modified_count

                logger.info(
                    f"Assertion updated Email:={assertion['assertion_email']}, orcid={orcid}"
                )

            logger.info(f" Successfully updated {modified_count} assertions")

            return modified_count

        except OperationFailure as e:
            logger.error(f" Failed to update assertions: {e}")
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
            logger.warning(f" {len(remaining)} problematic salesforce ids still exist")
            return False


def parse_arguments():
    parser = argparse.ArgumentParser(
        description='Add Missing Orcid from affiliations',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Interactive mode
  # find_short_sf_ids.py should be executed first
  python add_missing_ORCID_iD_from_affiliations.py

Environment Variables:
  MONGO_URI or MONGO_DB       - MongoDB connection string
        """
    )

    parser.add_argument('--mongo-uri', help='MongoDB URI (overrides env)')
    parser.add_argument(
        "--full_report",
        action="store_true",
        help="Print a full, verbose report"
    )
    return parser.parse_args()


def main():
    args = parse_arguments()

    config = Config()

    mongo_uri = args.mongo_uri or config.mongo_uri
    database = 'assertionservice'
    collection_assertion = 'assertion'
    collection_orcid_record = 'orcid_record'
    full_report = args.full_report

    logger.info("="*80)
    logger.info("Add missing ORCID from affiliations")
    logger.info("="*80)
    logger.info(f"Database: {database}")
    logger.info("Collections: assertion, orcid_record")
    logger.info(
        f"MongoDB URI: {mongo_uri[:20]}..." if len(mongo_uri) > 20 else f"MongoDB URI: {mongo_uri}"
    )
    logger.info("="*80 + "\n")

    connection = MongoDBConnection(mongo_uri, database)

    try:
        if not connection.connect():
            logger.error("Failed to connect to MongoDB. Exiting.")
            return 1

        fixer = AddMissingORCIDiDFROMAffiliations(
            connection,
            collection_assertion,
            collection_orcid_record,
            full_report
        )

        assertions = fixer.find_problematic_assertions()

        if not assertions:
            logger.info("\n No fixes needed. All assertions are correct.")
            return 0

        assertions_to_modify = fixer.print_report(assertions)

        if not assertions_to_modify:
            return 0

        logger.info("\n" + "=" * 80)
        logger.info("  WARNING: This will modify the database!")
        logger.info(f"  {len(assertions_to_modify)} assertions will be updated")
        logger.info("="*80)

        try:
            response = input("\nDo you want to proceed? (yes/no): ").strip().lower()
            if response not in ['yes', 'y']:
                logger.info("\n Operation cancelled by user")
                return 0
        except (KeyboardInterrupt, EOFError):
            logger.info("\n\n Operation cancelled by user")
            return 1

        updated_count = fixer.fix_assertions(assertions_to_modify)

        if updated_count > 0:
            if not fixer.verify_fixes():
                logger.warning(" Some assertions may still need attention")
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
