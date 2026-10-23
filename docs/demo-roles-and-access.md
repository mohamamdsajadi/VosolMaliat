# Demo roles and access

The three execution-unit head, team supervisor, and execution officer accounts
in the active local demo are now linked to **اداره کل امورمالیاتی مرکز تهران**.
Ten existing papers are ready for arrest-order testing; see
[Tehran demo instructions](tehran-arrest-demo.md).

The current catalogue contains ten operational roles and `ADMIN`. The fifteen
old province sample profiles are removed by a one-time bootstrap migration.
The system audit actor and non-sample profiles are preserved. Both local SQLite
databases have backups under `temp/before-demo-roles*.sqlite`.

| Role | Demo username |
| --- | --- |
| دفتر وصول و اجرای مالیاتی | `demo_tax_collection_office` |
| رئیس اداره اجرائیات وصول و اجرای ویژه | `demo_special_execution_department_head` |
| رئیس کل سازمان امور مالیاتی | `demo_tax_organization_head` |
| رئیس واحد اجرائیات اداره کل مربوطه | `demo_directorate_execution_unit_head` |
| سرپرست هیئت اجرایی اداره کل مربوطه | `demo_directorate_execution_team_supervisor` |
| مأمور اجرا | `demo_execution_officer` |
| مدیرکل وصول و اجرای ویژه | `demo_special_collection_director_general` |
| معاون اجرائیات وصول و اجرای ویژه | `demo_special_execution_deputy` |
| معاون اجرائیات | `demo_execution_deputy` |
| مدیرکل امور مالیاتی اداره کل مربوطه | `demo_tax_directorate_director_general` |
| مدیر سامانه | `admin` |

With the `mock-local` Spring profile, choose an account on `/login`. No password,
Keycloak connection, or Camunda connection is required. The mock task inbox is
empty; this change does not fabricate workflow tasks. Normal Keycloak mode
continues to use its existing account synchronization settings; remote Keycloak
accounts were not deleted or verified during offline preparation.

## Permission panel

Open **نقش‌ها و دسترسی‌ها** in the sidebar, or `/admin/permissions`.
Select a role and toggle a permission. Each toggle saves immediately through
the existing permission APIs; the panel reports loading, errors, and success.
The administrator has all configured permissions and cannot have grants revoked.
Non-admin roles initially receive nine demo permissions: six task permissions
and list/detail/history viewing for execution papers. They receive no user or
permission administration rights by default. Revoked grants stay revoked after
restarting the server; bootstrap defaults are applied once.

Viewing the panel requires both `GROUP_PERMISSION_VIEW` and `PERMISSION_VIEW`.
Editing requires `GROUP_PERMISSION_UPDATE`. Page and action permissions are
shown separately because granting a list page does not automatically grant its
detail, audit, or management actions. The server enforces these permissions.

Default task scopes: central organization and special-collection roles have
national scope; directorate roles, the execution deputy, and the execution
officer use the user's general directorate. The local demo profiles use Tehran.
These defaults do not define a delegation hierarchy. The user is supplying the
approved handoffs separately; new delegation remains unconfigured until then.
Existing workflow variable names are retained for integration compatibility;
future workflow tasks use the new candidate role codes. Existing remote
Camunda task assignments have not been migrated.

`app.security.demo-users.enabled=false` disables recreation of demo accounts on
startup. This does not disable the role catalogue or permission management.

## Verification

Backend checks cover the enum catalogue, profile login/logout, task scope,
permission enforcement and revocation persistence, administrator protection,
and a SQLite CHECK migration that preserves profile IDs, audit references, and
indexes. The new browser check uses the live local backend through a forwarding
adapter, with no API fixtures:

```powershell
$env:BACKEND_URL='http://localhost:8081'
src/main/frontend/node/node.exe scripts/verify-role-permissions.cjs
```

It logs into all eleven accounts, checks the sidebar and role catalogue, toggles
and reloads a permission, checks protected administrator access, denies an
unauthorized account, and inspects 1440px and 390px layouts. Run this against a
disposable database copy because it changes and restores a demo grant.
