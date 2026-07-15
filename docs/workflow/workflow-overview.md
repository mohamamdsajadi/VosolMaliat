# نمای کلی گردش‌کار

Last Updated: 2026-07-15

## هدف سند

نمایه فرایندهای BPMN و رابطه آنها.

## مخاطبان

تیم workflow، توسعه‌دهندگان، تحلیلگران فرایند.

## فرایندها

| فایل | Process ID | نام | سند |
|---|---|---|---|
| `src/main/resources/bpmn/ExecutionPapers.bpmn` | `Process_0rtbdc5` | `ExecutionPapers` | [فرایند برگ اجرا](processes/execution-paper-process.md) |
| `src/main/resources/bpmn/Inform.bpmn` | `Process_0lt64q4` | `NoticeProcess` | [فرایند ابلاغ](processes/inform-process.md) |

## رابطه فرایندها

`ExecutionPapers.bpmn` بعد از رسیدگی کارشناسی، با call activity به `Process_0lt64q4` وارد فرایند ابلاغ می‌شود و همه child variables را propagate می‌کند.

```mermaid
flowchart LR
    A[Process_0rtbdc5: ExecutionPapers] -->|Call Activity Activity_0xodqlk| B[Process_0lt64q4: NoticeProcess]
```

## متغیرهای مشترک مهم

`issuanceRequestId`, `debtPaperPayload`, `canIssue`, `executionPaperId`, `executionPaperCode`, `generalDirectorateId`, `taxAffairsOfficeId`, `taxUnitId`, `canInform`, `InformDate`, `reInformDate`, `nextDebtInquiryDate`.

## سوالات باز

- سیاست نسخه‌دهی و migration نمونه‌های در حال اجرا مشخص نیست.

