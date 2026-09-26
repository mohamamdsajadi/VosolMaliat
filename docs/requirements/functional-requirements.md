# نیازمندی‌های کارکردی

Last Updated: 2026-07-15

## هدف سند

ثبت نیازمندی‌های کارکردی با وضعیت پیاده‌سازی و شواهد ریپازیتوری.

## مخاطبان

مالک محصول، تحلیلگران، تیم QA، تیم توسعه.

## جدول نیازمندی‌ها

| ID | نیازمندی | Actor | اولویت | منبع | مؤلفه | معیار پذیرش | وضعیت |
|---|---|---|---|---|---|---|---|
| FR-001 | ورود از طریق Keycloak/OIDC | همه کاربران | Critical | `login/page.js`, `keycloak.js` | Frontend/Auth | کاربر با code flow وارد شود و token بگیرد. | Implemented |
| FR-002 | اعتبارسنجی JWT در بک‌اند | همه APIهای امن | Critical | `SecurityConfig` | Spring Security | API فقط token معتبر بپذیرد. | Implemented |
| FR-003 | استخراج نقش‌ها، گروه‌ها و مجوزها | سیستم | Critical | `KeycloakJwtAuthenticationConverter`, `SecurityIdentityService` | Security | authorityهای Spring ساخته شود. | Implemented |
| FR-004 | مدیریت پروفایل محلی کاربر | ADMIN | High | `UserManagementController` | User Management | CRUD، status، lock، tax-unit assignment. | Implemented |
| FR-005 | ایجاد/sync کاربران Keycloak | ADMIN | Medium | `SecurityBootstrapController`, `KeycloakAdminBootstrapService` | Identity Sync | endpoint وضعیت و sync نتیجه per-user بدهد. | Partially implemented |
| FR-006 | مدیریت مجوز و نگاشت گروه/نقش | ADMIN | High | `PermissionController` | Authorization | مجوزها لیست/ثبت و به groupName تخصیص داده شود. | Implemented |
| FR-007 | مدیریت اداره کل/اداره/واحد | ADMIN | High | `MasterdataController` | Masterdata | CRUD هر سه سطح سازمانی. | Implemented |
| FR-008 | ثبت برگ بدهی قطعی دستی | ADMIN | High | `AdminDataEntryController` | Data Entry | فرم frontend payload معتبر بفرستد. | Implemented |
| FR-009 | ارسال برگ بدهی به workflow | ADMIN/سامانه | Critical | `IssuanceController`, `CamundaProcessService` | Workflow | فرایند `Process_0rtbdc5` آغاز شود. | Implemented |
| FR-010 | ارزیابی قوانین صدور | سیستم | Critical | `RuleEngineService` | Rule Engine | `canIssue` و دلیل رد تولید شود. | Implemented |
| FR-011 | صدور برگ اجرا | سیستم | Critical | `IssueExecutionPaperWorker`, `ExecutionPaperService` | Execution | رکورد `ExecutionPaper` و متغیرهای سازمانی ثبت شود. | Implemented |
| FR-012 | مشاهده برگ‌های اجرا با فیلتر | کاربران مجاز | High | `ExecutionPaperController`, `/execution-papers` | Reporting | page و search criteria کار کند. | Implemented |
| FR-013 | مشاهده جزئیات و چاپ برگ اجرا | کاربران مجاز | High | `ExecutionPaperService.getDetails`, `/execution-papers/[paperId]` | Execution UI | جزئیات و چاپ A4 نمایش داده شود. | Implemented |
| FR-014 | مشاهده trace برگ اجرا | کاربران مجاز | Medium | `getTrace`, `AuditLog` | Audit | رخدادهای state/audit برگردد. | Implemented |
| FR-015 | مشاهده کارتابل شخصی | کاربران غیرادمین | Critical | `CartableController.myTasks` | Cartable | taskهای assigned/candidate در scope نمایش داده شود. | Implemented |
| FR-016 | claim/unclaim task | کاربران مجاز | High | `CartableService.claim/unclaim` | Camunda User Task | assign/unassign در Camunda انجام شود. | Implemented |
| FR-017 | complete task | assignee | High | `CartableService.complete` | Camunda User Task | task تکمیل و audit ثبت شود. | Implemented |
| FR-018 | delegation به کاربر پایین‌دست | مدیران | Critical | `CartableService.delegate` | Delegation | فقط نقش مستقیم و سازمان مرتبط مجاز باشد. | Implemented |
| FR-019 | ارزیابی قوانین ابلاغ | سیستم | High | `InformRuleEngineService` | Inform | `canInform` و rejection variables تولید شود. | Implemented |
| FR-020 | fake electronic notification | ADMIN/سیستم | Medium | `FakeElectronicNotificationService` | Integration | درخواست ساخته و callback/message منتشر شود. | Implemented |
| FR-021 | fake SANIM | ADMIN/سیستم | Medium | `FakeSanimInformService` | Integration | تاریخ ابلاغ ارسال و تایید شود. | Implemented |
| FR-022 | گزارش dashboard | ADMIN | Medium | `DashboardService` | Reporting | آمار و جمع بدهی گروه‌ها نمایش داده شود. | Partially implemented |
| FR-023 | Pagination | کاربران گزارش | Medium | `ExecutionPaperPageResponse` | Execution | صفحه‌بندی برگ اجرا. | Implemented |
| FR-024 | جستجوی task | کاربران کارتابل | Medium | `CartableTaskSearchCriteria` | Cartable | فیلتر taskها. | Implemented |
| FR-025 | notification واقعی | سیستم | High | فقط fake service | Integration | اتصال به API واقعی. | Planned |

## اسناد مرتبط

- [موارد کاربرد](../business/use-cases.md)
- [API](../technical/api.md)
- [ماتریس ردیابی](traceability-matrix.md)

## سوالات باز

- نیازمندی‌های SLA و ظرفیت تولیدی هنوز در ریپازیتوری مشخص نیست.

