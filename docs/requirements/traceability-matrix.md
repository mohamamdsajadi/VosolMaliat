# ماتریس ردیابی

Last Updated: 2026-07-15

## هدف سند

اتصال قابلیت‌های کسب‌وکاری به قوانین، نیازمندی‌ها، نقش‌ها، UI، API، سرویس، موجودیت، BPMN و تست.

## مخاطبان

QA، تحلیلگران، معماران و توسعه‌دهندگان.

| قابلیت | قانون | FR | نقش | صفحه | API | Controller | Service | Entity | BPMN/Task | Test | وضعیت |
|---|---|---|---|---|---|---|---|---|---|---|---|
| ورود | BR-AUD-001 | FR-001, FR-002 | همه | `/login` | `/api/v1/auth/me` | `AuthController` | `SecurityIdentityService` | `AppUserProfile` | ندارد | `KeycloakPrincipalMapperTest` | Implemented |
| مدیریت کاربران | BR-AUD-001 | FR-004 | ADMIN | `/admin/users` | `/api/v1/users` | `UserManagementController` | `AppUserProfileService` | `AppUserProfile` | ندارد | Not found | Implemented |
| مدیریت سازمان | BR-ORG-001 | FR-007 | ADMIN | `/admin/organizations` | `/api/v1/masterdata/*` | `MasterdataController` | `MasterdataService` | Masterdata | ندارد | Not found | Implemented |
| صدور برگ اجرا | BR-ISS-001 تا BR-ISS-004 | FR-008 تا FR-011 | سیستم/ADMIN | `/admin/data-entry/finalized-debt-paper` | `/api/v1/debt-papers/issue` | `IssuanceController` | `IssuanceRequestService`, `ExecutionPaperService` | `ExecutionPaper` | `ExecutionPapers.bpmn`, `Activity_0rwz3ak` | `IssuanceControllerTest` | Implemented |
| کارتابل | BR-TASK-001 | FR-015 | کاربران غیرادمین | `/tasks` | `/api/v1/cartable/my-tasks` | `CartableController` | `CartableService` | Camunda task | User tasks | `TaskAuthorizationServiceTest` | Implemented |
| ارجاع | BR-DEL-001, BR-DEL-002 | FR-018 | مدیران | `/tasks` | `/api/v1/cartable/tasks/{taskKey}/delegate` | `CartableController` | `CartableService` | `AppUserProfile` | sequential user tasks | `TaskAuthorizationServiceTest` | Implemented |
| تکمیل task | BR-TASK-003 | FR-017 | assignee | `/tasks/[taskId]` | `/complete` | `CartableController` | `CartableService` | Camunda task | UserTask | Not found | Implemented |
| جزئیات برگ اجرا | BR-AUD-001 | FR-013 | مجاز | `/execution-papers/[paperId]` | `/api/v1/execution-papers/{id}` | `ExecutionPaperController` | `ExecutionPaperService` | `ExecutionPaper` | ندارد | Not found | Implemented |
| trace | BR-AUD-001 | FR-014 | مجاز | `/execution-papers/[paperId]` | `/trace` | `ExecutionPaperController` | `ExecutionPaperService` | `AuditLog`, `ExecutionPaperStateHistory` | ندارد | Not found | Implemented |
| ابلاغ | BR-INF-001 تا BR-INF-003 | FR-019 تا FR-021 | سیستم/ADMIN | `/admin/fake-electronic-notification`, `/admin/fake-sanim` | fake APIs | `FakeElectronicNotificationController`, `FakeSanimInformController` | integration services | fake request entities | `Inform.bpmn` | Not found | Implemented آزمایشی |
| گزارش | ندارد | FR-022 | ADMIN | `/` | `/api/v1/dashboard/stats` | `DashboardController` | `DashboardService` | `ExecutionPaper` | ندارد | Not found | Partially implemented |

## موارد بدون ارتباط قابل تایید

- Test برای frontend و workflow end-to-end یافت نشد.
- migration رسمی برای همه entityها وجود ندارد؛ فقط `db/manual` بخشی از schema را پوشش می‌دهد.

## اسناد مرتبط

- [نیازمندی‌های کارکردی](functional-requirements.md)
- [قوانین کسب‌وکار](../business/business-rules.md)
- [API](../technical/api.md)

## سوالات باز

- کدام قابلیت‌ها باید تست پذیرش رسمی داشته باشند؟

