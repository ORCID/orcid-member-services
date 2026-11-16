#!/usr/bin/env python3
"""
Step 1 of 2: MongoDB half of the client_details test data.

The api-credentials-service container cannot reach MongoDB, so everything that touches
memberservice.member / userservice.jhi_user lives here; the Postgres half is
api-credentials-service's scripts/test-data/create_client_details_test_data.py, which
reads the JSON written here.

Creates one fixed set of test data, once:

    Member                              User (main contact)   Login
    Northwind University                Emma Clarke           emma.clarke@northwind.example.org
    Riverside Institute of Technology   Liam Patel            liam.patel@riverside.example.org
    Lakeshore Medical Research Centre   Sofia Moreno          sofia.moreno@lakeshore.example.org

All users have the password "test1234" and are activated. If the test data already exists,
nothing is inserted (run --cleanup first to recreate it); the members file is still
written. Members and users are tagged with created_by = "pd-13632-test-data" so --cleanup
can find them.

Output JSON:
    {
      "source": {"mongo_database": "memberservice", "collection": "member"},
      "stats": {"members_total": N, "members_exported": N, "members_skipped": N, "test_members": N},
      "members": [{"member_id": "<24-hex>", "salesforce_id": "<15 or 18 chars>",
                   "member_name": "...", "api_access_level": "premium" | "basic" | null,
                   "test_member": true | false}]
    }

member_name is only used to name the test groups and clients. The file can also be passed
as --members-file to the backfill (extra keys are ignored).

Next step: copy the file into the api-credentials-service container and run
    python3 /scripts/test-data/create_client_details_test_data.py --members-file <file>

Related to: PD-13632

Usage:
    python create_client_details_test_data.py                     # create + export
    python create_client_details_test_data.py --output /tmp/client_details_test_data_members.json
    python create_client_details_test_data.py --dry-run           # no Mongo writes, export only
    python create_client_details_test_data.py --cleanup
"""

import argparse
import json
import os
import secrets
import string
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import List, Set, Tuple

from bson import ObjectId
from pymongo.errors import OperationFailure

CURRENT_DIR = Path(__file__).resolve().parent
UTILS_DIR = CURRENT_DIR.parent / "utils"

if str(UTILS_DIR) not in sys.path:
    sys.path.insert(0, str(UTILS_DIR))

from logger_config import setup_logger
from db_connection import MongoDBConnection
from config import Config

logger = setup_logger(__name__, log_file='create-client-details-test-data.log')

MONGO_DATABASE = 'memberservice'
MONGO_COLLECTION = 'member'
USER_DATABASE = 'userservice'
USER_COLLECTION = 'jhi_user'
SF15_LEN = 15
MEMBER_CREATED_BY = 'pd-13632-test-data'
DEFAULT_MEMBER_CLASS = 'org.orcid.mp.member.domain.Member'
DEFAULT_USER_CLASS = 'org.orcid.mp.user.domain.User'
SF_ID_CHARS = string.ascii_letters + string.digits

# The one set of test data: (member name, user first name, user last name, user email).
# Emails use subdomains of example.org, which is reserved and never delivers mail.
TEST_DATA = [
    ('Northwind University', 'Emma', 'Clarke', 'emma.clarke@northwind.example.org'),
    ('Riverside Institute of Technology', 'Liam', 'Patel', 'liam.patel@riverside.example.org'),
    ('Lakeshore Medical Research Centre', 'Sofia', 'Moreno', 'sofia.moreno@lakeshore.example.org'),
]

# Test users log in with this password. The hash is BCryptPasswordEncoder's format
# (cost 10), as user-service-2 stores it; bcrypt is not installed in the containers.
TEST_USER_PASSWORD = 'test1234'
TEST_USER_PASSWORD_HASH = '$2a$10$b3Q1GF0vipKyJ14RnSPsVeyWRfgYmsL1ZIGIzAdWFjUu4Hz3lsY/e'


def random_sf15() -> str:
    # '001' is the Salesforce key prefix for Account records.
    return '001' + ''.join(secrets.choice(SF_ID_CHARS) for _ in range(SF15_LEN - 3))


def sf15_to_sf18(sf15: str) -> str:
    """Append Salesforce's case-safe checksum: one char per 5-char chunk, encoding
    which positions hold an uppercase letter."""
    alphabet = string.ascii_uppercase + '012345'
    suffix = ''
    for start in range(0, SF15_LEN, 5):
        chunk = sf15[start:start + 5]
        flags = sum(1 << i for i, ch in enumerate(chunk) if 'A' <= ch <= 'Z')
        suffix += alphabet[flags]
    return sf15 + suffix


def build_test_member_docs(taken_sf15: Set[str], member_class: str, now: datetime) -> List[dict]:
    """One member document per TEST_DATA entry, shaped like member-service-2's Member
    (null fields omitted, as Spring Data does)."""
    docs = []
    for member_name, _, _, _ in TEST_DATA:
        sf15 = random_sf15()
        while sf15 in taken_sf15:
            sf15 = random_sf15()
        taken_sf15.add(sf15)
        docs.append({
            '_id': ObjectId(),
            'salesforce_id': sf15_to_sf18(sf15),
            'client_name': member_name,
            'is_consortium_lead': False,
            'assertion_service_enabled': False,
            'superadmin_enabled': False,
            'created_by': MEMBER_CREATED_BY,
            'created_date': now,
            'last_modified_by': MEMBER_CREATED_BY,
            'last_modified_date': now,
            'default_language': 'en',
            'active': True,
            'activated_date': now,
            'api_access_level': 'premium',
            '_class': member_class,
        })
    return docs


def build_test_user_docs(member_docs: List[dict], user_class: str, now: datetime) -> List[dict]:
    """One activated main-contact user per test member, shaped like user-service-2's User
    after activation (null fields omitted, as Spring Data does)."""
    docs = []
    for member, (_, first_name, last_name, email) in zip(member_docs, TEST_DATA):
        docs.append({
            '_id': ObjectId(),
            'password': TEST_USER_PASSWORD_HASH,
            'first_name': first_name,
            'last_name': last_name,
            'email': email,
            'activated': True,
            'activation_date': now,
            'lang_key': 'en',
            'member_id': str(member['_id']),
            'member_name': member['client_name'],
            'main_contact': True,
            'deleted': False,
            'admin': False,
            'mfa_enabled': False,
            'manage_api_creds_enabled': True,
            'created_by': MEMBER_CREATED_BY,
            'created_date': now,
            'last_modified_by': MEMBER_CREATED_BY,
            'last_modified_date': now,
            '_class': user_class,
        })
    return docs


class MemberStore:
    """Access to the member-service `member` collection."""

    def __init__(self, connection: MongoDBConnection):
        self.collection = connection.get_collection(MONGO_COLLECTION)

    def find_all(self) -> List[dict]:
        try:
            return list(self.collection.find(
                {}, {'_id': 1, 'salesforce_id': 1, 'client_name': 1, 'api_access_level': 1,
                     'created_by': 1, '_class': 1, 'created_date': 1}
            ))
        except OperationFailure as e:
            logger.error(f"Failed to query members: {e}")
            raise

    def count_test_members(self) -> int:
        return self.collection.count_documents({'created_by': MEMBER_CREATED_BY})

    def insert(self, docs: List[dict]) -> int:
        return len(self.collection.insert_many(docs).inserted_ids)

    def delete_test_members(self) -> int:
        return self.collection.delete_many({'created_by': MEMBER_CREATED_BY}).deleted_count


class UserStore:
    """Access to the user-service `jhi_user` collection (a different database)."""

    def __init__(self, connection: MongoDBConnection):
        self.collection = connection.client[USER_DATABASE][USER_COLLECTION]

    def emails(self) -> Set[str]:
        return {(d.get('email') or '').lower() for d in self.collection.find({}, {'email': 1})}

    def user_class(self) -> str:
        """_class of the newest user, so test users look like the ones the app writes."""
        newest = self.collection.find_one({'_class': {'$exists': True}}, {'_class': 1},
                                          sort=[('created_date', -1)])
        return newest['_class'] if newest else DEFAULT_USER_CLASS

    def insert(self, docs: List[dict]) -> int:
        return len(self.collection.insert_many(docs).inserted_ids)

    def count_test_users(self) -> int:
        return self.collection.count_documents({'created_by': MEMBER_CREATED_BY})

    def delete_test_users(self) -> int:
        return self.collection.delete_many({'created_by': MEMBER_CREATED_BY}).deleted_count


def member_class_of(docs: List[dict]) -> str:
    """_class of the newest member, so test members look like the ones the app writes."""
    with_class = [d for d in docs if d.get('_class')]
    newest = max(with_class, key=lambda d: d.get('created_date') or datetime.min, default=None)
    return newest['_class'] if newest else DEFAULT_MEMBER_CLASS


def export_members(docs: List[dict]) -> Tuple[List[dict], int]:
    """Return (exported members in Mongo natural order, skipped count)."""
    exported: List[dict] = []
    skipped = 0
    for doc in docs:
        raw_id = doc.get('_id')
        sf = doc.get('salesforce_id')
        sf = sf.strip() if isinstance(sf, str) else sf
        if not raw_id or not sf:
            skipped += 1
            continue
        exported.append({
            'member_id': str(raw_id),
            'salesforce_id': sf,
            'member_name': doc.get('client_name'),
            'api_access_level': doc.get('api_access_level'),
            'test_member': doc.get('created_by') == MEMBER_CREATED_BY,
        })
    return exported, skipped


def write_json(payload: dict, output_path: Path) -> None:
    """Write atomically: tmp file then rename, so a partial file is never left behind."""
    output_path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = output_path.with_suffix(output_path.suffix + '.tmp')
    with tmp_path.open('w', encoding='utf-8') as fh:
        json.dump(payload, fh, ensure_ascii=False, indent=2)
        fh.write('\n')
    os.replace(tmp_path, output_path)


def confirm() -> bool:
    try:
        response = input("\nDo you want to proceed? (yes/no): ").strip().lower()
    except (KeyboardInterrupt, EOFError):
        response = ''
    if response not in ('yes', 'y'):
        logger.info("\n Operation cancelled by user")
        return False
    return True


def describe_target(mongo: MongoDBConnection):
    nodes = ', '.join(f'{host}:{port}' for host, port in mongo.client.nodes) or 'unknown'
    logger.info("  Mongo: %s (%s.%s, %s.%s)", nodes, MONGO_DATABASE, MONGO_COLLECTION,
                USER_DATABASE, USER_COLLECTION)


def export(args, docs: List[dict]) -> None:
    members, skipped = export_members(docs)
    test_members = sum(1 for m in members if m['test_member'])
    write_json({
        'source': {'mongo_database': MONGO_DATABASE, 'collection': MONGO_COLLECTION},
        'stats': {
            'members_total': len(docs),
            'members_exported': len(members),
            'members_skipped': skipped,
            'test_members': test_members,
        },
        'members': members,
    }, args.output)
    logger.info("\n Members exported: %d (%d test member(s)), skipped: %d",
                len(members), test_members, skipped)
    logger.info(" JSON written: %s", args.output)
    logger.info("\nNext: copy this file into the api-credentials-service container and run")
    logger.info("  python3 /scripts/test-data/create_client_details_test_data.py "
                "--members-file <path-in-container> --dry-run")


def run_seed(args, mongo: MongoDBConnection) -> int:
    store = MemberStore(mongo)
    user_store = UserStore(mongo)
    now = datetime.now(timezone.utc)

    docs = store.find_all()
    existing_members = store.count_test_members()
    existing_users = user_store.count_test_users()
    if existing_members or existing_users:
        logger.info("\n Test data already exists (%d member(s), %d user(s)); nothing inserted. "
                    "Run --cleanup first to recreate it.", existing_members, existing_users)
        export(args, docs)
        return 0

    taken_names = {(d.get('client_name') or '').strip().lower() for d in docs}
    name_clashes = [name for name, _, _, _ in TEST_DATA if name.lower() in taken_names]
    taken_emails = user_store.emails()
    email_clashes = [email for _, _, _, email in TEST_DATA if email in taken_emails]
    if name_clashes or email_clashes:
        logger.error(" Cannot create the test data: already used by existing (non-test) data: %s",
                     ', '.join(name_clashes + email_clashes))
        return 1

    taken_sf15 = {d['salesforce_id'].strip()[:SF15_LEN]
                  for d in docs if isinstance(d.get('salesforce_id'), str)}
    member_docs = build_test_member_docs(taken_sf15, member_class_of(docs), now)
    user_docs = build_test_user_docs(member_docs, user_store.user_class(), now)

    logger.info("\n" + "=" * 80)
    logger.info("PLAN (password for all users: %s)", TEST_USER_PASSWORD)
    logger.info("=" * 80)
    for member, user in zip(member_docs, user_docs):
        logger.info("  %-36s %-14s %s", member['client_name'],
                    f"{user['first_name']} {user['last_name']}", user['email'])

    if args.dry_run:
        logger.info("\n Dry run: nothing inserted; the exported file will not contain the test members.")
        export(args, docs)
        return 0

    logger.info("\n" + "=" * 80)
    logger.info("  WARNING: This will insert %d member(s) and %d user(s) into:",
                len(member_docs), len(user_docs))
    describe_target(mongo)
    logger.info("=" * 80)
    if not confirm():
        return 0
    logger.info(" Inserted %d test member(s) into %s.%s",
                store.insert(member_docs), MONGO_DATABASE, MONGO_COLLECTION)
    logger.info(" Inserted %d test user(s) into %s.%s",
                user_store.insert(user_docs), USER_DATABASE, USER_COLLECTION)
    export(args, store.find_all())
    return 0


def run_cleanup(args, mongo: MongoDBConnection) -> int:
    store = MemberStore(mongo)
    user_store = UserStore(mongo)
    count = store.count_test_members()
    user_count = user_store.count_test_users()

    logger.info("\n" + "=" * 80)
    logger.info("CLEANUP")
    logger.info("=" * 80)
    logger.info(" Test members: %d", count)
    logger.info(" Test users:   %d", user_count)
    if not (count or user_count):
        logger.info("\n No test members or users found. Nothing to do.")
        return 0
    if args.dry_run:
        logger.info("\n Dry run: nothing deleted.")
        return 0

    logger.info("\n" + "=" * 80)
    logger.info("  WARNING: This will delete %d test member(s) and %d test user(s) from:", count, user_count)
    describe_target(mongo)
    logger.info("=" * 80)
    if not confirm():
        return 0

    logger.info(" Deleted %d test user(s) from %s.%s",
                user_store.delete_test_users(), USER_DATABASE, USER_COLLECTION)
    logger.info(" Deleted %d test member(s) from %s.%s",
                store.delete_test_members(), MONGO_DATABASE, MONGO_COLLECTION)
    logger.info("\n Cleanup completed")
    return 0


def parse_arguments():
    parser = argparse.ArgumentParser(
        description='Mongo half of the client_details test data: '
                    'create/remove the test members and users, and export members to JSON.',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Create the test members and users, and export all members
  # Default output: scripts/test-data/logs/client_details_test_data_members.json
  python create_client_details_test_data.py

  # Explicit output path
  python create_client_details_test_data.py --output /tmp/client_details_test_data_members.json

  # No Mongo writes; export the current members only
  python create_client_details_test_data.py --dry-run

  # Remove the test members and users created by this script
  python create_client_details_test_data.py --cleanup

Then run api-credentials-service's scripts/test-data/create_client_details_test_data.py
with --members-file.

Environment Variables:
  SPRING_DATA_MONGODB_URI  - MongoDB connection string (member portal)
        """
    )
    parser.add_argument('--cleanup', action='store_true',
                        help='Delete the test members and test users created by this script.')
    parser.add_argument('--output', type=Path, default=None,
                        help='Path of the JSON file to write. Default: '
                             '<script dir>/logs/client_details_test_data_members.json '
                             '(overwritten each run)')
    parser.add_argument('--dry-run', action='store_true',
                        help='Never prompt or write to Mongo (the JSON is still written).')
    args = parser.parse_args()
    args.output = (args.output.resolve() if args.output
                   else CURRENT_DIR / 'logs' / 'client_details_test_data_members.json')
    return args


def main():
    args = parse_arguments()
    config = Config()

    logger.info("=" * 80)
    logger.info("Client details test data, Mongo half (%s)", 'cleanup' if args.cleanup else 'create + export')
    logger.info("=" * 80)
    if not args.cleanup:
        logger.info("Output file: %s", args.output)
    if args.dry_run:
        logger.info("Mode: DRY RUN (no Mongo writes)")
    logger.info("=" * 80 + "\n")

    mongo = MongoDBConnection(config.mongo_uri, MONGO_DATABASE)
    try:
        if not mongo.connect():
            logger.error("Failed to connect to memberservice MongoDB. Exiting.")
            return 1
        if args.cleanup:
            return run_cleanup(args, mongo)
        return run_seed(args, mongo)

    except KeyboardInterrupt:
        logger.info("\n\n Operation cancelled by user (Ctrl+C)")
        return 1
    except Exception as e:
        logger.error(f"\n Unexpected error: {e}", exc_info=True)
        return 1
    finally:
        mongo.disconnect()


if __name__ == '__main__':
    sys.exit(main())
