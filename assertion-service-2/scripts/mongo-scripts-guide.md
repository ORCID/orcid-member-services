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

Use `manage_organizations.py` to update or merge organizations, reassigning all references (assertions, orcid records, send_notifications_request, users) from one member to another. Records are matched by the internal `member_id` (member `_id`), not by `salesforce_id`, because some records may not have a `salesforce_id`.

```bash
# Reassign records without deleting source
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script query-fixes/manage_organizations.py -- --target=<target_member_id> --source=<source_member_id>

# Merge organizations (deletes source member after reassigning all references)
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script query-fixes/manage_organizations.py -- --target=<target_member_id> --source=<source_member_id> --merge
```

Notes:
- Use `--target` for the destination member_id (member `_id`, must exist).
- Use `--source` for the member_id whose records will be reassigned.
- Records are matched by `member_id`; each record's `salesforce_id` is set to the target member's `salesforce_id` (when the target member has one).
- When `--merge` is used, the source member's `client_id` becomes the surviving target member's `client_id` (applied to all related documents), and the source member is deleted after verification.
- `--force_update` is not supported when keying by member_id.
- The script will prompt for confirmation before modifying the database.
