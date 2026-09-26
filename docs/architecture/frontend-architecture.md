# معماری فرانت‌اند

Last Updated: 2026-07-15

## هدف سند

شرح معماری Next.js، routing، componentها، API client، authorization و وضعیت UI.

## مخاطبان

توسعه‌دهندگان فرانت‌اند، طراح UI، QA.

## فناوری‌ها

| مورد | مقدار |
|---|---|
| Framework | Next.js `16.1.1` |
| React | `^19.0.0` |
| Chart | `recharts` |
| BPMN form viewer | `@bpmn-io/form-js-viewer` |
| فونت | IranYekan در `public/fonts/iranyekan` |

## ساختار routing

| Route | فایل | هدف |
|---|---|---|
| `/login` | `login/page.js` | ورود Keycloak |
| `/auth/callback` | `auth/callback/page.js` | دریافت code و token |
| `/` | `page.js` | dashboard ادمین |
| `/tasks` | `tasks/page.js` | کارتابل فعال |
| `/tasks/[taskId]` | `tasks/[taskId]/page.js` | فرم task |
| `/execution-papers` | `execution-papers/page.js` | لیست برگ اجرا |
| `/execution-papers/[paperId]` | `execution-papers/[paperId]/page.js` | جزئیات/چاپ/trace |
| `/admin/*` | `admin/...` | مدیریت |

## componentهای مهم

| Component | مسئولیت |
|---|---|
| `AppShell` | layout، sidebar، header، user profile |
| `AuthGuard` | محافظت routeهای frontend |
| `JalaliInput` | date/datetime picker جلالی |
| `RefreshButton` | دکمه refresh یکپارچه |
| `Task` | نمایش و تکمیل task |
| `PieChartWidget` | نمودار dashboard |

## API Integration

APIها در `src/main/frontend/src/app/lib` تفکیک شده‌اند:

- `api.js`: wrapper fetch و Bearer token.
- `keycloak.js`: login callback و token exchange.
- `tasks.js`, `cartable.js`: کارتابل و task.
- `security.js`: user، permission، masterdata.
- `admin-data-entry.js`: ورود داده دستی.

## Authorization UI

فایل `navigation.js` آیتم‌های sidebar را بر اساس `localBusinessPermissions`, `allowedActions` و admin بودن فیلتر می‌کند. محدودیت‌های frontend نباید جایگزین کنترل backend شوند.

## مشکلات و بدهی‌ها

- برخی صفحات inline style قابل توجه دارند، مخصوصاً dashboard.
- route `/` در frontend برای non-admin redirect می‌کند؛ backend endpoint dashboard stats فعلاً permitAll است.
- state management ساده و local است؛ برای بزرگ‌شدن پروژه نیازمند الگوی پایدارتر است.

## اسناد مرتبط

- [فرانت‌اند فنی](../technical/frontend.md)
- [امنیت فنی](../technical/security.md)

## سوالات باز

- استاندارد طراحی نهایی فرم‌ها و جدول‌ها چیست؟
- آیا تست E2E برای role-based UI لازم است؟

