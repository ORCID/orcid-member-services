# AGENTS.md — assertion-service

Affiliations/assertions service. Spring Boot 3.5 / Java 21, MongoDB DB `assertionservice`, port **9020**. Package root: `org.orcid.mp.assertion`. Read the root `AGENTS.md` first for cross-service conventions.

## Role & responsibilities

- Owns **assertions** (an affiliation a member organization posts to a researcher's ORCID record) and **OrcidRecord** (the researcher's id-token/permission state).
- Integrates with the **ORCID member API** to create/update/delete affiliations, and with **Mailgun** to email researchers (permission requests, notifications, CSV reports).
- This is an OAuth2 **resource server** — it does not issue tokens. It validates JWTs against user-service's JWK set (`application.yaml` → `jwk-set-uri`).

## Key packages (under `org.orcid.mp.assertion`)

- `rest/AssertionResource.java` — the main REST controller. Public endpoints (`/assertions/id-token`, `/assertions/record/**`, `/assertions/member/**`) are `permitAll()`; `/internal/**` requires `SCOPE_internal`; everything else needs a user JWT. See `config/SecurityConfig.java`.
- `client/` — outbound calls only, one thin class per target: `OrcidApiClient`, `MailgunClient`, `UserServiceClient`/`InternalUserServiceClient`, `MemberServiceClient`/`InternalMemberServiceClient`. Internal clients use the `internal-assertion-client` `client_credentials` registration; add new cross-service calls here, qualified by named `RestClient` beans.
- `upload/` + `csv/` — CSV affiliation bulk upload (`AssertionsCsvReader`, `AssertionsUpload*`) and CSV report generation (`csv/download/`).
- `cron/ScheduledJobsManager.java` — scheduled jobs (resend notifications, sync affiliations, member assertion stats). Cron expressions come from `APPLICATION_*_CRON` / `*_DELAY` env vars.
- `data/migration/` — **Mongock** changelogs (e.g. `CreateAssertionCompoundIndex`, `RevokedDateCorrection`). Add DB migrations here as new Mongock change units; never hand-edit data assumptions in services.
- `service/`, `repository/` (Spring Data Mongo), `domain/` (Mongo documents), `validation/` (org-id validators: ROR/GRID/Ringgold), `normalizer/`, `error/` (`BadRequestAlertException`), `security/` (`EncryptUtil`, `SecurityUtil`).

## Conventions & gotchas

- Field injection with `@Autowired` is the prevailing style.
- Sensitive values are encrypted with `EncryptUtil` (keys from `APPLICATION_ENCRYPT_SALT`/`_KEY`) — keep them encrypted at rest.
- ORCID token exchange is configured via `APPLICATION_TOKEN_EXCHANGE_*` env vars; mail via `APPLICATION_MAIL_*` (set `MAIL_TEST_MODE` when developing).
- Operational/DB helper scripts live in `scripts/` (Python; see `scripts/setup-scripts.md`) — for one-off data fixes, not application logic.
- Run: `./mvnw` (or `.\mvnw.cmd`). Test: `./mvnw test`.

