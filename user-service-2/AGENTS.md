# AGENTS.md — user-service

Identity & OAuth2 Authorization Server. Spring Boot 3.5 / Java 21, MongoDB DB `userservice`, port **9000**. Package root: `org.orcid.mp.user`. Read the root `AGENTS.md` first for cross-service conventions.

## Role & responsibilities

- **The only authorization server in the suite.** Issues JWTs and exposes `/oauth2/token`, `/oauth2/jwks`, `/oauth2/authorize`, and OIDC `/.well-known/**`. The other two services validate tokens against this JWK set.
- Owns users, roles/authorities, account activation, password reset, and **MFA**.

## Auth model (see `config/SecurityConfig.java`)

- Two `SecurityFilterChain`s: `@Order(1)` for the OAuth2/OIDC authorization-server endpoints, `@Order(2)` for the app (form login + resource-server JWT).
- Registered clients (`InMemoryRegisteredClientRepository`):
  - `mp-ui-client` — public SPA client, `authorization_code` + `refresh_token`, **PKCE required**, scopes `openid`/`MP`.
  - internal clients (`internalClientId`, `assertionServiceClientId`) — `client_credentials`, scope `internal`, short-lived (5 min) tokens for service-to-service calls.
- JWTs are RSA-signed from `application.security.jwt.{private-key,public-key,key-id}` (env: `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` / `JWT_KEY_ID`). The `jwtTokenCustomizer` adds the user's roles to the **`authorities`** claim; resource servers read that claim (no prefix) for `ROLE_*`.
- MFA lives in `security/` (`MfaAuthenticationProvider`, `MfaDetailsSource`, `MfaAuthenticationFailureHandler`, `MfaRequiredException`, `MfaInvalidCodeException`). Login flow: `/account/login` (form), with MFA details sourced via `MfaDetailsSource`.

## Key packages (under `org.orcid.mp.user`)

- `rest/` — `AccountResource` (login/reset/release version), `UserResource`, `InternalResource` (`/internal/**`, `SCOPE_internal`).
- `service/`, `repository/`, `domain/`, `dto/`+`mapper/`, `client/`, `cron/`, `upload/`, `validation/`, `error/`, `security/`.

## Conventions & gotchas

- CORS is restricted to `application.ui.baseUrl` with credentials allowed — UI origin must match `UI_BASE_URL`.
- Redirect/logout URIs are env-driven (`UI_REDIRECT_URI`, `UI_POST_LOGOUT_REDIRECT_URI`) and locale-prefixed (e.g. `/en/auth/callback`).
- Passwords use `BCryptPasswordEncoder`. Sensitive fields use `EncryptUtil` (`APPLICATION_ENCRYPT_SALT`/`_KEY`).
- Run: `./mvnw` (or `.\mvnw.cmd`). Test: `./mvnw test`.

