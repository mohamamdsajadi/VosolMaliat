# لاگینگ و پایش

Last Updated: 2026-07-15

## وضعیت فعلی

بک‌اند از لاگ استاندارد Spring Boot استفاده می‌کند. `GlobalExceptionHandler` خطاهای کنترل‌نشده را ثبت می‌کند. در حال حاضر نشانه‌ای از پیکربندی کامل مانیتورینگ، metrics، tracing یا ارسال لاگ به سامانه مرکزی در ریپازیتوری دیده نشد.

## نیازهای پایش

| حوزه | شاخص پیشنهادی |
|---|---|
| احراز هویت | تعداد 401 و 403، خطای issuer، خطای token |
| Camunda | worker failure، job retry، task stuck، message correlation failure |
| پایگاه داده | خطای اتصال، کندی query، deadlock/lock |
| ابلاغ fake | درخواست‌های pending، callback موفق/ناموفق |
| سنیم fake | ارسال موفق/ناموفق InformDate |
| کارتابل | تعداد taskهای باز هر نقش و سازمان |
| صدور برگ اجرا | تعداد صادرشده، ردشده، ابلاغ‌شده، ابلاغ‌نشده |

## لاگ‌های حساس

موارد زیر نباید در لاگ ثبت شوند:

- access token
- refresh token
- رمز عبور
- secret سرویس‌اکانت Keycloak
- اطلاعات محرمانه غیرضروری مودی

## پیشنهاد ساختار لاگ

هر عملیات مهم باید حداقل این فیلدها را داشته باشد:

| فیلد | توضیح |
|---|---|
| `correlationId` | شناسه ردیابی درخواست |
| `actorUsername` | کاربر انجام‌دهنده |
| `action` | نوع عملیات |
| `targetType` | نوع موجودیت هدف |
| `targetId` | شناسه هدف |
| `result` | موفق یا ناموفق |
| `durationMs` | زمان اجرا |

## عملیات نیازمند audit

در پروژه `AuditLog` وجود دارد و باید برای این موارد کامل و یکسان استفاده شود:

- ایجاد/ویرایش کاربر
- تغییر نقش و مجوز
- تغییر سازمان کاربر
- صدور برگ اجرا
- تکمیل task
- ارجاع task
- ارسال ابلاغ
- دریافت callback ابلاغ
- ارسال InformDate به سنیم fake
- رد rule engine

## هشدارهای پیشنهادی

| هشدار | شرط |
|---|---|
| افزایش 403 | احتمال مشکل نقش/مجوز یا حمله |
| taskهای متوقف | task بیش از زمان مجاز در یک مرحله مانده باشد |
| callback ناموفق | fake service نتواند message را به Camunda برساند |
| rule rejection زیاد | احتمال تنظیم نادرست rule |
| failure worker | workerهای Camunda retry را تمام کنند |

## ابزارهای پیشنهادی

- Spring Boot Actuator برای health و metrics.
- Micrometer + Prometheus برای metric.
- Grafana برای داشبورد.
- ELK/OpenSearch برای لاگ مرکزی.
- OpenTelemetry برای tracing بین بک‌اند، Camunda و سرویس‌های fake.

## شکاف‌های فعلی

- endpoint health رسمی مستند نشده است.
- dashboard عملیاتی برای workerهای Camunda وجود ندارد.
- correlation id در همه requestها استاندارد نشده است.
- خطاهای fake integration باید قابل جستجو و قابل retry باشند.

