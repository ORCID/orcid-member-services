#!/usr/bin/env python3
"""
Find short Salesforce IDs in ORCID record tokens.

This script scans the `orcid_record`, `assertion`, `jhi_users` and `send_notifications_request` collections
looking for 15-character Salesforce IDs

Related to: https://app.clickup.com/t/9014437828/PD-3780

Usage:
    python find_short_sf_ids.py
"""

import argparse
import sys
from typing import List, Dict, Any
from pymongo.errors import OperationFailure

from logger_config import setup_logger
from db_connection import MongoDBConnection
from config import Config

logger = setup_logger(__name__, log_file='fix-short-sf-ids.log')

query_short_sf_ids = {
    "salesforce_id": {"$exists": True, "$ne": None},
    "$expr": {"$lt": [{"$strLenCP": "$salesforce_id"}, 18]}
}

class FindFindShortSfIdsAssertion:

    def __init__(self, connection_to_db: MongoDBConnection):
        self.connection_to_db = connection_to_db
        self.collection_assertion = connection_to_db.get_collection('assertion')
        self.collection_orcid_record = connection_to_db.get_collection('orcid_record')
        self.collection_send_notifications_request = connection_to_db.get_collection('send_notifications_request')

    def find_problematic_assertions(self) -> List[Dict[str, Any]]:
        """
        Find assertions.

        Returns:
            List of problematic assertions documents
        """

        try:
            logger.info("Searching for assertions...")

            assertions = list(self.collection_assertion.find(query_short_sf_ids))
            logger.info(f"Found {len(assertions)} assertions to fix")
            return assertions
        except OperationFailure as e:
            logger.error(f"Failed to query affiliations: {e}")
            return []
        except Exception as e:
            logger.error(f"Unexpected error during query: {e}")
            return []

    def find_problematic_orcid_records(self) -> List[Dict[str, Any]]:
        """
        Find orcid records.

        Returns:
            List of problematic orcid records documents
        """

        try:
            logger.info("Searching for orcid records...")

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
                                                '$lt': [
                                                    {
                                                        '$strLenCP': '$$token.salesforce_id'
                                                    }, 18
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

            orcid_records = list(self.collection_orcid_record.find(query))
            logger.info(f"Found {len(orcid_records)} orcid records to fix")
            return orcid_records
        except OperationFailure as e:
            logger.error(f"Failed to query affiliations: {e}")
            return []
        except Exception as e:
            logger.error(f"Unexpected error during query: {e}")
            return []

    def find_problematic_send_notifications_request(self) -> List[Dict[str, Any]]:
        """
        Find send_notifications_request.

        Returns:
            List of problematic send_notifications_request documents
        """

        try:
            logger.info("Searching for notifications...")

            assertions = list(self.collection_send_notifications_request.find(query_short_sf_ids))
            logger.info(f"Found {len(assertions)} send_notifications_request to fix")
            return assertions
        except OperationFailure as e:
            logger.error(f"Failed to query send_notifications_request: {e}")
            return []
        except Exception as e:
            logger.error(f"Unexpected error during query: {e}")
            return []


    def print_assertions_report(self, assertions: List[Dict[str, Any]]):
        if not assertions:
            logger.info("No problematic assertions found")
            return

        logger.info("\n" + "="*80)
        logger.info("PROBLEMATIC ASSERTIONS REPORT")
        logger.info("="*80)

        for i, rec in enumerate(assertions, 1):
            logger.info(f" _id: {rec.get('_id')}, Salesforce Id: {rec.get('salesforce_id')}")

        logger.info("\n" + "="*80)

    def print_orcid_records_report(self, orcid_records: List[Dict[str, Any]]):
        if not orcid_records:
            logger.info("No problematic orcid records found")
            return

        logger.info("\n" + "="*80)
        logger.info("PROBLEMATIC ORCID RECORDS REPORT")
        logger.info("="*80)

        for i, rec in enumerate(orcid_records, 1):
            logger.info(f" _id: {rec.get('_id')}, email: {rec.get('email')}")

        logger.info("\n" + "="*80)

    def print_send_notifications_request_report(self, send_notifications_request: List[Dict[str, Any]]):
        if not send_notifications_request:
            logger.info("No problematic send_notifications_request found")
            return

        logger.info("\n" + "="*80)
        logger.info("PROBLEMATIC NOTIFICATIONS REPORT")
        logger.info("="*80)

        for i, rec in enumerate(send_notifications_request, 1):
            logger.info(f" _id: {rec.get('_id')}, Salesforce Id: {rec.get('salesforce_id')}")

        logger.info("\n" + "="*80)


class FindShortSfIdsUser:

    def __init__(self, connection_to_db: MongoDBConnection, collection: str):
        self.connection_to_db = connection_to_db
        self.collection_users = connection_to_db.get_collection(collection)

    def find_problematic_users(self) -> List[Dict[str, Any]]:
        """
        Find users.

        Returns:
            List of problematic users documents
        """

        try:
            logger.info("Searching for users...")

            users = list(self.collection_users.find(query_short_sf_ids))
            logger.info(f"Found {len(users)} users to fix")
            return users
        except OperationFailure as e:
            logger.error(f"Failed to query users: {e}")
            return []
        except Exception as e:
            logger.error(f"Unexpected error during query: {e}")
            return []

    def print_users_report(self, users: List[Dict[str, Any]]):
        if not users:
            logger.info("No problematic users found")
            return

        logger.info("\n" + "="*80)
        logger.info("PROBLEMATIC USERS REPORT")
        logger.info("="*80)

        for i, rec in enumerate(users, 1):
            logger.info(f" _id: {rec.get('_id')}, email: {rec.get('email')} Salesforce Id: {rec.get('salesforce_id')}")

        logger.info("\n" + "="*80)

def parse_arguments():
    parser = argparse.ArgumentParser(
        description='Find ORCID records salesforce ids',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Interactive mode
  python find_short_sf_ids.py

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
    database_userservice = 'userservice'

    logger.info("="*80)
    logger.info("Find short SF iD")
    logger.info("="*80)
    logger.info(f"Databases: {database_assertionservice}, {database_userservice}")
    logger.info(f"Collections: assertion, orcid_record, jhi_user and send_notifications_request")
    logger.info(f"MongoDB URI: {mongo_uri[:20]}..." if len(mongo_uri) > 20 else f"MongoDB URI: {mongo_uri}")
    logger.info("="*80 + "\n")

    connection_assertionservice = MongoDBConnection(mongo_uri, database_assertionservice)
    connection_userservice = MongoDBConnection(mongo_uri, database_userservice)

    try:
        if not connection_assertionservice.connect():
            logger.error("Failed to connect to assertionservice MongoDB. Exiting.")
            return 1

        if not connection_userservice.connect():
            logger.error("Failed to connect to userservice MongoDB. Exiting.")
            return 1

        fixer_assertionservice = FindFindShortSfIdsAssertion(connection_assertionservice)

        assertions = fixer_assertionservice.find_problematic_assertions()

        orcid_records = fixer_assertionservice.find_problematic_orcid_records()

        send_notifications_request = fixer_assertionservice.find_problematic_send_notifications_request()

        fixer_assertionservice.print_assertions_report(assertions)

        fixer_assertionservice.print_orcid_records_report(orcid_records)

        fixer_assertionservice.print_send_notifications_request_report(send_notifications_request)

        fixer_userservice = FindShortSfIdsUser(connection_userservice, 'jhi_user')

        users = fixer_userservice.find_problematic_users()

        fixer_userservice.print_users_report(users)

        if not assertions and not orcid_records and not users:
            logger.info("\n No fixes needed. All assertions, orcid records and users are correct.")
            return 0

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
        connection_assertionservice.disconnect()
        connection_userservice.disconnect()


if __name__ == '__main__':
    sys.exit(main())
