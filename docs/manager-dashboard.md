# Dashboard reporting

Route `/` is shared by all signed-in roles. The existing RTL application shell,
navigation and account menu are retained. The report layout follows the approved
reference's information groups, using flat charts and the application's colours.
The desktop overview uses the available viewport height: summary cards and two
chart rows fit without page or panel scrolling at 1280×720 and larger desktop
sizes. On narrow screens (1200px or less), panels stack to retain readable text.
The shell adjustments apply only to the dashboard route.
Summary cards share an icon-and-label header and a separate value-and-unit row.
Report panels use equal financial columns, consistent heading dividers, darker
secondary text and shared surface, border and control tokens. A single responsive
stylesheet controls each breakpoint; chart slices and legends share one palette.
Report text is two pixels larger than the initial compact layout. The financial
row contains three separate reports so adjustments do not crowd debt composition.
All six statistical reports use the selected style 2 (simple doughnut): flat
colours, a roughly 60% inner-to-outer radius and narrow white slice separators.
Doughnut centres are empty. Percentages appear only in hover tooltips; static
percentage captions are removed. Chart headers contain only a larger bold title
(16px, 18px on wide screens), with no faded subtitle or subtotal line. The freed
space enlarges the charts while keeping the desktop overview within one screen.
The three count reports retain their paper-count captions below the plot in
normal layout flow. Exact values stay in adjacent keyboard-accessible legends;
hover tooltips show exact values and share percentages.

## Overview and drilldown

- Summary cards: distinct taxpayers with visible papers, execution-paper count,
  current outstanding balance, and cumulative recorded payments.
- Financial composition: a doughnut for combined principal tax and duties, and penalties.
  Both monetary values appear in the legend; share percentages appear on hover.
- Adjustments: a dedicated pie chart for remissions (teal) and credits
  (purple), with labelled amounts and units and independent financial drilldowns.
  They remain debt reductions, not positive slices of gross debt.
- Payment overview: a pie for recorded payments and remaining balance. Its percentage is
  `paid / (paid + remaining)`, not an assumed enforcement collection rate.
  Payments may predate execution-paper issuance. Component-level payment
  allocation and historical percentage trends are not inferred.
- Paper-status chart: an inner doughnut for workflow states and a separate outer ring
  for notified/not-notified counts. Both use their own denominator and drilldowns;
  notification counts are not added to workflow-state slices. The coloured
  notification selectors identify the ring and remain available for keyboard use.
- Unit comparison: top ten by paper count; general directorates for national
  roles, tax affairs offices for local roles. The enlarged pie has no visible
  legend. Hovering a slice shows the unit name, count, exact balance and payments.
  Each slice is independently keyboard-focusable and activates with Enter or Space.
  Shares are relative to the displayed top ten, not all offices.
- Enforcement path: a pie of papers with actually issued arrest orders versus
  papers without an issued order (including drafts). These groups partition the
  visible execution papers; the total paper count is not added as another slice.
  Seizure and auction remain visible as unavailable rows below the pie, with null
  values and disabled selectors until those modules provide data.
  Its four labels are `دستور بازداشت صادر شده`, `دستور بازداشت صادر نشده`,
  `توقیف شده` and `فروش رفته`. The latter two retain their unavailable state;
  renaming them does not infer a seizure or sale from an order's issuance.

Chart sectors, equivalent keyboard-accessible legend buttons and summary
cards open a paginated dialog without extending the overview. Details include paper code, taxpayer name and
identifiers, state, tax office, issuance date, remaining balance and, for monetary
selectors, the contribution to the selected total. Paper links require existing
detail permission. Five rows per page (three on viewports below 740px high) keep
desktop dialogs within the screen. Escape/backdrop/close dismiss the dialog and
native dialog focus returns to the initiating control. Closing does not change data.
Reporting dialogs explicitly use fixed viewport positioning, automatic margins
and content-sized height so the global CSS margin reset cannot move them to a
corner. Their horizontal and vertical centres remain at the viewport centre
through loading, paging, errors and responsive layout changes.

## Contracts and financial precision

`GET /api/v1/dashboard/overview` returns the current reporting aggregates.
`GET /api/v1/dashboard/details?group=...&key=...&page=0&size=10` returns matching papers
and their total, using the same scope and financial definitions. Groups are `all`,
`finance`, `state`, `notification`, `office`, and `enforcement`. Filters are validated;
page must be nonnegative, size must be 1–100. Unknown groups/states and unavailable
enforcement stages return 400. Details require `REPORT_VIEW` or `EXECUTION_PAPER_LIST_VIEW`.
The `enforcement` keys `ISSUED` and `NOT_ISSUED` filter actual `issuedAt` presence;
the latter includes existing drafts whose orders have not yet been issued.

Reports use a lightweight projection of current debt rows, never the sum of
cumulative debt-history snapshots. If multiple execution papers reference the same
debt, their paper count remains separate while financial totals count that debt once.
In a monetary detail list, only the first matching paper contributes that shared
amount; later papers show zero contribution, consistently across page boundaries.
Independent status/stage selections can overlap and should not be added together.

Money is aggregated with `BigDecimal` and sent as exact decimal strings. Unknown
balances propagate as explicit null instead of partial totals or invented zeros.
Financial details include unknown rows so their total remains unknown as well.
Summary cards, financial labels, totals and detail lists display full numbers
in rials with Persian digit grouping and their stored decimal precision. Amounts
are not abbreviated to millions or billions, rounded, or converted to JavaScript
floating-point numbers. Monetary labels use isolated left-to-right digit runs
inside the RTL layout. Chart geometry uses relative
weights calculated with `BigInt`; negative/unknown values are not forced into a pie.

## Scope

Admins and national roles see all reporting rows. Local roles are restricted to
their assigned general directorate. Supervisors and execution officers are also
restricted to their assigned execution-paper IDs. A local account without an
organization receives no rows. All selectors reuse these restrictions, including
arbitrary office IDs supplied in a request. Aggregate access does not grant access
to individual paper records.

## Personal work

The personal-work button in the bottom toolbar opens a dialog retaining pending/completed action counts,
arrest performance and the existing paged `GET /api/v1/dashboard/pending-arrest-papers`
table. Drafts awaiting the current user's approval precede eligible unstarted papers.
Its links continue to open the exact draft or the candidate filtered by paper code.
Opening the dashboard does not start workflows or mark paper details as read.

Existing `GET /api/v1/dashboard/stats` remains available for personal-work data.
Stats, reporting and pending-list failures are handled independently with retry.

## Verification

Production frontend build: run `node scripts/run-next.cjs build` from `src/main/frontend`
with the bundled Node 22 runtime. Targeted Java integration tests cover exact totals,
shared debt, pagination contributions, history exclusion, unknown balances, role and
organization boundaries, detail permissions, actual issued orders and invalid filters.

`scripts/verify-dashboard-overview.cjs` checks the rendered production frontend using
intercepted API contracts: sector selection, matching request filters, pagination,
paper links, errors/retries, empty/unknown values, unavailable stages, restricted
controls and desktop/mobile overflow. With all twelve states and ten offices,
it verifies page and panel height at 1280×720, 1366×768, 1440×900 and 1920×1080,
plus notification-ring filters, both enforcement groups, office keyboard access
without a legend, dialog pagination, Escape and focus restoration.
The overview is also checked at 320, 360, 390, 768, 1000, 1100 and 1200 pixels for
horizontal overflow, clipped financial values and overflowing panel contents.
It saves viewport screenshots under ignored-by-convention
`temp/` (do not stage generated files). Backend correctness is verified separately;
the browser script does not contact a live identity provider or mutate databases.
