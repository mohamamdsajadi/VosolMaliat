'use client'

import { useEffect, useMemo, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import AppHeader from '../../components/AppHeader'

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
    labels: { debtPaperUniqueCode: 'شناسه یکتا/ارجاع پرونده', debtPaperIssueDate: 'تاریخ قطعیت', notificationId: 'شماره ابلاغ', createdAt: 'تاریخ ثبت' }
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

export default function ExecutionPaperDetailsPage() {
  const { paperId } = useParams()
  const router = useRouter()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    setLoading(true)
    fetch(`/api/v1/execution-papers/${paperId}`)
      .then(r => { if (!r.ok) throw new Error(`HTTP ${r.status}`); return r.json() })
      .then(json => { setData(json); setLoading(false) })
      .catch(err => { setError(err.message); setLoading(false) })
  }, [paperId])

  return (
    <div style={{ minHeight: '100vh', background: '#f9fafb' }} dir="rtl">
      <AppHeader title="دستیار هوشمند حسابرسی" subtitle="جزئیات برگ اجرا" />
      <main style={{ padding: '28px 24px', maxWidth: 1200, margin: '0 auto' }}>
        <button onClick={() => router.push('/execution-papers')} style={backButtonStyle}>بازگشت به لیست</button>
        <h2 style={{ color: '#1e3a5f', margin: '16px 0 24px', fontSize: 24 }}>مشاهده جزئیات برگ اجرا</h2>
        {loading && <div style={cardStyle}>در حال دریافت اطلاعات...</div>}
        {!loading && error && <div style={{ ...cardStyle, color: '#dc2626' }}>خطا در دریافت اطلاعات: {error}</div>}
        {!loading && !error && data && SECTIONS.map(section => <DetailSection key={section.key} section={section} data={data[section.key]} />)}
      </main>
    </div>
  )
}

const cardStyle = { background: 'white', border: '1px solid #e5e7eb', borderRadius: 12, boxShadow: '0 1px 4px rgba(0,0,0,0.06)', padding: 20, marginBottom: 18 }
const sectionTitleStyle = { margin: '0 0 16px', color: '#1f2937', fontSize: 18 }
const gridStyle = { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }
const itemStyle = { border: '1px solid #eef2ff', borderRadius: 10, background: '#f8fafc', padding: 12 }
const labelStyle = { display: 'block', color: '#6b7280', fontSize: 12, marginBottom: 6 }
const valueStyle = { color: '#111827', fontSize: 14, wordBreak: 'break-word' }
const backButtonStyle = { background: '#2563eb', color: 'white', border: 'none', borderRadius: 8, cursor: 'pointer', fontSize: 14, fontWeight: 700, padding: '10px 18px' }
