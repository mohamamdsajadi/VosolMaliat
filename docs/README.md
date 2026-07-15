# مستندات سامانه وصول مالیات

Last Updated: 2026-07-15

## هدف سند

این پوشه نمایه رسمی مستندات کسب‌وکاری، معماری، فنی، گردش‌کار، عملیات و تحلیل ریسک سامانه وصول مالیات است. محتوای مستندات بر اساس بررسی ریپازیتوری فعلی تولید شده و رفتارهای «پیاده‌سازی‌شده»، «ناتمام»، «برنامه‌ریزی‌شده» و «نامشخص» را از هم جدا می‌کند.

## مخاطبان

مدیران کسب‌وکار، تحلیلگران، تیم فنی، تیم امنیت، مدیران سامانه، تیم استقرار و نگهداری.

## مشخصات بررسی

| مورد | مقدار |
|---|---|
| نام سیستم | VosolMaliat / سامانه وصول و اجرای مالیاتی |
| شاخه Git | `new-ui-2` |
| شناسه Commit | `95213f1b81a408c106dbdb4b0113b87119b6d768` |
| تاریخ تولید مستندات | 2026-07-15 |
| وضعیت کلی | پیاده‌سازی فعال با قابلیت‌های اصلی، چند بخش ناتمام و چند ریسک امنیتی/عملیاتی |

## خلاصه سامانه

سامانه برای ثبت، صدور، پیگیری، ابلاغ و پردازش برگ اجرای مالیاتی طراحی شده است. معماری فعلی شامل بک‌اند Java/Spring Boot، فرانت‌اند Next.js، پایگاه داده SQLite، موتور گردش‌کار Camunda 8، احراز هویت Keycloak/OIDC، کنترل دسترسی مبتنی بر نقش و مجوز، کارتابل کاربران و پنل‌های مدیریتی است.

## هشدارهای مهم

| هشدار | توضیح |
|---|---|
| اطلاعات حساس | در `src/main/resources/application.yaml` مقادیر secret واقعی دیده شد. در این مستندات مقدارها با placeholder توصیف شده‌اند. |
| پایگاه داده | `ddl-auto=update` و SQLite برای محیط تولید توصیه نمی‌شود. |
| تست | تست‌ها محدود هستند و پوشش مسیرهای workflow، امنیت و frontend کامل نیست. |
| BPMN و داده | نمونه‌های قدیمی Camunda ممکن است متغیرهای سازمانی ناقص داشته باشند. |
| احراز هویت | هم‌زمانی Keycloak و پروفایل محلی به تنظیم صحیح سرویس‌اکانت Keycloak وابسته است. |

## ساختار مستندات

### کسب‌وکار

- [نمای کلی کسب‌وکار](business/business-overview.md)
- [سازمان و نقش‌ها](business/organization-and-roles.md)
- [قوانین کسب‌وکار](business/business-rules.md)
- [فرایندهای کسب‌وکار](business/business-processes.md)
- [موارد کاربرد](business/use-cases.md)
- [واژه‌نامه](business/glossary.md)

### نیازمندی‌ها

- [نیازمندی‌های کارکردی](requirements/functional-requirements.md)
- [نیازمندی‌های غیرکارکردی](requirements/non-functional-requirements.md)
- [ماتریس ردیابی](requirements/traceability-matrix.md)

### معماری

- [معماری سیستم](architecture/system-architecture.md)
- [معماری بک‌اند](architecture/backend-architecture.md)
- [معماری فرانت‌اند](architecture/frontend-architecture.md)
- [معماری گردش‌کار](architecture/workflow-architecture.md)
- [معماری امنیت](architecture/security-architecture.md)

### فنی

- [بک‌اند](technical/backend.md)
- [فرانت‌اند](technical/frontend.md)
- [پایگاه داده](technical/database.md)
- [API](technical/api.md)
- [امنیت فنی](technical/security.md)
- [پیکربندی](technical/configuration.md)
- [یکپارچه‌سازی‌ها](technical/integrations.md)

### گردش‌کار

- [نمای کلی گردش‌کار](workflow/workflow-overview.md)
- [فرایند برگ اجرا](workflow/processes/execution-paper-process.md)
- [فرایند ابلاغ](workflow/processes/inform-process.md)
- [فرایندهای تکمیلی](workflow/processes/additional-processes.md)

### عملیات

- [استقرار](operations/deployment.md)
- [تست](operations/testing.md)
- [لاگینگ و پایش](operations/logging-and-monitoring.md)
- [پشتیبان‌گیری و بازیابی](operations/backup-and-recovery.md)

### تحلیل

- [وضعیت پیاده‌سازی](analysis/implementation-status.md)
- [مشکلات شناخته‌شده](analysis/known-issues.md)
- [بدهی فنی](analysis/technical-debt.md)
- [ریسک‌های امنیتی](analysis/security-risks.md)
- [پیشنهادها](analysis/recommendations.md)
- [نقشه راه آینده](analysis/future-roadmap.md)
- [پرسش‌های باز](analysis/open-questions.md)

## ترتیب پیشنهادی مطالعه

### کاربران کسب‌وکار

1. [نمای کلی کسب‌وکار](business/business-overview.md)
2. [سازمان و نقش‌ها](business/organization-and-roles.md)
3. [فرایندهای کسب‌وکار](business/business-processes.md)
4. [موارد کاربرد](business/use-cases.md)
5. [پرسش‌های باز](analysis/open-questions.md)

### توسعه‌دهندگان

1. [معماری سیستم](architecture/system-architecture.md)
2. [معماری بک‌اند](architecture/backend-architecture.md)
3. [معماری فرانت‌اند](architecture/frontend-architecture.md)
4. [API](technical/api.md)
5. [پایگاه داده](technical/database.md)
6. [مشکلات شناخته‌شده](analysis/known-issues.md)

### مدیران سامانه

1. [معماری امنیت](architecture/security-architecture.md)
2. [پیکربندی](technical/configuration.md)
3. [استقرار](operations/deployment.md)
4. [لاگینگ و پایش](operations/logging-and-monitoring.md)
5. [پشتیبان‌گیری و بازیابی](operations/backup-and-recovery.md)

## وضعیت کلی پیاده‌سازی

| حوزه | وضعیت |
|---|---|
| صدور برگ اجرا | پیاده‌سازی‌شده با rule engine و Camunda worker |
| ابلاغ الکترونیک | پیاده‌سازی آزمایشی با fake service |
| کارتابل | پیاده‌سازی‌شده با Camunda 8 user tasks |
| مدیریت کاربران | پیاده‌سازی محلی، sync اختیاری Keycloak |
| کنترل دسترسی | پیاده‌سازی‌شده، نیازمند بازبینی endpointهای public و secretها |
| عملیات تولید | ناقص، فاقد Docker/backup/monitoring رسمی |

## موضوعات نیازمند بازبینی انسانی

- منطق دقیق قوانین صدور و ابلاغ.
- سیاست نهایی sync کاربران Keycloak.
- مرزهای سازمانی استان/اداره/واحد برای مشاهده و ارجاع.
- تبدیل SQLite و `ddl-auto=update` به migration و PostgreSQL.

