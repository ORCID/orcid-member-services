#!/usr/bin/env python3
"""
Database connection module for ORCID scripts.

Provides MongoDB connection handling with proper error handling and validation.
"""

import logging
from typing import Optional, List
from pymongo import MongoClient
from pymongo.database import Database
from pymongo.collection import Collection
from pymongo.errors import ConnectionFailure, OperationFailure, ServerSelectionTimeoutError


logger = logging.getLogger(__name__)


class MongoDBConnection:
    """
    MongoDB connection manager with automatic connection handling.

    Usage:
        connection = MongoDBConnection(uri, database_name)
        if connection.connect():
            collection = connection.get_collection("my_collection")
            # Use collection...
            connection.disconnect()
    """

    def __init__(
        self,
        uri: str,
        database_name: str,
        timeout_ms: int = 5000
    ):
        self.uri = uri
        self.database_name = database_name
        self.timeout_ms = timeout_ms
        self.client: Optional[MongoClient] = None
        self.db: Optional[Database] = None

    def connect(self) -> bool:
        try:
            logger.info(f"Connecting to MongoDB database: {self.database_name}")

            # Create client with timeout
            self.client = MongoClient(
                self.uri,
                serverSelectionTimeoutMS=self.timeout_ms
            )

            self.client.admin.command('ping')

            self.db = self.client[self.database_name]

            if self.database_name not in self.client.list_database_names():
                logger.warning(f"Database '{self.database_name}' does not exist yet")

            logger.info(" Successfully connected to MongoDB")
            return True

        except ServerSelectionTimeoutError as e:
            logger.error(f" Connection timeout: {e}")
            logger.error("Check if MongoDB is accessible and the URI is correct")
            return False
        except ConnectionFailure as e:
            logger.error(f" Failed to connect to MongoDB: {e}")
            return False
        except Exception as e:
            logger.error(f" Unexpected error during connection: {e}")
            return False

    def disconnect(self):
        if self.client is not None:
            self.client.close()
            logger.info("Disconnected from MongoDB")
            self.client = None
            self.db = None

    def get_collection(self, collection_name: str) -> Optional[Collection]:
        if self.db is None:
            logger.error("Not connected to database. Call connect() first.")
            return None

        return self.db[collection_name]

    def __enter__(self):
        self.connect()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.disconnect()


def create_connection(
    uri: str,
    database: str,
    timeout_ms: int = 5000
) -> Optional[MongoDBConnection]:
    """
    Create and connect to MongoDB database.

    Convenience function that creates a connection and verifies it works.

    Args:
        uri: MongoDB connection URI
        database: Database name
        timeout_ms: Connection timeout in milliseconds

    Returns:
        Connected MongoDBConnection instance or None if connection failed
    """
    connection = MongoDBConnection(uri, database, timeout_ms)

    if connection.connect():
        return connection
    else:
        return None
