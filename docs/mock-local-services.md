# Test the frontend without Keycloak or Camunda

Start the Spring server with the **`mock-local`** profile. In IntelliJ, set
**Active profiles** to `mock-local` in the Spring Boot run configuration, or add
`--spring.profiles.active=mock-local` to program arguments.

From a terminal with Java 21+ and Maven:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=mock-local"
```

In a second terminal:

```powershell
cd src/main/frontend
npm run dev
```

Open **http://localhost:3000/login**. Select **`tehran_general_office_head`**
(the default) and click **ورود آزمایشی** to open the Figma dashboard at `/`.
The dropdown also contains the seeded admin, office heads, and experts; they
retain their existing landing pages, business roles, and database permissions.
No password is required in this local development mode. The API binds to
`127.0.0.1:8025`; the frontend proxies `/api` to it by default.

## Configuration and switching back

| Server configuration | Mock-local profile | Default / real mode |
| --- | --- | --- |
| `app.security.provider` / `AUTH_PROVIDER` | `mock` | `keycloak` |
| `camunda.client.enabled` / `CAMUNDA_CLIENT_ENABLED` | `false` | `true` |
| Database | `ef03.mock.sqlite` (`MOCK_DB_PATH` overrides) | Existing `DB_PATH` / `ef03.sqlite` |
| Keycloak bootstrap synchronization | Disabled | Existing configuration |
| Execution-paper inbox / destructive test utilities | Disabled | Existing configuration |

To reconnect, stop the server, remove the `mock-local` profile and any
`AUTH_PROVIDER=mock` or `CAMUNDA_CLIENT_ENABLED=false` environment overrides,
then restart normally with your existing Keycloak/Camunda settings. The frontend
reads `/api/v1/auth/config` at runtime; **no frontend rebuild is needed** to switch.
Old mock tokens will fail authentication in real mode and return you to login.
Changing modes requires a server restart. Do not expose passwordless mock login
outside your local development environment.

The separate SQLite database is created automatically and seeds the existing
sample users/permissions. It does not copy or modify your normal `ef03.sqlite`.
To inspect existing imported records, copy a stopped database to the mock file
before starting; dashboard statistics still come from that local database.
Existing user subjects and edited sample profiles are preserved during mock startup.

## What is mocked

- Identity: random opaque sessions, backed by existing local user profiles.
  Sessions expire after eight hours, disappear on restart, and are revoked by
  logout. Disabled/locked users cannot log in or use existing sessions. Roles
  come from the server database; authorization and `@PreAuthorize` checks stay active.
- Camunda: workers and the real client are disabled. The offline cartable returns
  an empty list. Task details/actions and engine commands report unavailable;
  this mode does not emulate BPMN execution, create fake tasks, or pretend to
  complete business workflows. Database-backed screens remain usable.
- Dashboard figures remain database values. Missing reporting integrations still
  show unavailable states. A visible Persian banner identifies offline/mock mode.

Mock login endpoints exist only when `app.security.provider=mock`:
`GET /api/v1/auth/mock/users`, `POST /api/v1/auth/mock/login` with
`{"username":"tehran_general_office_head"}`, and authenticated
`POST /api/v1/auth/mock/logout`. The config endpoint exposes mode names only.

## Verification

Backend tests cover opaque tokens, expiration, logout revocation, disabled/locked
users, opt-in bean registration, and a real HTTP server started without external
identity/workflow services. HTTP checks exercise role/permission enforcement,
dashboard statistics, empty cartable, and unavailable workflow operations.
The frontend production build and direct ESLint checks cover the changed files;
browser smoke tests exercise actual local login, dashboard navigation, and logout.

Run the browser check with Node 22+ after starting both servers:

```powershell
src/main/frontend/node/node.exe scripts/verify-mock-login.cjs
```

The script defaults to frontend port 3100; set `FRONTEND_URL` if you use port 3000.
`CHROME_PATH` can override the installed Chrome executable location. It uses an
isolated browser profile and does not intercept API responses.
