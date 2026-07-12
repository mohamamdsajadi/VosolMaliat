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

const SECTIONS = [
  {
    key: 'executionPaper',
    title: 'اطلاعات برگ اجرا',
    labels: {
      uniqueCode: 'شناسه یکتای برگ اجرا', currentState: 'وضعیت فعلی', finalizationReason: 'علت قطعیت', taxSource: 'منبع مالیات',
      mainTaxBureauId: 'کد اداره کل', taxBureauId: 'کد امور مالیاتی', taxUnitId: 'کد واحد مالیاتی', sendingUnitId: 'کد واحد ارسال‌کننده', createdAt: 'تاریخ ایجاد'
    }
  },
  {
    key: 'finalizedDebtPaper',
    title: 'اطلاعات برگ بدهی قطعی',
    labels: { debtPaperUniqueCode: 'شناسه یکتا/ارجاع پرونده', debtPaperIssueDate: 'تاریخ قطعیت', notificationId: 'شماره ابلاغ', finalizationReason: 'علت قطعیت', taxSource: 'منبع مالیاتی', performanceYear: 'سال عملکرد', performancePeriod: 'دوره عملکرد', activityTypeCode: 'نوع فعالیت', mainTaxBureauId: 'اداره کل', taxBureauId: 'اداره امور مالیاتی', taxUnitId: 'واحد مالیاتی', totalFinalizedAmount: 'کل مبلغ قطعی‌شده', finalizedTaxAndNonDeductibleAmount: 'مبلغ قطعی‌شده بدون جریمه ق.ب', createdAt: 'تاریخ ثبت' }
  },
  {
    key: 'taxpayer',
    title: 'اطلاعات مودی',
    labels: { type: 'نوع مودی', firstName: 'نام', lastName: 'نام خانوادگی', nationalCode: 'کد ملی', birthDate: 'تاریخ تولد', fatherName: 'نام پدر', nationalId: 'شناسه ملی', companyName: 'نام/شهرت تجاری', registrationNumber: 'شماره ثبت' }
  },
  {
    key: 'debtInformation',
    title: 'ریز مبالغ بدهی',
    labels: { taxAmount: 'اصل مالیات', dutyAmount: 'اصل عوارض', penaltyArticle22: 'جریمه ثابت', penaltyArticle23: 'جریمه دیرکرد', nonDeductiblePenalty: 'جریمه غیرقابل بخشش', deductiblePenalty: 'بخشودگی/کسر جریمه', taxPaidAmount: 'پرداخت اصل', penaltyPaidAmount: 'پرداخت جریمه', taxCredit: 'اعتبار', remainingTax: 'مانده بدهی ارسالی', totalDebt: 'مانده محاسبه‌شده' }
  },
  {
    key: 'notification',
    title: 'اطلاعات ابلاغ',
    labels: { electronicNotificationDate: 'تاریخ ابلاغ الکترونیکی', createdAt: 'تاریخ ثبت ابلاغ' }
  },
]

function DetailSection({ section, data }) {
  const entries = useMemo(() => Object.entries(section.labels).filter(([key]) => data && Object.prototype.hasOwnProperty.call(data, key)), [section, data])
  return (
    <section style={cardStyle}>
      <h3 style={sectionTitleStyle}>{section.title}</h3>
      {entries.length === 0 ? <p style={{ color: '#9ca3af', margin: 0 }}>اطلاعاتی ثبت نشده است.</p> : (
        <div style={gridStyle}>
          {entries.map(([key, label]) => (
            <div key={key} style={itemStyle}>
              <span style={labelStyle}>{label}</span>
              <strong style={valueStyle}>{formatValue(data[key])}</strong>
            </div>
          ))}
        </div>
      )}
    </section>
  )
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
                  <span style={timelineDateStyle}>{formatDateTime(item.occurredAt)}</span>
                </div>
                <div style={timelineMetaStyle}>
                  <span>انجام‌دهنده: {formatValue(item.actorUsername || 'system')}</span>
                  {item.oldValue && <span>قبلی: {formatAuditValue(item.oldValue)}</span>}
                  {item.newValue && <span>جدید: {formatAuditValue(item.newValue)}</span>}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
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
      .then(json => { setData(json); setLoading(false) })
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

  return (
    <AppShell
      active="execution-papers"
      title="دستیار هوشمند حسابرسی"
      eyebrow="برگ‌های اجرا"
      subtitle="جزئیات برگ اجرا"
    >
      <main style={{ maxWidth: 1200, margin: '0 auto' }}>
        <button onClick={() => router.push('/execution-papers')} style={backButtonStyle}>بازگشت به لیست</button>
        <h2 style={{ color: '#1e3a5f', margin: '16px 0 24px', fontSize: 24 }}>مشاهده جزئیات برگ اجرا</h2>
        {loading && <div style={cardStyle}>در حال دریافت اطلاعات...</div>}
        {!loading && error && <div style={{ ...cardStyle, color: '#dc2626' }}>خطا در دریافت اطلاعات: {error}</div>}
        {!loading && !error && data && SECTIONS.map(section => <DetailSection key={section.key} section={section} data={data[section.key]} />)}
        {!loading && !error && canViewTrace && <TraceSection trace={trace} loading={traceLoading} error={traceError} />}
      </main>
    </AppShell>
  )
}

function formatDateTime(value) {
  if (!value) return '—'
  try {
    return new Intl.DateTimeFormat('fa-IR', {
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
  return formatValue(String(value).replace(/taskKey=\d+;\s*/g, '').replace('value=', ''))
}

const cardStyle = { background: 'white', border: '1px solid #e5e7eb', borderRadius: 12, boxShadow: '0 1px 4px rgba(0,0,0,0.06)', padding: 20, marginBottom: 18 }
const sectionTitleStyle = { margin: '0 0 16px', color: '#1f2937', fontSize: 18 }
const gridStyle = { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }
const itemStyle = { border: '1px solid #eef2ff', borderRadius: 10, background: '#f8fafc', padding: 12 }
const labelStyle = { display: 'block', color: '#6b7280', fontSize: 12, marginBottom: 6 }
const valueStyle = { color: '#111827', fontSize: 14, wordBreak: 'break-word' }
const backButtonStyle = { background: '#2563eb', color: 'white', border: 'none', borderRadius: 8, cursor: 'pointer', fontSize: 14, fontWeight: 700, padding: '10px 18px' }
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
