#!/usr/bin/env python3
"""
Test MongoDB connection and display database information.

Usage:
    python test_connection.py
    python test_connection.py --database assertionservice
"""

import argparse
from pathlib import Path
import sys

CURRENT_DIR = Path(__file__).resolve().parent
UTILS_DIR = "utils"

if str(UTILS_DIR) not in sys.path:
    sys.path.insert(0, str(UTILS_DIR))

# Import shared modules
from db_connection import MongoDBConnection
from config import Config

def test_database_connection(uri: str, database_name: str) -> bool:
    """
    Test connection to a specific database.

    Returns:
        True if connection successful, False otherwise
    """
    print("\n" + "="*80)
    print(f"Testing Connection: {database_name}")
    print("="*80)

    connection = MongoDBConnection(uri, database_name)

    try:
        if not connection.connect():
            print(f"❌ Failed to connect to {database_name}")
            return False

        print(f"✅ Successfully connected to database: {database_name}")

        # List collections
        collections = connection.db.list_collection_names()
        print(f"\nCollections found: {len(collections)}")

        if collections:
            for coll_name in sorted(collections):
                count = connection.db[coll_name].count_documents({})
                print(f"  • {coll_name}: {count:,} documents")
        else:
            print("  (No collections found)")

        return True

    except Exception as e:
        print(f"❌ Error: {e}")
        return False
    finally:
        connection.disconnect()


def parse_arguments():
    parser = argparse.ArgumentParser(description='Test MongoDB connection')
    parser.add_argument('--mongo-uri', help='MongoDB URI (overrides env)')
    parser.add_argument('--database', default='memberservice', help='Database to test (default: memberservice)')
    return parser.parse_args()


def main():
    args = parse_arguments()

    config = Config()
    mongo_uri = args.mongo_uri or config.mongo_uri
    database = args.database

    print("="*80)
    print("MongoDB Connection Test")
    print("="*80)
    print(f"URI: {mongo_uri[:30]}..." if len(mongo_uri) > 30 else f"URI: {mongo_uri}")
    print(f"Database: {database}")

    success = test_database_connection(mongo_uri, database)

    print("\n" + "="*80)
    if success:
        print("✅ Connection test passed")
        return 0
    else:
        print("❌ Connection test failed")
        return 1


if __name__ == '__main__':
    sys.exit(main())
