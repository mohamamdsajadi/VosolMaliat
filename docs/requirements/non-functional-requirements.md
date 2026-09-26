# نیازمندی‌های غیرکارکردی

Last Updated: 2026-07-15

## هدف سند

ثبت ویژگی‌های کیفی اثبات‌شده و توصیه‌های غیرکارکردی.

## مخاطبان

معماران، تیم امنیت، DevOps، تیم QA.

## نیازمندی‌ها و توصیه‌ها

| حوزه | نیاز/توصیه | شواهد | وضعیت |
|---|---|---|---|
| امنیت | اعتبارسنجی JWT باید از Keycloak انجام شود. | `spring.security.oauth2.resourceserver.jwt.issuer-uri` | اثبات‌شده |
| امنیت | secretها نباید در فایل repo باشند. | `application.yaml` دارای مقدار پیش‌فرض secret است | ریسک، نیازمند اصلاح |
| امنیت | CSRF برای API stateless غیرفعال است. | `SecurityConfig` | قابل قبول برای Bearer token، نیازمند بازبینی CORS |
| کارایی | جستجوی Camunda task با page limit 100 انجام می‌شود. | `CartableService.searchTasks` | نیازمند طراحی pagination |
| مقیاس‌پذیری | SQLite برای production مناسب نیست. | `application.yaml`, `sqlite-jdbc` | توصیه به PostgreSQL |
| نگهداشت‌پذیری | لایه‌بندی controller/service/repository وجود دارد. | package structure | اثبات‌شده |
| Auditability | audit log برای رخدادهای مهم وجود دارد. | `AuditLogService`, `AuditLog` | پیاده‌سازی نسبی |
| داده | `ddl-auto=update` ریسک drift schema دارد. | `application.yaml` | نیازمند migration |
| بومی‌سازی | UI فارسی، RTL و فونت IranYekan دارد. | `layout.js`, `globals.css`, `public/fonts` | پیاده‌سازی‌شده |
| دسترس‌پذیری | شواهد تست accessibility وجود ندارد. | تست‌ها محدود | Not found |
| پایش | Actuator/metrics یافت نشد. | `pom.xml` فاقد actuator | Not implemented |
| Backup | راهبرد backup یافت نشد. | فاقد سند/اسکریپت | Not implemented |
| تست‌پذیری | چند تست واحد وجود دارد. | `src/test/java/...` | Partially implemented |

## اسناد مرتبط

- [ریسک‌های امنیتی](../analysis/security-risks.md)
- [استقرار](../operations/deployment.md)
- [تست](../operations/testing.md)

## سوالات باز

- اهداف RPO/RTO چیست؟
- حجم کاربران و taskهای هم‌زمان چقدر است؟

