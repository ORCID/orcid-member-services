#!/usr/bin/env python3
"""
Script to correct the status of affiliations with no put code.

This script identifies affiliations that have 'added_to_orcid' field but no 'put_code'
field and resets them to 'pending' so they can be processed correctly by the next cron job.

Related to: https://app.clickup.com/t/9014437828/PD-3779

Usage:
    python fix_affiliation_status.py
"""

import argparse
import sys
from typing import List, Dict, Any
from pymongo.errors import OperationFailure

from logger_config import setup_logger
from db_connection import MongoDBConnection
from config import Config

logger = setup_logger(__name__, log_file='affiliation_fix.log')


class AffiliationStatusFixer:

    TARGET_STATUS = 'pending'

    def __init__(self, connection: MongoDBConnection, collection_name: str):
        self.connection = connection
        self.collection = connection.get_collection(collection_name)

    def find_problematic_affiliations(self) -> List[Dict[str, Any]]:
        """
        Find affiliations with added_to_orcid but no put_code.

        Returns:
            List of problematic affiliation documents
        """
        query = {
            'added_to_orcid': {'$exists': True},
            'put_code': {'$exists': True, '$ne': ""}
        }

        try:
            logger.info("Searching for problematic affiliations...")
            affiliations = list(self.collection.find(query))
            logger.info(f"Found {len(affiliations)} affiliations to fix")
            return affiliations
        except OperationFailure as e:
            logger.error(f"Failed to query affiliations: {e}")
            return []
        except Exception as e:
            logger.error(f"Unexpected error during query: {e}")
            return []

    def print_report(self, affiliations: List[Dict[str, Any]]):
        if not affiliations:
            logger.info("No problematic affiliations found")
            return

        logger.info("\n" + "="*80)
        logger.info("PROBLEMATIC AFFILIATIONS REPORT")
        logger.info("="*80)

        status_counts = {}
        for aff in affiliations:
            status = aff.get('status', 'Unknown')
            status_counts[status] = status_counts.get(status, 0) + 1

        logger.info(f"\nTotal affiliations to fix: {len(affiliations)}")
        logger.info("\nBreakdown by status:")
        for status, count in status_counts.items():
            logger.info(f"  - {status}: {count}")

        logger.info("\nSample affiliations (first 5):")
        for i, aff in enumerate(affiliations[:5], 1):
            logger.info(f"\n  {i}. ID: {aff.get('_id')}")
            logger.info(f"     Status: {aff.get('status', 'Unknown')}")
            logger.info(f"     Put Code: {'(missing)' if 'put_code' not in aff else aff.get('put_code')}")
            logger.info(f"     Added to ORCID: {aff.get('added_to_orcid', 'Unknown')}")
            logger.info(f"     ORCID: {aff.get('orcidId', 'Unknown')}")
            logger.info(f"     Organization: {aff.get('organizationName', 'Unknown')}")

        logger.info("\n" + "="*80)

    def fix_affiliations(self, affiliations: List[Dict[str, Any]]) -> int:
        """
        Fix the status of problematic affiliations.

        Returns:
            Number of affiliations successfully updated
        """
        if not affiliations:
            logger.info("No affiliations to fix")
            return 0

        logger.info(f"\n Applying fixes to {len(affiliations)} affiliations...")

        affiliation_ids = [aff['_id'] for aff in affiliations]

        try:
            result = self.collection.update_many(
                {'_id': {'$in': affiliation_ids}},
                {'$set': {'status': self.TARGET_STATUS}}
            )

            logger.info(f" Successfully updated {result.modified_count} affiliations")
            logger.info(f"   Matched: {result.matched_count}")
            logger.info(f"   Modified: {result.modified_count}")

            return result.modified_count

        except OperationFailure as e:
            logger.error(f" Failed to update affiliations: {e}")
            return 0
        except Exception as e:
            logger.error(f" Unexpected error during update: {e}")
            return 0

    def verify_fixes(self) -> bool:
        logger.info("\n Verifying fixes...")
        remaining = self.find_problematic_affiliations()

        if not remaining:
            logger.info(" Verification passed: No problematic affiliations found")
            return True
        else:
            logger.warning(f" Verification failed: {len(remaining)} problematic affiliations still exist")
            return False


def parse_arguments():
    parser = argparse.ArgumentParser(
        description='Fix ORCID affiliation statuses with no put code',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Interactive mode
  python fix_affiliation_status.py

Environment Variables:
  MONGO_URI or MONGO_DB       - MongoDB connection string
  MONGO_DATABASE or DATABASE  - Database name (default: assertionservice)
  MONGO_COLLECTION or COLLECTION - Collection name (default: assertion)
        """
    )

    parser.add_argument('--mongo-uri', help='MongoDB URI (overrides env)')

    return parser.parse_args()


def main():
    args = parse_arguments()

    config = Config()

    mongo_uri = args.mongo_uri or config.mongo_uri
    database_assertionservice = 'assertionservice'

    logger.info("="*80)
    logger.info("ORCID Affiliation Status Fix Script")
    logger.info("="*80)
    logger.info(f"Database: {database_assertionservice}")
    logger.info(f"Collection: assertion")
    logger.info(f"MongoDB URI: {mongo_uri[:20]}..." if len(mongo_uri) > 20 else f"MongoDB URI: {mongo_uri}")
    logger.info("="*80 + "\n")

    connection = MongoDBConnection(mongo_uri, database_assertionservice)

    try:
        if not connection.connect():
            logger.error("Failed to connect to MongoDB. Exiting.")
            return 1

        fixer = AffiliationStatusFixer(connection, 'assertion')

        affiliations = fixer.find_problematic_affiliations()

        fixer.print_report(affiliations)

        if not affiliations:
            logger.info("\n No fixes needed. All affiliations are correct.")
            return 0

        logger.info("\n" + "="*80)
        logger.info("  WARNING: This will modify the database!")
        logger.info(f"  {len(affiliations)} affiliations will be updated to status 'pending'")
        logger.info("="*80)

        try:
            response = input("\nDo you want to proceed? (yes/no): ").strip().lower()
            if response not in ['yes', 'y']:
                logger.info("\n Operation cancelled by user")
                return 0
        except (KeyboardInterrupt, EOFError):
            logger.info("\n\n Operation cancelled by user")
            return 1

        updated_count = fixer.fix_affiliations(affiliations)

        if updated_count > 0:
            if not fixer.verify_fixes():
                logger.warning("\n Some affiliations may still need attention")
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
