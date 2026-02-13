#!/usr/bin/env python3
"""
Handles loading configuration from environment variables with sensible defaults.
"""

import os
from typing import Dict, Optional


class Config:
    """
    Configuration manager for scripts.

    Reads configuration from environment variables with fallbacks.
    Supports both Docker Compose and local development setups.
    """

    def __init__(self):
        # MongoDB connection settings
        self.mongo_uri = self._get_mongo_uri()
        self.mongo_database = self._get_env('MONGO_DATABASE', 'assertionservice')
        self.mongo_collection = self._get_env('MONGO_COLLECTION', 'assertion')

    def _get_mongo_uri(self) -> str:
        return (
            os.getenv('MONGO_URI') or
            'mongodb://localhost:27017'
        )

    def _get_env(self, primary_key: str, default: str) -> str:
        return os.getenv(primary_key) or default

    def to_dict(self) -> Dict[str, str]:
        return {
            'mongo_uri': self.mongo_uri,
            'mongo_database': self.mongo_database,
            'mongo_collection': self.mongo_collection,
        }

    def __repr__(self) -> str:
        masked_uri = self.mongo_uri[:20] + '...' if len(self.mongo_uri) > 20 else self.mongo_uri
        return (
            f"database={self.mongo_database}, "
            f"collection={self.mongo_collection}, "
            f"uri={masked_uri})"
        )


def load_config() -> Config:
    return Config()


def get_mongo_uri(default: str = 'mongodb://localhost:27017') -> str:
    return os.getenv('MONGO_URI') or default


def get_database_name(default: str = 'caca') -> str:
    return os.getenv('MONGO_DATABASE') or default


def get_collection_name(default: str = 'assertion') -> str:
    return os.getenv('MONGO_COLLECTION') or default
