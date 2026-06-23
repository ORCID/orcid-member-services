#!/bin/bash

set -euo pipefail

usage() {
    echo "Usage: $0 --username <user> --server <host> --assertion-docker <container> [--script <path>]"
    echo ""
    echo "Options:"
    echo "  --username           SSH username"
    echo "  --server             SSH server hostname"
    echo "  --assertion-docker   Docker container name"
    echo "  --script             Python script path relative to /app/scripts (optional)"
    echo ""
    echo "Examples:"
    echo "  $0 --username dpalafox --server qa-server --assertion-docker assertion-app"
    echo "  $0 --username dpalafox --server qa-server --assertion-docker assertion-app --script test_connection.py"
    echo "  $0 --username dpalafox --server qa-server --assertion-docker assertion-app --script test-data/delete_documents.py"
    exit 1
}

USERNAME=""
SERVER=""
CONTAINER=""
SCRIPT_PATH=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --username)           USERNAME="$2"; shift 2 ;;
        --server)             SERVER="$2"; shift 2 ;;
        --assertion-docker)   CONTAINER="$2"; shift 2 ;;
        --script)             SCRIPT_PATH="$2"; shift 2 ;;
        -h|--help)            usage ;;
        *)                    echo "Unknown option: $1"; usage ;;
    esac
done

if [[ -z "$USERNAME" || -z "$SERVER" || -z "$CONTAINER" ]]; then
    echo "Error: --username, --server, and --assertion-docker are required"
    echo ""
    usage
fi

if [[ -n "$SCRIPT_PATH" ]]; then
    ssh "$USERNAME@$SERVER" "docker exec $CONTAINER python3 /app/scripts/$SCRIPT_PATH"
else
    ssh -t "$USERNAME@$SERVER" "docker exec -it $CONTAINER /bin/bash"
fi
