#!/bin/bash
# Runs once, on first start with an empty volume. Files are named <service>.<collection>.json;
# every collection goes into the one database.
set -euo pipefail

db=${SEED_DB:-orcid_db}

for file in /seed/*.json; do
  name=$(basename "$file" .json)
  collection=${name#*.}
  echo "Importing $db.$collection"
  mongoimport --quiet --db "$db" --collection "$collection" --jsonArray --file "$file"
done
