**Execution paper model fit review — 2026-09-12**

**Conclusion.** The application has a useful foundation for routing execution papers, identifying taxpayers, linking a source debt paper, and tracking electronic notification. Its financial structure most closely matches the simplified legacy VAT forms. It cannot currently preserve and reproduce all the supplied forms faithfully. A targeted domain refactor and immediate calculation/printing fixes are needed; a complete application rewrite is not justified by this evidence.

This review covers `docs/sample-execution-papers` in the repository, the JPA entities, issuance DTO/service, rule calculations, debt repositories, debt-status update service, notification scheduling, and the existing frontend print component. All nine files were text-extracted; distinct document bodies and the meaningful continuation/receipt pages were also inspected visually. The `.aspx` files are PDFs, not ASP.NET source. Two files are byte-identical, leaving **eight distinct papers and six observed form profiles**. The second pages of the browser-printed ASPX samples contain browser furniture rather than additional business content.

This is a sample-to-software analysis. References to articles and deadlines describe what the supplied papers print; they are not an independent verification of currently applicable law. No production behavior was exercised and no application code was changed. Arithmetic examples below were checked with decimal arithmetic. Personal names, national identifiers, and addresses are deliberately not repeated in this report.

**1. What varieties are actually present?**

Links below point to the reviewed samples; page numbers refer to PDF pages.

| Profile | Samples | Distinguishing business content |
|---|---|---|
| Legacy direct tax, finalized debt | [AZ_EJRA_L](<../sample-execution-papers/AZ_EJRA_L 598141 .aspx>), [AZ_EJRA_N](<../sample-execution-papers/AZ_EJRA_N 598912 (2).aspx>), p. 1 | Legal and natural persons; article 210 wording; source year, tax source, unit file number, final document number/date and finalization basis. Principal, fixed penalty, municipality/guild shares, late penalty, remission, compliance reward, payments, balance. The legal example distinguishes withholding obligation (تکلیفی) from the natural example’s performance obligation (عملکرد). |
| Legacy simplified VAT, finalized debt | [AZ_EJ_L_VA](<../sample-execution-papers/AZ_EJ_L_VA  598020 (2).aspx>), [AZ_EJ_N_VA](<../sample-execution-papers/AZ_EJ_N_VA 598860 (1).aspx>), p. 1 | Legal and natural persons; article 33 VAT wording; year and period; tax and duty separately; article 22/23 penalties; gross sum, payment, credit, balance. |
| Business income, self-declared liability | [LTR_EF03](<../sample-execution-papers/LTR_EF03  200000374736440.pdf>), pp. 1–3 | Group-three business income return; annual start/end dates; self-declaration reference; principal, paid tax, principal balance; separate article 190 penalty table; fractional rial amounts; issuer fields. Page 2 contains legal text and page 3 a service receipt. |
| Detailed VAT with separate tax/duty columns | [LTR_EF23](<../sample-execution-papers/LTR_EF23  200000305859250.pdf>), pp. 1–2 | Year 1396, period 1, legal person, final-paper basis. Sales and purchases by category, returns/discounts, deductible purchases, carry-forward credit, article 22/23 penalties and payments, separately for tax and duty. Both final columns show negative debt/credit balances. |
| Detailed VAT with combined tax/duty column | [LTR_EF23_1](<../sample-execution-papers/LTR_EF23_1 200000359313767.pdf>), pp. 1–2 | Year 1401, period 2, legal person, final-paper basis. Combined tax/duty; sales, purchase deductions, carry-forward credit, audit penalties and late-payment penalties; different commodity categories. |
| VAT self-declaration with itemized article penalties | [LTR_EF23_2](<../sample-execution-papers/LTR_EF23_2  200000359979083.pdf>), pp. 1–2 | Year 1404, period 3, natural person, self-declaration basis. Payable/transferable VAT, separate penalty rows referencing articles 26, 36, 37 and 12, payments and total. A second table breaks principal down by goods/services, fuel, harmful goods, tobacco, and gold/jewellery/platinum. |

[AZ_EJRA_L (1)](<../sample-execution-papers/AZ_EJRA_L 598141  (1).aspx>) is a byte-for-byte duplicate of the first legal direct-tax sample, not another type.

The six profiles are observed document layouts, not six proven official type codes. Filename suffixes alone should not define a business taxonomy. Model these dimensions separately: taxpayer type, tax source, obligation category, source-document kind, applicable regime/version, tax period, and rendering template. The issue date alone cannot select the regime: papers issued around 1404–1405 refer to materially different performance years and structures.

All distinct samples contain service-receipt fields, either on the main page or a separate page: recipient identity/relationship, receipt date/signature, serving or postal officer, residence/workplace, absence/refusal, affixation and a postal revisit. These are blank form fields; the samples do not demonstrate completed service events.

**2. Business-field coverage**

“Partial” means some data exists but semantics, ingestion, output, or historical preservation is incomplete.

| Business field group | Existing representation | Fit / gap |
|---|---|---|
| Paper identity | `ExecutionPaper.executionPaperUniqueCode` | Partial. String supports varied formats, but issuance generates `EP-...`; there is no distinct original paper number/source namespace or template identity. Samples use slash-form and long numeric references. |
| Issue date, print date, penalty calculation cutoff | `BaseEntity.createdAt`; source-paper date elsewhere | Missing explicit execution-paper issue date and penalty cutoff. Print date is distinct on legacy samples. Technical creation time cannot reconstruct imported issuance dates. |
| Taxpayer person type and identity | `Taxpayer`, `NaturalPerson`, `LegalPerson` | Good basic foundation: names, national identifiers, father/birth details and company registration details. `Taxpayer.getType()` infers type from links without enforcing exactly one person link. |
| Tax account and file identity | Person identifiers and source debt code | Missing separate taxpayer/account identifier, TIN, TFN, execution file, national file and unit file references. These must not be equated with national identity. Newer natural-person forms visibly use a taxpayer identifier different from the national code. |
| Additional identity/contact | `ContactInfo`; person fields | Address/postcode supported and ingested. Phone exists in entity but not issuance `TaxpayerInfo` or its contact upsert. Trade name, birth-certificate number/place and registration place are absent. Blank/unknown fields must not be invented. |
| Issuing organization | Tax unit → office → directorate, including contact data | Good master-data structure, partial historical coverage. `sendingUnitId` is accepted and can survive in stored request JSON, but is not persisted into the execution aggregate or exposed by details. Preserve sending versus issuing units separately. |
| Period and activity | `FinalizedDebtPaper.performanceYear`, `performancePeriod`, activity code/title | Partial. Strings accept values but have no explicit calendar/period type or annual start/end dates. |
| Basis/source paper | One `FinalizedDebtPaper`, code/date, reason, notification ID | Good for one finalized source; semantically incomplete for self-declarations and letters. `DebtDetectionDocument` has only its ID and execution-paper link. Source notification ID alone is not notification date/evidence. |
| Debt classification | `taxSource`, label enum, `finalizationReason` | Partial. No regime/version, document kind or separate withholding/performance obligation category. Free text cannot drive reliable template and calculation selection. |
| Legacy VAT amounts | `taxAmount`, `dutyAmount`, article 22/23, payments, credit | Closest fit. Credit exists but the entity total ignores it; penalty/payment tax-versus-duty allocation cannot be preserved. |
| Direct-tax amounts | Principal, article-number fields, deductible/non-deductible fields, shares/reward | Partial. General fixed/late penalties lack their own legal identity. “Eligible for remission” is not “remission already granted.” Shares and reward are stored but omitted from details output. |
| Detailed VAT accounting | One flat `DebtInformation` | Missing sales/purchase category rows, returns/discounts, input-credit details, carry-forward semantics and split/combined tax-duty dimensions. A scalar net balance cannot reproduce these tables. |
| Article-specific penalties | Fixed article 22/23 fields plus generic buckets | Missing structured article/paragraph, penalty category, basis, rate, calculation interval and remission treatment for the observed alternatives. |
| Issuer, attachments and output | Generic audit metadata; frontend print template | Blank issuer fields exist in print; no explicit signed issuer snapshot, template/legal-text version, receipt attachment or issued-document artifact on the execution model. `createdBy` is not automatically the authorized issuer. |
| Notification | Electronic ID/date/view date, status, rejection/re-inform dates | Useful electronic summary. Missing physical service events, recipient/officer details, reason/place, affixation/revisit evidence and receipt attachment. |
| History | State history, audits, stored request JSON/hash, `DebtInquiryResult` | Valuable existing provenance. Not an immutable snapshot of everything printed at issuance: the print view resolves live people, contact and organization data. |

Source entities: [execution model](../../src/main/java/com/mparsict/vosolmaliat/domain/model/execution/ExecutionPaper.java), [debt information](../../src/main/java/com/mparsict/vosolmaliat/domain/model/execution/DebtInformation.java), [finalized debt paper](../../src/main/java/com/mparsict/vosolmaliat/domain/model/execution/FinalizedDebtPaper.java), [notification](../../src/main/java/com/mparsict/vosolmaliat/domain/model/execution/ExecutionPaperNotification.java), [issuance request DTO](../../src/main/java/com/mparsict/vosolmaliat/application/dto/request/DebtPaperIssuanceRequest.java).

**3. Confirmed implementation problems, in priority order**

**P0 — Printed deadline disagrees with the samples and other application paths.** `PrintExecutionPaper` says ten days from notification, but computes its displayed deadline as record creation plus ten days. The supplied execution-paper notices say one month from notification. Meanwhile `ExecutionPaperService.toDto()` uses more than 30 elapsed days and `InformProcessService.setNextDebtInquiryDate()` uses `plusMonths(1)`. These are three different rules/bases. Establish a shared, approved deadline policy with an explicit effective-notification date and calendar. Do not infer a payment deadline while notification is unknown. The inquiry schedule should have its own purpose even if it uses the same date policy. This finding establishes a document mismatch, not an independent legal opinion.

**P0 — Financial totals have incompatible definitions.** `DebtInformation.getTotalDebt()` computes:

```
tax + duty + article22 + article23 - taxPaid - penaltyPaid
```

It ignores `taxCredit`, both generic penalty fields, municipality/guild shares, compliance bonus and the supplied remaining balance. Some of these may be dimensions or subtotals rather than additional amounts; blindly adding every column would also be wrong. The business meaning must be resolved first.

`RuleEngineService.computeTotalDebtAmount()` additionally adds both generic penalty fields. Thus the same input can pass an issuance threshold at one amount and display/be evaluated for notification at another. The frontend labels `deductiblePenalty` as remission/deduction, while the DTO describes a penalty eligible for remission and issuance adds it. Article-based and remission-based classifications may overlap, creating a double-counting risk. “Total finalized amount” also uses a separate narrow formula whose intended scope requires clarification.

The entity formula is repeated in repository search filters and dashboard sums; correcting only the getter would leave inconsistent business decisions. See [RuleEngineService](../../src/main/java/com/mparsict/vosolmaliat/application/service/RuleEngineService.java), [DebtInformationRepository](../../src/main/java/com/mparsict/vosolmaliat/domain/repository/DebtInformationRepository.java), [ExecutionPaperRepository](../../src/main/java/com/mparsict/vosolmaliat/domain/repository/ExecutionPaperRepository.java), and [ExecutionPaperService](../../src/main/java/com/mparsict/vosolmaliat/application/service/ExecutionPaperService.java).

**P0 — Printed subtotal double-counts balance.** The frontend computes `totalTax = taxAmount + dutyAmount + remainingTax` and prints this as total tax debt in digits and words. A remaining balance is not another principal charge. With the legacy legal VAT sample mapped naturally, that subtotal becomes **1,588,331,332** instead of the document's gross **1,238,639,043** or outstanding **1,202,039,043**. These are different measures; they need separately named totals. The fallback grand total inherits this problem when backend `totalDebt` is absent.

**P1 — Exact financial fidelity is missing.** Java monetary fields are `Double`; SANIM totals use `Long`. `LTR_EF03` explicitly prints a penalty of **230,731,485.60 rials** and a total of **1,966,798,821.60 rials**, including the fractional portion in words. The frontend `formatMoney()` and `numberToPersianWords()` round to integers. Use `BigDecimal` and explicit decimal persistence/serialization, with an agreed precision and rounding policy. Preserve upstream precision before applying any payable-amount rounding. An integer-rial-only refactor would lose observed data.

**P1 — Credit balances are a real sample case.** `LTR_EF23` prints final balances of **−1,126,680,867 tax** and **−563,340,434 duty**, under a debt/credit heading. Preserve those signed results and their dimensions. This sample alone does not establish why an execution form exists for a credit balance or whether issuance should be allowed. Flag it for business review; do not silently clamp it to zero or treat its absolute value as collectible debt. The current Persian amount-to-words loop also does not handle negative amounts.

**P1 — No versioned, reproducible issued document.** The current single print component cannot select the observed layouts or legal appendices, substitutes national identity for taxpayer identifier, always describes the source as a finalized paper, and lacks service-receipt pages. Its barcode is a CSS repeating gradient, not an encoding of the paper number. `createdAt` is used as issuance date and live master/contact data is resolved on read. Preserve issuance snapshots, approved template/legal-text versions and an artifact reference/hash for reliable reprints. Existing payload hashes and audit records should be retained and linked, not discarded.

Print evidence: [execution-paper page, PrintExecutionPaper and formatters](<../../src/main/frontend/src/app/execution-papers/[paperId]/page.js>), [barcode styling](../../src/main/frontend/src/app/globals.css).

**P1 — Original claim and current balance need explicit separation.** [DebtStatusUpdateService](../../src/main/java/com/mparsict/vosolmaliat/application/service/DebtStatusUpdateService.java) correctly retains response JSON/hash in `DebtInquiryResult` and updates dedicated SANIM fields. It does not overwrite the original component fields. However, list totals, filters and notification debt rules still use the original component formula. Define and label `issuedClaim`, `latestReportedBalance` and their as-of dates. Confirm whether a SANIM response covers this particular source paper or the whole taxpayer before using it as this paper’s balance.

**P2 — Relationship constraints do not match service assumptions.** Debt and notification records declare `ManyToOne`, while their repositories return one `Optional` per paper and the shown mappings do not enforce uniqueness. If debt remains a single summary, enforce one summary per paper. If notifications become repeated events, introduce event collections and an explicit current summary instead of relying on an arbitrary single-row lookup. Enforce one natural/legal person link per taxpayer. `Taxpayer` is currently resolved by source debt-paper code; separate the taxpayer/account identity from that document association when adding account/file references.

**4. Arithmetic anchors for acceptance tests**

These are document reconciliations, not proof that the existing application imports these files correctly.

| Sample | Reconciliation in rials | Implication |
|---|---|---|
| AZ_EJRA_L | 33,553,575 + 17,895,240 = **51,448,815** | Direct-tax fixed penalty needs correct classification. |
| AZ_EJRA_N | 190,980,000 + 4,774,500 − 22,500,000 = **173,254,500** | Separate original principal, late penalty and payment. |
| AZ_EJ_L_VA | 257,528,193 + 128,764,096 + 852,346,754 − 36,600,000 = **1,202,039,043** | Existing simplified VAT total shape fits this zero-credit example. |
| AZ_EJ_N_VA | 1,105,613,445 + 552,806,723 + 414,605,042 + 3,326,902,900 − 14,940,000 = **5,384,988,110** | Existing simplified VAT total shape fits this zero-credit example. |
| LTR_EF03 | 2,307,312,856 − 571,245,520 + 230,731,485.60 = **1,966,798,821.60** | Preserve fractional rials and article 190 identity. |
| LTR_EF23_1 | 1,426,010,558 − 746,054,770 + 597,629,958 = **1,277,585,746** | Purchase credit is different from paying the assessed debt. |
| LTR_EF23_2 | 514,702,078 + 1,372,539 = **516,074,617** | Principal breakdown total 514,702,078 must not be added again to the grand total. Preserve article 37 penalty identity. |

The negative balances in `LTR_EF23` provide an additional credit/sign fixture. Preserve printed intermediate values and flag rounding discrepancies rather than forcing reconciliation by altering source amounts. Blank amount cells should remain distinguishable from explicitly printed zeros when reproducing a source document.

**5. How much does the model fit the business?**

| Capability | Assessment |
|---|---|
| Routing, basic organization/person links, source reference, electronic notification summary | Useful foundation. The samples cannot establish end-to-end workflow correctness. |
| Simplified legacy VAT headline arithmetic | Closest fit: two of eight distinct samples have directly corresponding amount fields and matching formula shape, with zero credits. This is not full form coverage. |
| Direct-tax financial semantics | Partial; principal/payments exist, but fixed/late penalty classification and actual remission are ambiguous. |
| Detailed VAT, self-declaration basis and versioned penalties | Low fit; major dimensions and source kinds are missing. |
| Complete historical document reproduction and physical-service evidence | Low fit; no distinct sample is fully reproduced by the current model plus print path. |

A single percentage would imply a validated requirements catalogue and business weighting that these eight samples cannot supply. The defensible conclusion is **partial business fit, strongest for a workflow demo and legacy VAT summaries; insufficient for faithful issuance/reproduction across this sample set**. The print and calculation defects are correctness blockers even before adding new form profiles.

**6. Recommended refactor**

Keep `ExecutionPaper` as the workflow aggregate and preserve its ID/state/integration relationships. Expand its document semantics through composition:

| Component | Proposed responsibility |
|---|---|
| `ExecutionPaper` header | Internal identity, external/source identity, explicit issue date, person/account reference, issuing/sending organization, obligation category, regime and template version, penalty cutoff, issuance-request link. |
| `SourceDocument` reference | Kind (`FINAL_ASSESSMENT`, `SELF_DECLARATION`, `LETTER`, etc.), original number/date, period, finalization basis and source-notification reference/evidence. Initially one source per paper is supported by these samples; do not impose multi-source aggregation without a business requirement. |
| Issuance snapshot | Exact person/account identifiers, address/contact, organization labels, issuer identity and rendering/legal-text versions used at issuance. Keep references to master data as well. |
| Financial statement + component rows | Decimal amount, component kind, tax/duty/combined dimension, commodity category when relevant, penalty article/paragraph and remission eligibility. Distinguish contributing rows, deductions and non-additive breakdown/subtotal rows. |
| Balance observation | Issued totals and later reported balances with source, scope and as-of time; reuse `DebtInquiryResult` provenance. |
| Notification event + evidence | Channel, attempted/effective dates, recipient and relationship, officer, location, result/reason, revisit, attachment. Retain a convenient latest summary for workflow queries. |
| Document artifact | Issued file reference/hash, version, template version, issue/print events, receipt attachments. Generate real encoded barcodes from the agreed identifier. |

Do not create a separate execution-paper entity/BPMN for every filename. Do not replace all financial fields with an unvalidated JSON blob either. Typed components plus versioned mappings can preserve meaning, while original payload/document retention preserves evidence. A shared financial calculation service should supply named totals to issuance rules, notification rules, search/dashboard projections and rendering; it must understand which rows contribute and which merely explain a subtotal.

**Suggested sequence:**

1. Correct the deadline/creation-date mismatch and print subtotal; resolve generic penalty versus remission semantics and centralize named calculations. Add focused tests using the sample amounts, nonzero credits, overlapping penalty classifications and unknown notification dates.
2. Add decimal amounts, explicit issue/cutoff dates, source-document kind, tax-account/file references and sending unit. Add immutable issuance snapshots and consistent calendar parsing; retain original source date strings for traceability.
3. Add component tables and versioned profile mappings for detailed VAT and self-declared income. Update request DTOs, manual entry, rules, repositories, detail APIs and frontend together. Keep a compatibility adapter for existing requests; do not guess missing article/category data during migration.
4. Implement profile-specific rendering, receipt/appendix pages, decimal/signed amount-to-words, real barcode and artifact retention. Add physical-service recording if it is within the product’s operational scope.
5. Backfill only recoverable data from stored request payloads and existing records; mark unavailable historical values as unknown. Reconcile old/new totals before switching consumers and removing obsolete fields. Add relationship constraints after checking existing duplicates.

**Business decisions needed before final schema/calculation design:** Who assigns the official execution-paper number? Which tax regime/template codes does SANIM provide? Are generic penalties mutually exclusive with article-specific fields? Does `deductiblePenalty` mean eligible amount or granted remission? Are municipality/guild shares additive or allocations? What rounding rules apply to fractional rials? What scope does each SANIM balance cover? How should the credit-balance sample be handled? Which notification channels must the application record, and what event/calendar defines the payment deadline? These decisions affect implementation details; they do not change the need to fix the confirmed calculation and print mismatches.
