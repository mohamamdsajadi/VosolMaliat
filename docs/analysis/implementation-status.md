# وضعیت پیاده‌سازی

Last Updated: 2026-07-15

## جمع‌بندی

سامانه اکنون بخش‌های اصلی مدیریت کاربران، مجوزها، سازمان‌ها، صدور برگ اجرا، کارتابل Camunda، ابلاغ fake، سنیم fake، گزارش داشبورد و چاپ برگ اجرا را دارد. با این حال، برای تولید نیازمند سخت‌سازی امنیتی، migration رسمی، تست بیشتر و استانداردسازی عملیات است.

## وضعیت حوزه‌ها

| حوزه | وضعیت | توضیح |
|---|---|---|
| احراز هویت Keycloak | پیاده‌سازی‌شده | Spring Security Resource Server با JWT |
| پروفایل محلی کاربر | پیاده‌سازی‌شده | `AppUserProfile` بدون password |
| نقش‌ها | پیاده‌سازی‌شده | نقش یکتا برای کاربران عملیاتی |
| مجوزها | پیاده‌سازی‌شده | `Permission` و `GroupPermission` |
| سازمان | پیاده‌سازی‌شده | اداره کل، اداره مالیات، واحد مالیاتی |
| کارتابل | پیاده‌سازی‌شده | مبتنی بر Camunda 8 user task |
| delegation | پیاده‌سازی‌شده | با تکمیل task و حرکت به user task بعدی |
| صدور برگ اجرا | پیاده‌سازی‌شده | worker و rule engine |
| ابلاغ | پیاده‌سازی آزمایشی | fake electronic notification service |
| ارسال به سنیم | پیاده‌سازی آزمایشی | fake SANIM service |
| trace/audit | پیاده‌سازی‌شده | trace برگ اجرا و audit |
| چاپ برگ اجرا | پیاده‌سازی‌شده | قالب A4 با داده‌های اصلی |
| migration | ناقص | SQL دستی و `ddl-auto=update` |
| تست | ناقص | تست‌های محدود |
| عملیات تولید | ناقص | backup/monitoring/deployment رسمی ندارد |

## پیاده‌سازی‌های شاخص

- کنترل دسترسی مبتنی بر JWT و مجوز محلی.
- mapping نقش‌های Keycloak به authorityهای برنامه.
- پروفایل کاربر مستقل از password.
- سازمان‌دهی کاربر با masterdata.
- کارتابل مبتنی بر Camunda 8، نه Camunda 7.
- rule engine برای صدور و ابلاغ.
- fake service برای ابلاغ الکترونیک و سنیم.
- پنل‌های مدیریتی برای کاربران، مجوزها، سازمان‌ها و ruleها.
- نمایش تاریخ شمسی در بسیاری از صفحات.

## بخش‌های ناتمام یا نیازمند تثبیت

- migration واقعی با Flyway/Liquibase.
- استانداردسازی همه DTOها برای جلوگیری از lazy loading exception.
- حذف secretها از config.
- جداسازی fake service از محیط تولید.
- تست end-to-end با Keycloak و Camunda.
- طراحی rollback برای deploy و BPMN change.
- monitoring و alerting.

