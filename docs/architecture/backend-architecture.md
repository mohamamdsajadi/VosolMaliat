# معماری بک‌اند

Last Updated: 2026-07-15

## هدف سند

شرح ساختار لایه‌ای، کلاس‌های مهم، سرویس‌ها، repositoryها، security و workerها.

## مخاطبان

توسعه‌دهندگان بک‌اند، معماران، تیم تست.

## ساختار package

| Package | مسئولیت |
|---|---|
| `presentation.api` | REST controllers |
| `application.service` | منطق کاربردی، rule engine، reporting |
| `application.service.cartable` | کارتابل و authorization task |
| `application.service.security` | هویت، پروفایل، مجوز، audit |
| `domain.model` | entityها |
| `domain.repository` | Spring Data JPA |
| `infrastructure.camunda` | شروع فرایند، message، workers |
| `common.config` | پیکربندی عمومی و امنیتی |
| `common.exception` | خطاهای استاندارد |

## کلاس‌های مهم

| کلاس | مسیر | مسئولیت | وابستگی مهم |
|---|---|---|---|
| `SecurityConfig` | `common/config/security` | Resource Server و endpoint security | `KeycloakJwtAuthenticationConverter` |
| `SecurityIdentityService` | `application/service/security` | ساخت `CurrentUserContext` | `AppUserProfileRepository`, `GroupPermissionRepository` |
| `CartableService` | `application/service/cartable` | جستجو/claim/complete/delegate task | `CamundaClient`, `TaskAuthorizationService` |
| `TaskAuthorizationService` | `application/service/cartable` | قوانین مشاهده و اقدام task | `OrganizationScopeService` |
| `ExecutionPaperService` | `application/service` | ایجاد، جزئیات، trace و جستجوی برگ اجرا | repositories, `AuditLogService` |
| `RuleEngineService` | `application/service` | قوانین صدور | `ExecutionPaperRejectionRuleRepository` |
| `InformRuleEngineService` | `application/service` | قوانین ابلاغ | `ExecutionPaperInformRejectionRuleRepository` |
| `CamundaProcessService` | `infrastructure/camunda/service` | شروع process instance | `CamundaClient` |
| `IssueExecutionPaperWorker` | `infrastructure/camunda/worker` | صدور برگ اجرا در workflow | `ExecutionPaperService`, `TaxUnitRepository` |

## کنترلرها

کنترلرهای اصلی شامل `IssuanceController`, `ExecutionPaperController`, `CartableController`, `UserManagementController`, `PermissionController`, `MasterdataController`, `AdminDataEntryController`, fake integration controllers و rule controllers هستند. جزئیات در [API](../technical/api.md) آمده است.

## Exception Handling

`GlobalExceptionHandler` خطاهای `RuleViolationException`, `BusinessException`, validation، `AccessDeniedException` و خطای عمومی را به پاسخ JSON تبدیل می‌کند.

## Transaction Boundaries

سرویس‌های داده‌ای عمدتاً از `@Transactional` استفاده می‌کنند. در workerها باید از خروج entityهای lazy جلوگیری شود؛ چند اصلاح اخیر از queryهای ID محور برای جلوگیری از `LazyInitializationException` استفاده کرده‌اند.

## اسناد مرتبط

- [بک‌اند فنی](../technical/backend.md)
- [API](../technical/api.md)
- [پایگاه داده](../technical/database.md)

## سوالات باز

- آیا برای همه عملیات state-changing audit کافی وجود دارد؟
- آیا باید repositoryهای projection محور برای همه detail APIها ساخته شود؟

