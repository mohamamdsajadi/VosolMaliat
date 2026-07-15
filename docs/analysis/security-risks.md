# ریسک‌های امنیتی

Last Updated: 2026-07-15

## ریسک‌های بحرانی

| ریسک | شرح | اقدام پیشنهادی |
|---|---|---|
| secret در configuration | مقدار secret سرویس Keycloak در فایل config دیده شد | rotation فوری و استفاده از secret manager |
| fake callback endpoint | اگر public بماند، امکان ثبت ابلاغ جعلی وجود دارد | محدودسازی با profile، token یا امضای درخواست |
| دسترسی مدیریتی | endpointهای admin باید فقط با مجوز دقیق باز باشند | بازبینی `@PreAuthorize` و security config |

## ریسک‌های بالا

| ریسک | شرح | اقدام پیشنهادی |
|---|---|---|
| sync Keycloak | سرویس‌اکانت ممکن است دسترسی بیش از حد داشته باشد | least privilege و audit |
| داده مالی در لاگ | احتمال لاگ شدن داده حساس | سیاست logging و masking |
| CORS | اگر باز باشد، سطح حمله افزایش می‌یابد | allowlist دقیق originها |
| JWT claim mapping | نقش/گروه اشتباه باعث دسترسی نادرست می‌شود | تست claim mapper |

## ریسک‌های متوسط

| ریسک | شرح |
|---|---|
| frontend-only hiding | مخفی کردن منو کافی نیست؛ backend باید کنترل کند |
| نبود rate limit | endpointهای admin و callback باید محدود شوند |
| نبود audit کامل | برخی عملیات ممکن است قابل ردیابی نباشند |
| نبود session/device policy | وابسته به تنظیمات Keycloak است |

## کنترل‌های موجود

- عدم ذخیره password در دیتابیس برنامه.
- اعتبارسنجی JWT با Resource Server.
- استفاده از `@PreAuthorize` در بسیاری از endpointها.
- مدل permission محلی.
- audit log برای عملیات کلیدی.

## کنترل‌های پیشنهادی

- فعال‌سازی HTTPS اجباری.
- تنظیم دقیق token lifetime در Keycloak.
- فعال‌سازی brute-force protection در Keycloak.
- محدود کردن service account به realm و عملیات ضروری.
- افزودن security headers.
- افزودن rate limiting.
- افزودن audit برای ورود، خطاهای 403 و callbackها.

## سیاست پیشنهادی secret

هیچ secret نباید در repository ذخیره شود. مقدارها باید از یکی از منابع زیر خوانده شوند:

- متغیر محیطی امن.
- secret manager زیرساخت.
- Kubernetes Secret با رمزنگاری مناسب.
- vault سازمانی.

