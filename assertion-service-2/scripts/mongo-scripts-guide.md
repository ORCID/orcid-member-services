# MONGO DB Scripts Setup Guide

## Run scripts via helper

Use `run-script.sh` from your local machine — no manual SSH or docker exec needed.

```bash
# Interactive shell inside the container
./run-script.sh --username <user> --server <host> --assertion-docker <container>

# Run a script
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script test_connection.py
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script query-fixes/backfill_orcid_record.py
```

## Clean test data

Use one of the options below to remove all affiliations shown in Affiliation Manager for a single member.

```bash
# Option 1: use Salesforce member ID
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script "test-data/delete_documents.py --database assertionservice --collections assertion --member-salesforce-id <salesforce_id>"

# Option 2: use internal member ID (memberservice.member._id)
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script "test-data/delete_documents.py --database assertionservice --collections assertion --member-id <member_id>"
```

Notes:
- Use exactly one selector: `--member-salesforce-id` or `--member-id`.
- The script will prompt for confirmation before deleting data.
