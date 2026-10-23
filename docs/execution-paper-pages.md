# Execution-paper list and details

List design source: Figma file `YtvKeaR3IsRNcjfb6RXez7`, V.2 frame
[`572:28611`](https://www.figma.com/design/YtvKeaR3IsRNcjfb6RXez7?node-id=572-28611),
its retrieved layout/row properties, and the screenshot supplied by the user.
The desktop frame is 1440 × 1024. The list has the V.2 search toolbar, collapsible
filters, nine data columns plus a per-row eye/details button, refresh timestamp,
and navigation order.

The standalone detail page follows the four screenshots subsequently supplied by
the user: debt, actions, taxpayer, and file information. These screenshots supplied
the remaining design reference after the Figma connector reached its plan limit.
No identity-verification column, flag, or person-model field is included, as requested.

## Routes and behavior

- `/execution-papers`: server-paginated list, combined search, taxpayer type,
  workflow status, issue-date range, and exact debt/date sorting.
- `/execution-papers/{id}`: breadcrumb navigation, a single-paper summary table,
  and four keyboard-accessible tabs. Search, scope, sorting, and pagination are
  preserved when opening details and returning through the breadcrumb.
- The debt tab shows stored financial snapshots and links to other active papers
  for the same taxpayer. The actions tab shows seven workflow stages with an
  issue-details card, document view/print, audit history, and existing task links.
  Taxpayer and file tabs show real identity, contact, and organization information.
  Related-person refresh remains available in an expandable section.
- Printing uses the existing full document, hiding the app shell and open dialogs.
- The toolbar's **فیلترها** button reveals existing type, status, date, amount,
  and notification filters. Assigned/unassigned scopes remain in the status menu.
- Display settings control visible columns and row density; the guide describes
  search, sorting, and opening details. Both dialogs support Escape and keyboard focus.
- The issuance link in display settings opens the existing manual entry page and
  requires its actual `USER_PROFILE_CREATE` permission. No new issuance endpoint
  or workflow is introduced.

The shared app bar, drawer, menus, and Persian clock now live in `FigmaShell` and
are reused by the dashboard and these pages. Original SVG exports are local under
`public/images/execution-papers`; common header/navigation assets reuse their
original dashboard exports. SVG intrinsic dimensions are preserved.

## Data and access

The existing `/api/v1/execution-papers` (including assigned/unassigned), detail,
trace, external-request, and related-person endpoints remain the data sources.
The existing list API now accepts `search`, `taxpayerType` (`NATURAL`/`LEGAL`),
`issuedFrom`/`issuedTo` (Gregorian ISO dates, inclusive), `sortBy`
(`id`/`issuanceDate`/`totalDebt`/`taxYear`/`taxPeriod`), and `sortDirection`
(`asc`/`desc`). Existing filters are retained. List DTOs include actual
`issuanceDate`, taxpayer TIN (`taxpayerCode`, shown as **شناسه مودی**), the Persian
paper-type title from `paper_type.paper_title` (`taxType`, e.g. EF03/EF23 titles),
`taxYear`, and `taxPeriod`. Year and period come from the related
`final_tax_debt_paper.tax_year` and `tax_period_id` fields. Paper type uses its
canonical Persian title only if the lookup title is unavailable;
sample categories or periods from the design are not substituted for real data.
Missing values display a dash. Notification dates are not substituted for issue dates.

The detail response includes two `debtInformation.history` rows: the earliest
saved `debt_information_history` record, followed by the authoritative current
`debt_information` record, even when the last history entry is stale. With no
initial history, the first row explicitly shows unavailable amounts. The current
record is never presented as the initial record.

Each row maps `total_liability` to **اصل مالیات و عوارض**, `total_credits` to
**اعتبار مالیاتی**, `total_penalties` to **جرائم**, `total_remissions` to **بخشودگی**,
`total_payments` to **پرداختی**, and `outstanding_tax_debt` to **مانده بدهی**.
The six corresponding detail columns are returned per snapshot. Each total is
a button opening a breakdown table with a stored-total footer. XML fragments,
legacy item/value XML, and legacy JSON rows are supported. Labels come from
`docs/analysis/sanim-field-inventory.csv` via `debt-field-labels.json`.
Repeated rows and exact decimals are preserved; subtotal lines are not added
together. Empty or unreadable breakdowns keep their stored total visible.
`executionPaper.otherActivePapers` contains actual paper IDs/codes for the same
registered taxpayer, excluding the selected paper and completed/cancelled papers.

The calendar displays Persian dates and submits ISO dates. Identifiers retain
leading zeros and mixed-direction text uses `bdi`. Amounts remain exact decimal
strings in JavaScript. SQLite stores money as TEXT, so debt sorting and amount
filtering compare `BigDecimal` values before pagination, matching existing exact
amount-filter behavior rather than casting amounts to floating point.

Lists allow `REPORT_VIEW` or `EXECUTION_PAPER_LIST_VIEW`; details allow
`REPORT_VIEW` or `EXECUTION_PAPER_DETAIL_VIEW`; trace access follows existing
trace permissions. The ten replacement operational roles receive list, detail,
and trace viewing permissions as one-time demo defaults in both identity modes.
Administrators can change these defaults through `/admin/permissions`;
revocations survive restart. See [demo roles](demo-roles-and-access.md).
Broader reporting, administration, and issuance permissions are not granted by default.
A detail-only user can view the paper without gaining list access.
Loading, empty, error, retry, and permission states are explicit.

## Design/data limitations

- The project keeps local IranYekan fonts; Figma's Dana/Estedad fonts are absent.
- Workflow stages show completion only when issue data, current state, or trace
  events provide evidence. Unrepresented stages show **ثبت نشده**. No completion
  dates, current stages, actors, or operations are fabricated from the visual sample.
- Payment deadline remains unavailable when the existing response supplies none.
- The database stores tax and duty together in `total_liability`; this is shown
  as a combined total, with source components available through its details button.

## Verification

Production frontend build and direct ESLint checks cover changed frontend files.
`ExecutionPaperSearchIntegrationTest` exercises SQLite name/identifier/economic
code search, type and inclusive date filters, actual issue-date/source fields,
exact debt and year/period sorting across pages, invalid-range/sort rejection,
exact financial history, and filtering other active papers by taxpayer/state.
The latest debt-mapping run passed all nine cases of this integration suite,
including stale-history/current-table separation and missing initial history.

With the frontend running on port 3100, Node 22+ and Chrome can run:

```powershell
src/main/frontend/node/node.exe scripts/verify-execution-papers.cjs
```

This browser test uses intercepted API fixtures, not production records. It checks
1440 × 1024 geometry, every visible SVG's local file and intrinsic rendered size,
row details navigation, filters, display/guide dialogs, calendar, pagination,
all four detail tabs, related-paper/document/history dialogs, print styling,
errors/retry, empty and permission states, and a 390px viewport. Screenshots are
saved under `temp/`. Browser API data is intercepted, so these checks do not
claim an end-to-end connection to a running backend or external identity/workflow servers.
