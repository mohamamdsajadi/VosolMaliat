# بدهی فنی

Last Updated: 2026-07-15

## بدهی‌های اصلی

| بدهی | اولویت | توضیح |
|---|---|---|
| نبود migration رسمی | بسیار بالا | schema فعلی با Hibernate update مدیریت می‌شود |
| SQLite به عنوان دیتابیس اصلی | بسیار بالا | مناسب تولید نیست |
| پراکندگی style فرانت‌اند | متوسط | نیازمند design system کوچک و یکپارچه |
| تست ناکافی | بالا | workflow و امنیت حساس هستند |
| fake service بدون مرزبندی محیط | بالا | باید dev/test-only شود |
| secret hardcoded | بسیار بالا | باید فوراً حذف و rotate شود |

## بدهی‌های معماری

- contract متغیرهای Camunda باید مستند و version شود.
- DTO mapping باید در همه endpointها یکدست شود.
- endpointهای مدیریتی باید با permission matrix رسمی کنترل شوند.
- rule engine فعلی باید قابلیت versioning و audit دقیق rule evaluation داشته باشد.

## بدهی‌های داده

- migration از SQLite به PostgreSQL نیازمند mapping نوع داده‌هاست.
- relationshipها باید با constraintهای واقعی دیتابیس پشتیبانی شوند.
- seed data باید جدا از runtime initializer باشد.
- داده‌های نمونه کاربر نباید در محیط تولید بدون کنترل ایجاد شوند.

## بدهی‌های عملیاتی

- نبود Dockerfile/Compose یا deployment manifest رسمی.
- نبود health check و readiness/liveness.
- نبود backup script و restore runbook.
- نبود monitoring stack.

## پیشنهاد اولویت‌بندی

1. حذف secret و rotation.
2. افزودن migration رسمی.
3. افزودن profile PostgreSQL.
4. پوشش تست برای امنیت و workflow.
5. محدود کردن fake serviceها.
6. استانداردسازی DTO و mapper.
7. ایجاد راهنمای deploy و rollback اجرایی.

