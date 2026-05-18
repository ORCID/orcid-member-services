#!/usr/bin/env python3
"""
Backfill internal member IDs onto related records.

Given a single Salesforce ID (--source), look up the corresponding member
and backfill its internal `_id` onto related records in the assertion and
user services. The member document itself is never modified.

Backfilled targets:
  - assertionservice.assertion                   (set member_id where salesforce_id matches)
  - assertionservice.orcid_record                (set tokens[].member_id where tokens[].salesforce_id matches)
  - assertionservice.send_notifications_request  (set member_id where salesforce_id matches)
  - userservice.jhi_user                         (set member_id where salesforce_id matches)

Related to: https://orcid.clickup.com/t/9014437828/PD-5585

Usage:
    python backfill_member_id.py --source=0012i00000aQxlxAAC
"""

import argparse
import re
import sys
from pathlib import Path
from typing import List, Dict, Any, Tuple
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
logger = setup_logger(__name__, log_file='manage-organizations.log')

SALESFORCE_ID_PATTERN = re.compile(r"^[a-zA-Z0-9]{18}$")


class InvalidSalesforceIdError(ValueError):
    pass


class MemberNotFoundError(LookupError):
    pass


class MissingMemberIdError(ValueError):
    pass


class BackfillError(RuntimeError):
    pass


def validate_salesforce_id(value: str) -> str:
    if not value or not SALESFORCE_ID_PATTERN.match(value):
        raise InvalidSalesforceIdError(
            f"Invalid Salesforce ID: {value!r}. "
            f"Expected exactly 18 alphanumeric characters."
        )
    return value


class MemberLookup:
    """Read-only lookup of a member by Salesforce ID."""

    def __init__(self, connection_to_db: MongoDBConnection, source: str):
        self.collection_member = connection_to_db.get_collection('member')
        self.source = source

    def find_member_id(self) -> Tuple[Any, Any]:
        logger.info("\n" + "="*80)
        logger.info("Looking up member by Salesforce ID: %s", self.source)
        logger.info("="*80)

        try:
            member = self.collection_member.find_one(
                {"salesforce_id": self.source}
            )
        except OperationFailure as e:
            logger.error(f"Failed to query member: {e}")
            raise

        if not member:
            raise MemberNotFoundError(
                f"No member found for salesforce_id={self.source}"
            )

        raw_member_id = member.get("_id")
        if not raw_member_id:
            raise MissingMemberIdError(
                f"Member with salesforce_id={self.source} has no _id field"
            )

        # Java entities declare memberId as String, so persist the hex
        # representation of the ObjectId to stay consistent with Spring Data.
        member_id = str(raw_member_id)
        client_name = member.get("client_name")
        logger.info(
            "Found member: _id=%s, salesforce_id=%s, client_name=%s",
            member_id,
            member.get("salesforce_id"),
            client_name,
        )
        logger.info("="*80)
        return member_id, client_name


class BackfillAssertionRecords:
    """Backfill member_id on assertion-service collections."""

    def __init__(self, connection_to_db: MongoDBConnection, source: str, member_id: Any):
        self.collection_assertion = connection_to_db.get_collection('assertion')
        self.collection_orcid_record = connection_to_db.get_collection('orcid_record')
        self.collection_send_notifications_request = connection_to_db.get_collection('send_notifications_request')
        self.source = source
        self.member_id = member_id

    def _mismatch_filter(self) -> Dict[str, Any]:
        return {
            'salesforce_id': self.source,
            '$or': [
                {'member_id': {'$exists': False}},
                {'member_id': {'$ne': self.member_id}},
            ],
        }

    def count_total_matching(self) -> int:
        """Total records matching salesforce_id regardless of member_id state."""
        try:
            assertion_total = self.collection_assertion.count_documents({'salesforce_id': self.source})
            orcid_total = self.collection_orcid_record.count_documents({'tokens.salesforce_id': self.source})
            notifications_total = self.collection_send_notifications_request.count_documents({'salesforce_id': self.source})
            return assertion_total + orcid_total + notifications_total
        except OperationFailure as e:
            logger.error(f"Failed to count assertion-service records: {e}")
            return 0

    def find_assertions_needing_backfill(self) -> List[Dict[str, Any]]:
        try:
            logger.info("Searching for assertions with salesforce_id=%s...", self.source)
            assertions = list(self.collection_assertion.find(self._mismatch_filter()))
            logger.info(f"Found {len(assertions)} assertions needing backfill")
            return assertions
        except OperationFailure as e:
            logger.error(f"Failed to query assertions: {e}")
            return []

    def find_orcid_records_needing_backfill(self) -> List[Dict[str, Any]]:
        try:
            logger.info("Searching for orcid records with tokens.salesforce_id=%s...", self.source)
            query = {
                'tokens': {
                    '$elemMatch': {
                        'salesforce_id': self.source,
                        '$or': [
                            {'member_id': {'$exists': False}},
                            {'member_id': {'$ne': self.member_id}},
                        ],
                    }
                }
            }
            orcid_records = list(self.collection_orcid_record.find(query))
            logger.info(f"Found {len(orcid_records)} orcid records needing backfill")
            return orcid_records
        except OperationFailure as e:
            logger.error(f"Failed to query orcid records: {e}")
            return []

    def find_send_notifications_needing_backfill(self) -> List[Dict[str, Any]]:
        try:
            logger.info("Searching for send_notifications_request with salesforce_id=%s...", self.source)
            notifications = list(self.collection_send_notifications_request.find(self._mismatch_filter()))
            logger.info(f"Found {len(notifications)} send_notifications_request needing backfill")
            return notifications
        except OperationFailure as e:
            logger.error(f"Failed to query send_notifications_request: {e}")
            return []

    def print_assertions_report(self, assertions: List[Dict[str, Any]]):
        if not assertions:
            logger.info("No assertions need backfilling")
            return

        logger.info("\n" + "="*80)
        logger.info("ASSERTIONS NEEDING BACKFILL")
        logger.info("="*80)

        for rec in assertions:
            logger.info(f" Email: {rec.get('email')}, Salesforce Id: {rec.get('salesforce_id')}")

        logger.info("\n" + "="*80)

    def print_orcid_records_report(self, orcid_records: List[Dict[str, Any]]):
        if not orcid_records:
            logger.info("No orcid records need backfilling")
            return

        logger.info("\n" + "="*80)
        logger.info("ORCID RECORDS NEEDING BACKFILL")
        logger.info("="*80)

        for rec in orcid_records:
            logger.info(f" Email: {rec.get('email')}")

        logger.info("\n" + "="*80)

    def print_send_notifications_report(self, notifications: List[Dict[str, Any]]):
        if not notifications:
            logger.info("No send_notifications_request need backfilling")
            return

        logger.info("\n" + "="*80)
        logger.info("SEND NOTIFICATIONS REQUEST NEEDING BACKFILL")
        logger.info("="*80)

        for rec in notifications:
            logger.info(f" Email: {rec.get('email')}, Salesforce Id: {rec.get('salesforce_id')}")

        logger.info("\n" + "="*80)

    def backfill_assertions(self) -> int:
        logger.info("\n Backfilling member_id on assertions...")
        try:
            result = self.collection_assertion.update_many(
                {'salesforce_id': self.source},
                {'$set': {'member_id': self.member_id}}
            )
            logger.info(f" Successfully backfilled {result.modified_count} assertions")
            logger.info(f"   Matched: {result.matched_count}")
            logger.info(f"   Modified: {result.modified_count}")
            return result.modified_count
        except OperationFailure as e:
            logger.error(f" Failed to backfill assertions: {e}")
            raise BackfillError(f"assertion backfill failed: {e}") from e

    def backfill_orcid_records(self) -> int:
        logger.info("\n Backfilling tokens[].member_id on orcid_record...")
        try:
            result = self.collection_orcid_record.update_many(
                {'tokens.salesforce_id': self.source},
                {'$set': {'tokens.$[token].member_id': self.member_id}},
                array_filters=[{'token.salesforce_id': self.source}]
            )
            logger.info(f" Successfully backfilled {result.modified_count} orcid records")
            logger.info(f"   Matched: {result.matched_count}")
            logger.info(f"   Modified: {result.modified_count}")
            return result.modified_count
        except OperationFailure as e:
            logger.error(f" Failed to backfill orcid records: {e}")
            raise BackfillError(f"orcid_record backfill failed: {e}") from e

    def backfill_send_notifications(self) -> int:
        logger.info("\n Backfilling member_id on send_notifications_request...")
        try:
            result = self.collection_send_notifications_request.update_many(
                {'salesforce_id': self.source},
                {'$set': {'member_id': self.member_id}}
            )
            logger.info(f" Successfully backfilled {result.modified_count} send_notifications_request")
            logger.info(f"   Matched: {result.matched_count}")
            logger.info(f"   Modified: {result.modified_count}")
            return result.modified_count
        except OperationFailure as e:
            logger.error(f" Failed to backfill send_notifications_request: {e}")
            raise BackfillError(f"send_notifications_request backfill failed: {e}") from e

    def verify(self) -> bool:
        logger.info("\n Verifying assertion-service backfills...")
        mismatch_filter = {
            'salesforce_id': self.source,
            '$or': [
                {'member_id': {'$exists': False}},
                {'member_id': {'$ne': self.member_id}},
            ],
        }
        remaining = (
            self.collection_assertion.count_documents(mismatch_filter)
            + self.collection_orcid_record.count_documents({
                'tokens': {'$elemMatch': mismatch_filter}
            })
            + self.collection_send_notifications_request.count_documents(mismatch_filter)
        )
        if remaining == 0:
            logger.info(" Verification passed: all assertion-service records carry the expected member_id")
            return True
        logger.warning(f" Verification failed: {remaining} assertion-service records still missing or mismatched")
        return False


class BackfillUserRecords:
    """Backfill member_id on user-service collections."""

    def __init__(self, connection_to_db: MongoDBConnection, collection: str, source: str, member_id: Any):
        self.collection_users = connection_to_db.get_collection(collection)
        self.source = source
        self.member_id = member_id

    def _user_filter(self) -> Dict[str, Any]:
        return {
            'salesforce_id': self.source,
            '$or': [
                {'member_id': {'$exists': False}},
                {'member_id': {'$ne': self.member_id}},
            ],
        }

    def count_total_matching(self) -> int:
        """Total users matching salesforce_id regardless of member_id state."""
        try:
            return self.collection_users.count_documents({'salesforce_id': self.source})
        except OperationFailure as e:
            logger.error(f"Failed to count users: {e}")
            return 0

    def find_users_needing_backfill(self) -> List[Dict[str, Any]]:
        try:
            logger.info("Searching for users with salesforce_id=%s...", self.source)
            users = list(self.collection_users.find(self._user_filter()))
            logger.info(f"Found {len(users)} users needing backfill")
            return users
        except OperationFailure as e:
            logger.error(f"Failed to query users: {e}")
            return []

    def print_users_report(self, users: List[Dict[str, Any]]):
        if not users:
            logger.info("No users need backfilling")
            return

        logger.info("\n" + "="*80)
        logger.info("USERS NEEDING BACKFILL")
        logger.info("="*80)

        for rec in users:
            logger.info(f" email: {rec.get('email')}, Salesforce Id: {rec.get('salesforce_id')}, Main contact: {rec.get('main_contact')}")

        logger.info("\n" + "="*80)

    def backfill_users(self) -> int:
        logger.info("\n Backfilling member_id on users...")
        try:
            result = self.collection_users.update_many(
                {'salesforce_id': self.source},
                {'$set': {'member_id': self.member_id}}
            )
            logger.info(f" Successfully backfilled {result.modified_count} users")
            logger.info(f"   Matched: {result.matched_count}")
            logger.info(f"   Modified: {result.modified_count}")
            return result.modified_count
        except OperationFailure as e:
            logger.error(f" Failed to backfill users: {e}")
            raise BackfillError(f"jhi_user backfill failed: {e}") from e

    def verify(self) -> bool:
        logger.info("\n Verifying user backfills...")
        remaining = self.collection_users.count_documents({
            'salesforce_id': self.source,
            '$or': [
                {'member_id': {'$exists': False}},
                {'member_id': {'$ne': self.member_id}},
            ],
        })
        if remaining == 0:
            logger.info(" Verification passed: all users carry the expected member_id")
            return True
        logger.warning(f" Verification failed: {remaining} users still missing or mismatched")
        return False


def parse_arguments():
    parser = argparse.ArgumentParser(
        description='Backfill internal member IDs onto related records',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python backfill.py --source=0012i00000aQxlxAAC

  SPRING_DATA_MONGODB_URI - MongoDB connection string (read from env)
        """
    )

    parser.add_argument(
        '--source',
        required=True,
        help='Salesforce ID of the member whose related records should be backfilled'
    )

    return parser.parse_args()


def main():
    args = parse_arguments()

    try:
        source = validate_salesforce_id(args.source)
    except InvalidSalesforceIdError as e:
        logger.error(str(e))
        return 1

    config = Config()
    mongo_uri = config.mongo_uri

    database_assertionservice = 'assertionservice'
    database_userservice = 'userservice'
    database_memberservice = 'memberservice'

    logger.info("="*80)
    logger.info("Backfill internal member IDs")
    logger.info("="*80)
    logger.info(f"Databases: {database_assertionservice}, {database_userservice}, {database_memberservice}")
    logger.info(f"Collections: assertion, orcid_record, send_notifications_request, jhi_user")
    logger.info(f"MongoDB URI: {mongo_uri[:20]}..." if len(mongo_uri) > 20 else f"MongoDB URI: {mongo_uri}")
    logger.info(f"Source SF iD: {source}")
    logger.info("="*80 + "\n")

    connection_assertionservice = MongoDBConnection(mongo_uri, database_assertionservice)
    connection_userservice = MongoDBConnection(mongo_uri, database_userservice)
    connection_memberservice = MongoDBConnection(mongo_uri, database_memberservice)

    try:
        if not connection_memberservice.connect():
            logger.error("Failed to connect to memberservice MongoDB. Exiting.")
            return 1
        if not connection_assertionservice.connect():
            logger.error("Failed to connect to assertionservice MongoDB. Exiting.")
            return 1
        if not connection_userservice.connect():
            logger.error("Failed to connect to userservice MongoDB. Exiting.")
            return 1

        member_lookup = MemberLookup(connection_memberservice, source)
        member_id, client_name = member_lookup.find_member_id()

        assertion_backfiller = BackfillAssertionRecords(connection_assertionservice, source, member_id)
        user_backfiller = BackfillUserRecords(connection_userservice, 'jhi_user', source, member_id)

        assertions = assertion_backfiller.find_assertions_needing_backfill()
        orcid_records = assertion_backfiller.find_orcid_records_needing_backfill()
        notifications = assertion_backfiller.find_send_notifications_needing_backfill()
        users = user_backfiller.find_users_needing_backfill()

        assertion_backfiller.print_assertions_report(assertions)
        assertion_backfiller.print_orcid_records_report(orcid_records)
        assertion_backfiller.print_send_notifications_report(notifications)
        user_backfiller.print_users_report(users)

        if not assertions and not orcid_records and not notifications and not users:
            total_existing = (
                assertion_backfiller.count_total_matching()
                + user_backfiller.count_total_matching()
            )
            if total_existing > 0:
                logger.info(
                    "\n member_id is already backfilled on all %d records matching salesforce_id=%s. "
                    "No changes needed.",
                    total_existing,
                    source,
                )
            else:
                logger.info(
                    "\n No records found with salesforce_id=%s in any collection. Nothing to do.",
                    source,
                )
            return 0

        logger.info("\n" + "="*80)
        logger.info("  WARNING: This will modify the database!")
        logger.info(f"  {len(assertions)} assertions will be backfilled")
        logger.info(f"  {len(orcid_records)} orcid records will be backfilled")
        logger.info(f"  {len(notifications)} send_notifications_request will be backfilled")
        logger.info(f"  {len(users)} users will be backfilled")
        logger.info(f"  member_id will be set to {member_id} (client_name={client_name})")
        logger.info("  The member document itself will NOT be modified")
        logger.info("="*80)

        try:
            response = input("\nDo you want to proceed? (yes/no): ").strip().lower()
            if response not in ['yes', 'y']:
                logger.info("\n Operation cancelled by user")
                return 0
        except (KeyboardInterrupt, EOFError):
            logger.info("\n\n Operation cancelled by user")
            return 1

        assertion_backfiller.backfill_assertions()
        assertion_backfiller.backfill_orcid_records()
        assertion_backfiller.backfill_send_notifications()
        user_backfiller.backfill_users()

        if not assertion_backfiller.verify() or not user_backfiller.verify():
            logger.warning("\n Some records may still need attention")
            return 1

        logger.info("\n" + "="*80)
        logger.info("Script completed successfully")
        logger.info("="*80)
        return 0

    except InvalidSalesforceIdError as e:
        logger.error(f"\nInvalid Salesforce ID: {e}")
        return 1
    except MemberNotFoundError as e:
        logger.error(f"\nMember not found: {e}")
        return 1
    except MissingMemberIdError as e:
        logger.error(f"\nMember is missing _id: {e}")
        return 1
    except BackfillError as e:
        logger.error(f"\nBackfill operation failed: {e}")
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
