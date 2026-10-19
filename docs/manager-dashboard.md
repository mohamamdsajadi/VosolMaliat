# General office dashboard

Route: `/`, after Keycloak login with `businessRole: GENERAL_OFFICE_HEAD`.
Admins retain the existing dashboard; other operational roles retain their task landing page.
No backend permissions are added. Execution-paper/report links respect existing permissions.

## Design

Source: Figma file `YtvKeaR3IsRNcjfb6RXez7`, frame `324:8876`, 1440 × 1024.
The desktop uses the frame's 84px app bar, 263px drawer, 284px secondary column,
20px grid gutters, 129px statistics cards, 321px task/performance panels, and
343px unit panel. Original Figma SVGs are stored in `public/images/dashboard`.
The source uses Dana and Estedad; the project retains its local IranYekan fonts.
No temporary asset URLs or sample design figures are used at runtime.

## Data and integration gaps

| Component | Current source / behavior |
| --- | --- |
| Profile / role / organization | `GET /api/v1/auth/me` |
| Statistics / collection amount | `GET /api/v1/dashboard/stats`, existing system-wide aggregates |
| Closed cases | Existing completed + cancelled execution-paper counts |
| Pending tasks / category badges | `GET /api/v1/cartable/my-tasks`, existing user access scope |
| Task links | Existing `/tasks/{taskKey}` page; no automatic workflow mutation |
| Category navigation | Existing tasks API's `taskName` filter |
| Priority | Only explicit `priority` or `taskPriority` variables; otherwise unknown |
| Performance, monthly series, collection case count, recent activity, trends | Unavailable states: no existing reporting endpoint or agreed definitions |
| Notifications | Unavailable state: no notification-feed endpoint |
| Settings | Account menu and existing Keycloak logout |

Stats and tasks fail independently and support retry. Failed or missing values are
never replaced by zero or mock values. Decimal strings remain strings during
Persian number formatting. Mobile tables scroll horizontally within a focusable
region, and the drawer starts closed on narrow screens.

## Verification

Production build: `npm run build` from `src/main/frontend` (uses bundled Node 22).
The repository's existing `npm run lint` invokes `next lint`, which Next 16 no longer
provides; the changed JavaScript files were linted directly through ESLint 9 using
the installed `eslint-config-next/core-web-vitals` configuration.
Browser checks use an isolated Chrome profile and intercepted API responses to
exercise the documented contracts, access rules, loading/error/empty states,
retry, identifiers, large decimal strings, asset loading, and responsive layout.
These checks do not validate a live Keycloak/backend deployment.
