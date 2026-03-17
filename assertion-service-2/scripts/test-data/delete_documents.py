#!/usr/bin/env python3
"""
MongoDB Data Cleanup Script

This script removes documents from specified MongoDB collections using
filters defined in a JSON file. It is intended for targeted documents of
invalid, obsolete, or incorrect data.

Related ticket:
https://app.clickup.com/t/9014437828/PD-4941

Parameters
----------
--database
    Name of the MongoDB database containing the collections.

--collections
    One or more collection names to clean up.

--file_name
    Path to a JSON file containing the document filters to delete.

Behavior
--------
For each specified collection, the script reads the filters from the
provided JSON file and deletes documents that match those filters.

Example JSON format:
[
  { "_id": "603f675aec57dc0008ae0952" },
  { "email": "test@orcid.org" }
]

Usage
-----
python delete_documents.py --database <database> --collections <collection1> [collection2 ...] --file_name <file.json>

Examples
--------
python delete_documents.py --database assertionservice --collections assertions orcid_record --file_name json/delete.json
"""

import argparse
import json
import sys
from pathlib import Path
from typing import List, Dict, Any
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


class DeleteDocuments:

    def __init__(self, connection: MongoDBConnection, collection_name: str, file_name: str):
        self.connection = connection
        self.collection = connection.get_collection(collection_name)
        self.file_name = file_name

    def find_problematic_collections(self) -> List[Dict[str, Any]]:
        """
        Find documents of the JSON provided.

        Returns:
            List of problematic documents
        """
        try:
            with open(self.file_name, 'r') as f:
                items = json.load(f)
        except FileNotFoundError:
            print(f"Error: {self.file_name} not found.")
            return
        except json.JSONDecodeError:
            print(f"Error: {self.file_name} contains invalid JSON.")
            return

        cleaned_items = [self.prepare_item(item) for item in items]

        query = {"$or": cleaned_items}

        logger.info(query)

        try:
            logger.info(f"Searching for documents in collection '{self.collection.name}' where '{query}'")
            documents = list(self.collection.find(query))
            for doc in documents:
                logger.info(f"Document with _id '{doc['_id']}' is problematic")
            logger.info(f"Found {len(documents)} documents to fix")
            return documents
        except OperationFailure as e:
            logger.error(f"Failed to query documents: {e}")
            return []
        except Exception as e:
            logger.error(f"Unexpected error during query: {e}")
            return []

    def print_report(self, documents: List[Dict[str, Any]]):
        if not documents:
            logger.info("No problematic documents found")
            return

        logger.info("\n" + "="*80)
        logger.info("PROBLEMATIC DOCUMENTS REPORT")
        logger.info("="*80)

        status_counts = {}
        for doc in documents:
            status = doc.get('_id', 'Unknown')
            status_counts[status] = status_counts.get(status, 0) + 1

        logger.info(f"\nTotal documents to fix: {len(documents)}")

        logger.info("\n" + "="*80)

    def fix_documents(self, documents: List[Dict[str, Any]]) -> int:
        """
        Fix the status of problematic documents.

        Returns:
            Number of documents successfully updated
        """
        if not documents:
            logger.info("No documents to fix")
            return 0

        logger.info(f"\n Applying fixes to {len(documents)} documents...")

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
  FILE_NAME - Collection name (default: json/delete.json)
        """
    )

    parser.add_argument('--mongo-uri', help='MongoDB URI (overrides env)')
    parser.add_argument('--database', help='MongoDB database name (overrides env)')
    parser.add_argument('--collections', help='MongoDB collection(s) name')
    parser.add_argument('--file_name', help='File that contains the documents to delete')

    return parser.parse_args()


def main():
    args = parse_arguments()

    config = Config()

    mongo_uri = args.mongo_uri or config.mongo_uri
    database = args.database or config.mongo_database
    collections = args.collections or config.mongo_collection
    file_name = args.file_name or config.file_name

    logger.info("="*80)
    logger.info("MongoDB Delete Documents Script")
    logger.info("="*80)
    logger.info(f"MongoDB URI: {mongo_uri[:20]}..." if len(mongo_uri) > 20 else f"MongoDB URI: {mongo_uri}")
    logger.info(f"Database: {database}")
    logger.info(f"Collection: {collections}")
    logger.info(f"Filename: {file_name}")
    logger.info("="*80 + "\n")

    connection = MongoDBConnection(mongo_uri, database)

    try:
        if not connection.connect():
            logger.error("Failed to connect to MongoDB. Exiting.")
            return 1

        fixer = DeleteDocuments(connection, collections, file_name)

        documents = fixer.find_problematic_collections()

        fixer.print_report(documents)

        if not documents:
            logger.info("\n No fixes needed. All documents are correct.")
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

        deleted_count = fixer.fix_documents(documents)

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
