# یکپارچه‌سازی‌ها

Last Updated: 2026-07-15

## هدف سند

شرح اتصال‌های داخلی و بیرونی سامانه.

## مخاطبان

معماران، توسعه‌دهندگان integration، DevOps.

| Integration | هدف | داده تبادلی | احراز هویت | وضعیت | فایل‌های مرتبط |
|---|---|---|---|---|---|
| Keycloak | login، JWT، نقش‌ها، ایجاد اختیاری کاربران | users, roles, tokens | OIDC + client credentials | پیاده‌سازی‌شده | `SecurityConfig`, `KeycloakAdminBootstrapService`, `keycloak.js` |
| Camunda 8 | workflow و user task | process variables, task commands | OIDC client | پیاده‌سازی‌شده | `ProcessConstants`, workers, BPMN |
| SQLite | persistence فعلی | entity tables | JDBC | پیاده‌سازی‌شده | `application.yaml`, repositories |
| سامانه ابلاغ الکترونیک fake | دریافت برگ اجرا و callback InformDate | execution paper payload | در fake بدون احراز جداگانه؛ callback permitAll | آزمایشی | `FakeElectronicNotificationService` |
| سنیم fake | دریافت تاریخ ابلاغ | executionPaperId, informDate | endpoint امن برای پنل | آزمایشی | `FakeSanimInformService` |
| Rule engine داخلی | ارزیابی قوانین | rule_json, request/execution data | داخلی | پیاده‌سازی‌شده | `RuleEngineService`, `InformRuleEngineService` |

## Failure Handling

- Camunda workerها در خطا fail command ارسال می‌کنند.
- fake services `BusinessException` تولید می‌کنند.
- Keycloak sync per-user نتیجه `CREATED`, `EXISTING`, `FAILED` برمی‌گرداند.

## سوالات باز

- قرارداد API واقعی ابلاغ و سنیم هنوز در ریپازیتوری موجود نیست.

