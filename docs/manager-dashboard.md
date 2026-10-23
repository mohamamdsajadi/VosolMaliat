# Figma dashboard

Route: `/`, for every signed-in role, including `ADMIN`. Both sidebar variants
label this entry “داشبورد” and link to the same Figma dashboard. The old admin
dashboard and role-based redirect to `/tasks` have been removed from this route.
Existing API authorization and permission checks for navigation remain in place.
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
| Statistics / collection amount | Authenticated `GET /api/v1/dashboard/stats`; supervisors/officers receive only their assigned execution-paper aggregates |
| Closed cases | Existing completed + cancelled execution-paper counts |
| Pending work / completed work | Current actionable Camunda tasks plus the local arrest-order task list; completed work counts distinct tasks/orders acted on by this user, not closed papers |
| Arrest candidates / local referrals | Eligible papers for the initiating head plus `/api/v1/arrest-orders?mine=true`; dashboard rows link to the local workflow |
| Arrest performance | Distinct orders acted on by this user / (their responsible orders + their eligible unstarted papers), counted once per order; zero when no work exists |
| Task links | Existing `/tasks/{taskKey}` page; no automatic workflow mutation |
| Category navigation | Existing tasks API's `taskName` filter |
| Priority | Only explicit `priority` or `taskPriority` variables; otherwise unknown |
| Monthly series, collection case count, recent activity, trends | Unavailable states: no existing reporting endpoint or agreed definitions |
| Notifications | Unavailable state: no notification-feed endpoint |
| Settings | Account menu and existing Keycloak logout |

Stats and tasks fail independently and support retry. Failed or missing values are
never replaced by zero or mock values. Decimal strings remain strings during
Persian number formatting. Mobile tables scroll horizontally within a focusable
region, and the drawer starts closed on narrow screens. Page instructions are available
through the info button. Pending and acted counts can overlap when a user has started
an order and still needs to approve it; forwarding preserves their action history.

## Verification

Production build: `npm run build` from `src/main/frontend` (uses bundled Node 22).
The repository's existing `npm run lint` invokes `next lint`, which Next 16 no longer
provides; the changed JavaScript files were linted directly through ESLint 9 using
the installed `eslint-config-next/core-web-vitals` configuration.
Browser checks use an isolated Chrome profile and intercepted API responses to
exercise the documented contracts, access rules, loading/error/empty states,
retry, identifiers, large decimal strings, asset loading, and responsive layout.
These checks do not validate a live Keycloak/backend deployment.
