# فرانت‌اند

Last Updated: 2026-07-15

## هدف سند

مستندسازی صفحات، componentها، API client، احراز هویت، مجوزدهی UI و وضعیت فعلی frontend.

## مخاطبان

توسعه‌دهندگان فرانت‌اند، QA، طراح UI.

## صفحات مهم

| Route | فایل | نقش‌های مجاز/منطق نمایش | APIهای اصلی | وضعیت |
|---|---|---|---|---|
| `/login` | `login/page.js` | عمومی | Keycloak auth URL | پیاده‌سازی‌شده |
| `/auth/callback` | `auth/callback/page.js` | عمومی | token endpoint Keycloak | پیاده‌سازی‌شده |
| `/` | `page.js` | ADMIN | `/api/v1/dashboard/stats` | پیاده‌سازی‌شده نسبی |
| `/tasks` | `tasks/page.js` | کاربران دارای task permissions | `/api/v1/cartable/my-tasks`, `/delegate` | پیاده‌سازی‌شده |
| `/tasks/[taskId]` | `tasks/[taskId]/page.js` | assignee/view مجاز | `/api/v1/cartable/tasks/{id}` | پیاده‌سازی‌شده |
| `/execution-papers` | `execution-papers/page.js` | `REPORT_VIEW` | `/api/v1/execution-papers` | پیاده‌سازی‌شده |
| `/execution-papers/[paperId]` | `execution-papers/[paperId]/page.js` | detail/trace permissions | `/details`, `/trace` | پیاده‌سازی‌شده |
| `/admin/users` | `admin/users/page.js` | user permissions | `/api/v1/users` | پیاده‌سازی‌شده |
| `/admin/permissions` | `admin/permissions/page.js` | permission permissions | `/permissions`, `/group-permissions` | پیاده‌سازی‌شده |
| `/admin/organizations` | `admin/organizations/page.js` | org assignment permission | `/masterdata/*` | پیاده‌سازی‌شده |
| `/admin/rules` | `admin/rules/page.js` | permission assign | `/admin/rejection-rules` | پیاده‌سازی‌شده |
| `/admin/inform-rules` | `admin/inform-rules/page.js` | permission assign | `/admin/inform-rejection-rules` | پیاده‌سازی‌شده |
| `/admin/fake-electronic-notification` | `admin/fake-electronic-notification/page.js` | audit/admin | fake notification API | پیاده‌سازی‌شده |
| `/admin/fake-sanim` | `admin/fake-sanim/page.js` | audit/admin | fake sanim API | پیاده‌سازی‌شده |

## API Client

`api.js` Bearer token را به درخواست‌ها اضافه می‌کند. `security.js`, `tasks.js`, `cartable.js`, `admin-data-entry.js` clientهای feature-based هستند.

## Styling و RTL

CSS اصلی در `globals.css` است. فونت IranYekan از `public/fonts/iranyekan` بارگذاری می‌شود. `JalaliInput` برای تاریخ جلالی استفاده شده است.

## مشکلات قابل بررسی

- بخشی از dashboard هنوز inline style دارد.
- `AppHeader.js` و `app-shell.js` ممکن است overlap مسئولیتی داشته باشند.
- route guard frontend نباید جایگزین backend authorization تلقی شود.

## اسناد مرتبط

- [معماری فرانت‌اند](../architecture/frontend-architecture.md)
- [امنیت فنی](security.md)

## سوالات باز

- آیا باید design system مستقل با componentهای form/table/button ساخته شود؟

