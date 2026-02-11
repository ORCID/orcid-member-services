#!/usr/bin/env python3
"""
Script to Update or Merge Organizations

This script accepts the following parameters:

- Target Salesforce Organization ID (--target)

- Salesforce Organization ID to update (--source)
- Delete member flag (--delete)

All references to the organization being updated (including users and assertions) are reassigned to the Target Salesforce Organization ID.

If the --delete flag is provided, the script deletes the updated (now obsolete) Salesforce Organization record from the member collection after all references have been successfully updated.
If the --ignore flag is provided, the script ignore if the target salesforce Id exists


Related to: https://app.clickup.com/t/9014437828/PD-3781

Usage:
    python manage_organizations.py --target=0012i00000eiI3CAAU --source=0012i00000aQxlxAAC
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
logger = setup_logger(__name__, log_file='manage-organizations.log')



class UpdateOrganizationMember:

    def __init__(self, connection_to_db: MongoDBConnection, target: str, source: str, delete: bool):
        self.connection = connection_to_db
        self.collection_member = connection_to_db.get_collection('member')
        self.target = target
        self.source = source
        self.delete = delete

    def find_problematic_members(self):
        """
        Find members to update.
        """

        try:
            logger.info("Searching for members to update...")

            source = self.collection_member.find_one(
                {"salesforce_id": self.source}
            )

            target = self.collection_member.find_one(
                {"salesforce_id": self.target}
            )

            if not source:
                raise ValueError(f"Member to update not found: {self.source}")

            logger.info(
                "Found member to update salesforce_id=%s, client_name=%s",
                source.get("salesforce_id"),
                source.get("client_name"),
            )

            if not target:
                if not self.delete:
                    raise ValueError(f"Target member not found: {self.source}")
            else:
                logger.info(
                    "Found target member salesforce_id=%s, client_name=%s",
                    target.get("salesforce_id"),
                    target.get("client_name"),
                )

        except OperationFailure as e:
            logger.error(f"Failed to query members: {e}")
            raise
        except Exception as e:
            logger.exception("Unexpected error during member update search")
            raise

    def update_member(self):
        try:
            logger.info("Updating member salesforce_id=%s", self.source)

            if self.delete:
                result = self.collection_member.delete_one({"salesforce_id": self.source})

                if result.deleted_count == 1:
                    logger.info(
                        "Updated member from memberToDelete=%s to target=%s",
                        self.source,
                        self.target,
                    )
                else:
                    logger.error(
                        "Failed to update member salesforce_id=%s",
                        self.source
                    )
            else :
                result = self.collection_member.update_one(
                    {"salesforce_id": self.source},
                    {"$set": {"salesforce_id": self.target}}
                )

            if result.modified_count == 1:
                logger.info(
                    "Updated member salesforce_id from %s to %s",
                    self.source,
                    self.target
                )
            else:
                logger.warning(
                    "Member salesforce_id=%s already up to date",
                    self.source
                )



        except OperationFailure as e:
            logger.error(f"Failed to query members: {e}")
        except Exception as e:
            logger.exception("Unexpected error during member update search")

class UpdateOrganizationsAssertionAndOrcidRecords:

    def __init__(self, connection_to_db: MongoDBConnection, target: str, source: str):
        self.connection_to_db = connection_to_db
        self.collection_assertion = connection_to_db.get_collection('assertion')
        self.collection_orcid_record = connection_to_db.get_collection('orcid_record')
        self.target = target
        self.source = source

    def find_problematic_assertions(self) -> List[Dict[str, Any]]:
        """
        Find assertions to update.

        Returns:
            List of problematic assertions documents
        """

        try:
            logger.info("Searching for assertions to update...")
            assertions = list(self.collection_assertion.find({ 'salesforce_id': self.source }))
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
        Find orcid records to update.

        Returns:
            List of problematic orcid records documents
        """

        try:
            logger.info("Searching for orcid records to update...")

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
                                                    self.source
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

            orcid_records = list(self.collection_orcid_record.find(query))
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
            logger.info("No problematic orcid records found")
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
            Number of assertions updated
        """
        if not assertions:
            logger.info("No assertions to fix")
            return 0

        logger.info(f"\n Applying fixes to {len(assertions)} assertions...")

        try:

            result = self.collection_assertion.update_many(
                {'salesforce_id': self.source},
                {'$set': {'salesforce_id': self.target}}
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
        Fix the orcid records that we wanted to update.

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
                    salesforce_id_update = self.source
                    salesforce_id_target = self.target
                    if salesforce_id_update == t.get("salesforce_id"):
                        result = self.collection_orcid_record.update_one(
                            {"_id": orcid_record["_id"]},
                            {
                                "$set": {
                                    "tokens.$[token].salesforce_id": salesforce_id_target,
                                }
                            },
                            array_filters=[
                                {"token.salesforce_id": salesforce_id_update}
                            ]
                        )
                        modified_count += result.modified_count
                        logger.info(
                            f"Updated SF iD: source={salesforce_id_update}, target={salesforce_id_target}"
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

class UpdateOrganizationsUser:

    def __init__(self, connection_to_db: MongoDBConnection, collection: str, target: str, source: str):
        self.connection_to_db = connection_to_db
        self.collection_users = connection_to_db.get_collection(collection)
        self.target = target
        self.source = source

    def find_problematic_users(self) -> List[Dict[str, Any]]:
        """
        Find users to update.

        Returns:
            List of problematic users documents
        """

        try:
            logger.info("Searching for users to update...")
            users = list(self.collection_users.find({ 'salesforce_id': self.source }))
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
        Fix the users to update.

        Returns:
            Number of users successfully updated
        """
        if not users:
            logger.info("No users to fix")
            return 0

        logger.info(f"\n Applying fixes to {len(users)} users...")

        try:

            result = self.collection_users.update_many(
                {'salesforce_id': self.source},
                {'$set': {'salesforce_id': self.target}}
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
        description='Manage assertions',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Interactive mode
  python update_organizations.py

  MONGO_URI or MONGO_DB       - MongoDB connection string
  MONGO_DATABASE or DATABASE  - Database name (default: assertionservice)
  MONGO_COLLECTION or COLLECTION - Collection name (default: assertion)
        """
    )

    parser.add_argument('--mongo-uri', help='MongoDB URI (overrides env)')
    parser.add_argument('--target', help='Target organization SF iD')
    parser.add_argument('--source', help='Organization SF iD to update')
    parser.add_argument(
        "--delete",
        action="store_true",
        help="Delete the member after references are updated"
    )

    return parser.parse_args()


def main():
    args = parse_arguments()

    config = Config()

    mongo_uri = args.mongo_uri or config.mongo_uri
    database_assertionservice = 'assertionservice'
    database_userservice = 'userservice'
    database_memberservice = 'memberservice'
    target = args.target
    source = args.source
    delete = args.delete

    logger.info("="*80)
    logger.info("Manage organizations")
    logger.info("="*80)
    logger.info(f"Databases: {database_assertionservice}, {database_userservice} and {database_memberservice} ")
    logger.info(f"Collections: assertion, orcid_record, jhi_user and member")
    logger.info(f"MongoDB URI: {mongo_uri[:20]}..." if len(mongo_uri) > 20 else f"MongoDB URI: {mongo_uri}")
    logger.info(f"Target SF iD: {target}")
    logger.info(f"Source SF iD: {source}")
    logger.info(f"Delete option: {delete}")
    logger.info("="*80 + "\n")

    connection_assertionservice = MongoDBConnection(mongo_uri, database_assertionservice)
    connection_userservice = MongoDBConnection(mongo_uri, database_userservice)
    connection_memberservice = MongoDBConnection(mongo_uri, database_memberservice)

    try:
        if not connection_assertionservice.connect():
            logger.error("Failed to connect to assertionservice MongoDB. Exiting.")
            return 1

        if not connection_userservice.connect():
            logger.error("Failed to connect to userservice MongoDB. Exiting.")
            return 1

        if not connection_memberservice.connect():
            logger.error("Failed to connect to memberservice MongoDB. Exiting.")
            return 1

        fixer_memberservice = UpdateOrganizationMember(connection_memberservice, target, source, delete)

        fixer_memberservice.find_problematic_members()

        fixer_assertionservice = UpdateOrganizationsAssertionAndOrcidRecords(connection_assertionservice, target, source)

        assertions = fixer_assertionservice.find_problematic_assertions()

        orcid_records = fixer_assertionservice.find_problematic_orcid_records()

        fixer_assertionservice.print_assertions_report(assertions)

        fixer_assertionservice.print_orcid_records_report(orcid_records)

        fixer_userservice = UpdateOrganizationsUser(connection_userservice, 'jhi_user', target, source)

        users = fixer_userservice.find_problematic_users()

        fixer_userservice.print_users_report(users)

        if not assertions and not orcid_records and not users:
            logger.info("\n No fixes needed. All assertions, orcid records and users are correct.")
            return 0

        logger.info("\n" + "="*80)
        logger.info("  WARNING: This will modify the database!")
        logger.info(f"  {len(assertions)} assertions will be updated")
        logger.info(f"  {len(orcid_records)} orcid records will be updated")
        logger.info(f"  {len(users)} users will be updated")

        if delete:
            logger.info(f"  Member {source} will be deleted")
        logger.info("="*80)

        try:
            response = input("\nDo you want to proceed? (yes/no): ").strip().lower()
            if response not in ['yes', 'y']:
                logger.info("\n Operation cancelled by user")
                return 0
        except (KeyboardInterrupt, EOFError):
            logger.info("\n\n Operation cancelled by user")
            return 1

        updated_count_assertions = fixer_assertionservice.find_assertions(assertions)

        if updated_count_assertions > 0:
            if not fixer_assertionservice.verify_fixes_assertions():
                logger.warning("\n Some assertions may still need attention")
                return 1

        updated_count_orcid_records = fixer_assertionservice.find_orcid_records(orcid_records)

        if updated_count_orcid_records > 0:
            if not fixer_assertionservice.verify_fixes_orcid_records():
                logger.warning("\n Some orcid records may still need attention")
                return 1

        updated_count_users = fixer_userservice.find_users(users)

        if updated_count_users > 0:
            if not fixer_userservice.verify_fixes_users():
                logger.warning("\n Some users may still need attention")
                return 1

        fixer_memberservice.update_member()

        logger.info("\n" + "="*80)
        logger.info("Script completed successfully")
        logger.info("="*80)
        return 0

    except ValueError as e:
        logger.error(f"\nOperation failed: {str(e)}")
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
