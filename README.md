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

### Testing emails locally

`member-service` sends email through the Mailgun HTTP API (not SMTP), so SMTP catchers such as MailHog won't see it. To check an email without sending anything, point the service at a small local server that pretends to be Mailgun and saves what it receives.

1. Start the fake Mailgun server (it listens on port 9999 and writes each email to `last-email.html` in the current directory):

   ```bash
   python - <<'EOF'
   from http.server import BaseHTTPRequestHandler, HTTPServer
   from urllib.parse import parse_qs

   class FakeMailgun(BaseHTTPRequestHandler):
       def do_POST(self):
           form = parse_qs(self.rfile.read(int(self.headers['Content-Length'])).decode())
           open('last-email.html', 'w', encoding='utf-8').write(form.get('html', [''])[0])
           print('TO:', form.get('to'), '| CC:', form.get('cc'), '| SUBJECT:', form.get('subject'), flush=True)
           self.send_response(200)
           self.end_headers()
           self.wfile.write(b'{"message":"Queued"}')

   HTTPServer(('localhost', 9999), FakeMailgun).serve_forever()
   EOF
   ```

2. In `member-service-2/.env`, set the mail URL and a sender address. Keep only one `APPLICATION_MAIL_API_URL` line, because a duplicate later in the file overrides the earlier one:

   ```properties
   APPLICATION_MAIL_API_URL=http://localhost:9999/messages
   APPLICATION_MAIL_FROM_ADDRESS=no-reply@example.org
   ```

   If `member-service` runs in Docker, use `http://host.docker.internal:9999/messages` instead.

3. Restart `member-service` so it picks up the `.env` values. When running from an IDE, make sure the run configuration loads `.env`, because the app does not read the file itself.
4. Trigger the email in the UI, then open `~/orcid-member-services/member-service-2/target/last-email.htmllast-email.html` in a browser. The server's terminal prints the To, CC and subject, and `member-service` logs `Preparing email <subject> for sending to ...` when it tries to send.

Things to know:

- **Recipients:** `application.mail.contactUpdateRecipient` (`APPLICATION_MAIL_CONTACT_UPDATE_RECIPIENT`) is the support address that receives or is CC'd on most emails. Other recipients are worked out per email from the member and user data, so they differ by email type. Check the TO and CC lines the fake server prints.
- **Triggers:** each email is sent from `MailService` after the action it describes succeeds, so the action has to succeed for an email to be sent. Some emails are only sent when something actually changed, so make a real change when testing.
- **Salesforce:** emails that look up recipients in Salesforce are affected if the Salesforce credentials are expired or unreachable. The lookup is skipped and a warning is logged, so check the TO and CC lines.
- **Real Mailgun instead:** set the real Mailgun `APPLICATION_MAIL_API_URL` and `APPLICATION_MAIL_API_KEY`. `APPLICATION_MAIL_TEST_MODE=true` makes Mailgun accept messages without delivering them, and the Mailgun dashboard (Sending > Logs) shows what was accepted. Set it to `false` only when you really want to deliver, and point `APPLICATION_MAIL_CONTACT_UPDATE_RECIPIENT` at your own address first.

## Docker-based setup

```bash
./docker-build.sh
```

This builds all three service JARs (`-DskipTests`), runs `docker compose build`, and starts the containers detached. Requires a local `.env`. All Java services expose JVM debug on port 5005 in Docker.

## Notes

- Keep `.env` local and uncommitted.
- `.env.example` should remain a safe template for new contributors.