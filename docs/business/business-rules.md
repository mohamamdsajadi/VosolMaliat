# قوانین کسب‌وکار

Last Updated: 2026-07-15

## هدف سند

ثبت قوانین کشف‌شده از کد، BPMN و تنظیمات با تفکیک وضعیت پیاده‌سازی.

## مخاطبان

تحلیلگران، تیم توسعه، تیم تست، مالک فرایند.

## جدول قوانین

| شناسه | قانون | منبع | نقش/موجودیت | وضعیت | استثنا/ابهام |
|---|---|---|---|---|---|
| BR-ISS-001 | درخواست برگ بدهی تکراری با hash مشابه رد می‌شود. | `IssuanceRequestService.createAndSave` | `ExecutionPaperIssuanceRequest` | پیاده‌سازی‌شده | معیار hash باید توسط کسب‌وکار تایید شود. |
| BR-ISS-002 | قبل از صدور برگ اجرا، قوانین صدور ارزیابی می‌شوند. | `CheckIssuancePossibilityWorker`, `RuleEngineService` | برگ بدهی | پیاده‌سازی‌شده | محتوای rule_json نیازمند قرارداد رسمی است. |
| BR-ISS-003 | در صورت رد rule engine، دلیل و تاریخ رفع محدودیت ثبت/ارسال می‌شود. | `SendRejectionResponseWorker`, BPMN `Activity_010iyzn` | صدور | پیاده‌سازی‌شده | اتصال واقعی سنیم وجود ندارد. |
| BR-ISS-004 | برگ اجرا با شناسه یکتا ایجاد می‌شود. | `IssueExecutionPaperWorker`, `UniqueCodeGenerator` | `ExecutionPaper` | پیاده‌سازی‌شده | یکتایی در DB روی `execution_paper_unique_code`. |
| BR-TASK-001 | task فقط برای assignee، candidate group/user یا مجوز سازمانی قابل مشاهده است. | `TaskAuthorizationService.canView` | Camunda UserTask | پیاده‌سازی‌شده | admin همه taskها را read-only می‌بیند. |
| BR-TASK-002 | claim فقط برای task بدون assignee، candidate معتبر و در محدوده سازمانی مجاز است. | `TaskAuthorizationService.canClaim` | Cartable | پیاده‌سازی‌شده | نیازمند `TASK_CLAIM`. |
| BR-TASK-003 | complete فقط توسط assignee و با `TASK_COMPLETE` مجاز است. | `TaskAuthorizationService.canComplete` | Cartable | پیاده‌سازی‌شده | admin در وضعیت فعلی complete عملیاتی ندارد. |
| BR-TASK-004 | مسئول هر مرحله بررسی سازمانی می‌تواند مراحل باقی‌مانده را پایان دهد. | `finishExecutionPaperCheck`, review decision gateways | `ExecutionPapers` | پیاده‌سازی‌شده | فقط شاخه بررسی پایان می‌یابد؛ شاخه‌های موازی ادامه دارند. |
| BR-DEL-001 | هر مدیر فقط به نقش مستقیم پایین‌تر ارجاع می‌دهد. | `BusinessRole.nextDelegationRole`, `CartableService.delegate` | نقش‌ها | پیاده‌سازی‌شده | cross-org وجود ندارد. |
| BR-DEL-002 | مقصد ارجاع باید در محدوده سازمانی مرتبط باشد. | `CartableService.isInDelegationScope` | سازمان | پیاده‌سازی‌شده | نیازمند تعریف دقیق استان/صلاحیت. |
| BR-ORG-001 | متغیرهای `generalDirectorateId`, `taxAffairsOfficeId`, `taxUnitId` محدوده task را تعیین می‌کنند. | `OrganizationScopeService` | Camunda variables | پیاده‌سازی‌شده | نمونه‌های قدیمی Camunda ممکن است ناقص باشند. |
| BR-INF-001 | قبل از ابلاغ، قوانین ابلاغ بررسی می‌شوند. | `CheckInformPossibilityWorker`, `InformRuleEngineService` | `ExecutionPaperNotification` | پیاده‌سازی‌شده | ruleهای ابلاغ مستقل از rule صدور هستند. |
| BR-INF-002 | اگر ابلاغ مجاز نباشد، وضعیت rejection و reInformDate تنظیم می‌شود. | `InformProcessService.updateInformStatus`, `setReInformDate` | ابلاغ | پیاده‌سازی‌شده | تایمر BPMN با `reInformDate`. |
| BR-INF-003 | پس از دریافت `InformDate`، وضعیت ابلاغ و تاریخ استعلام بعدی تنظیم می‌شود. | `UpdateInformStatusWorker`, `SetDebtInquiryDateWorker` | ابلاغ | پیاده‌سازی‌شده | پیام Camunda توسط fake callback منتشر می‌شود. |
| BR-AUD-001 | عملیات مهم کاربر و task audit می‌شوند. | `AuditLogService`, `CartableService`, `ExecutionPaperService` | `AuditLog` | پیاده‌سازی‌شده نسبی | پوشش همه رخدادها باید تست شود. |

## قوانین اعتبارسنجی

- DTOها از `jakarta.validation` مانند `@NotBlank`, `@NotNull`, `@Valid` استفاده می‌کنند.
- unique بودن username، email و keycloakUserId در `AppUserProfileService` بررسی می‌شود.
- خطاهای `BusinessException` و validation در `GlobalExceptionHandler` تبدیل به پاسخ JSON می‌شوند.

## اسناد مرتبط

- [فرایندهای کسب‌وکار](business-processes.md)
- [فرایند برگ اجرا](../workflow/processes/execution-paper-process.md)
- [فرایند ابلاغ](../workflow/processes/inform-process.md)
- [ماتریس ردیابی](../requirements/traceability-matrix.md)

## سوالات باز

- قرارداد نهایی ساختار `rule_json` چیست؟
- در چه شرایطی ارجاع خارج از سازمان مجاز است؟
- وضعیت‌های نهایی برگ اجرا و لغو چه چرخه رسمی دارند؟
