# فرایند دریافت آخرین وضعیت بدهی

Last Updated: 2026-08-03

## هدف

دریافت غیرهمزمان آخرین وضعیت تجمیعی بدهی مؤدی از سنیم و ثبت نتیجه روی برگ اجرا، بدون تغییر وضعیت چرخه عمر برگ اجرا.

## قرارداد فراخوانی

این فرایند با شناسه `Process_09srbi7` برای فراخوانی از یک Call Activity طراحی شده است.

| جهت | متغیر | نوع | توضیح |
|---|---|---|---|
| ورودی | `executionPaperId` | Long | شناسه برگ اجرا؛ نوع و شناسه مؤدی از آن استخراج می‌شود. |
| داخلی | `debtInquiryRequestId` | String | شناسه همبستگی پایدار با قالب `debt-inquiry-{processInstanceKey}`. |
| پیام | `sanimDebtStatus` | Object | آخرین وضعیت تجمیعی بدهی که با پیام `taxPayerDebt` دریافت می‌شود. |
| خروجی | `debtStatusUpdated` | Boolean | پس از ذخیره موفق پاسخ برابر `true` است. |

## مراحل

| Element ID | عنوان | نوع | Worker/Message |
|---|---|---|---|
| `Activity_1khsio8` | ارسال استعلام بدهی به سنیم | Send Task | `get-debt-from-sanim` |
| `Event_0f49589` | دریافت پاسخ بدهی | Message Catch Event | `taxPayerDebt`, correlation: `debtInquiryRequestId` |
| `Activity_0d55kj2` | به‌روزرسانی وضعیت پرداخت | Service Task | `update-taxpayer-payment-status` |

## رفتار ذخیره‌سازی

- درخواست خروجی در `fake_sanim_debt_inquiry_request` ثبت می‌شود و پاسخ فقط از مسیر callback آزمایشی منتشر می‌شود.
- پاسخ کامل و hash آن به‌صورت immutable در `debt_inquiry_result` نگهداری می‌شود.
- مقادیر تجمیعی سنیم در ستون‌های مستقل `DebtInformation` ثبت می‌شوند تا جزئیات اصلی برگ بدهی بازنویسی نشوند.
- این فرایند فعلاً وضعیت `ExecutionPaper` را تغییر نمی‌دهد.

## API سامانه آزمایشی سنیم

- `GET /api/v1/fake-sanim/debt-inquiries`: فهرست درخواست‌های منتظر یا پاسخ‌داده‌شده.
- `POST /api/v1/fake-sanim/debt-inquiries/{id}/respond`: محاسبه پاسخ از دفتر رویدادهای سنیم و انتشار پیام همبسته به Camunda.
