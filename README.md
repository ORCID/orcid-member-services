# ORCID Member Portal

The ORCID Member Portal is a new suite of tools intended to help organizations make the most of their ORCID membership. This application is currently under development and has not yet been released.

The first phase of development includes features that simplify the process of posting affiliation information (employment, education, etc) to researchers’ ORCID records.

The ORCID Member Portal is a suite of tools that help organizations make the most of their ORCID membership — in particular, posting affiliation information (employment, education, etc.) to researchers' ORCID records.

## Architecture

Three Spring Boot services (Spring Boot 3.5 / **Java 21**) plus an Angular 21 SPA. Each Java service owns its own MongoDB database and lives under the `org.orcid.mp.<service>` package.

| Service | Dir | Port | Role |
|---|---|---|---|
| user-service | `user-service-2` | 9000 | **OAuth2 Authorization Server** — issues JWTs, owns users/auth/MFA |
| member-service | `member-service-2` | 9010 | Member orgs, Salesforce integration, reporting dashboards |
| assertion-service | `assertion-service-2` | 9020 | Affiliations/assertions, ORCID API integration, CSV upload |
| ui-2 | `ui-2` | 4200 (dev) | Angular 21 SPA |

`user-service` is the only authorization server; `member-service` and `assertion-service` are OAuth2 resource servers that validate JWTs against user-service's JWK set (`/oauth2/jwks`). See `AGENTS.md` for the detailed architecture and conventions.

## Prerequisites

- **JDK 21** (each service ships a Maven wrapper, `./mvnw` / `.\mvnw.cmd`)
- **Node.js** + **npm** (Angular CLI 21; installed locally via `npm install`)
- **MongoDB** (Community Edition) running on `localhost:27017`
- [MongoDB Compass](https://www.mongodb.com/products/compass) recommended for inspecting data

## Environment configuration

Copy `.env.example` to a local `.env` and fill in the values for your environment. Keep `.env` uncommitted; `.env.example` stays a safe template. Config is injected into the services via `APPLICATION_*` / `SPRING_*` env vars (see `docker-compose.yml`).

## Running locally

Start MongoDB, then run each service from its directory (Windows: use `.\mvnw.cmd`):

```bash
cd user-service-2      ; ./mvnw   # http://localhost:9000
cd member-service-2    ; ./mvnw   # http://localhost:9010
cd assertion-service-2 ; ./mvnw   # http://localhost:9020
```

> **IMPORTANT!** For running locally without an email server connected, disable mail health check for oauth2-services before starting. Edit [oauth2-service/src/main/resources/config/application.yml](https://github.com/ORCID/orcid-member-services/blob/master/oauth2-service/src/main/resources/config/application.yml#L60) and set health - mail - enabled to false.

Start the UI dev server (proxies to the services via `ui-2/src/proxy.conf.json`):

```bash
cd ui-2
npm install
npm run start          # http://localhost:4200
```

## Testing

- Java: `cd <service>-2 ; ./mvnw test`
- UI unit tests (Karma/Jasmine, single run): `cd ui-2 ; npm test`
- UI e2e (Cypress): see `ui-2/cypress/`
- UI lint + format: `npm run format`

## Docker-based setup

```bash
./docker-build.sh
```

This builds all three service JARs (`-DskipTests`), runs `docker compose build`, and starts the containers detached. Requires a local `.env`. All Java services expose JVM debug on port 5005 in Docker.

## Notes

- Keep `.env` local and uncommitted.
- `.env.example` should remain a safe template for new contributors.