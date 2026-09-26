# پیشنهادها

Last Updated: 2026-07-15

## پیشنهادهای فوری

1. secretهای Keycloak را rotate کنید.
2. مقدارهای حساس را از `application.yaml` حذف کنید.
3. fake serviceها را پشت profile توسعه یا مجوز داخلی قرار دهید.
4. migration رسمی اضافه کنید.
5. smoke test امنیت و workflow را خودکار کنید.

## پیشنهادهای کوتاه‌مدت

| حوزه | پیشنهاد |
|---|---|
| دیتابیس | افزودن PostgreSQL profile و Flyway |
| امنیت | بازبینی کامل endpointهای public و admin |
| Camunda | مستندسازی contract متغیرهای BPMN |
| فرانت‌اند | ایجاد component مشترک برای جدول، فیلتر، دکمه و تاریخ |
| تست | تست integration برای Keycloak mapper و Cartable |

## پیشنهادهای میان‌مدت

- اضافه کردن event-driven audit برای همه عملیات workflow.
- اضافه کردن پنل مدیریتی برای خطاهای worker و retry.
- versioning برای ruleها.
- ایجاد گزارش عملیاتی برای taskهای معوق.
- تعریف SLA برای هر مرحله کارتابل.

## پیشنهادهای بلندمدت

- مهاجرت کامل به PostgreSQL.
- استقرار containerized با CI/CD.
- observability کامل با metrics، tracing و log مرکزی.
- پشتیبانی از SSO سازمانی از طریق Keycloak federation.
- افزودن موتور گزارش‌گیری تحلیلی.

## پیشنهاد UX

- همه تاریخ‌ها با تقویم شمسی و ساعت نمایش داده شوند.
- همه فرم‌های تاریخ/زمان از date picker واقعی استفاده کنند.
- همه جدول‌ها فیلتر، sort و pagination یکسان داشته باشند.
- دکمه‌ها و actionها بر اساس design system یکپارچه شوند.
- عملیات غیرمجاز به جای نمایش ناقص، پیام دسترسی روشن بدهند.

## پیشنهاد BPMN

- نام taskها و candidate groupها باید با نقش‌های سیستم دقیقاً هماهنگ بمانند.
- هر user task باید خروجی مشخص برای audit داشته باشد.
- message name و correlation key باید version و مستند شود.
- برای تغییر process فعال، migration strategy Camunda تعریف شود.

