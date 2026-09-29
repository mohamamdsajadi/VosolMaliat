'use client'

import { useEffect, useState } from 'react'
import { Cell, Pie, PieChart, Tooltip } from 'recharts'
import AppShell from '../../components/app-shell'
import RefreshButton from '../../components/refresh-button'
import { sendFinalizedDebtPaper } from '../../lib/admin-data-entry'
import { apiJson } from '../../lib/api'
import { formatJalaliDateTime } from '../../lib/display'
import { fetchGeneralDirectorates, fetchTaxAffairsOffices } from '../../lib/security'
import {
  randomDebtPaperSample,
  randomLegalPersonDebtPaperSample,
  toDebtPaperPayload, MoneyFields
} from '../data-entry/form-parts'

const CHART_COLORS = ['#1d4ed8', '#dc2626', '#059669']

const TABLE_HEADERS = [
  '#',
  'نوع پیام',
  'شناسه مرجع',
  'وضعیت',
  'اطلاعات ابلاغ',
  'تاریخ رفع محدودیت',
  'زمان صدور برگه اجرا',
  'زمان ارسال',
  'زمان تایید',
  'عملیات'
]

function isRejection(item) {
  return item.messageType === 'ISSUANCE_REJECTION'
}

function messageTypeLabel(item) {
  return {
    EXECUTION_PAPER_ID: 'ارسال شناسه یکتا برگه اجرا',
    INFORM_DATE: 'ارسال تاریخ ابلاغ',
    ISSUANCE_REJECTION: 'رد صدور برگ اجرا'
  }[item.messageType] || item.messageType || 'نوع پیام نامشخص'
}

function statusLabel(status) {
  return {
    SENT: 'ارسال شده به سنیم',
    CONFIRMED: 'دریافت توسط سنیم تایید شد'
  }[status] || status || 'نامشخص'
}

function countByStatus(items, status) {
  return items.filter(item => item.status === status).length
}

function countRejections(items) {
  return items.filter(isRejection).length
}

function toPersianDigits(value) {
  return String(value ?? 0).replace(/\d/g, digit => '۰۱۲۳۴۵۶۷۸۹'[digit])
}

function money(value) {
  const number = Number(value || 0)
  return number.toLocaleString('fa-IR')
}

function SanimStatsPie({ items, stats }) {
  const data = [
    { name: 'برگ اجرا صادر شده', value: Number(stats?.totalIssuedExecutionPapers || 0) },
    { name: 'برگ اجرا رد شده', value: Number(stats?.rejectedDebtPapers || 0) },
    { name: 'تایید شده', value: countByStatus(items, 'CONFIRMED') }
  ]
  const total = data.reduce((sum, item) => sum + item.value, 0)

  return (
    <section className="fake-sanim-chart-card">
      <div className="fake-sanim-chart-card__heading">
        <div>
          <p className="eyebrow">گزارش آماری</p>
          <h2>وضعیت کلی برگ‌های اجرا</h2>
        </div>
        <span>{toPersianDigits(total.toLocaleString('en-US'))} مورد</span>
      </div>
      {total === 0 ? (
        <div className="empty-state compact">داده‌ای برای نمایش نمودار وجود ندارد.</div>
      ) : (
        <div className="fake-sanim-chart-layout">
          <PieChart width={250} height={210}>
            <Pie data={data} dataKey="value" nameKey="name" cx={125} cy={100} innerRadius={48} outerRadius={86} paddingAngle={3}>
              {data.map((entry, index) => <Cell key={entry.name} fill={CHART_COLORS[index]} />)}
            </Pie>
            <Tooltip
              formatter={(value) => toPersianDigits(Number(value).toLocaleString('en-US'))}
              contentStyle={{ direction: 'rtl', borderRadius: 8, borderColor: '#e5e7eb' }}
            />
          </PieChart>
          <div className="fake-sanim-chart-legend">
            {data.map((item, index) => (
              <div key={item.name}>
                <span style={{ background: CHART_COLORS[index] }} />
                <strong>{item.name}</strong>
                <em>{toPersianDigits(item.value.toLocaleString('en-US'))}</em>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  )
}

export default function FakeSanimPage() {
  const [items, setItems] = useState([])
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [sendingRandom, setSendingRandom] = useState(false)
  const [sampleModalOpen, setSampleModalOpen] = useState(false)
  const [sampleForm, setSampleForm] = useState(null)
  const [masterdata, setMasterdata] = useState({ generals: [], offices: [], units: [] })
  const [message, setMessage] = useState('')

  const load = async () => {
    setLoading(true)
    const [requests, dashboardStats] = await Promise.all([
      apiJson('/api/v1/fake-sanim/inform-requests'),
      apiJson('/api/v1/dashboard/stats')
    ])
    setItems(requests)
    setStats(dashboardStats)
    setLoading(false)
  }

  useEffect(() => {
    load().catch(error => { setMessage(error.message); setLoading(false) })
  }, [])

  const confirm = async id => {
    setMessage('')
    await apiJson(`/api/v1/fake-sanim/inform-requests/${id}/confirm`, { method: 'POST' })
    setMessage('دریافت پیام در سنیم آزمایشی تایید شد.')
    await load()
  }

  const prepareRandomDebtPaper = async (taxpayerType = null) => {
    setMessage('')
    setSendingRandom(true)
    try {
      const [generals, offices] = await Promise.all([
        fetchGeneralDirectorates(),
        fetchTaxAffairsOffices()
      ])
      const loadedMasterdata = { generals, offices }
      setMasterdata(loadedMasterdata)
      setSampleForm(
        taxpayerType === 'LEGAL'
          ? randomLegalPersonDebtPaperSample(loadedMasterdata)
          : randomDebtPaperSample(loadedMasterdata)
      )
      setSampleModalOpen(true)
    } catch (error) {
      setMessage(error.message || 'ساخت برگ بدهی تصادفی ناموفق بود.')
    } finally {
      setSendingRandom(false)
    }
  }

  const updateSampleForm = (event) => {
    const { name, value } = event.target
    setSampleForm(current => ({ ...current, [name]: value }))
  }

  const submitSampleForm = async () => {
    if (!sampleForm) return
    setMessage('')
    setSendingRandom(true)
    try {
      const result = await sendFinalizedDebtPaper(toDebtPaperPayload(sampleForm))
      setMessage(`برگ بدهی با شناسه ${sampleForm.debtPaperUniqueCode} ارسال شد. ${result.message || ''}`)
      setSampleModalOpen(false)
      setSampleForm(null)
      await load()
    } catch (error) {
      setMessage(error.message || 'ارسال برگ بدهی ناموفق بود.')
    } finally {
      setSendingRandom(false)
    }
  }

  return (
    <AppShell
      active="test-front"
      title="سامانه عملیاتی سنیم آزمایشی"
      eyebrow="Fake External System"
      actions={(
        <div className="row-actions">
          <button
            className="fake-service-random-action fake-service-random-action--legal"
            type="button"
            onClick={() => prepareRandomDebtPaper('LEGAL')}
            disabled={sendingRandom}
          >
            <span aria-hidden="true">◆</span>
            {sendingRandom ? 'در حال آماده‌سازی...' : 'ساخت برگ بدهی شخص حقوقی تصادفی'}
          </button>
          <button className="fake-service-random-action" type="button" onClick={() => prepareRandomDebtPaper()} disabled={sendingRandom}>
            <span aria-hidden="true">✦</span>
            {sendingRandom ? 'در حال آماده‌سازی...' : 'ساخت و ارسال برگ بدهی تصادفی'}
          </button>
          <RefreshButton onClick={load} loading={loading} />
        </div>
      )}
    >
      <main className="fake-service-page fake-service-page--sanim">
        <section className="fake-service-hero">
          <div>
            <p className="eyebrow">سامانه بیرونی آزمایشی</p>
            <h2>درخواست‌های ارسال تاریخ ابلاغ به سنیم</h2>
          </div>
          <div className="fake-service-stats">
            <span>کل: {items.length.toLocaleString('fa-IR')}</span>
            <span>ارسال شده: {countByStatus(items, 'SENT').toLocaleString('fa-IR')}</span>
            <span>تایید شده: {countByStatus(items, 'CONFIRMED').toLocaleString('fa-IR')}</span>
            <span className="fake-service-stat--danger">رد صدور: {countRejections(items).toLocaleString('fa-IR')}</span>
          </div>
        </section>

        {message && <div className="notice">{message}</div>}
        <SanimStatsPie items={items} stats={stats} />
        {loading ? <div className="empty-state compact">در حال دریافت درخواست‌ها...</div> : items.length === 0 ? (
          <section className="empty-state">هنوز تاریخ ابلاغی به سنیم آزمایشی ارسال نشده است.</section>
        ) : (
          <div className="task-table-card fake-service-table-card">
            <div className="task-table-header">
              <div>
                <p className="eyebrow">درخواست‌ها</p>
                <h2>فهرست ارسال به سنیم</h2>
              </div>
              <span>{items.length.toLocaleString('fa-IR')} مورد</span>
            </div>
            <div className="task-table-scroll">
              <table className="task-grid-table fake-service-table fake-service-table--sanim">
                <thead>
                  <tr>{TABLE_HEADERS.map(header => <th key={header}>{header}</th>)}</tr>
                </thead>
                <tbody>
                  {items.map((item, index) => (
                    <tr key={item.id} className={`task-grid-row ${index % 2 ? 'task-grid-row--zebra' : ''} ${isRejection(item) ? 'fake-service-row--rejection' : ''}`}>
                      <td className="task-grid-cell task-grid-cell--index">{(index + 1).toLocaleString('fa-IR')}</td>
                      <td className="task-grid-cell">
                        <span className={`fake-service-badge ${isRejection(item) ? 'fake-service-badge--rejection' : 'fake-service-badge--inform'}`}>
                          {messageTypeLabel(item)}
                        </span>
                      </td>
                      <td className="task-grid-cell task-grid-cell--paper">
                        <span>{isRejection(item) ? 'شناسه برگ بدهی' : 'شناسه برگ اجرا'}</span>
                        <strong>{isRejection(item) ? item.debtPaperId || '-' : item.executionPaperCode || `#${item.executionPaperId}`}</strong>
                      </td>
                      <td className="task-grid-cell">
                        <span className={`fake-service-badge fake-service-badge--${item.status === 'CONFIRMED' ? 'confirmed' : 'sent'}`}>
                          {statusLabel(item.status)}
                        </span>
                      </td>
                      <td className="task-grid-cell">
                        {isRejection(item) ? (
                          <div className="fake-service-rejection-detail">
                            <strong>{item.rejectionReasonCode || 'کد نامشخص'}</strong>
                            <span>{item.rejectionReasonDescription || 'شرح رد ثبت نشده است'}</span>
                          </div>
                        ) : (
                          formatJalaliDateTime(item.informDate)
                        )}
                      </td>
                      <td className="task-grid-cell">{isRejection(item) ? formatJalaliDateTime(item.restrictionRemovalDate) : '-'}</td>
                      <td className="task-grid-cell">{formatJalaliDateTime(item.executionPaperCreatedAt)}</td>
                      <td className="task-grid-cell">{formatJalaliDateTime(item.sentAt)}</td>
                      <td className="task-grid-cell">{formatJalaliDateTime(item.confirmedAt)}</td>
                      <td className="task-grid-cell task-grid-cell--actions">
                        {item.status !== 'CONFIRMED' ? (
                          <button className={`task-table-primary-action ${isRejection(item) ? 'task-table-primary-action--danger' : ''}`} onClick={() => confirm(item.id)}>تایید دریافت در سنیم</button>
                        ) : (
                          <span className="task-assignee-pill task-assignee-pill--assigned">تکمیل شده</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {sampleModalOpen && sampleForm && (
          <div className="fake-sanim-modal-backdrop" role="dialog" aria-modal="true">
            <section className="fake-sanim-modal">
              <div className="fake-sanim-modal__header">
                <div>
                  <p className="eyebrow">بازبینی قبل از ارسال</p>
                  <h2>برگ بدهی تصادفی</h2>
                </div>
                <button type="button" className="fake-sanim-modal__close" onClick={() => setSampleModalOpen(false)}>×</button>
              </div>
              <div className="fake-sanim-sample-grid">
                <div className="fake-sanim-sample-section-title">اطلاعات اصلی برگ بدهی</div>
                <label>
                  <span>شناسه برگ بدهی</span>
                  <input name="debtPaperUniqueCode" value={sampleForm.debtPaperUniqueCode} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>تاریخ صدور سند</span>
                  <input name="issueDateRaw" value={sampleForm.issueDateRaw} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>شماره سند</span>
                  <input name="documentNumber" value={sampleForm.documentNumber} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>دلیل صدور</span>
                  <input name="issuanceReason" value={sampleForm.issuanceReason} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>منبع مالیاتی</span>
                  <input name="taxSource" value={sampleForm.taxSource} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>سال عملکرد</span>
                  <input name="taxYear" value={sampleForm.taxYear} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>دوره عملکرد</span>
                  <input name="taxPeriod" value={sampleForm.taxPeriod} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>عنوان فعالیت</span>
                  <input name="activityTitle" value={sampleForm.activityTitle} onChange={updateSampleForm} />
                </label>

                <div className="fake-sanim-sample-section-title">ساختار سازمانی</div>
                <label>
                  <span>اداره کل</span>
                  <select name="mainTaxBureauId" value={sampleForm.mainTaxBureauId} onChange={updateSampleForm}>
                    <option value="">انتخاب نشده</option>
                    {masterdata.generals.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
                  </select>
                </label>
                <label>
                  <span>اداره مالیات</span>
                  <select name="taxBureauId" value={sampleForm.taxBureauId} onChange={updateSampleForm}>
                    <option value="">انتخاب نشده</option>
                    {masterdata.offices.map(item => <option key={item.id} value={item.id}>{item.officeName}</option>)}
                  </select>
                </label>
                <div className="fake-sanim-sample-section-title">اطلاعات مودی</div>
                <label>
                  <span>نوع مودی</span>
                  <select name="taxpayerType" value={sampleForm.taxpayerType} onChange={updateSampleForm}>
                    <option value="NATURAL">حقیقی</option>
                    <option value="LEGAL">حقوقی</option>
                  </select>
                </label>
                <label>
                  <span>کد ملی</span>
                  <input name="nationalCode" value={sampleForm.nationalCode} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>فیدا</span>
                  <input name="fida" value={sampleForm.fida} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>نام</span>
                  <input name="firstName" value={sampleForm.firstName} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>نام خانوادگی</span>
                  <input name="lastName" value={sampleForm.lastName} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>نام پدر</span>
                  <input name="fatherName" value={sampleForm.fatherName} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>تاریخ تولد</span>
                  <input name="birthDate" value={sampleForm.birthDate} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>شناسه ملی</span>
                  <input name="nationalId" value={sampleForm.nationalId} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>نام شرکت</span>
                  <input name="companyName" value={sampleForm.companyName} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>شماره ثبت</span>
                  <input name="registrationNumber" value={sampleForm.registrationNumber} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>تاریخ ثبت</span>
                  <input name="registrationDate" value={sampleForm.registrationDate} onChange={updateSampleForm} />
                </label>
                <label>
                  <span>کد پستی</span>
                  <input name="postalCode" value={sampleForm.postalCode} onChange={updateSampleForm} />
                </label>
                <label className="fake-sanim-sample-grid__wide">
                  <span>نشانی</span>
                  <input name="address" value={sampleForm.address} onChange={updateSampleForm} />
                </label>

                <div className="fake-sanim-sample-section-title">مبالغ تجمیعی</div>
                <MoneyFields form={sampleForm} onChange={updateSampleForm} />
              </div>
              <div className="fake-sanim-modal__summary">
                <span>نوع مودی: {sampleForm.taxpayerType === 'LEGAL' ? 'حقوقی' : 'حقیقی'}</span>
                <span>مبلغ مالیات: {money(sampleForm.totalTaxAmount)} ریال</span>
                <span>باقی‌مانده: {money(sampleForm.outstandingDebtAmount)} ریال</span>
              </div>
              <div className="fake-sanim-modal__actions">
                <button type="button" className="ghost-action" onClick={() => setSampleModalOpen(false)}>انصراف</button>
                <button type="button" className="fake-service-random-action" onClick={submitSampleForm} disabled={sendingRandom}>
                  {sendingRandom ? 'در حال ارسال...' : 'تایید و ارسال درخواست'}
                </button>
              </div>
            </section>
          </div>
        )}
      </main>
    </AppShell>
  )
}
