# AGENTS.md

Guidance for AI coding agents working in the ORCID Member Portal (`orcid-member-services`).

## Architecture: 3 Spring Boot services + 1 Angular UI

A microservice suite that helps organizations post affiliation data to ORCID records. All Java services are Spring Boot 3.5 / Java 21, persist to **MongoDB** (one database per service), and live in `org.orcid.mp.<service>` packages.

| Service | Dir | Port | Role |
|---|---|---|---|
| user-service | `user-service-2` | 9000 | **OAuth2 Authorization Server** — issues JWTs (`/oauth2/token`, `/oauth2/jwks`), owns users/auth |
| member-service | `member-service-2` | 9010 | Member orgs, Salesforce integration, reporting dashboards |
| assertion-service | `assertion-service-2` | 9020 | Affiliations/assertions, ORCID API integration, CSV upload |
| ui-2 | `ui-2` | 4200 (dev) | Angular 21 SPA |

**Security model is central:** `user-service` is the only authorization server. `member-service` and `assertion-service` are OAuth2 **resource servers** that validate JWTs against user-service's JWK set. Inter-service calls use the `client_credentials` grant with `scope: internal` (see `application.yaml` `internal-assertion-client` registration). Endpoints under `/internal/**` require `hasAuthority("SCOPE_internal")`; user requests carry `ROLE_*` authorities from the `authorities` JWT claim. See `assertion-service-2/.../config/SecurityConfig.java` for the dual scopes-vs-roles `JwtAuthenticationConverter`.

## Inter-service & external communication

- Java→Java calls use Spring `RestClient` beans qualified by name (e.g. `@Qualifier("userServiceRestClient")`), wrapped in thin `client/` classes — see `assertion-service-2/.../client/UserServiceClient.java`. Add new cross-service calls there, not inline in resources/services.
- UI→backend goes through proxy prefixes defined in `ui-2/src/proxy.conf.json`: `/userservice`, `/memberservice`, `/assertionservice` (prefix stripped via `pathRewrite`), plus `/oauth2` and `/.well-known` to user-service. Angular services set `resourceUrl = '/memberservice'` etc. (see `ui-2/src/app/member/service/member.service.ts`).
- External: Salesforce (member-service), ORCID public/member API + Mailgun (assertion-service). All config is injected via `APPLICATION_*` env vars (see `docker-compose.yml`).

## Java package conventions (per service, under `org.orcid.mp.<service>`)

`rest/` (REST controllers named `*Resource`, e.g. `AssertionResource`), `service/`, `repository/` (Spring Data Mongo), `domain/` (Mongo documents), `dto/`+`mapper/`, `client/` (outbound calls), `config/`, `security/`, `cron/` (scheduled jobs), `upload/`+`csv/`, `validation/`. Errors throw `BadRequestAlertException`-style exceptions from `error/`. Field injection with `@Autowired` is the prevailing style.

## Developer workflows

- Run a Java service locally: `cd <service>-2 ; ./mvnw` (or `.\mvnw.cmd` on Windows). Build a jar: `./mvnw package`.
- Run Java tests: `cd <service>-2 ; ./mvnw test`.
- UI dev server: `cd ui-2 ; npm run start` (binds 0.0.0.0:4200). Unit tests: `npm test` (Karma/Jasmine, single run). E2E: Cypress (`cypress/`). Lint+format: `npm run format` (eslint --fix + prettier).
- Full Docker stack: `./docker-build.sh` — builds all three jars with `-DskipTests`, then `docker compose build && up -d`. Requires a local `.env` (copy from `.env.example`).
- All Java services expose JVM debug on port 5005 in Docker (see `_JAVA_OPTIONS` in `docker-compose.yml`).
- Note: there is no `oauth2-service` despite the README mention — auth lives in `user-service-2`.

## i18n (do not skip)

The UI ships **localized builds per locale** (`cs, de, en, es, fr, it, ja, ko, pt, ru, zh-CN, zh-TW`). UI translations are Angular xlf in `ui-2/src/i18n/messages*.xlf`; regenerate with `npm run extract`. Java services have parallel `src/main/resources/i18n/messages_*.properties`. When adding user-facing strings, add the key to the base file AND keep locale files in sync.

## Conventions

- Java formatting: `eclipse_formatter.xml`; JS/TS: prettier + `@angular-eslint`. IntelliJ users: `intellij_codestyle.xml`.
- Keep `.env` uncommitted; `.env.example` stays a safe template.
- Git commits: never add a `Co-Authored-By` / AI attribution trailer.

