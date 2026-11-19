# Local MongoDB

Runs MongoDB in Docker for local development and seeds it from a data dump.

## Seed data

The dump is not in the repository. Put the exported JSON files in:

```
~/.orcid-member-services/mongo-seed/
```

Each file is a JSON array named `<service>.<collection>.json`, for example
`memberservice.member.json`. Every collection is imported into one database,
`orcid_db` by default. Set `SEED_DIR` to read the files from somewhere else.

## Usage

```
scripts/mongo/mongo.sh start    # start; imports the seed on first start
scripts/mongo/mongo.sh status   # container status and document counts
scripts/mongo/mongo.sh shell    # mongosh in the container
scripts/mongo/mongo.sh logs     # follow the logs
scripts/mongo/mongo.sh stop     # stop, keeping the data
scripts/mongo/mongo.sh reseed   # delete the data and import the seed again
```

The seed is imported only when the data volume is empty. After you change the
seed files, run `reseed`.

The services connect with `mongodb://host.docker.internal:27017/orcid_db`
(`MONGO_DB` in `.env`).

## Settings

| Variable        | Default                               |
| --------------- | ------------------------------------- |
| `SEED_DIR`      | `~/.orcid-member-services/mongo-seed` |
| `SEED_DB`       | `orcid_db`                            |
| `MONGO_PORT`    | `27017`                               |
| `MONGO_VERSION` | `7.0`                                 |
