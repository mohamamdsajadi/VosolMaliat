# تست

Last Updated: 2026-07-15

## وضعیت فعلی تست‌ها

در ریپازیتوری چند تست محدود وجود دارد:

| تست | هدف |
|---|---|
| `TaskAuthorizationServiceTest` | بررسی قواعد مجوز کارتابل |
| `KeycloakPrincipalMapperTest` | بررسی استخراج اطلاعات و نقش‌ها از JWT |
| `IssuanceControllerTest` | بررسی endpoint صدور برگ بدهی/برگ اجرا |

این تست‌ها برای شروع مناسب‌اند، اما برای سامانه‌ای با workflow، امنیت و داده مالی کافی نیستند.

## اجرای تست بک‌اند

```bash
./mvnw test
```

## اجرای build فرانت‌اند

```bash
cd src/main/frontend
npm run build
```

## حوزه‌های نیازمند تست بیشتر

| حوزه | تست پیشنهادی |
|---|---|
| امنیت | JWT معتبر/نامعتبر، نقش‌ها، مجوزها، 401 و 403 |
| کاربران | ایجاد پروفایل، sync با Keycloak، نقش یکتا، سازمان |
| کارتابل | مشاهده assigned task، محدودیت سازمانی، تکمیل task، ارجاع |
| BPMN | مسیر صدور، مسیر رد، مسیر ابلاغ، retry rule |
| fake service | callback ابلاغ، ثبت InformDate، ارسال به سنیم fake |
| گزارش‌ها | آمار وضعیت برگ اجرا و مجموع بدهی |
| تاریخ | نمایش و ورود تاریخ شمسی در همه صفحات |
| چاپ | قالب A4 برگ اجرا، فونت، داده سازمانی و اطلاعات مودی |

## تست‌های پیشنهادی بک‌اند

| کلاس/سرویس | سناریو |
|---|---|
| `SecurityIdentityService` | کاربر فعال، غیرفعال، قفل‌شده |
| `PermissionService` | ADMIN همه مجوزها را دارد |
| `OrganizationScopeService` | محدودیت اداره کل، اداره مالیات و واحد مالیاتی |
| `CartableService` | task خارج از scope نمایش داده نشود |
| `ExecutionPaperService` | DTO بدون lazy loading exception برگردد |
| `InformRuleEngineService` | رد rule، محاسبه `reInformDate`، پذیرش rule |
| `CamundaMessageService` | correlate message با business key درست |

## تست‌های پیشنهادی فرانت‌اند

| صفحه | سناریو |
|---|---|
| `/login` | redirect به Keycloak |
| `/` | نمایش داشبورد بر اساس مجوز |
| `/tasks` | فیلتر، جدول، مشاهده جزئیات، عدم نمایش عملیات غیرمجاز |
| `/execution-papers` | فیلترها، تاریخ شمسی، جزئیات |
| `/execution-papers/[id]` | جزئیات، trace، print |
| `/admin/users` | CRUD کاربر و نقش |
| `/admin/permissions` | انتخاب نقش/گروه و برچسب فارسی |
| fake serviceها | جدول، تایید، تاریخ شمسی |

## Smoke Test پیشنهادی

1. ورود با کاربر `ADMIN`.
2. فراخوانی `/api/v1/auth/me`.
3. مشاهده فهرست کاربران.
4. ایجاد یا انتخاب یک برگ بدهی نهایی‌شده.
5. صدور برگ اجرا.
6. مشاهده برگ در `/execution-papers`.
7. بررسی trace برگ اجرا.
8. ورود با `GENERAL_OFFICE_HEAD`.
9. مشاهده task اختصاصی.
10. تکمیل task و انتقال به مرحله بعد BPMN.
11. بررسی ثبت audit.
12. اجرای مسیر ابلاغ fake و ثبت `InformDate`.

## داده تست

پروژه دارای initializer برای داده‌های امنیتی و نمونه کاربران است. برای تست قابل تکرار، داده seed باید از اجرای برنامه جدا شود و به migration یا fixture مستقل منتقل شود.

## ریسک تست فعلی

- تست end-to-end برای Camunda و Keycloak وجود ندارد.
- frontend regression test وجود ندارد.
- تست‌های فعلی خطاهای schema migration یا ناسازگاری SQLite/PostgreSQL را پوشش نمی‌دهند.

