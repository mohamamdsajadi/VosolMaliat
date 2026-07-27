'use client'

import { useEffect, useState } from 'react'
import AppShell from '../../components/app-shell'
import JalaliInput from '../../components/jalali-input'
import RefreshButton from '../../components/refresh-button'
import { apiJson } from '../../lib/api'
import { formatJalaliDateTime } from '../../lib/display'

const TABLE_HEADERS = [
  '#',
  'برگ اجرا',
  'کد رهگیری',
  'وضعیت',
  'زمان ارسال',
  'تاریخ ابلاغ',
  'زمان تایید',
  'ارسال پیام',
  'عملیات'
]

function statusLabel(status) {
  return {
    SENT: 'ارسال شده و در انتظار تایید',
    CONFIRMED: 'تایید شده'
  }[status] || status || 'نامشخص'
}

function currentDateTimeLocal() {
  const date = new Date()
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset())
  return date.toISOString().slice(0, 16)
}

function toDateTimeInputValue(value) {
  if (!value) return currentDateTimeLocal()
  const normalized = String(value).trim().replace(' ', 'T')
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(normalized)) {
    return normalized.slice(0, 16)
  }
  if (/^\d{4}-\d{2}-\d{2}$/.test(normalized)) {
    return `${normalized}T${currentDateTimeLocal().slice(11, 16)}`
  }
  return currentDateTimeLocal()
}

function countByStatus(items, status) {
  return items.filter(item => item.status === status).length
}

export default function FakeElectronicNotificationPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')
  const [informDates, setInformDates] = useState({})

  const load = async () => {
    setLoading(true)
    const data = await apiJson('/api/v1/fake-electronic-notification/requests')
    setItems(data)
    setInformDates(Object.fromEntries(data.map(item => [item.id, toDateTimeInputValue(item.informDate)])))
    setLoading(false)
  }

  useEffect(() => {
    load().catch(error => { setMessage(error.message); setLoading(false) })
  }, [])

  const confirm = async item => {
    setMessage('')
    await apiJson(`/api/v1/fake-electronic-notification/requests/${item.id}/confirm`, {
      method: 'POST',
      body: JSON.stringify({ InformDate: informDates[item.id] || currentDateTimeLocal() })
    })
    setMessage('تایید دریافت ابلاغ ثبت و پیام Camunda ارسال شد.')
    await load()
  }

  return (
    <AppShell
      active="test-front"
      title="سامانه ابلاغ الکترونیک آزمایشی"
      eyebrow="Fake External System"
      actions={<RefreshButton onClick={load} loading={loading} />}
    >
      <main className="fake-service-page">
        <section className="fake-service-hero">
          <div>
            <p className="eyebrow">سامانه بیرونی آزمایشی</p>
            <h2>درخواست‌های ابلاغ الکترونیک</h2>
          </div>
          <div className="fake-service-stats">
            <span>کل: {items.length.toLocaleString('fa-IR')}</span>
            <span>در انتظار: {countByStatus(items, 'SENT').toLocaleString('fa-IR')}</span>
            <span>تایید شده: {countByStatus(items, 'CONFIRMED').toLocaleString('fa-IR')}</span>
          </div>
        </section>

        {message && <div className="notice">{message}</div>}
        {loading ? <div className="empty-state compact">در حال دریافت درخواست‌ها...</div> : items.length === 0 ? (
          <section className="empty-state">درخواستی برای ابلاغ الکترونیک ثبت نشده است.</section>
        ) : (
          <div className="task-table-card fake-service-table-card">
            <div className="task-table-header">
              <div>
                <p className="eyebrow">درخواست‌ها</p>
                <h2>فهرست ابلاغ الکترونیک</h2>
              </div>
              <span>{items.length.toLocaleString('fa-IR')} مورد</span>
            </div>
            <div className="task-table-scroll">
              <table className="task-grid-table fake-service-table">
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
                      <td className="task-grid-cell fake-service-ltr">{item.externalTrackingCode || '-'}</td>
                      <td className="task-grid-cell">
                        <span className={`fake-service-badge fake-service-badge--${item.status === 'CONFIRMED' ? 'confirmed' : 'sent'}`}>
                          {statusLabel(item.status)}
                        </span>
                      </td>
                      <td className="task-grid-cell">{formatJalaliDateTime(item.sentAt)}</td>
                      <td className="task-grid-cell">{formatJalaliDateTime(item.informDate)}</td>
                      <td className="task-grid-cell">{formatJalaliDateTime(item.confirmedAt)}</td>
                      <td className="task-grid-cell">{formatJalaliDateTime(item.callbackDeliveredAt)}</td>
                      <td className="task-grid-cell task-grid-cell--actions">
                        {item.status !== 'CONFIRMED' ? (
                          <div className="fake-service-row-actions">
                            <JalaliInput
                              mode="datetime"
                              value={informDates[item.id] || currentDateTimeLocal()}
                              onChangeValue={value => setInformDates(current => ({ ...current, [item.id]: value }))}
                            />
                            <button className="task-table-primary-action" onClick={() => confirm(item)}>تایید دریافت ابلاغ</button>
                          </div>
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
