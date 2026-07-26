'use client'

import { useEffect, useMemo, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import AppShell from '../../components/app-shell'
import { apiFetch } from '../../lib/api'

function toPersianDigits(str) {
  const persianDigits = '۰۱۲۳۴۵۶۷۸۹'
  return String(str ?? '').replace(/\d/g, d => persianDigits[d])
}

function formatValue(value) {
  if (value == null || value === '') return '—'
  if (typeof value === 'number') return toPersianDigits(value.toLocaleString('en-US'))
  return toPersianDigits(value)
}

function formatDetailValue(key, value) {
  if (value == null || value === '') return '—'
  if (isDateTimeField(key)) return formatPersianDateTime(value)
  if (isDateOnlyField(key)) return formatPersianDate(value)
  return formatValue(value)
}

function isDateTimeField(key) {
  return [
    'createdAt',
    'updatedAt',
    'electronicNotificationDate',
    'informRejectedAt',
    'reInformDate',
    'nextDebtInquiryDate',
    'rejectedAt'
  ].includes(key)
}

function isDateOnlyField(key) {
  return ['debtPaperIssueDate', 'birthDate', 'registrationDate'].includes(key)
}

function formatMoney(value) {
  if (value == null || value === '') return '۰'
  const number = Number(value)
  if (Number.isNaN(number)) return formatValue(value)
  return toPersianDigits(Math.round(number).toLocaleString('en-US'))
}

function orgDisplay(name, id) {
  if (name && id) return `${name} - کد ${id}`
  if (name) return name
  if (id) return `کد ${id}`
  return ''
}

function prepareDetailData(sectionKey, data) {
  if (!data) return data
  if (sectionKey === 'executionPaper') {
    return {
      ...data,
      currentStateDisplay: data.currentStateDisplay || data.currentState,
      taxSourceDisplay: data.taxSourceDisplay || data.taxSource,
      mainTaxBureauDisplay: orgDisplay(data.mainTaxBureauName || data.generalDirectorateName, data.mainTaxBureauId),
      taxBureauDisplay: orgDisplay(data.taxBureauName || data.taxAffairsOfficeName, data.taxBureauId),
      taxUnitDisplay: orgDisplay(data.taxUnitName, data.taxUnitId)
    }
  }
  if (sectionKey === 'finalizedDebtPaper') {
    return {
      ...data,
      taxSourceDisplay: data.taxSourceDisplay || data.taxSource,
      activityTypeDisplay: data.activityTypeDisplay || orgDisplay(data.activityTypeTitle, data.activityTypeCode),
      mainTaxBureauDisplay: orgDisplay(data.mainTaxBureauName || data.generalDirectorateName, data.mainTaxBureauId),
      taxBureauDisplay: orgDisplay(data.taxBureauName || data.taxAffairsOfficeName, data.taxBureauId),
      taxUnitDisplay: orgDisplay(data.taxUnitName, data.taxUnitId)
    }
  }
  if (sectionKey === 'taxpayer') {
    return {
      ...data,
      typeDisplay: data.typeDisplay || data.typeLabel || data.type
    }
  }
  return data
}

const SECTIONS = [
  {
    key: 'executionPaper',
    title: 'اطلاعات برگ اجرا',
    labels: {
      uniqueCode: 'شناسه یکتای برگ اجرا', currentStateDisplay: 'وضعیت فعلی', finalizationReason: 'علت قطعیت', taxSourceDisplay: 'منبع مالیات',
      mainTaxBureauDisplay: 'اداره کل امور مالیاتی', taxBureauDisplay: 'اداره امور مالیاتی', taxUnitDisplay: 'واحد مالیاتی',
      createdAt: 'تاریخ ایجاد'
    }
  },
  {
    key: 'finalizedDebtPaper',
    title: 'اطلاعات برگ بدهی قطعی',
    labels: { debtPaperUniqueCode: 'شناسه یکتا/ارجاع پرونده', debtPaperIssueDate: 'تاریخ قطعیت', notificationId: 'شماره ابلاغ', finalizationReason: 'علت قطعیت', taxSourceDisplay: 'منبع مالیاتی', performanceYear: 'سال عملکرد', performancePeriod: 'دوره عملکرد', activityTypeDisplay: 'نوع فعالیت', mainTaxBureauDisplay: 'اداره کل امور مالیاتی', taxBureauDisplay: 'اداره امور مالیاتی', taxUnitDisplay: 'واحد مالیاتی', totalFinalizedAmount: 'کل مبلغ قطعی‌شده', finalizedTaxAndNonDeductibleAmount: 'مبلغ قطعی‌شده بدون جریمه ق.ب', createdAt: 'تاریخ ثبت' }
  },
  {
    key: 'taxpayer',
    title: 'اطلاعات مودی',
    labels: { typeDisplay: 'نوع مودی', firstName: 'نام', lastName: 'نام خانوادگی', nationalCode: 'کد ملی', birthDate: 'تاریخ تولد', fatherName: 'نام پدر', nationalId: 'شناسه ملی', companyName: 'نام/شهرت تجاری', registrationNumber: 'شماره ثبت', postalCode: 'کد پستی', address: 'اقامتگاه قانونی' }
  },
  {
    key: 'debtInformation',
    title: 'ریز مبالغ بدهی',
    labels: { taxAmount: 'اصل مالیات', dutyAmount: 'اصل عوارض', penaltyArticle22: 'جریمه ثابت', penaltyArticle23: 'جریمه دیرکرد', nonDeductiblePenalty: 'جریمه غیرقابل بخشش', deductiblePenalty: 'بخشودگی/کسر جریمه', taxPaidAmount: 'پرداخت اصل', penaltyPaidAmount: 'پرداخت جریمه', taxCredit: 'اعتبار', remainingTax: 'مانده بدهی ارسالی', totalDebt: 'مانده محاسبه‌شده' }
  },
  {
    key: 'notification',
    title: 'اطلاعات ابلاغ',
    labels: {
      informStatusLabel: 'وضعیت ابلاغ',
      electronicNotificationDate: 'تاریخ ابلاغ الکترونیکی',
      informRejectionReason: 'علت رد ابلاغ',
      informRejectedAt: 'زمان رد ابلاغ',
      reInformDate: 'زمان بررسی مجدد ابلاغ',
      nextDebtInquiryDate: 'زمان استعلام بعدی بدهی',
      createdAt: 'تاریخ ثبت ابلاغ'
    }
  },
]

function DetailSection({ section, data }) {
  const displayData = useMemo(() => prepareDetailData(section.key, data), [section.key, data])
  const entries = useMemo(() => Object.entries(section.labels).filter(([key]) => displayData && Object.prototype.hasOwnProperty.call(displayData, key)), [section, displayData])
  if (section.hideWhenEmpty && entries.length === 0) return null
  return (
    <section style={cardStyle}>
      <h3 style={sectionTitleStyle}>{section.title}</h3>
      {entries.length === 0 ? <p style={{ color: '#9ca3af', margin: 0 }}>اطلاعاتی ثبت نشده است.</p> : (
        <div style={gridStyle}>
          {entries.map(([key, label]) => (
            <div key={key} style={itemStyle}>
              <span style={labelStyle}>{label}</span>
              <strong style={valueStyle}>{formatDetailValue(key, displayData[key])}</strong>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

const CRITICAL_TRACE_FIELD_LABELS = {
  TASK_CLAIMED: 'مسئول فعلی',
  TASK_UNCLAIMED: 'مسئول فعلی',
  TASK_ASSIGNED: 'مسئول فعلی',
  TASK_DELEGATED_TO_USER: 'مسئول فعلی',
  TASK_DELEGATED_TO_GROUP: 'گروه/نقش مقصد',
  ELECTRONIC_NOTIFICATION_CONFIRMED: 'تاریخ ابلاغ الکترونیکی',
  EXECUTION_PAPER_INFORM_REJECTED: 'وضعیت و علت رد ابلاغ',
  EXECUTION_PAPER_INFORMED: 'تاریخ ابلاغ',
  EXECUTION_PAPER_RE_INFORM_SCHEDULED: 'تاریخ بررسی مجدد ابلاغ',
  NEXT_DEBT_INQUIRY_DATE_SET: 'تاریخ استعلام بعدی بدهی',
  INFORM_DATE_SENT_TO_SANIM: 'تاریخ ابلاغ ارسال‌شده به سنیم',
  SANIM_MESSAGE_CONFIRMED: 'وضعیت دریافت سنیم',
  SANIM_INFORM_DATE_CONFIRMED: 'وضعیت دریافت سنیم',
  RELATED_PERSONS_REFRESHED_FROM_COMPANY_REGISTRY: 'اطلاعات اشخاص مرتبط دریافت‌شده'
}

function extractTraceValue(value) {
  if (value == null || value === '') return ''
  const text = String(value)
  const match = text.match(/(?:^|;\s*)value=(.*)$/)
  return match ? match[1] : text
}

function hasTraceChange(item) {
  if (!item || item.type !== 'AUDIT') return false
  if (!CRITICAL_TRACE_FIELD_LABELS[item.action]) return false
  const oldValue = extractTraceValue(item.oldValue)
  const newValue = extractTraceValue(item.newValue)
  return oldValue !== '' || newValue !== ''
}

function traceFieldLabel(item) {
  return CRITICAL_TRACE_FIELD_LABELS[item.action] || 'داده حساس'
}

function formatTraceChangeValue(value) {
  return formatAuditValue(extractTraceValue(value))
}

function TraceSection({ trace, loading, error }) {
  return (
    <section style={traceCardStyle}>
      <div style={traceHeaderStyle}>
        <div>
          <p style={traceEyebrowStyle}>ردیابی پرونده</p>
          <h3 style={sectionTitleStyle}>سوابق ارجاع و عملیات برگ اجرا</h3>
        </div>
      </div>
      {loading && <p style={mutedTextStyle}>در حال دریافت سوابق...</p>}
      {!loading && error && <p style={errorTextStyle}>خطا در دریافت سوابق: {error}</p>}
      {!loading && !error && (!trace?.items || trace.items.length === 0) && (
        <p style={mutedTextStyle}>هنوز سابقه عملیاتی برای این برگ اجرا ثبت نشده است.</p>
      )}
      {!loading && !error && trace?.items?.length > 0 && (
        <div style={timelineStyle}>
          {trace.items.map((item, index) => (
            <div key={`${item.type}-${item.action}-${item.occurredAt}-${index}`} style={timelineItemStyle}>
              <div style={timelineDotStyle} />
              <div style={timelineContentStyle}>
                <div style={timelineTopStyle}>
                  <strong style={timelineTitleStyle}>{item.label || item.action}</strong>
                  <span style={timelineDateStyle}>{formatPersianDateTime(item.occurredAt)}</span>
                </div>
                <div style={timelineMetaStyle}>
                  <span>انجام‌دهنده: {formatValue(item.actorUsername || 'system')}</span>
                </div>
                {hasTraceChange(item) && (
                  <div style={traceChangeBoxStyle}>
                    <span style={traceChangeFieldStyle}>{traceFieldLabel(item)}</span>
                    <div style={traceChangeValuesStyle}>
                      <div style={traceChangeValueStyle}>
                        <span style={traceChangeLabelStyle}>مقدار قبلی</span>
                        <strong>{formatTraceChangeValue(item.oldValue)}</strong>
                      </div>
                      <div style={traceChangeArrowStyle}>←</div>
                      <div style={traceChangeValueStyle}>
                        <span style={traceChangeLabelStyle}>مقدار جدید</span>
                        <strong>{formatTraceChangeValue(item.newValue)}</strong>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

function RelatedPersonsSection({ data, loading, refreshing, error, onRefresh }) {
  const items = data?.relatedPersons || []
  return (
    <section style={relatedPersonsCardStyle}>
      <div style={relatedPersonsHeaderStyle}>
        <div>
          <p style={traceEyebrowStyle}>سامانه ثبت شرکت‌ها</p>
          <h3 style={{ ...sectionTitleStyle, marginBottom: 4 }}>اشخاص مرتبط مودی حقوقی</h3>
          <span style={mutedMetaStyle}>
            سال عملکرد: {formatValue(data?.debtYear)} · آخرین بروزرسانی: {formatPersianDateTime(data?.lastSyncedAt)}
          </span>
        </div>
        <button
          type="button"
          onClick={onRefresh}
          disabled={refreshing}
          style={{ ...refreshRelatedButtonStyle, ...(refreshing ? printButtonDisabledStyle : {}) }}
        >
          {refreshing ? 'در حال دریافت...' : 'دریافت اطلاعات تازه'}
        </button>
      </div>

      {loading && <p style={mutedTextStyle}>در حال دریافت اشخاص مرتبط...</p>}
      {!loading && error && <p style={errorTextStyle}>خطا در دریافت اشخاص مرتبط: {error}</p>}
      {!loading && !error && items.length === 0 && (
        <p style={mutedTextStyle}>برای سال عملکرد این برگ بدهی، شخص مرتبطی ثبت نشده است.</p>
      )}
      {!loading && !error && items.length > 0 && (
        <div style={relatedPersonsTableScrollStyle}>
          <table style={relatedPersonsTableStyle}>
            <thead>
              <tr>
                <th style={relatedPersonHeaderCellStyle}>نوع شخص</th>
                <th style={relatedPersonHeaderCellStyle}>نام</th>
                <th style={relatedPersonHeaderCellStyle}>شناسه</th>
                <th style={relatedPersonHeaderCellStyle}>سمت</th>
                <th style={relatedPersonHeaderCellStyle}>تاریخ شروع</th>
                <th style={relatedPersonHeaderCellStyle}>تاریخ پایان</th>
                <th style={relatedPersonHeaderCellStyle}>حق امضا</th>
              </tr>
            </thead>
            <tbody>
              {items.map(item => (
                <tr key={item.relationshipId}>
                  <td style={relatedPersonCellStyle}>{item.personType === 'NATURAL' ? 'حقیقی' : 'حقوقی'}</td>
                  <td style={relatedPersonCellStyle}>
                    <strong>{item.personType === 'NATURAL'
                      ? formatValue([item.firstName, item.lastName].filter(Boolean).join(' '))
                      : formatValue(item.companyName)}</strong>
                  </td>
                  <td style={relatedPersonCellStyle}>{formatValue(item.nationalCode || item.fida || item.nationalId)}</td>
                  <td style={relatedPersonCellStyle}>{formatValue(item.roleLabel)}</td>
                  <td style={relatedPersonCellStyle}>{formatPersianDate(item.startDate)}</td>
                  <td style={relatedPersonCellStyle}>{formatPersianDate(item.endDate)}</td>
                  <td style={relatedPersonCellStyle}>{item.hasSignatoryAuthority ? 'دارد' : 'ندارد'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function PrintIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M7 8V4h10v4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M7 17H5a2 2 0 0 1-2-2v-4a3 3 0 0 1 3-3h12a3 3 0 0 1 3 3v4a2 2 0 0 1-2 2h-2" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M7 14h10v6H7z" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M17 11.5h.01" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" />
    </svg>
  )
}

function BackIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M9 6l6 6-6 6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function PrintExecutionPaper({ data }) {
  if (!data) return null
  const execution = data.executionPaper || {}
  const finalized = data.finalizedDebtPaper || {}
  const taxpayer = data.taxpayer || {}
  const debt = data.debtInformation || {}
  const fullName = taxpayer.type === 'LEGAL'
    ? taxpayer.companyName
    : [taxpayer.firstName, taxpayer.lastName].filter(Boolean).join(' ')
  const identityCode = taxpayer.type === 'LEGAL' ? taxpayer.nationalId : taxpayer.nationalCode
  const totalTax = sumValues([
    debt.taxAmount,
    debt.dutyAmount,
    debt.remainingTax
  ])
  const totalPenalty = sumValues([
    debt.penaltyArticle22,
    debt.penaltyArticle23,
    debt.nonDeductiblePenalty
  ])
  const totalDebt = Number(debt.totalDebt ?? totalTax + totalPenalty)
  const issueDate = jalaliDate(execution.createdAt || finalized.createdAt)
  const legalDeadlineDate = addDaysJalali(execution.createdAt || finalized.createdAt, 10)
  const generalId = execution.mainTaxBureauId || finalized.mainTaxBureauId
  const officeId = execution.taxBureauId || finalized.taxBureauId
  const unitId = execution.taxUnitId || finalized.taxUnitId
  const generalName = execution.mainTaxBureauName || execution.generalDirectorateName || finalized.mainTaxBureauName || finalized.generalDirectorateName
  const officeName = execution.taxBureauName || execution.taxAffairsOfficeName || finalized.taxBureauName || finalized.taxAffairsOfficeName
  const unitName = execution.taxUnitName || finalized.taxUnitName
  const generalDisplay = orgDisplay(generalName, generalId)
  const officeDisplay = orgDisplay(officeName, officeId)
  const unitDisplay = orgDisplay(unitName, unitId)
  const officePhone = finalized.taxAffairsOfficePhone || execution.taxAffairsOfficePhone || finalized.generalDirectoratePhone || execution.generalDirectoratePhone
  const officeAddress = finalized.taxAffairsOfficeAddress || execution.taxAffairsOfficeAddress || finalized.generalDirectorateAddress || execution.generalDirectorateAddress
  const activityType = finalized.activityTypeDisplay || orgDisplay(finalized.activityTypeTitle, finalized.activityTypeCode) || finalized.taxSourceDisplay || execution.taxSourceDisplay

  return (
    <div className="execution-print-template" dir="rtl">
      <section className="execution-print-page">
        <header className="print-header">
          <div className="print-heading">
            <strong>برگ اجرایی</strong>
            <span>سال مالیاتی {formatValue(finalized.performanceYear || '-')}</span>
          </div>
          <div className="print-logo-block">
            <img src="/images/iran-tax-logo.png" alt="سازمان امور مالیاتی کشور" />
            <strong>وزارت امور اقتصادی و دارایی</strong>
            <span>سازمان امور مالیاتی کشور</span>
          </div>
          <div className="print-barcode-block">
            <div className="print-barcode" aria-hidden="true" />
          </div>
        </header>

        <table className="print-meta-table">
          <tbody>
            <tr>
              <th>شماره</th>
              <td>{formatValue(execution.uniqueCode)}</td>
              <th>اداره کل امور مالیاتی</th>
              <td>{formatValue(generalDisplay)}</td>
            </tr>
            <tr>
              <th>تاریخ</th>
              <td>{formatValue(issueDate)}</td>
              <th>اداره امور مالیاتی</th>
              <td>{formatValue(officeDisplay)}</td>
            </tr>
            <tr>
              <th>صفحه</th>
              <td>۱ از ۱</td>
              <th>تلفن</th>
              <td>{formatValue(officePhone)}</td>
            </tr>
            <tr>
              <th>واحد مالیاتی</th>
              <td>{formatValue(unitDisplay)}</td>
              <th>نشانی</th>
              <td>{formatValue(officeAddress)}</td>
            </tr>
          </tbody>
        </table>

        <table className="print-info-table">
          <caption>مشخصات مؤدی</caption>
          <tbody>
            <tr>
              <th>شناسه مؤدی</th>
              <td>{formatValue(identityCode)}</td>
              <th>نوع شخص</th>
              <td>{taxpayer.type === 'LEGAL' ? 'حقوقی' : 'حقیقی'}</td>
            </tr>
            <tr>
              <th>نام شخص حقوقی / نام و نام خانوادگی</th>
              <td>{formatValue(fullName)}</td>
              <th>نام تجاری / شهرت کسبی</th>
              <td>{formatValue(taxpayer.companyName || fullName)}</td>
            </tr>
            <tr>
              <th>نوع فعالیت</th>
              <td>{formatValue(activityType)}</td>
              <th>شماره پستی</th>
              <td>{formatValue(taxpayer.postalCode)}</td>
            </tr>
            <tr>
              <th>اقامتگاه قانونی</th>
              <td colSpan="3">{formatValue(taxpayer.address)}</td>
            </tr>
          </tbody>
        </table>

        <p className="print-legal-text">
          به دلیل عدم پرداخت مالیات موضوع برگ قطعی به شماره {formatValue(finalized.debtPaperUniqueCode)} که طی برگ تشخیص/قطعی
          مربوط به عملکرد {formatValue(finalized.performanceYear || '-')} ابلاغ گردیده است، حسب مقررات قانون مالیات‌های مستقیم
          اخطار می‌شود چنانچه ظرف مدت ده روز از تاریخ ابلاغ این برگ نسبت به پرداخت / ترتیب پرداخت بدهی مالیاتی خود به شرح
          جداول زیر اقدام ننمایید، عملیات اجرایی وصول مطالبات مالیاتی شامل توقیف اموال، ممنوع‌الخروجی و سایر اقدامات قانونی
          برابر مقررات انجام خواهد شد. تاریخ پایان مهلت اقدام: {formatValue(legalDeadlineDate)}.
        </p>

        <table className="print-amount-table">
          <caption>جدول محاسبه مالیات متعلقه</caption>
          <thead>
            <tr>
              <th>شرح</th>
              <th>مبلغ (ریال)</th>
            </tr>
          </thead>
          <tbody>
            <tr><td>مالیات متعلقه</td><td>{formatMoney(debt.taxAmount)}</td></tr>
            <tr><td>عوارض و بدهی‌های مرتبط</td><td>{formatMoney(debt.dutyAmount)}</td></tr>
            <tr><td>مانده مالیات ارسالی</td><td>{formatMoney(debt.remainingTax)}</td></tr>
            <tr><td>جمع بدهی مالیاتی به عدد</td><td>{formatMoney(totalTax)}</td></tr>
            <tr><td>جمع بدهی مالیاتی به حروف</td><td>{numberToPersianWords(totalTax)} ریال</td></tr>
          </tbody>
        </table>

        <table className="print-amount-table">
          <caption>جدول محاسبه جرایم مالیاتی</caption>
          <thead>
            <tr>
              <th>ردیف</th>
              <th>شرح جریمه</th>
              <th>مبلغ (ریال)</th>
            </tr>
          </thead>
          <tbody>
            <tr><td>۱</td><td>جریمه ثابت موضوع پرونده</td><td>{formatMoney(debt.penaltyArticle22)}</td></tr>
            <tr><td>۲</td><td>جریمه دیرکرد پرداخت</td><td>{formatMoney(debt.penaltyArticle23)}</td></tr>
            <tr><td>۳</td><td>جرایم غیرقابل بخشش</td><td>{formatMoney(debt.nonDeductiblePenalty)}</td></tr>
            <tr><td colSpan="2">جمع جرایم</td><td>{formatMoney(totalPenalty)}</td></tr>
            <tr><td colSpan="2">جمع کل قابل وصول</td><td>{formatMoney(totalDebt)}</td></tr>
          </tbody>
        </table>

        <footer className="print-signature">
          <div>
            <strong>مشخصات صادرکننده</strong>
            <span>نام و نام خانوادگی:</span>
            <span>سمت:</span>
          </div>
          <div>
            <strong>مهر و امضاء</strong>
          </div>
        </footer>
      </section>
    </div>
  )
}

export default function ExecutionPaperDetailsPage() {
  const { paperId } = useParams()
  const router = useRouter()
  const [data, setData] = useState(null)
  const [trace, setTrace] = useState(null)
  const [canViewTrace, setCanViewTrace] = useState(true)
  const [loading, setLoading] = useState(true)
  const [traceLoading, setTraceLoading] = useState(true)
  const [error, setError] = useState(null)
  const [traceError, setTraceError] = useState(null)
  const [relatedPersons, setRelatedPersons] = useState(null)
  const [relatedPersonsLoading, setRelatedPersonsLoading] = useState(false)
  const [relatedPersonsRefreshing, setRelatedPersonsRefreshing] = useState(false)
  const [relatedPersonsError, setRelatedPersonsError] = useState(null)

  const loadRelatedPersons = () => {
    setRelatedPersonsLoading(true)
    setRelatedPersonsError(null)
    return apiFetch(`/api/v1/execution-papers/${paperId}/related-persons`)
      .then(async response => {
        if (!response.ok) {
          const details = await response.text()
          throw new Error(details || `HTTP ${response.status}`)
        }
        return response.json()
      })
      .then(json => {
        setRelatedPersons(json)
        setRelatedPersonsLoading(false)
        return json
      })
      .catch(error => {
        setRelatedPersonsError(error.message)
        setRelatedPersonsLoading(false)
        throw error
      })
  }

  useEffect(() => {
    setLoading(true)
    setTraceLoading(true)
    setError(null)
    setTraceError(null)
    setCanViewTrace(true)
    apiFetch(`/api/v1/execution-papers/${paperId}`)
      .then(r => {
        if (!r.ok) {
          if (r.status === 401) throw new Error('نشست شما معتبر نیست. دوباره وارد شوید.')
          if (r.status === 403) throw new Error('شما مجوز مشاهده جزئیات برگ اجرا را ندارید.')
          throw new Error(`HTTP ${r.status}`)
        }
        return r.json()
      })
      .then(json => {
        setData(json)
        setLoading(false)
        if (json?.taxpayer?.type === 'LEGAL') {
          loadRelatedPersons().catch(() => {})
        } else {
          setRelatedPersons(null)
          setRelatedPersonsLoading(false)
        }
      })
      .catch(err => { setError(err.message); setLoading(false) })

    apiFetch(`/api/v1/execution-papers/${paperId}/trace`)
      .then(async r => {
        if (r.status === 403 || r.status === 401) {
          setCanViewTrace(false)
          setTraceLoading(false)
          return null
        }
        if (!r.ok) {
          throw new Error(`HTTP ${r.status}`)
        }
        return r.json()
      })
      .then(json => {
        if (json) {
          setTrace(json)
          setTraceLoading(false)
        }
      })
      .catch(err => { setTraceError(err.message); setTraceLoading(false) })
  }, [paperId])

  const refreshRelatedPersons = () => {
    setRelatedPersonsRefreshing(true)
    setRelatedPersonsError(null)
    apiFetch(`/api/v1/execution-papers/${paperId}/related-persons/refresh`, { method: 'POST' })
      .then(async response => {
        if (!response.ok) {
          const details = await response.text()
          throw new Error(details || `HTTP ${response.status}`)
        }
        return response.json()
      })
      .then(json => {
        setRelatedPersons(json)
        setRelatedPersonsRefreshing(false)
        return apiFetch(`/api/v1/execution-papers/${paperId}/trace`)
      })
      .then(response => response.ok ? response.json() : null)
      .then(json => { if (json) setTrace(json) })
      .catch(error => {
        setRelatedPersonsError(error.message)
        setRelatedPersonsRefreshing(false)
      })
  }

  const printExecutionPaper = () => {
    document.body.classList.add('execution-printing')
    const cleanup = () => {
      document.body.classList.remove('execution-printing')
      window.removeEventListener('afterprint', cleanup)
    }
    window.addEventListener('afterprint', cleanup)
    window.setTimeout(() => {
      window.print()
      window.setTimeout(cleanup, 500)
    }, 50)
  }

  return (
    <>
      <AppShell
        active="execution-papers"
        title="دستیار هوشمند حسابرسی"
        eyebrow="برگ‌های اجرا"
      >
        <main style={{ maxWidth: 1200, margin: '0 auto' }}>
          <div style={pageTitleRowStyle}>
            <h2 style={pageTitleStyle}>مشاهده جزئیات برگ اجرا</h2>
            <div style={pageActionGroupStyle}>
              <button type="button" onClick={() => router.push('/execution-papers')} style={backButtonStyle}>
                <BackIcon />
                <span>بازگشت به لیست</span>
              </button>
              <button
                type="button"
                onClick={printExecutionPaper}
                disabled={loading || error || !data}
                style={{
                  ...printButtonStyle,
                  ...((loading || error || !data) ? printButtonDisabledStyle : {})
                }}
              >
                <PrintIcon />
                <span>چاپ برگ اجرا</span>
              </button>
            </div>
          </div>
          {loading && <div style={cardStyle}>در حال دریافت اطلاعات...</div>}
          {!loading && error && <div style={{ ...cardStyle, color: '#dc2626' }}>خطا در دریافت اطلاعات: {error}</div>}
          {!loading && !error && data && SECTIONS.map(section => <DetailSection key={section.key} section={section} data={data[section.key]} />)}
          {!loading && !error && data?.taxpayer?.type === 'LEGAL' && (
            <RelatedPersonsSection
              data={relatedPersons}
              loading={relatedPersonsLoading}
              refreshing={relatedPersonsRefreshing}
              error={relatedPersonsError}
              onRefresh={refreshRelatedPersons}
            />
          )}
          {!loading && !error && canViewTrace && <TraceSection trace={trace} loading={traceLoading} error={traceError} />}
        </main>
      </AppShell>
      {!loading && !error && data && <PrintExecutionPaper data={data} />}
    </>
  )
}

function shortDate(value) {
  if (!value) return ''
  const raw = String(value)
  if (/^\d{4}-\d{2}-\d{2}/.test(raw)) {
    const [year, month, day] = raw.slice(0, 10).split('-')
    return `${year}/${month}/${day}`
  }
  return raw
}

function jalaliDate(value) {
  if (!value) return ''
  const raw = String(value)
  if (/^1[34]\d{2}\/\d{1,2}\/\d{1,2}$/.test(raw)) {
    return normalizeDateSlash(raw)
  }
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) {
    return normalizeDateSlash(raw)
  }
  return new Intl.DateTimeFormat('fa-IR-u-ca-persian', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(date).replace(/\u200e/g, '')
}

function formatPersianDate(value) {
  if (!value) return '—'
  const raw = String(value).trim()
  if (/^1[34]\d{2}[/-]\d{1,2}[/-]\d{1,2}$/.test(raw)) {
    return toPersianDigits(normalizeDateSlash(raw))
  }
  const date = parseDateValue(raw)
  if (!date) return formatValue(raw)
  return cleanPersianDateTime(new Intl.DateTimeFormat('fa-IR-u-ca-persian', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(date))
}

function formatPersianDateTime(value) {
  if (!value) return '—'
  const raw = String(value).trim()
  if (/^1[34]\d{2}[/-]\d{1,2}[/-]\d{1,2}$/.test(raw)) {
    return toPersianDigits(normalizeDateSlash(raw))
  }
  const date = parseDateValue(raw)
  if (!date) return formatValue(raw)
  return cleanPersianDateTime(new Intl.DateTimeFormat('fa-IR-u-ca-persian', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date))
}

function parseDateValue(value) {
  const raw = String(value).trim()
  const dateOnly = raw.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})$/)
  if (dateOnly) {
    if (Number(dateOnly[1]) < 1700) return null
    return new Date(Number(dateOnly[1]), Number(dateOnly[2]) - 1, Number(dateOnly[3]))
  }
  const normalized = raw.includes(' ') && !raw.includes('T')
    ? raw.replace(' ', 'T')
    : raw
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

function cleanPersianDateTime(value) {
  return toPersianDigits(String(value).replace(/\u200e/g, '').replace(',', ' -'))
}

function addDaysJalali(value, days) {
  const date = value ? new Date(value) : new Date()
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  date.setDate(date.getDate() + days)
  return jalaliDate(date.toISOString())
}

function normalizeDateSlash(value) {
  return String(value).replaceAll('-', '/')
}

function sumValues(values) {
  return values.reduce((sum, value) => {
    const number = Number(value)
    return Number.isNaN(number) ? sum : sum + number
  }, 0)
}

function numberToPersianWords(value) {
  const number = Math.round(Number(value || 0))
  if (number === 0) return 'صفر'
  const groups = ['', 'هزار', 'میلیون', 'میلیارد', 'هزار میلیارد']
  const parts = []
  let remaining = number
  let index = 0
  while (remaining > 0 && index < groups.length) {
    const chunk = remaining % 1000
    if (chunk) {
      parts.unshift(`${threeDigitToWords(chunk)}${groups[index] ? ` ${groups[index]}` : ''}`)
    }
    remaining = Math.floor(remaining / 1000)
    index += 1
  }
  return parts.join(' و ')
}

function threeDigitToWords(number) {
  const ones = ['', 'یک', 'دو', 'سه', 'چهار', 'پنج', 'شش', 'هفت', 'هشت', 'نه']
  const teens = ['ده', 'یازده', 'دوازده', 'سیزده', 'چهارده', 'پانزده', 'شانزده', 'هفده', 'هجده', 'نوزده']
  const tens = ['', '', 'بیست', 'سی', 'چهل', 'پنجاه', 'شصت', 'هفتاد', 'هشتاد', 'نود']
  const hundreds = ['', 'یکصد', 'دویست', 'سیصد', 'چهارصد', 'پانصد', 'ششصد', 'هفتصد', 'هشتصد', 'نهصد']
  const parts = []
  const h = Math.floor(number / 100)
  const rest = number % 100
  if (h) parts.push(hundreds[h])
  if (rest >= 10 && rest < 20) {
    parts.push(teens[rest - 10])
  } else {
    const t = Math.floor(rest / 10)
    const o = rest % 10
    if (t) parts.push(tens[t])
    if (o) parts.push(ones[o])
  }
  return parts.join(' و ')
}

function formatDateTime(value) {
  if (!value) return '—'
  try {
    return new Intl.DateTimeFormat('fa-IR-u-ca-persian', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(value))
  } catch {
    return formatValue(value)
  }
}

function formatAuditValue(value) {
  if (!value) return '—'
  const cleaned = String(value)
    .replace(/taskKey=\d+;\s*/g, '')
    .replace('value=', '')
    .trim()

  if (/^\d{4}-\d{2}-\d{2}(?:$|[T\s])/.test(cleaned)) {
    return formatPersianDateTime(cleaned)
  }

  return formatValue(cleaned)
}

const cardStyle = { background: 'white', border: '1px solid #e5e7eb', borderRadius: 12, boxShadow: '0 1px 4px rgba(0,0,0,0.06)', padding: 20, marginBottom: 18 }
const pageTitleRowStyle = { display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap', margin: '16px 0 24px' }
const pageTitleStyle = { color: '#1e3a5f', margin: 0, fontSize: 24 }
const pageActionGroupStyle = { display: 'inline-flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }
const sectionTitleStyle = { margin: '0 0 16px', color: '#1f2937', fontSize: 18 }
const gridStyle = { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }
const itemStyle = { border: '1px solid #eef2ff', borderRadius: 10, background: '#f8fafc', padding: 12 }
const labelStyle = { display: 'block', color: '#6b7280', fontSize: 12, marginBottom: 6 }
const valueStyle = { color: '#111827', fontSize: 14, wordBreak: 'break-word' }
const backButtonStyle = {
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: 8,
  minHeight: 40,
  padding: '9px 14px',
  borderRadius: 8,
  border: '1px solid #d6dde8',
  background: '#ffffff',
  color: '#334155',
  cursor: 'pointer',
  fontSize: 14,
  fontWeight: 800,
  boxShadow: '0 1px 2px rgba(15, 23, 42, 0.04)'
}
const printButtonStyle = {
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: 8,
  minHeight: 40,
  padding: '9px 16px',
  borderRadius: 8,
  border: '1px solid #b6c9e4',
  background: '#f8fbff',
  color: '#14365d',
  cursor: 'pointer',
  fontSize: 14,
  fontWeight: 800,
  boxShadow: '0 1px 2px rgba(15, 23, 42, 0.05)'
}
const printButtonDisabledStyle = { opacity: 0.55, cursor: 'not-allowed' }
const traceCardStyle = { ...cardStyle, borderColor: '#dbeafe', background: 'linear-gradient(180deg, #ffffff, #f8fbff)' }
const traceHeaderStyle = { display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, marginBottom: 16 }
const traceEyebrowStyle = { margin: 0, color: '#15803d', fontSize: 12, fontWeight: 800 }
const mutedTextStyle = { color: '#64748b', margin: 0, lineHeight: 1.9 }
const errorTextStyle = { color: '#dc2626', margin: 0, lineHeight: 1.9 }
const timelineStyle = { display: 'grid', gap: 14, borderRight: '2px solid #dbeafe', paddingRight: 18 }
const timelineItemStyle = { position: 'relative', display: 'grid', gap: 8 }
const timelineDotStyle = { position: 'absolute', right: -24, top: 10, width: 10, height: 10, borderRadius: 999, background: '#15803d', boxShadow: '0 0 0 4px #dcfce7' }
const timelineContentStyle = { border: '1px solid #e5e7eb', borderRadius: 10, background: '#ffffff', padding: 14 }
const timelineTopStyle = { display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center', marginBottom: 10 }
const timelineTitleStyle = { color: '#14365d', fontSize: 14 }
const timelineDateStyle = { color: '#64748b', fontSize: 12, whiteSpace: 'nowrap' }
const timelineMetaStyle = { display: 'flex', flexWrap: 'wrap', gap: 8, color: '#475569', fontSize: 12, lineHeight: 1.8 }
const traceChangeBoxStyle = {
  display: 'grid',
  gap: 8,
  marginTop: 12,
  padding: 10,
  border: '1px solid #dbeafe',
  borderRadius: 8,
  background: '#f8fbff'
}
const traceChangeFieldStyle = { color: '#1e3a8a', fontSize: 12, fontWeight: 900 }
const traceChangeValuesStyle = {
  display: 'grid',
  gridTemplateColumns: 'minmax(0, 1fr) auto minmax(0, 1fr)',
  alignItems: 'stretch',
  gap: 8
}
const traceChangeValueStyle = {
  display: 'grid',
  gap: 4,
  minWidth: 0,
  padding: '8px 10px',
  border: '1px solid #e2e8f0',
  borderRadius: 7,
  background: '#ffffff',
  color: '#0f172a',
  fontSize: 12,
  lineHeight: 1.8,
  overflowWrap: 'anywhere'
}
const traceChangeLabelStyle = { color: '#64748b', fontSize: 11, fontWeight: 800 }
const traceChangeArrowStyle = {
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  color: '#2563eb',
  fontWeight: 900
}
const relatedPersonsCardStyle = { ...cardStyle, borderColor: '#bbf7d0', background: 'linear-gradient(180deg, #ffffff, #f7fff9)' }
const relatedPersonsHeaderStyle = { display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 14, flexWrap: 'wrap', marginBottom: 16 }
const mutedMetaStyle = { color: '#64748b', fontSize: 12, lineHeight: 1.8 }
const refreshRelatedButtonStyle = {
  minHeight: 38,
  padding: '7px 13px',
  border: '1px solid #86efac',
  borderRadius: 8,
  background: '#15803d',
  color: '#ffffff',
  cursor: 'pointer',
  fontSize: 12,
  fontWeight: 900
}
const relatedPersonsTableScrollStyle = { overflowX: 'auto', border: '1px solid #dcfce7', borderRadius: 9 }
const relatedPersonsTableStyle = { width: '100%', minWidth: 850, borderCollapse: 'collapse', background: '#ffffff' }
const relatedPersonHeaderCellStyle = { padding: '10px 12px', color: '#166534', background: '#f0fdf4', borderBottom: '1px solid #bbf7d0', fontSize: 12, whiteSpace: 'nowrap', textAlign: 'center' }
const relatedPersonCellStyle = { padding: '11px 12px', color: '#334155', borderBottom: '1px solid #ecfdf5', fontSize: 12, lineHeight: 1.7, textAlign: 'center', whiteSpace: 'nowrap' }
