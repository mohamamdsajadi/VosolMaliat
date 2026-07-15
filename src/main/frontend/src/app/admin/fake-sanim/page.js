'use client'

import { useEffect, useState } from 'react'
import AppShell from '../../components/app-shell'
import RefreshButton from '../../components/refresh-button'
import { apiJson } from '../../lib/api'
import { formatJalaliDateTime } from '../../lib/display'

const TABLE_HEADERS = [
  '#',
  'برگ اجرا',
  'وضعیت',
  'تاریخ ابلاغ ارسال‌شده',
  'زمان ارسال',
  'زمان تایید',
  'عملیات'
]

function statusLabel(status) {
  return {
    SENT: 'ارسال شده به سنیم',
    CONFIRMED: 'دریافت توسط سنیم تایید شد'
  }[status] || status || 'نامشخص'
}

function countByStatus(items, status) {
  return items.filter(item => item.status === status).length
}

export default function FakeSanimPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')

  const load = async () => {
    setLoading(true)
    setItems(await apiJson('/api/v1/fake-sanim/inform-requests'))
    setLoading(false)
  }

  useEffect(() => {
    load().catch(error => { setMessage(error.message); setLoading(false) })
  }, [])

  const confirm = async id => {
    setMessage('')
    await apiJson(`/api/v1/fake-sanim/inform-requests/${id}/confirm`, { method: 'POST' })
    setMessage('دریافت تاریخ ابلاغ در سنیم آزمایشی تایید شد.')
    await load()
  }

  return (
    <AppShell
      active="fake-sanim"
      title="سامانه عملیاتی سنیم آزمایشی"
      eyebrow="Fake External System"
      actions={<RefreshButton onClick={load} loading={loading} />}
    >
      <main className="fake-service-page">
        <section className="fake-service-hero">
          <div>
            <p className="eyebrow">سامانه بیرونی آزمایشی</p>
            <h2>درخواست‌های ارسال تاریخ ابلاغ به سنیم</h2>
          </div>
          <div className="fake-service-stats">
            <span>کل: {items.length.toLocaleString('fa-IR')}</span>
            <span>ارسال شده: {countByStatus(items, 'SENT').toLocaleString('fa-IR')}</span>
            <span>تایید شده: {countByStatus(items, 'CONFIRMED').toLocaleString('fa-IR')}</span>
          </div>
        </section>

        {message && <div className="notice">{message}</div>}
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
                    <tr key={item.id} className={`task-grid-row ${index % 2 ? 'task-grid-row--zebra' : ''}`}>
                      <td className="task-grid-cell task-grid-cell--index">{(index + 1).toLocaleString('fa-IR')}</td>
                      <td className="task-grid-cell task-grid-cell--paper">
                        <span>شناسه برگ اجرا</span>
                        <strong>{item.executionPaperCode || `#${item.executionPaperId}`}</strong>
                      </td>
                      <td className="task-grid-cell">
                        <span className={`fake-service-badge fake-service-badge--${item.status === 'CONFIRMED' ? 'confirmed' : 'sent'}`}>
                          {statusLabel(item.status)}
                        </span>
                      </td>
                      <td className="task-grid-cell">{formatJalaliDateTime(item.informDate)}</td>
                      <td className="task-grid-cell">{formatJalaliDateTime(item.sentAt)}</td>
                      <td className="task-grid-cell">{formatJalaliDateTime(item.confirmedAt)}</td>
                      <td className="task-grid-cell task-grid-cell--actions">
                        {item.status !== 'CONFIRMED' ? (
                          <button className="task-table-primary-action" onClick={() => confirm(item.id)}>تایید دریافت در سنیم</button>
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
      </main>
    </AppShell>
  )
}
