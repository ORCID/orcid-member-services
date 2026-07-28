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

## Manage Organizations

Use `manage_organizations.py` to update or merge organizations, reassigning all references (assertions, orcid records, send_notifications_request, users) from one Salesforce Organization ID to another.

```bash
# Update member without deleting source
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script query-fixes/manage_organizations.py -- --target=<target_sf_id> --source=<source_sf_id>

# Merge organizations (deletes source member after updating all references)
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script query-fixes/manage_organizations.py -- --target=<target_sf_id> --source=<source_sf_id> --merge

# Force update source member's salesforce_id
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script query-fixes/manage_organizations.py -- --target=<target_sf_id> --source=<source_sf_id> --force_update
```

Notes:
- Use `--target` for the destination Salesforce Organization ID (must exist).
- Use `--source` for the organization to update.
- When `--merge` is used, the script also updates `client_id` on all related documents and deletes the source member after verification.
- When `--force_update` is used, the script updates the source member's `salesforce_id` to the target.
- The script will prompt for confirmation before modifying the database.
