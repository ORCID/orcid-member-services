# AGENTS.md — member-service

Member organizations & reporting service. Spring Boot 3.5 / Java 21, MongoDB DB `memberservice`, port **9010**. Package root: `org.orcid.mp.member`. Read the root `AGENTS.md` first for cross-service conventions.

## Role & responsibilities

- Owns **member** organizations and **consortium** relationships (a consortium lead manages multiple consortium members).
- Integrates with **Salesforce** as the system of record for member/contact/org-id data, and serves **Holistics** reporting dashboards (signed dashboard URLs).
- OAuth2 **resource server** — validates user JWTs and `/internal/**` `SCOPE_internal` calls; does not issue tokens.

## Key packages (under `org.orcid.mp.member`)

- `rest/` — `MemberResource` (member CRUD/lookup), `ReportResource` (Holistics dashboard URLs), `InternalResource` (`/internal/**`, `SCOPE_internal` only, for service-to-service calls).
- `salesforce/` — Salesforce DTOs/domain: `MemberDetails`, `MemberContact(s)`, `MemberOrgId(s)`, `ConsortiumLeadDetails`, `ConsortiumMember`, `Country`/`State`, `BillingAddress`, `MemberUpdateData`. This is the integration boundary with Salesforce — keep SF-shaped types here.
- `report/ReportInfo.java` — report/dashboard descriptors. Dashboard URLs+secrets come from `APPLICATION_REPORTS_*` env vars (member/integration/consortia/affiliation dashboards).
- `client/` — outbound calls (one thin class per target, named `RestClient` beans). Add cross-service/Salesforce calls here, not inline.
- `service/`, `repository/` (Spring Data Mongo), `domain/`, `config/`, `security/`, `upload/`, `validation/`, `error/`.

## Conventions & gotchas

- Field injection with `@Autowired` is the prevailing style.
- Salesforce config is injected via `APPLICATION_SALESFORCE_*` env vars (base URL, client id/secret, login URL, username/password, token + client endpoints). ORCID API client credentials reuse `TOKEN_EXCHANGE_CLIENT_*`.
- Consortium vs. plain member is a recurring distinction — check whether a member is a consortium lead before assuming a flat org.
- Run: `./mvnw` (or `.\mvnw.cmd`). Test: `./mvnw test`.

