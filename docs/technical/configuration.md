# پیکربندی

Last Updated: 2026-07-15

## هدف سند

شرح فایل‌های configuration و متغیرهای محیطی بدون افشای secret.

## مخاطبان

DevOps، مدیر سامانه، توسعه‌دهندگان.

## فایل‌ها

| فایل | هدف |
|---|---|
| `src/main/resources/application.yaml` | تنظیمات Spring، DB، JPA، Keycloak، Camunda، logging |
| `pom.xml` | dependencyها و build Maven |
| `src/main/frontend/package.json` | dependency و scriptهای frontend |
| `src/main/frontend/next.config.js` | تنظیمات Next.js |

## متغیرهای مهم

| متغیر | هدف | مقدار نمونه امن |
|---|---|---|
| `DB_PATH` | مسیر SQLite | `/var/lib/vosol/identifier.sqlite` |
| `KEYCLOAK_ISSUER_URI` | issuer realm | `https://host/realms/vosol-maliat` |
| `KEYCLOAK_BOOTSTRAP_USERS_ENABLED` | فعال‌سازی sync کاربران | `false` در production مگر با کنترل |
| `KEYCLOAK_BOOTSTRAP_CLIENT_ID` | client محرمانه sync | placeholder |
| `KEYCLOAK_BOOTSTRAP_CLIENT_SECRET` | secret client | باید از secret manager بیاید |
| `KEYCLOAK_BOOTSTRAP_DEFAULT_PASSWORD` | password اولیه Keycloak | فقط محیط dev |
| `CAMUNDA_GRPC_ADDRESS` | آدرس gRPC Camunda | placeholder |
| `CAMUNDA_REST_ADDRESS` | آدرس REST Camunda | placeholder |
| `CAMUNDA_CLIENT_SECRET` | secret Camunda client | secret manager |
| `SERVER_PORT` | پورت backend | `8025` |

## نکات امنیتی

هیچ secret واقعی نباید در این سند یا فایل repo قرار گیرد. مقادیر پیش‌فرض فعلی باید پاک‌سازی و با placeholder جایگزین شوند.

## اسناد مرتبط

- [استقرار](../operations/deployment.md)
- [امنیت فنی](security.md)

## سوالات باز

- آیا profileهای جداگانه dev/test/prod تعریف خواهد شد؟

