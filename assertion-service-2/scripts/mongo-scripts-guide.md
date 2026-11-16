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

## Export members for api-credentials-service client_details backfill

Use `migration/backfill_client_details_member_id_by_sfid.py` to export `memberservice.member`
(`_id`, `salesforce_id`) to a JSON file. It is read-only: MongoDB is not
modified. The api-credentials-service container cannot reach MongoDB, so its
`client_details.member_id` backfill reads this file instead.

```bash
# Default output: /app/scripts/migration/logs/members_by_sfid.json (overwritten each run)
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script migration/backfill_client_details_member_id_by_sfid.py

# Explicit output path
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script migration/backfill_client_details_member_id_by_sfid.py -- --output /tmp/members_by_sfid.json
```

Then follow the api-credentials-service `scripts/mongo-scripts-guide.md` section
*Backfill client_details.member_id by Salesforce ID* for steps 2–3 (copy the file and run
the import).

## Test members for the api-credentials-service client_details test data

Use `test-data/create_client_details_test_data.py` for the Mongo half of the client_details
test data. It creates one fixed set of test data, once: 3 members and one activated
main-contact user per member in `userservice.jhi_user` (table below). If the test data
already exists it inserts nothing (run `--cleanup` first to recreate it). Members and users
are tagged `created_by = pd-13632-test-data` for `--cleanup`. It then exports all members
(`_id`, `salesforce_id`, name, `api_access_level`, test flag) to JSON for
api-credentials-service's `test-data/create_client_details_test_data.py --members-file`.

| Member | User | Login | Password |
|---|---|---|---|
| Northwind University | Emma Clarke | `emma.clarke@northwind.example.org` | `test1234` |
| Riverside Institute of Technology | Liam Patel | `liam.patel@riverside.example.org` | `test1234` |
| Lakeshore Medical Research Centre | Sofia Moreno | `sofia.moreno@lakeshore.example.org` | `test1234` |

```bash
# Create the test data; default output: /app/scripts/test-data/logs/client_details_test_data_members.json (overwritten each run)
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script test-data/create_client_details_test_data.py

# Explicit output path
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script test-data/create_client_details_test_data.py -- --output /tmp/client_details_test_data_members.json

# No Mongo writes; export only
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script test-data/create_client_details_test_data.py -- --dry-run

# Delete the test members and their users
./run-script.sh --username <user> --server <host> --assertion-docker <container> --script test-data/create_client_details_test_data.py -- --cleanup
```

Then follow the api-credentials-service `scripts/mongo-scripts-guide.md` section
*Create client_details test data*.
