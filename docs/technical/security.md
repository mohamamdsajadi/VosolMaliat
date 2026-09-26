# امنیت فنی

Last Updated: 2026-07-15

## هدف سند

شرح پیاده‌سازی امنیت، token، role/permission، scope سازمانی و ریسک‌های مشخص.

## مخاطبان

تیم امنیت، توسعه‌دهندگان، مدیران سامانه.

## اجزای امنیتی

| کلاس/فایل | مسئولیت |
|---|---|
| `SecurityConfig` | Resource server، stateless session، CSRF disabled، permitAllهای محدود |
| `KeycloakJwtAuthenticationConverter` | استخراج نقش و permission به authority |
| `KeycloakPrincipalMapper` | خواندن claimهای Keycloak |
| `SecurityIdentityService` | ترکیب JWT با `AppUserProfile` و permissions |
| `OrganizationScopeService` | کنترل محدوده سازمانی task |
| `TaskAuthorizationService` | canView/canClaim/canComplete/canDelegate |

## Password Handling

Spring password ذخیره نمی‌کند. Keycloak مسئول password است. `KeycloakAdminBootstrapService` می‌تواند password اولیه را در Keycloak تنظیم کند؛ این مقدار نباید در Git ذخیره شود.

## Secret Management

در `application.yaml` مقدار پیش‌فرض secret دیده شد. در production باید همه secretها فقط از environment/secret manager بیایند و فایل repo placeholder داشته باشد.

## Endpoint Security

بیشتر endpointها `@PreAuthorize` دارند. `DashboardController` در `SecurityConfig` permitAll شده و باید بررسی شود که آیا عمومی بودن آمار مجاز است یا خیر.

## Frontend Token

ذخیره token در frontend از طریق `keycloak.js` و `api.js` انجام می‌شود. جزئیات storage باید برای production بازبینی شود.

## اسناد مرتبط

- [معماری امنیت](../architecture/security-architecture.md)
- [ریسک‌های امنیتی](../analysis/security-risks.md)

## سوالات باز

- سیاست refresh token، logout کامل و token storage در production چیست؟

