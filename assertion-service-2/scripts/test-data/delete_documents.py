#!/usr/bin/env python3
"""
MongoDB Data Cleanup Script

This script removes affiliations for a target member from MongoDB.

Related ticket:
https://app.clickup.com/t/9014437828/PD-4941

Parameters
----------
--database
    Name of the MongoDB database containing the collections.

--collections
    One or more collection names to clean up.

--member-id
    Internal member ID. When provided, the script deletes all affiliations
    listed for that member in Affiliation Manager (assertion.member_id).

--member-salesforce-id
    Salesforce member ID. The script resolves it via memberservice.member,
    then deletes all affiliations listed in Affiliation Manager for the
    resolved member ID.

Behavior
--------
The script builds the delete query from member scope and removes all matching
documents in the target collection.

Usage
-----
python delete_documents.py --database assertionservice --collections assertion --member-id <member_id>
python delete_documents.py --database assertionservice --collections assertion --member-salesforce-id <sf_id>

Examples
--------
python delete_documents.py --database assertionservice --collections assertion --member-id 689f6f2abc1234def56789ab
"""

import argparse
import re
import sys
from pathlib import Path
from typing import List, Dict, Any, Optional
from pymongo.errors import OperationFailure
from bson import ObjectId

CURRENT_DIR = Path(__file__).resolve().parent
UTILS_DIR = CURRENT_DIR.parent / "utils"

if str(UTILS_DIR) not in sys.path:
    sys.path.insert(0, str(UTILS_DIR))

from logger_config import setup_logger
from db_connection import MongoDBConnection
from config import Config


logger = setup_logger(__name__, log_file='delete_documents.log')

SALESFORCE_ID_PATTERN = re.compile(r"^[a-zA-Z0-9]{18}$")


class DeleteDocuments:

    def __init__(
            self,
            connection: MongoDBConnection,
            collection_name: str,
            inline_filters: Optional[List[Dict[str, Any]]] = None
    ):
        self.connection = connection
        self.collection = connection.get_collection(collection_name)
        self.inline_filters = inline_filters or []

    def _load_items(self) -> Optional[List[Dict[str, Any]]]:
        if self.inline_filters:
            return self.inline_filters

        logger.error("No inline filters were provided")
        return None

    def find_problematic_collections(self) -> List[Dict[str, Any]]:
        """
        Find documents of the JSON provided.

        Returns:
            List of problematic documents
        """
        items = self._load_items()
        if not items:
            return []

        cleaned_items = [self.prepare_item(item) for item in items]

        query = {"$or": cleaned_items}

        try:
            logger.info(f"Searching for documents in collection '{self.collection.name}' where '{query}'")
            documents = list(self.collection.find(query))
            for doc in documents:
                logger.info(f"Document with _id '{doc['_id']}' found")
                logger.info(f"Found {len(documents)} documents to delete")
            return documents
        except OperationFailure as e:
            logger.error(f"Failed to query documents: {e}")
            return []
        except Exception as e:
            logger.error(f"Unexpected error during query: {e}")
            return []

    def print_report(self, documents: List[Dict[str, Any]]):
        if not documents:
            logger.info("No assertions found")
            return

        logger.info("\n" + "="*80)
        logger.info("ASSERTIONS DOCUMENTS REPORT")
        logger.info("="*80)

        status_counts = {}
        for doc in documents:
            status = doc.get('_id', 'Unknown')
            status_counts[status] = status_counts.get(status, 0) + 1

        logger.info(f"\nTotal documents to delete: {len(documents)}")

        logger.info("\n" + "="*80)

    def delete_documents(self, documents: List[Dict[str, Any]]) -> int:
        """
        Delete the specified documents.

        Returns:
            Number of documents successfully deleted
        """
        if not documents:
            logger.info("No documents to delete")
            return 0

        logger.info(f"\n Deleting {len(documents)} documents...")

        document_ids = [doc['_id'] for doc in documents if '_id' in doc and doc['_id'] is not None]

        try:
            object_ids = [ObjectId(aid) if not isinstance(aid, ObjectId) else aid for aid in document_ids]

            result = self.collection.delete_many(
                {'_id': {'$in': object_ids}},
            )

            logger.info(f" Successfully deleted {result.deleted_count} documents")
            logger.info(f"   Matched: {result.deleted_count}")
            logger.info(f"   Deleted: {result.deleted_count}")

            return result.deleted_count

        except OperationFailure as e:
            logger.error(f" Failed to delete documents: {e}")
            return 0
        except Exception as e:
            logger.error(f" Unexpected error during deletion: {e}")
            return 0

    def verify_fixes(self) -> bool:
        logger.info("\n Verifying fixes...")
        remaining = self.find_problematic_collections()

        if not remaining:
            logger.info(" Verification passed: No problematic documents found")
            return True
        else:
            logger.warning(f" Verification failed: {len(remaining)} problematic documents still exist")
            return False

    def prepare_item(self, item):  # <-- Add 'self' here
        if "_id" in item and isinstance(item["_id"], str):
            try:
                # Ensure it's a valid 24-character hex string for ObjectId
                if len(item["_id"]) == 24:
                    item["_id"] = ObjectId(item["_id"])
            except Exception:
                pass
        return item


def parse_arguments():
    parser = argparse.ArgumentParser(
        description='Delete problematic documents from MongoDB collection',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Interactive mode
  python delete_documents.py

Environment Variables:
  MONGO_URI or MONGO_DB       - MongoDB connection string
  MONGO_DATABASE or DATABASE  - Database name (default: assertionservice)
  MONGO_COLLECTION or COLLECTION - Collection name (default: assertion)
        """
    )

    parser.add_argument('--mongo-uri', help='MongoDB URI (overrides env)')
    parser.add_argument('--database', help='MongoDB database name (overrides env)')
    parser.add_argument('--collections', help='MongoDB collection(s) name')
    parser.add_argument('--member-id', dest='member_id', help='Internal member ID to clean all affiliations for')
    parser.add_argument(
        '--member-salesforce-id',
        dest='member_salesforce_id',
        help='Salesforce ID to resolve member and clean all affiliations for that member'
    )

    return parser.parse_args()


def resolve_member_id_from_salesforce_id(mongo_uri: str, salesforce_id: str) -> Optional[str]:
    if not SALESFORCE_ID_PATTERN.match(salesforce_id):
        logger.error(
            "Invalid Salesforce ID: %r. Expected exactly 18 alphanumeric characters.",
            salesforce_id
        )
        return None

    members_connection = MongoDBConnection(mongo_uri, 'memberservice')
    try:
        if not members_connection.connect():
            logger.error("Failed to connect to memberservice database to resolve Salesforce ID")
            return None

        member_collection = members_connection.get_collection('member')
        if member_collection is None:
            logger.error("Could not access memberservice.member collection")
            return None

        member = member_collection.find_one({'salesforce_id': salesforce_id}, {'_id': 1, 'salesforce_id': 1})
        if not member:
            logger.error("No member found for salesforce_id=%s", salesforce_id)
            return None

        member_id = str(member.get('_id'))
        logger.info("Resolved member salesforce_id=%s to member_id=%s", salesforce_id, member_id)
        return member_id
    except Exception as e:
        logger.error("Unexpected error resolving member by salesforce id: %s", e)
        return None
    finally:
        members_connection.disconnect()


def main():
    args = parse_arguments()

    config = Config()

    mongo_uri = args.mongo_uri or config.mongo_uri
    database = args.database or config.mongo_database
    collections = args.collections or config.mongo_collection
    member_id = args.member_id
    member_salesforce_id = args.member_salesforce_id

    if member_id and member_salesforce_id:
        logger.error("Use either --member-id or --member-salesforce-id, not both")
        return 1

    if not member_id and not member_salesforce_id:
        logger.error("A member selector is required. Use --member-id or --member-salesforce-id")
        return 1

    if member_salesforce_id:
        resolved_member_id = resolve_member_id_from_salesforce_id(mongo_uri, member_salesforce_id)
        if not resolved_member_id:
            return 1
        member_id = resolved_member_id

    if member_id and collections != 'assertion':
        logger.warning(
            "Member cleanup is intended for Affiliation Manager data in 'assertion'. "
            "Current collection is '%s'.",
            collections,
        )

    inline_filters: List[Dict[str, Any]] = []
    if member_id:
        inline_filters = [{'member_id': member_id}]

    logger.info("="*80)
    logger.info("MongoDB Delete Documents Script")
    logger.info("="*80)
    logger.info(f"MongoDB URI: {mongo_uri[:20]}..." if len(mongo_uri) > 20 else f"MongoDB URI: {mongo_uri}")
    logger.info(f"Database: {database}")
    logger.info(f"Collection: {collections}")
    if member_id:
        logger.info(f"Member ID scope: {member_id}")
    logger.info("="*80 + "\n")

    connection = MongoDBConnection(mongo_uri, database)

    try:
        if not connection.connect():
            logger.error("Failed to connect to MongoDB. Exiting.")
            return 1

        fixer = DeleteDocuments(connection, collections, inline_filters)

        documents = fixer.find_problematic_collections()

        fixer.print_report(documents)

        if not documents:
            logger.info("\n No deletions needed.")
            return 0

        logger.info("\n" + "="*80)
        logger.info("  WARNING: This will modify the database!")
        logger.info(f"  {len(documents)} documents will be deleted from collection '{collections}'")
        logger.info("="*80)

        try:
            response = input("\nDo you want to proceed? (yes/no): ").strip().lower()
            if response not in ['yes', 'y']:
                logger.info("\n Operation cancelled by user")
                return 0
        except (KeyboardInterrupt, EOFError):
            logger.info("\n\n Operation cancelled by user")
            return 1

        deleted_count = fixer.delete_documents(documents)

        if deleted_count > 0:
            if not fixer.verify_fixes():
                logger.warning("\n Some documents may still need attention")
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
