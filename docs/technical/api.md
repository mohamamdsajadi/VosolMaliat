# API

Last Updated: 2026-07-15

## هدف سند

مستندسازی endpointهای مهم REST.

## مخاطبان

توسعه‌دهندگان frontend/backend، QA، تیم integration.

## Authentication

| Method | URL | Controller | هدف | امنیت |
|---|---|---|---|---|
| GET | `/api/v1/auth/me` | `AuthController` | اطلاعات کاربر جاری | authenticated |
| GET | `/api/v1/auth/me/permissions` | `AuthController` | مجوزهای کاربر جاری | authenticated |

## Users و Permissions

| Method | URL | Controller | Body/Param | Permission |
|---|---|---|---|---|
| POST | `/api/v1/users` | `UserManagementController` | `CreateAppUserProfileRequest` | `USER_PROFILE_CREATE` |
| GET | `/api/v1/users` | همان | - | `USER_PROFILE_VIEW` |
| GET | `/api/v1/users/{id}` | همان | path id | `USER_PROFILE_VIEW` |
| PUT | `/api/v1/users/{id}` | همان | `UpdateAppUserProfileRequest` | `USER_PROFILE_UPDATE` |
| PATCH | `/api/v1/users/{id}/status` | همان | `UserStatusRequest` | `USER_PROFILE_ENABLE_DISABLE` |
| PATCH | `/api/v1/users/{id}/lock` | همان | `UserLockRequest` | `USER_PROFILE_ENABLE_DISABLE` |
| PATCH | `/api/v1/users/{id}/tax-unit` | همان | `AssignTaxUnitRequest` | `USER_PROFILE_ASSIGN_ORGANIZATION` |
| DELETE | `/api/v1/users/{id}` | همان | path id | `USER_PROFILE_UPDATE` |
| GET | `/api/v1/permissions` | `PermissionController` | - | `PERMISSION_VIEW` |
| POST | `/api/v1/permissions` | همان | `CreatePermissionRequest` | `PERMISSION_ASSIGN` |
| GET | `/api/v1/group-permissions` | همان | `groupName?` | `GROUP_PERMISSION_VIEW` |
| POST | `/api/v1/group-permissions` | همان | `AssignGroupPermissionRequest` | `GROUP_PERMISSION_UPDATE` |
| DELETE | `/api/v1/group-permissions/{id}` | همان | path id | `GROUP_PERMISSION_UPDATE` |

## Masterdata

Endpointهای `/api/v1/masterdata/general-directorates`, `/tax-affairs-offices`, `/tax-units` برای GET/POST/PUT/DELETE وجود دارند. مشاهده با `USER_PROFILE_VIEW` یا `USER_PROFILE_ASSIGN_ORGANIZATION` و تغییر با `USER_PROFILE_ASSIGN_ORGANIZATION` کنترل می‌شود.

## Execution Papers

| Method | URL | هدف | Permission |
|---|---|---|---|
| GET | `/api/v1/execution-papers` | لیست/جستجو با فیلتر و pagination | `REPORT_VIEW` |
| GET | `/api/v1/execution-papers/{id}` | جزئیات | `REPORT_VIEW` یا `EXECUTION_PAPER_DETAIL_VIEW` |
| GET | `/api/v1/execution-papers/{id}/trace` | trace و audit | `REPORT_VIEW` یا `AUDIT_VIEW` یا `EXECUTION_PAPER_TRACE_VIEW` |
| GET | `/api/v1/execution-papers/assigned` | برگ‌های assigned | `REPORT_VIEW` |
| GET | `/api/v1/execution-papers/unassigned` | برگ‌های unassigned | `REPORT_VIEW` |

## Cartable

| Method | URL | هدف | Permission |
|---|---|---|---|
| GET | `/api/v1/cartable/my-tasks` | taskهای کاربر | `TASK_VIEW_OWN` |
| GET | `/api/v1/cartable/candidate-tasks` | taskهای candidate | `TASK_VIEW_CANDIDATE` |
| GET | `/api/v1/cartable/tasks/{taskKey}` | جزئیات task | task view authorities |
| POST | `/api/v1/cartable/tasks/{taskKey}/claim` | claim | `TASK_CLAIM` |
| POST | `/api/v1/cartable/tasks/{taskKey}/unclaim` | unclaim | `TASK_UNCLAIM` |
| POST | `/api/v1/cartable/tasks/{taskKey}/complete` | complete | `TASK_COMPLETE` |
| POST | `/api/v1/cartable/tasks/{taskKey}/assign` | assign مستقیم | `TASK_ASSIGN` |
| GET | `/api/v1/cartable/tasks/{taskKey}/delegation-targets` | مقصدهای ارجاع | `TASK_DELEGATE` |
| POST | `/api/v1/cartable/tasks/{taskKey}/delegate` | تکمیل مرحله و ارجاع | `TASK_DELEGATE` |

## Rules و Data Entry

- `/api/v1/admin/rejection-rules`: CRUD قوانین صدور.
- `/api/v1/admin/inform-rejection-rules`: CRUD قوانین ابلاغ.
- `/api/v1/admin/data-entry/summary`: خلاصه داده دستی.
- `/api/v1/admin/data-entry/finalized-debt-papers`: ایجاد مستقیم.
- `/api/v1/admin/data-entry/finalized-debt-papers/send`: ارسال به workflow.
- `/api/v1/admin/data-entry/execution-papers`: ایجاد دستی برگ اجرا.

## Integration و Bootstrap

- `/api/v1/debt-papers/issue`: شروع فرایند صدور.
- `/api/v1/fake-electronic-notification/requests`: لیست fake notification.
- `/api/v1/fake-electronic-notification/requests/{id}/confirm`: تایید.
- `/api/v1/fake-electronic-notification/callback`: callback public.
- `/api/v1/fake-sanim/inform-requests`: لیست fake SANIM.
- `/api/v1/security-bootstrap/keycloak-sync-status`: وضعیت sync.
- `/api/v1/security-bootstrap/sync-keycloak-users`: sync کاربران محلی با Keycloak.

## خطاها

خطاهای business با `errorCode` و message برگردانده می‌شوند. نمونه‌ها: `DUPLICATE_REQUEST`, `UNAUTHORIZED_TASK_ACCESS`, `TASK_COMPLETE_NOT_ALLOWED`, `DELEGATION_TARGET_NOT_ALLOWED`.

## سوالات باز

- مستند OpenAPI/Swagger در ریپازیتوری یافت نشد.

