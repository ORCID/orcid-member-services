#!/usr/bin/env python3
"""
Script to merge organizations from affiliations.

This script accepts two parameters: a Primary Salesforce Organization ID and a Deleted Salesforce Organization ID.
All references to the Deleted Organization ID are updated to use the Primary Organization ID.

Related to: https://app.clickup.com/t/9014437828/PD-3781

Usage:
    python merge_organizations.py --primary=0012i00000eiI3CAAU --deleted=0012i00000aQxlxAAC
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
logger = setup_logger(__name__, log_file='merge-organizations.log')


class MergeOrganizationsAssertionAndOrcidRecords:

    def __init__(self, connection: MongoDBConnection, primary: str, deleted: str):
        self.connection = connection
        self.collection_assertion = connection.get_collection('assertion')
        self.collection_orcid_record = connection.get_collection('orcid_record')
        self.primary = primary
        self.deleted = deleted

    def find_problematic_assertions(self) -> List[Dict[str, Any]]:
        """
        Find assertions to merge.

        Returns:
            List of problematic assertions documents
        """

        try:
            logger.info("Searching for assertions to merge...")
            assertions = list(self.collection_assertion.find({ 'salesforce_id': self.deleted }))
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
        Find orcid records to merge.

        Returns:
            List of problematic orcid records documents
        """

        try:
            logger.info("Searching for orcid records to merge...")

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
                                                    {'$type': '$$token.salesforce_id'},
                                                    'string'
                                                ]
                                            },
                                            {
                                                '$eq': [
                                                    '$$token.salesforce_id',
                                                    self.deleted
                                                ]
                                            }
                                        ]
                                    }
                                }
                            }
                        },
                        0
                    ]
                }
            }

            orcid_records = list(self.collection.find(query))
            logger.info(f"Found {len(orcid_records)} orcid records to fix")
            return orcid_records
        except OperationFailure as e:
            logger.error(f"Failed to query affiliations: {e}")
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
            logger.info("No problematic assertions found")
            return

        logger.info("\n" + "="*80)
        logger.info("PROBLEMATIC ORCID RECORDS REPORT")
        logger.info("="*80)

        for i, rec in enumerate(orcid_records, 1):
            logger.info(f" _id: {rec.get('_id')}, email: {rec.get('email')} Salesforce Id: {rec.get('salesforce_id')}")

        logger.info("\n" + "="*80)

    def find_assertions(self, assertions: List[Dict[str, Any]]) -> int:
        """
        Fix the assertions without Orcid iD.

        Returns:
            Number of sf successfully updated
        """
        if not assertions:
            logger.info("No orcid records to fix")
            return 0

        logger.info(f"\n Applying fixes to {len(assertions)} assertions...")

        try:

            result = self.collection.update_many(
                {'salesforce_id': self.deleted},
                {'$set': {'salesforce_id': self.primary}}
            )

            logger.info(f" Successfully updated {result.modified_count} affiliations")
            logger.info(f"   Matched: {result.matched_count}")
            logger.info(f"   Modified: {result.modified_count}")

            return result.modified_count

        except OperationFailure as e:
            logger.error(f" Failed to update orcid records: {e}")
            return 0
        except Exception as e:
            logger.error(f" Unexpected error during update: {e}")
            return 0

    def find_orcid_records(self, orcid_records: List[Dict[str, Any]]) -> int:
        """
        Fix the orcid records that we wanted to merge.

        Returns:
            Number of orcid records successfully updated
        """
        if not orcid_records:
            logger.info("No orcid records to fix")
            return 0

        logger.info(f"\n Applying fixes to {len(orcid_records)} orcid records...")

        modified_count = 0

        try:

            for orcid_record in orcid_records:
                tokens = orcid_record.get("tokens", [])

                for t in tokens:
                    salesforce_id_delete = self.deleted
                    salesforce_id_primary = self.primary
                    if salesforce_id_delete == t.get("salesforce_id"):
                        result = self.collection_orcid_record.update_one(
                            {"_id": orcid_record["_id"]},
                            {
                                "$set": {
                                    "tokens.$[token].salesforce_id": salesforce_id_primary,
                                }
                            },
                            array_filters=[
                                {"token.salesforce_id": salesforce_id_delete}
                            ]
                        )
                        modified_count += result.modified_count
                        logger.info(
                            f"Updated SF iD: deleted={salesforce_id_delete}, primary={salesforce_id_primary}"
                        )

            logger.info(f" Successfully updated {modified_count} orcid records")

            return modified_count


        except OperationFailure as e:
            logger.error(f" Failed to update orcid records: {e}")
            return 0
        except Exception as e:
            logger.error(f" Unexpected error during update: {e}")
            return 0


    def verify_fixes_assertions(self) -> bool:
        logger.info("\n Verifying fixes assertions...")
        remaining = self.find_problematic_assertions()

        if not remaining:
            logger.info(" Verification passed: No problematic assertions found")
            return True
        else:
            logger.warning(f" Verification failed: {len(remaining)} problematic assertions still exist")
            return False

    def verify_fixes_orcid_records(self) -> bool:
        logger.info("\n Verifying fixes orcid records...")
        remaining = self.find_problematic_orcid_records()

        if not remaining:
            logger.info(" Verification passed: No problematic orcid records found")
            return True
        else:
            logger.warning(f" Verification failed: {len(remaining)} problematic orcid records still exist")
            return False

class MergeOrganizationsUser:

    def __init__(self, connection: MongoDBConnection, collection: str, primary: str, deleted: str):
        self.connection = connection
        self.collection = connection.get_collection(collection)
        self.primary = primary
        self.deleted = deleted

    def find_problematic_users(self) -> List[Dict[str, Any]]:
        """
        Find users to merge.

        Returns:
            List of problematic users documents
        """

        try:
            logger.info("Searching for users to merge...")
            users = list(self.collection.find({ 'salesforce_id': self.deleted }))
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

    def find_users(self, users: List[Dict[str, Any]]) -> int:
        """
        Fix the users to merge.

        Returns:
            Number of users successfully updated
        """
        if not users:
            logger.info("No users to fix")
            return 0

        logger.info(f"\n Applying fixes to {len(users)} users...")

        try:

            result = self.collection.update_many(
                {'salesforce_id': self.deleted},
                {'$set': {'salesforce_id': self.primary}}
            )

            logger.info(f" Successfully updated {result.modified_count} users")
            logger.info(f"   Matched: {result.matched_count}")
            logger.info(f"   Modified: {result.modified_count}")

            return result.modified_count

        except OperationFailure as e:
            logger.error(f" Failed to update orcid records: {e}")
            return 0
        except Exception as e:
            logger.error(f" Unexpected error during update: {e}")
            return 0

    def verify_fixes_users(self) -> bool:
        logger.info("\n Verifying fixes users...")
        remaining = self.find_problematic_users()

        if not remaining:
            logger.info(" Verification passed: No problematic users found")
            return True
        else:
            logger.warning(f" Verification failed: {len(remaining)} problematic users still exist")
            return False

def parse_arguments():
    parser = argparse.ArgumentParser(
        description='Merge assertions',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Interactive mode
  python merge_organizations.py

  MONGO_URI or MONGO_DB       - MongoDB connection string
  MONGO_DATABASE or DATABASE  - Database name (default: assertionservice)
  MONGO_COLLECTION or COLLECTION - Collection name (default: assertion)
        """
    )

    parser.add_argument('--mongo-uri', help='MongoDB URI (overrides env)')
    parser.add_argument('--database', help='Database name (overrides env)')
    parser.add_argument('--collection', help='Collection name (overrides env)')
    parser.add_argument('--primary', help='Primary organization SF iD')
    parser.add_argument('--deleted', help='Deleted organization SF iD')

    return parser.parse_args()


def main():
    args = parse_arguments()

    config = Config()

    mongo_uri = args.mongo_uri or config.mongo_uri
    database_assertionservice = 'assertionservice'
    database_userservice = 'userservice'
    primary = args.primary
    deleted = args.deleted

    logger.info("="*80)
    logger.info("Merge organizations")
    logger.info("="*80)
    logger.info(f"Databases: {database_assertionservice} and {database_userservice} ")
    logger.info(f"Collections: assertion")
    logger.info(f"MongoDB URI: {mongo_uri[:20]}..." if len(mongo_uri) > 20 else f"MongoDB URI: {mongo_uri}")
    logger.info(f"Primary SF iD: {primary}")
    logger.info(f"Deleted SF iD: {deleted}")
    logger.info("="*80 + "\n")

    connection_assertionservice = MongoDBConnection(mongo_uri, database_assertionservice)
    connection_userservice = MongoDBConnection(mongo_uri, database_userservice)

    try:
        if not connection_assertionservice.connect():
            logger.error("Failed to connect to MongoDB. Exiting.")
            return 1

        if not connection_userservice.connect():
            logger.error("Failed to connect to MongoDB. Exiting.")
            return 1

        fixer_assertionservice = MergeOrganizationsAssertionAndOrcidRecords(connection_assertionservice, primary, deleted)

        assertions = fixer_assertionservice.find_problematic_assertions()

        orcid_records = fixer_assertionservice.find_problematic_orcid_records()

        fixer_assertionservice.print_assertions_report(assertions)

        fixer_assertionservice.print_orcid_records_report(orcid_records)

        fixer_userservice = MergeOrganizationsUser(connection_userservice, 'jhi_user', primary, deleted)

        users = fixer_userservice.find_problematic_users()

        fixer_userservice.print_users_report(assertions)

        if not assertions and orcid_records and users:
            logger.info("\n No fixes needed. All assertions, orcid records and users are correct.")
            return 0

        logger.info("\n" + "="*80)
        logger.info("  WARNING: This will modify the database!")
        logger.info(f"  {len(assertions)} assertions will be updated")
        logger.info(f"  {len(orcid_records)} orcid records will be updated")
        logger.info(f"  {len(users)} users will be updated")
        logger.info("="*80)

        try:
            response = input("\nDo you want to proceed? (yes/no): ").strip().lower()
            if response not in ['yes', 'y']:
                logger.info("\n Operation cancelled by user")
                return 0
        except (KeyboardInterrupt, EOFError):
            logger.info("\n\n Operation cancelled by user")
            return 1

        # updated_count_assertions = fixer_assertionservice.find_assertions(assertions)

        # if updated_count_assertions > 0:
        #     if not fixer_assertionservice.verify_fixes_assertions():
        #         logger.warning("\n Some assertions may still need attention")
        #         return 1

        # updated_count_orcid_records = fixer_assertionservice.find_orcid_records(assertions)

        # if updated_count_orcid_records > 0:
        #     if not fixer_assertionservice.verify_fixes_orcid_records():
        #         logger.warning("\n Some orcid records may still need attention")
        #         return 1

        updated_count_users = fixer_userservice.find_users(users)

        # if updated_count_users > 0:
        #     if not fixer_userservice.verify_fixes_users():
        #         logger.warning("\n Some users may still need attention")
        #         return 1

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
