# MONGO DB Scripts Setup Guide

## Run scripts via helper

Use `run-script.sh` from your local machine — no manual SSH or docker exec needed.

```bash
# Interactive shell inside the container
./run-script.sh --username <user> --server <host> --assertion-docker <container>

# Run a script
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script test_connection.py
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script query-fixes/backfill_orcid_record.py
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script test-data/delete_documents.py
```
