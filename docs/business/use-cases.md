# موارد کاربرد

Last Updated: 2026-07-15

## هدف سند

ثبت سناریوهای مهم کاربری و اتصال آنها به frontend، API، service، entity و BPMN.

## مخاطبان

تحلیلگران، تیم QA، توسعه‌دهندگان و مالک محصول.

## موارد کاربرد

| شناسه | نام | Actor | جریان اصلی | Frontend | API | Service | Entity/BPMN | وضعیت |
|---|---|---|---|---|---|---|---|---|
| UC-001 | ورود کاربر | همه کاربران | هدایت به Keycloak، دریافت code، token، فراخوانی `/me` | `/login`, `/auth/callback` | `/api/v1/auth/me` | `SecurityIdentityService` | `AppUserProfile` | پیاده‌سازی‌شده |
| UC-002 | مشاهده کارتابل | کاربران غیرادمین | دریافت taskهای assigned/candidate در محدوده سازمانی | `/tasks` | `/api/v1/cartable/my-tasks` | `CartableService` | Camunda UserTask | پیاده‌سازی‌شده |
| UC-003 | ارسال برگ بدهی برای صدور | ADMIN/سامانه | ثبت payload، شروع فرایند Camunda | `/admin/data-entry/finalized-debt-paper` | `/api/v1/admin/data-entry/finalized-debt-papers/send` | `AdminDataEntryService` | `ExecutionPapers.bpmn` | پیاده‌سازی‌شده |
| UC-004 | رد صدور برگ اجرا | سیستم | rule engine مانع صدور می‌شود و دلیل ثبت می‌شود | پنل rules | BPMN worker | `RuleEngineService`, `SendRejectionResponseWorker` | `ExecutionPaperIssuanceRequest` | پیاده‌سازی‌شده |
| UC-005 | ارجاع توسط رئیس اداره کل | `GENERAL_OFFICE_HEAD` | انتخاب رئیس اداره مالیات مرتبط، delegate | `/tasks` | `/api/v1/cartable/tasks/{taskKey}/delegate` | `CartableService` | `Activity_0generalHeadReview` | پیاده‌سازی‌شده |
| UC-006 | ارجاع توسط رئیس اداره مالیات | `TAX_OFFICE_HEAD` | انتخاب رئیس واحد مرتبط | `/tasks` | `/delegate` | `CartableService` | `Activity_0taxOfficeHeadReview` | پیاده‌سازی‌شده |
| UC-007 | ارجاع به کارشناس | `TAX_UNIT_HEAD` | انتخاب `TAX_EXPERT` همان واحد | `/tasks` | `/delegate` | `CartableService` | `Activity_0taxUnitHeadReview` | پیاده‌سازی‌شده |
| UC-008 | تکمیل task | assignee | ارسال فرم مرحله به Camunda | `/tasks/[taskId]` | `/api/v1/cartable/tasks/{taskKey}/complete` | `CartableService` | UserTask | پیاده‌سازی‌شده |
| UC-009 | مشاهده جزئیات برگ اجرا | کاربران مجاز | باز کردن جزئیات و چاپ | `/execution-papers/[paperId]` | `/api/v1/execution-papers/{id}` | `ExecutionPaperService` | `ExecutionPaper` | پیاده‌سازی‌شده |
| UC-010 | مشاهده تاریخچه | کاربران مجاز | دریافت trace و audit | `/execution-papers/[paperId]` | `/api/v1/execution-papers/{id}/trace` | `ExecutionPaperService` | `AuditLog`, `ExecutionPaperStateHistory` | پیاده‌سازی‌شده |
| UC-011 | مدیریت کاربران | ADMIN/دارای مجوز | CRUD پروفایل محلی | `/admin/users` | `/api/v1/users` | `AppUserProfileService` | `AppUserProfile` | پیاده‌سازی‌شده |
| UC-012 | مدیریت سازمان | ADMIN/دارای مجوز | CRUD اداره کل، اداره، واحد | `/admin/organizations` | `/api/v1/masterdata/*` | `MasterdataService` | Masterdata entities | پیاده‌سازی‌شده |
| UC-013 | مدیریت مجوزها | ADMIN/دارای مجوز | تخصیص مجوز به گروه/نقش | `/admin/permissions` | `/api/v1/group-permissions` | `PermissionService` | `Permission`, `GroupPermission` | پیاده‌سازی‌شده |

## قالب پذیرش نمونه

برای هر use case باید آزمون پذیرش شامل نقش، token معتبر، داده نمونه، انتظار UI/API و اثر پایگاه داده تعریف شود. پوشش تست فعلی کامل نیست؛ جزئیات در [تست](../operations/testing.md) آمده است.

## اسناد مرتبط

- [نیازمندی‌های کارکردی](../requirements/functional-requirements.md)
- [ماتریس ردیابی](../requirements/traceability-matrix.md)
- [فرایندهای کسب‌وکار](business-processes.md)

## سوالات باز

- سناریوی کامل «پردازش کارشناس» پس از مشاهده برگ اجرا چیست؟
- آیا مدیریت نقش‌های Keycloak باید از UI انجام شود؟

