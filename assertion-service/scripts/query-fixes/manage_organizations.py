#!/usr/bin/env python3
"""
Script to Update or Merge Organizations

This script accepts the following parameters:

- Target Salesforce Organization ID (--target)

- Salesforce Organization ID to update (--source)
- Merge member flag (--merge)
- Force update member SF id (--force_update)

All references to the organization being updated (including users and assertions) are reassigned to the Target Salesforce Organization ID.

If the --merge flag is provided, the script deletes the updated (now obsolete) Salesforce Organization record from the member collection after all references have been successfully updated.
If the --force_update flag is provided, the script update the SF id of the source member.


Related to: https://app.clickup.com/t/9014437828/PD-3781

Usage:
    python manage_organizations.py --target=0012i00000eiI3CAAU --source=0012i00000aQxlxAAC
"""

import argparse
import sys
from typing import List, Dict, Any, Tuple
from pymongo.errors import OperationFailure

# Import shared modules
from logger_config import setup_logger
from db_connection import MongoDBConnection
from config import Config

# Set up logging
logger = setup_logger(__name__, log_file='manage-organizations.log')



class UpdateOrganizationMember:

    def __init__(self, connection_to_db: MongoDBConnection, target: str, source: str, merge: bool, force_update: bool):
        self.connection = connection_to_db
        self.collection_member = connection_to_db.get_collection('member')
        self.target = target
        self.source = source
        self.merge = merge
        self.force_update = force_update

    def find_problematic_members(self) -> Any:
        """
        Find members to update.
        """

        try:
            logger.info("\n" + "="*80)
            logger.info("Searching for members to update...")
            logger.info("="*80)

            if len(self.target) != 18:
                raise ValueError(
                    f"Error! Target should be 18 characters: target={self.target}"
                )

            if self.target == self.source:
                raise ValueError(
                    f"Error! Source and target member cannot be the same source={self.source} target={self.target}"
                )

            source = self.collection_member.find_one(
                {"salesforce_id": self.source}
            )

            target = self.collection_member.find_one(
                {"salesforce_id": self.target}
            )

            if self.merge:
                if not source and not target:
                    raise ValueError(
                        f"Error! Members to merge not found: source={self.source}, target={self.target}"
                    )
                if not target:
                    raise ValueError(
                        f"Error! Member to merge not found: target={self.target}"
                    )
                if not source:
                    raise ValueError(
                        f"Error! Member to merge not found: source={self.source}"
                    )

            if self.force_update:
                if not source:
                    raise ValueError(
                        f"Error! Member to update not found: source={self.source}"
                    )

                if target:
                    raise ValueError(
                        f"Error! Member to update already exist: target={self.target}"
                    )
            else:
                if not target:
                    raise ValueError(
                        f"Error! Target member should exist: target={self.target}"
                    )

            action = "merge" if self.merge else "update"

            if source:
                logger.info(
                    "Found source member to %s salesforce_id=%s, client_name=%s",
                    action,
                    source.get("salesforce_id"),
                    source.get("client_name"),
                )
            else:
                logger.info(
                    "Source member not found to %s salesforce_id=%s",
                    action,
                    self.source
                )

            if target:
                logger.info(
                    "Found target member to %s salesforce_id=%s, client_name=%s",
                    action,
                    target.get("salesforce_id"),
                    target.get("client_name"),
                )
            else:
                logger.info(
                    "Target member to %s not found salesforce_id=%s",
                    action,
                    self.target
                )
            logger.info("\n" + "="*80)

            return target

        except OperationFailure as e:
            logger.error(f"Failed to query members: {e}")
            raise
        except Exception as e:
            logger.exception("Unexpected error during member update search")
            raise

    def update_member(self):
        try:
            logger.info("Updating member salesforce_id=%s", self.source)

            if self.merge:
                result = self.collection_member.delete_one({"salesforce_id": self.source})

                if result.deleted_count == 1:
                    logger.info(
                        "Member deleted %s",
                        self.source,
                    )
                else:
                    logger.error(
                        "Failed to delete member salesforce_id=%s",
                        self.source
                    )

            if self.force_update:
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

class UpdateOrganizationsAssertions:

    def __init__(self, connection_to_db: MongoDBConnection, target: str, source: str):
        self.connection_to_db = connection_to_db
        self.collection_assertion = connection_to_db.get_collection('assertion')
        self.collection_orcid_record = connection_to_db.get_collection('orcid_record')
        self.collection_send_notifications_request = connection_to_db.get_collection('send_notifications_request')
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

    def find_problematic_send_notifications_request(self) -> List[Dict[str, Any]]:
        """
        Find send notifications request to update.

        Returns:
            List of problematic send_notifications_request documents
        """

        try:
            logger.info("Searching for send notifications request to update...")
            send_notifications_request = list(self.collection_send_notifications_request.find({ 'salesforce_id': self.source }))
            logger.info(f"Found {len(send_notifications_request)} send notifications request to fix")
            return send_notifications_request
        except OperationFailure as e:
            logger.error(f"Failed to query send notifications request: {e}")
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
            logger.info(f" email: {rec.get('email')} Salesforce Id: {rec.get('salesforce_id')}")

        logger.info("\n" + "="*80)

    def print_send_notifications_request_report(self, send_notifications_request: List[Dict[str, Any]]):
        if not send_notifications_request:
            logger.info("No problematic send notifications found")
            return

        logger.info("\n" + "="*80)
        logger.info("PROBLEMATIC SEND NOTIFICATIONS REQUEST REPORT")
        logger.info("="*80)

        for i, rec in enumerate(send_notifications_request, 1):
            logger.info(f" _id: {rec.get('_id')}, email: {rec.get('email')} Salesforce Id: {rec.get('salesforce_id')}")

        logger.info("\n" + "="*80)

    def fix_assertions(self, assertions: List[Dict[str, Any]]) -> int:
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

    def fix_orcid_records(self, orcid_records: List[Dict[str, Any]]) -> int:
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

    def fix_send_notifications_request(self, send_notifications_request: List[Dict[str, Any]]) -> int:
        """
            Fix the send notifications request to update.

        Returns:
            Number of send_notifications_request updated
        """
        if not send_notifications_request:
            logger.info("No send notifications request to fix")
            return 0

        logger.info(f"\n Applying fixes to {len(send_notifications_request)} send notifications request...")

        try:

            result = self.collection_send_notifications_request.update_many(
                {'salesforce_id': self.source},
                {'$set': {'salesforce_id': self.target}}
            )

            logger.info(f" Successfully updated {result.modified_count} send notifications request")
            logger.info(f"   Matched: {result.matched_count}")
            logger.info(f"   Modified: {result.modified_count}")

            return result.modified_count

        except OperationFailure as e:
            logger.error(f" Failed to update send notifications request: {e}")
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

    def verify_fixes_send_notifications_request(self) -> bool:
        logger.info("\n Verifying fixes send notifications request...")
        remaining = self.find_problematic_send_notifications_request()

        if not remaining:
            logger.info(" Verification passed: No problematic send notifications request found")
            return True
        else:
            logger.warning(f" Verification failed: {len(remaining)} problematic send notifications request still exist")
            return False

class UpdateOrganizationsUser:

    def __init__(self, connection_to_db: MongoDBConnection, collection: str, target: str, source: str, member_target: Any, merge: bool, force_update: bool):
        self.connection_to_db = connection_to_db
        self.collection_users = connection_to_db.get_collection(collection)
        self.target = target
        self.source = source
        self.merge = merge
        self.force_update = force_update
        self.owner_from_source_users = None
        self.remove_owner_from_source_users = False
        self.member_target = member_target

    def find_problematic_users(self) -> Tuple[List[Dict[str, Any]], bool]:
        """
        Find users to update.

        Returns:
            Tuple containing:
            - List of problematic users documents
            - Boolean indicating if the owner should be removed from source users
        """

        try:
            logger.info("\n" + "="*80)
            logger.info("Searching for users to update...")
            logger.info("="*80)

            users_source = list(self.collection_users.find({ 'salesforce_id': self.source }))
            users_target = list(self.collection_users.find({ 'salesforce_id': self.target }))

            logger.info(f"Found {len(users_source)} users to fix")
            owner_target = False

            if self.merge or self.force_update:
                if users_source:
                    for user in users_source:
                        if user.get("main_contact"):
                            self.remove_owner_from_source_users = True
                            self.owner_from_source_users = user.get("_id")
                            break

                if users_target:
                    for user in users_target:
                        if user.get("main_contact"):
                            logger.info(
                                "User email=%s is the organization owner of the target salesforce_id=%s",
                                user.get("email"),
                                self.target,
                            )
                            owner_target = True
                            break

                if not owner_target and not self.remove_owner_from_source_users:
                    raise ValueError(
                        f"Error! There is no organization owner"
                    )

            return users_source, self.remove_owner_from_source_users
        except OperationFailure as e:
            logger.error(f"Failed to query users: {e}")
            return [], False
        except Exception as e:
            logger.error(f"Unexpected error during query: {e}")
            return [], False

    def print_users_report(self, users: List[Dict[str, Any]]):
        if not users:
            logger.info("No problematic users found")
            return

        logger.info("\n" + "="*80)
        logger.info("PROBLEMATIC USERS REPORT")
        logger.info("="*80)

        for i, rec in enumerate(users, 1):
            logger.info(f" email: {rec.get('email')} Salesforce Id: {rec.get('salesforce_id')} Main contact {rec.get('main_contact')}")

        logger.info("\n" + "="*80)

    def fix_users(self, users: List[Dict[str, Any]]) -> int:
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

            if self.remove_owner_from_source_users:
                result_owner = self.collection_users.update_one(
                    {"_id": self.owner_from_source_users},
                    {"$set": {"main_contact": False}}
                )

                if result_owner.modified_count == 1:
                    logger.info(
                        "Removing organization owner flag from source member salesforce_id %s",
                        self.source
                    )
                else:
                    logger.info(
                        "Error updating organization owner flag from source member salesforce_id %s",
                        self.source
                    )

            result = self.collection_users.update_many(
                {'salesforce_id': self.source},
                {'$set': {'salesforce_id': self.target, 'member_name': self.member_target.get('client_name')}}
            )

            logger.info(f" Successfully updated salesforce id and member name in {result.modified_count} users")
            logger.info(f"   Matched: {result.matched_count}")
            logger.info(f"   Modified: {result.modified_count}")

            return result.modified_count

        except OperationFailure as e:
            logger.error(f" Failed to update users: {e}")
            return 0
        except Exception as e:
            logger.error(f" Unexpected error during update: {e}")
            return 0

    def verify_fixes_users(self) -> bool:
        logger.info("\n Verifying fixes users...")
        remaining, remove_owner_flag = self.find_problematic_users()

        if not remaining:
            logger.info(" Verification passed: No problematic users found")
            return True
        else:
            logger.warning(f" Verification failed: {len(remaining)} problematic users still exist")
            return False

def parse_arguments():
    parser = argparse.ArgumentParser(
        description='Manage organizations',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Interactive mode
  python manage_organizations.py

  MONGO_URI or MONGO_DB       - MongoDB connection string
        """
    )

    parser.add_argument('--mongo-uri', help='MongoDB URI (overrides env)')
    parser.add_argument('--target', help='Target organization SF iD')
    parser.add_argument('--source', help='Organization SF iD to update')
    parser.add_argument(
        "--merge",
        action="store_true",
        help="Delete the source member after references are updated"
    )
    parser.add_argument(
        "--force_update",
        action="store_true",
        help="Update the source member salesforce id"
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
    merge = args.merge
    force_update = args.force_update

    logger.info("="*80)
    logger.info("Manage organizations")
    logger.info("="*80)
    logger.info(f"Databases: {database_assertionservice}, {database_userservice} and {database_memberservice} ")
    logger.info(f"Collections: assertion, orcid_record, send_notifications_request, jhi_user and member")
    logger.info(f"MongoDB URI: {mongo_uri[:20]}..." if len(mongo_uri) > 20 else f"MongoDB URI: {mongo_uri}")
    logger.info(f"Target SF iD: {target}")
    logger.info(f"Source SF iD: {source}")
    logger.info(f"Merge option: {merge}")
    logger.info(f"Force update member option: {force_update}")
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

        fixer_memberservice = UpdateOrganizationMember(connection_memberservice, target, source, merge, force_update)

        member_target = fixer_memberservice.find_problematic_members()

        fixer_assertionservice = UpdateOrganizationsAssertions(connection_assertionservice, target, source)

        assertions = fixer_assertionservice.find_problematic_assertions()

        orcid_records = fixer_assertionservice.find_problematic_orcid_records()

        send_notifications_request = fixer_assertionservice.find_problematic_send_notifications_request()

        fixer_assertionservice.print_assertions_report(assertions)

        fixer_assertionservice.print_orcid_records_report(orcid_records)

        fixer_assertionservice.print_send_notifications_request_report(send_notifications_request)

        fixer_userservice = UpdateOrganizationsUser(connection_userservice, 'jhi_user', target, source, member_target, merge, force_update)

        users_list, remove_owner_flag = fixer_userservice.find_problematic_users()

        fixer_userservice. print_users_report(users_list)

        if not assertions and not orcid_records and not send_notifications_request and not users_list:
            if not merge and not force_update:
                logger.info("\n No fixes needed. All assertions, orcid records, send notifications request and users are correct.")
                return 0

        logger.info("\n" + "="*80)
        logger.info("  WARNING: This will modify the database!")
        logger.info(f"  {len(assertions)} assertions will be updated")
        logger.info(f"  {len(orcid_records)} orcid records will be updated")
        logger.info(f"  {len(send_notifications_request)} send notifications request will be updated")
        logger.info(f"  {len(users_list)} users will be updated")
        if remove_owner_flag:
            logger.info(f"  The organization owner user from the source member will be removed, since there is already one on the target")

        if merge:
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

        updated_count_assertions = fixer_assertionservice.fix_assertions(assertions)

        if updated_count_assertions > 0:
            if not fixer_assertionservice.verify_fixes_assertions():
                logger.warning("\n Some assertions may still need attention")
                return 1

        updated_count_orcid_records = fixer_assertionservice.fix_orcid_records(orcid_records)

        if updated_count_orcid_records > 0:
            if not fixer_assertionservice.verify_fixes_orcid_records():
                logger.warning("\n Some orcid records may still need attention")
                return 1

        updated_count_send_notifications_request = fixer_assertionservice.fix_send_notifications_request(send_notifications_request)

        if updated_count_send_notifications_request > 0:
            if not fixer_assertionservice.verify_fixes_send_notifications_request():
                logger.warning("\n Some send notifications request may still need attention")
                return 1

        updated_count_users = fixer_userservice.fix_users(users_list)

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
