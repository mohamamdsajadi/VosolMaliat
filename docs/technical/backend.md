# بک‌اند

Last Updated: 2026-07-15

## هدف سند

شرح جزئیات پیاده‌سازی بک‌اند، سرویس‌های دامنه، transactionها، mapping، rule engine، workflow و audit.

## مخاطبان

توسعه‌دهندگان بک‌اند، معماران و تیم تست.

## فناوری‌ها

| فناوری | نسخه/شواهد |
|---|---|
| Java | `maven.compiler.release=21` |
| Spring Boot | `3.5.3` |
| Camunda | `8.8.8` |
| JPA/Hibernate | Spring Data JPA و Hibernate community dialect |
| Database فعلی | SQLite |
| Security | Spring Security OAuth2 Resource Server |

## ماژول‌ها

| ماژول | کلاس‌های کلیدی |
|---|---|
| صدور برگ اجرا | `IssuanceController`, `IssuanceRequestService`, `RuleEngineService`, `ExecutionPaperService` |
| کارتابل | `CartableController`, `CartableService`, `TaskAuthorizationService`, `CamundaIdentityMappingService` |
| امنیت | `SecurityConfig`, `SecurityIdentityService`, `KeycloakJwtAuthenticationConverter` |
| کاربران و مجوزها | `UserManagementController`, `PermissionController`, `AppUserProfileService`, `PermissionService` |
| سازمان | `MasterdataController`, `MasterdataService` |
| ابلاغ | `InformProcessService`, `FakeElectronicNotificationService`, `FakeSanimInformService` |

## Transaction و Lazy Loading

`spring.jpa.open-in-view=false` است، بنابراین APIها و workerها نباید entity lazy را خارج از transaction serialize یا dereference کنند. برای APIهای جزئیات برگ اجرا، `ExecutionPaperService` از Map/DTO استفاده می‌کند. در workerها استفاده از queryهای projection/ID محور توصیه شده و برای `IssueExecutionPaperWorker` انجام شده است.

## Rule Engine

`RuleEngineService` قوانین صدور را از `ExecutionPaperRejectionRule` و `InformRuleEngineService` قوانین ابلاغ را از `ExecutionPaperInformRejectionRule` می‌خواند. ruleها به صورت JSON در ستون `rule_json` ذخیره می‌شوند.

## Audit

`AuditLogService.record` رخدادهای مهم مثل ایجاد برگ اجرا، claim، complete، delegate، ارسال/تایید ابلاغ و تغییر مجوز را ثبت می‌کند. جزئیات کامل در [لاگینگ و پایش](../operations/logging-and-monitoring.md) آمده است.

## اسناد مرتبط

- [معماری بک‌اند](../architecture/backend-architecture.md)
- [API](api.md)
- [پایگاه داده](database.md)

## سوالات باز

- آیا همه سرویس‌های state-changing باید `@Transactional` داشته باشند؟
- آیا لازم است برای همه repositoryها projectionهای اختصاصی ساخته شود؟

