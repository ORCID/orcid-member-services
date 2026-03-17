"""
ORCID Scripts Shared Modules

Common modules for ORCID database scripts to eliminate code duplication.

Modules:
- logger_config: Logging configuration
- db_connection: MongoDB connection handling

Usage:
    from logger_config import setup_logger
    from db_connection import MongoDBConnection
    from config import Config
"""

__version__ = '1.0.0'
__all__ = ['logger_config', 'db_connection', 'config']
