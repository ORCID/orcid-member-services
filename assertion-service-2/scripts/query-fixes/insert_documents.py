#!/usr/bin/env python3
"""
MongoDB Insert Documents Script

This script inserts documents into a specified MongoDB collection using
data defined in a JSON file. It is intended for persisting test data
for validation and troubleshooting workflows.

Related ticket:
https://app.clickup.com/t/9014437828/PD-5160

Parameters
----------
--database
    Name of the MongoDB database containing the collection.

--collection
    Collection name where the data will be inserted.

--file_name
    Path to a JSON file containing one document or a list of documents to insert.

Behavior
--------
The script reads document data from the provided JSON file and inserts
it into the specified collection.

Example JSON format:
[
  {
    "email": "sideshow.bob.orcid@mailinator.com",
    "affiliation_section": "SERVICE"
  }
]

Usage
-----
python insert_documents.py --database <database> --collection <collection> --file_name <file.json>

Examples
--------
python3 insert_documents.py --file_name test/assertions.json
python3 insert_documents.py --collection orcid_record --file_name test/orcid_records.json
python3 insert_documents.py --database memberservice --collection member --file_name test/members.json
python3 insert_documents.py --database userservice --collection jhi_user --file_name test/users.json
"""

import argparse
import sys
from typing import List, Dict, Any, Optional

from bson import json_util
from pymongo.errors import OperationFailure

from logger_config import setup_logger
from db_connection import MongoDBConnection
from config import Config


logger = setup_logger(__name__, log_file='insert_documents.log')


def remove_mongo_oid(document: Dict[str, Any]) -> Dict[str, Any]:
    cleaned_document = document.copy()

    if (
            "_id" in cleaned_document
            and isinstance(cleaned_document["_id"], dict)
            and "$oid" in cleaned_document["_id"]
    ):
        cleaned_document.pop("_id")

    return cleaned_document


class InsertDocuments:

    def __init__(self, connection: MongoDBConnection, collection_name: str, file_name: str):
        self.connection = connection
        self.collection = connection.get_collection(collection_name)
        self.file_name = file_name

    def analyze_documents(self) -> Optional[List[Dict[str, Any]]]:
        """
        Read document data from JSON file.

        Returns:
            A list of documents, or None if the file is invalid.
        """
        try:
            with open(self.file_name, 'r', encoding='utf-8') as f:
                documents = json_util.loads(f.read())

            if isinstance(documents, dict):
                cleaned_documents = [remove_mongo_oid(documents)]
                logger.info("Loaded 1 document from JSON file")
                return cleaned_documents

            if isinstance(documents, list):
                if not all(isinstance(document, dict) for document in documents):
                    logger.error(
                        f"Error: {self.file_name} must contain only JSON objects."
                    )
                    return None

                cleaned_documents = [
                    remove_mongo_oid(document) for document in documents
                ]
                logger.info(f"Loaded {len(cleaned_documents)} documents from JSON file")
                return cleaned_documents

            logger.error(
                f"Error: {self.file_name} must contain either a JSON object "
                f"or a JSON array of objects."
            )
            return None

        except FileNotFoundError:
            logger.error(f"Error: {self.file_name} not found.")
            return None
        except ValueError as e:
            logger.error(f"Error: {self.file_name} contains invalid JSON. {e}")
            return None

    def print_report(self, documents: List[Dict[str, Any]]):
        if not documents:
            logger.info("No documents found")
            return

        logger.info("\n" + "=" * 80)
        logger.info("DOCUMENT INSERT REPORT")
        logger.info("=" * 80)
        logger.info(f"Documents loaded: {len(documents)}")
        logger.info(f"Target collection: {self.collection.name}")
        logger.info("=" * 80)

    def insert_documents(self, documents: List[Dict[str, Any]]) -> int:
        """
        Insert documents into the collection.

        Returns:
            Number of documents successfully inserted
        """
        if not documents:
            logger.info("No documents to insert")
            return 0

        try:
            logger.info(f"\nInserting {len(documents)} document(s)...")

            if len(documents) == 1:
                result = self.collection.insert_one(documents[0])
                logger.info(f"Successfully inserted 1 document. (ID: {result.inserted_id})")
                return 1

            result = self.collection.insert_many(documents)
            logger.info(f"Successfully inserted {len(result.inserted_ids)} documents")
            return len(result.inserted_ids)

        except OperationFailure as e:
            logger.error(f"Failed to insert documents: {e}")
            return 0
        except Exception as e:
            logger.error(f"Unexpected error during insertion: {e}")
            return 0


def parse_arguments():
    parser = argparse.ArgumentParser(
        description='Insert documents into a MongoDB collection',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python insert_documents.py
  python insert_documents.py --database assertionservice --collection assertion --file_name data.json

Environment Variables:
  MONGO_URI or MONGO_DB            - MongoDB connection string
  MONGO_DATABASE or DATABASE       - Database name
  MONGO_COLLECTION or COLLECTION   - Collection name
  FILE_NAME                        - JSON file name
        """
    )

    parser.add_argument('--mongo-uri', help='MongoDB URI (overrides env)')
    parser.add_argument('--database', help='MongoDB database name (overrides env)')
    parser.add_argument('--collection', help='MongoDB collection name')
    parser.add_argument('--file_name', help='File containing document(s) to insert')

    return parser.parse_args()


def main():
    args = parse_arguments()

    config = Config()

    mongo_uri = args.mongo_uri or config.mongo_uri
    database = args.database or config.mongo_database
    collection = args.collection or config.mongo_collection
    file_name = args.file_name or config.file_name

    logger.info("=" * 80)
    logger.info("MongoDB Insert Documents Script")
    logger.info("=" * 80)
    logger.info(f"MongoDB URI: {mongo_uri[:20]}..." if len(mongo_uri) > 20 else f"MongoDB URI: {mongo_uri}")
    logger.info(f"Database: {database}")
    logger.info(f"Collection: {collection}")
    logger.info(f"Filename: {file_name}")
    logger.info("=" * 80 + "\n")

    connection = MongoDBConnection(mongo_uri, database)

    try:
        if not connection.connect():
            logger.error("Failed to connect to MongoDB. Exiting.")
            return 1

        inserter = InsertDocuments(connection, collection, file_name)

        documents = inserter.analyze_documents()

        if not documents:
            logger.info("\nNo documents to insert.")
            return 0

        inserter.print_report(documents)

        logger.info("\n" + "=" * 80)
        logger.info("WARNING: This will modify the database!")
        logger.info(f"{len(documents)} document(s) will be inserted into collection '{collection}'")
        logger.info("=" * 80)

        try:
            response = input("\nDo you want to proceed? (yes/no): ").strip().lower()
            if response not in ['yes', 'y']:
                logger.info("\nOperation cancelled by user")
                return 0
        except (KeyboardInterrupt, EOFError):
            logger.info("\n\nOperation cancelled by user")
            return 1

        inserted_count = inserter.insert_documents(documents)

        if inserted_count == 0:
            logger.error("No documents were inserted")
            return 1

        logger.info("\n" + "=" * 80)
        logger.info("Script completed successfully")
        logger.info("=" * 80)
        return 0

    except KeyboardInterrupt:
        logger.info("\n\nOperation cancelled by user (Ctrl+C)")
        return 1
    except Exception as e:
        logger.error(f"\nUnexpected error: {e}", exc_info=True)
        return 1
    finally:
        connection.disconnect()


if __name__ == '__main__':
    sys.exit(main())
