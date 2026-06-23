# AGENTS.md — ui-2

Angular 21 SPA. Dev server on port **4200**. Read the root `AGENTS.md` first for cross-service conventions.

## Structure & patterns

- Feature modules under `src/app/`: `account/`, `affiliation/`, `member/`, `report/`, `user/`, `home/`, `landing-page/`, `layout/`, `error/`, plus `shared/`.
- Each feature follows the same shape: `*.component.ts/html/scss/spec.ts`, a `*.module.ts`, a `*.route.ts`, plus `model/` (interfaces) and `service/` (data access). Mirror this when adding features.
- **Services** are `@Injectable({ providedIn: 'root' })`, use `inject(HttpClient)`, and target a backend via `resourceUrl` proxy prefix — e.g. `MemberService.resourceUrl = '/memberservice'` (see `src/app/member/service/member.service.ts`). Use `/userservice`, `/memberservice`, `/assertionservice`; these are proxied (and prefix-stripped) per `src/proxy.conf.json`.
- **Models** use `I`-prefixed interfaces (`IMember`, `IAffiliation`) and `ISF*` for Salesforce-shaped data. Keep raw-vs-mapped SF types distinct (`ISFRaw*` → `SF*`).
- RxJS is used heavily (`BehaviorSubject` caches, `combineLatest`, `switchMap`). Build query params with `createRequestOption` from `src/app/shared/request-util.ts`. Paged responses use `shared/model/page.model`.
- Auth uses `angular-auth-oidc-client` against user-service (`/oauth2`, `/.well-known`).

## i18n (do not skip)
- The app is built **per locale**: `en` (source) plus `cs, es, fr, it, ja, ko, pt, ru, zh-CN, zh-TW`. Translations are Angular xlf in `src/i18n/messages*.xlf` (`messages.xlf` is the source/base). Locales are configured in `angular.json` (`i18n.locales` + `build.options.localize`); `development` builds only `en`.
- Mark template/code strings for translation (`i18n` attributes / `$localize`), then regenerate with `npm run extract` (merges into all locale files). Keep locale files in sync — don't leave new keys untranslated only in the base file.

## Workflows

- Dev server: `npm run start` (binds `0.0.0.0:4200`). HTTPS variant: `npm run start_ssl`.
- Unit tests (Karma/Jasmine, single run): `npm test`. Watch: `npm run test:watch`.
- E2E: Cypress (`cypress/`, fixtures in `cypress/fixtures/`).
- Lint + format before committing: `npm run format` (eslint --fix + prettier). Prettier config + `@angular-eslint` enforce style.
- Build (all locales): `npm run build`.

