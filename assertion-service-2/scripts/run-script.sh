#!/bin/bash

set -euo pipefail

usage() {
    echo "Usage: $0 --username <user> --server <host> --assertion-docker <container> [--script <path>] [-- <script-args>]"
    echo ""
    echo "Options:"
    echo "  --username           SSH username"
    echo "  --server             SSH server hostname"
    echo "  --assertion-docker   Docker container name"
    echo "  --script             Python script path relative to /app/scripts (optional)"
    echo "  --                   Separator for script arguments (optional)"
    echo ""
    echo "Examples:"
    echo "  $0 --username dpalafox --server qa-server --assertion-docker assertion-app"
    echo "  $0 --username dpalafox --server qa-server --assertion-docker assertion-app --script test_connection.py"
    echo "  $0 --username dpalafox --server qa-server --assertion-docker assertion-app --script test-data/delete_documents.py"
    echo "  $0 --username dpalafox --server qa-server --assertion-docker assertion-app --script query-fixes/manage_organizations.py -- --target=0012i00000eiI3CAAU --source=0012i00000aQxlxAAC --merge"
    exit 1
}

USERNAME=""
SERVER=""
CONTAINER=""
SCRIPT_PATH=""
SCRIPT_ARGS=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --username)           USERNAME="$2"; shift 2 ;;
        --server)             SERVER="$2"; shift 2 ;;
        --assertion-docker)   CONTAINER="$2"; shift 2 ;;
        --script)             SCRIPT_PATH="$2"; shift 2 ;;
        --)                   shift; SCRIPT_ARGS="$@"; break ;;
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
    if [[ -n "$SCRIPT_ARGS" ]]; then
        ssh -t "$USERNAME@$SERVER" "docker exec -i $CONTAINER python3 /app/scripts/$SCRIPT_PATH $SCRIPT_ARGS"
    else
        ssh -t "$USERNAME@$SERVER" "docker exec -i $CONTAINER python3 /app/scripts/$SCRIPT_PATH"
    fi
else
    ssh -t "$USERNAME@$SERVER" "docker exec -it $CONTAINER /bin/bash"
fi