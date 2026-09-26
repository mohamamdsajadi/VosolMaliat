# واژه‌نامه

Last Updated: 2026-07-15

## هدف سند

تعریف واژه‌های کسب‌وکاری و فنی استفاده‌شده در سامانه.

## مخاطبان

همه اعضای پروژه.

| واژه فارسی | معادل انگلیسی/کد | تعریف |
|---|---|---|
| برگ اجرا | Execution Paper | سند اجرایی برای پیگیری وصول بدهی مالیاتی. |
| برگ بدهی قطعی | Finalized Debt Paper | مبنای صدور برگ اجرا. |
| اداره کل امور مالیاتی | General Directorate of Tax Affairs | بالاترین سطح سازمانی در این سامانه. |
| اداره مالیات | Tax Affairs Office | سطح میانی زیرمجموعه اداره کل. |
| واحد مالیاتی | Tax Unit | واحد عملیاتی زیرمجموعه اداره مالیات. |
| کارشناس مالیاتی | Tax Expert | کاربر عملیاتی پردازش‌کننده برگ اجرا. |
| کارتابل | Task Inbox / Cartable | لیست taskهای Camunda قابل مشاهده یا اقدام توسط کاربر. |
| ابلاغ الکترونیک | Electronic Notification | ارسال برگ اجرا به سامانه ابلاغ. فعلاً fake service دارد. |
| سنیم | SANIM | سامانه عملیاتی مالیاتی. فعلاً fake service دارد. |
| نقش کسب‌وکاری | Business Role | مقدار enum مانند `TAX_EXPERT`. |
| مجوز | Permission | اجازه محلی مانند `TASK_COMPLETE`. |
| گروه کاندید | Candidate Group | گروه Camunda/Keycloak که task برای آن قابل دریافت است. |
| Assignee | Assignee | کاربر اختصاص‌یافته به task. |
| Rule Engine | Rule Engine | منطق ارزیابی قوانین صدور یا ابلاغ. |
| BPMN | Business Process Model and Notation | مدل فرایند Camunda. |
| Keycloak | Identity Provider | سامانه احراز هویت و صدور token. |
| JWT | JSON Web Token | token دسترسی صادرشده توسط Keycloak. |
| OIDC | OpenID Connect | پروتکل login و identity. |
| Audit Log | Audit Log | ثبت رخدادهای مهم سامانه. |

## اسناد مرتبط

- [نمای کلی کسب‌وکار](business-overview.md)
- [معماری سیستم](../architecture/system-architecture.md)

## سوالات باز

- واژگان حقوقی دقیق برای برخی وضعیت‌های رد و ابلاغ نیازمند تایید حقوقی است.

