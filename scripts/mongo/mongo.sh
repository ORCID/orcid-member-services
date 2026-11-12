#!/bin/bash
# Local MongoDB for the member portal, seeded from a data dump in $SEED_DIR.
set -euo pipefail
cd "$(dirname "$0")"

export SEED_DIR=${SEED_DIR:-$HOME/.orcid-member-services/mongo-seed}

usage() {
  cat <<USAGE
Usage: $0 <command>

  start    Start MongoDB (seeds the data on first start)
  stop     Stop MongoDB, keeping the data
  reseed   Delete the data and re-import the dump
  status   Show the container status and document counts
  shell    Open mongosh in the container
  logs     Follow the MongoDB logs

Seed files are read from $SEED_DIR (override with SEED_DIR).
USAGE
}

require_seed() {
  if ! compgen -G "$SEED_DIR/*.json" >/dev/null; then
    echo "No *.json seed files in $SEED_DIR; see scripts/mongo/README.md" >&2
    exit 1
  fi
}

counts() {
  docker compose exec -T mongo mongosh --quiet "${SEED_DB:-orcid_db}" --eval '
    for (const c of db.getCollectionNames().sort()) print(`${db.getName()}.${c}: ${db[c].countDocuments()}`);'
}

case "${1:-}" in
  start)
    require_seed
    docker compose up -d --wait
    counts
    echo "MongoDB is running on mongodb://localhost:${MONGO_PORT:-27017}"
    ;;
  stop)
    docker compose stop
    ;;
  reseed)
    require_seed
    docker compose down -v
    docker compose up -d --wait
    counts
    ;;
  status)
    docker compose ps
    counts
    ;;
  shell)
    docker compose exec mongo mongosh "${@:2}"
    ;;
  logs)
    docker compose logs -f mongo
    ;;
  *)
    usage
    exit 1
    ;;
esac
