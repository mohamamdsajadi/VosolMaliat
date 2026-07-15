# استقرار

Last Updated: 2026-07-15

## وضعیت فعلی

پروژه در وضعیت توسعه فعال است و استقرار تولیدی کامل هنوز در ریپازیتوری استانداردسازی نشده است. بک‌اند با Spring Boot اجرا می‌شود، فرانت‌اند با Next.js در مسیر `src/main/frontend` قرار دارد، Camunda 8 به صورت self-managed پیکربندی شده و پایگاه داده فعلی SQLite است.

## اجزای مورد نیاز

| جزء | وضعیت فعلی | توضیح |
|---|---|---|
| Java | Java 21 | برای اجرای Spring Boot |
| Spring Boot | 3.5.3 | بک‌اند اصلی |
| Camunda | 8.8.8 | اجرای فرایندها و user taskها |
| Keycloak | خارجی | احراز هویت و صدور JWT |
| پایگاه داده | SQLite | مناسب توسعه، نامناسب برای تولید |
| فرانت‌اند | Next.js 16.1.1 | رابط کاربری |

## اجرای محلی

### بک‌اند

```bash
./mvnw spring-boot:run
```

پورت پیش‌فرض طبق تنظیمات پروژه `8025` است.

### فرانت‌اند

```bash
cd src/main/frontend
npm run dev
```

پورت معمول توسعه فرانت‌اند `3000` است.

## تنظیمات حیاتی محیط

| متغیر | کاربرد |
|---|---|
| `KEYCLOAK_ISSUER_URI` | آدرس realm در Keycloak |
| `KEYCLOAK_BOOTSTRAP_USERS_ENABLED` | فعال/غیرفعال کردن sync نمونه کاربران با Keycloak |
| `KEYCLOAK_BOOTSTRAP_CLIENT_ID` | client سرویس‌اکانت برای ایجاد/به‌روزرسانی کاربر |
| `KEYCLOAK_BOOTSTRAP_CLIENT_SECRET` | secret سرویس‌اکانت؛ باید فقط از محیط امن خوانده شود |
| `CAMUNDA_CLIENT_MODE` | حالت اتصال Camunda |
| `CAMUNDA_CLIENT_ZEEBE_GRPC-ADDRESS` | آدرس gRPC Zeebe |
| `CAMUNDA_CLIENT_ZEEBE_REST-ADDRESS` | آدرس REST Zeebe |
| `CAMUNDA_CLIENT_TASKLIST_BASE-URL` | آدرس Tasklist API |

## مراحل پیشنهادی استقرار

1. ساخت image یا artifact بک‌اند.
2. ساخت نسخه تولیدی فرانت‌اند.
3. تنظیم Keycloak realm، clientها، نقش‌ها و mapperها.
4. تنظیم Camunda 8 و deploy کردن BPMNها.
5. اعمال migration پایگاه داده.
6. تنظیم secretها از environment یا secret manager.
7. اجرای health check بک‌اند، فرانت‌اند، Keycloak، Camunda و پایگاه داده.
8. اجرای smoke test روی login، `/api/v1/auth/me`، صدور برگ اجرا و کارتابل.

## چک‌لیست تولید

| مورد | وضعیت |
|---|---|
| حذف secret از فایل‌های version control | ضروری |
| جایگزینی SQLite با PostgreSQL | ضروری |
| حذف `ddl-auto=update` | ضروری |
| تعریف migration رسمی | ضروری |
| محدودسازی endpointهای fake service | ضروری |
| فعال‌سازی TLS | ضروری |
| تنظیم CORS دقیق | ضروری |
| مانیتورینگ و لاگ مرکزی | ضروری |
| backup پایگاه داده | ضروری |
| تست smoke بعد از deploy | ضروری |

## نکات Camunda

BPMNهای فعلی در مسیر `src/main/resources/bpmn` قرار دارند:

- `ExecutionPapers.bpmn`
- `Inform.bpmn`

برای استقرار تولید، باید نسخه BPMN، شناسه process و contract متغیرها ثبت و کنترل شود. تغییر BPMN بدون هماهنگی با workerها می‌تواند باعث توقف instanceهای فعال شود.

## نکات Keycloak

در Keycloak باید نقش‌های زیر وجود داشته باشند:

- `ADMIN`
- `GENERAL_OFFICE_HEAD`
- `TAX_OFFICE_HEAD`
- `TAX_UNIT_HEAD`
- `TAX_EXPERT`

همچنین access token باید claimهای لازم مانند `preferred_username`، `email`، `given_name`، `family_name` و role/group را شامل شود.

## ریسک‌های استقرار فعلی

- استفاده از SQLite در محیط تولید باعث محدودیت همزمانی، backup و مقیاس‌پذیری می‌شود.
- `ddl-auto=update` می‌تواند schema را بدون کنترل تغییر دهد.
- fake serviceها اگر در تولید باز بمانند، امکان دستکاری وضعیت ابلاغ را ایجاد می‌کنند.
- secret فعلی Keycloak باید rotation شود.

