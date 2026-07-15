'use client'

import { useEffect, useState } from 'react'
import AppShell from '../../components/app-shell'
import { apiJson } from '../../lib/api'
import { formatJalaliDateTime } from '../../lib/display'

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
      active="fake-electronic-notification"
      title="سامانه ابلاغ الکترونیک آزمایشی"
      eyebrow="Fake External System"
      actions={<button className="ghost-action" onClick={load} disabled={loading}>بروزرسانی</button>}
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
          <div className="fake-service-grid">
            {items.map(item => (
              <article key={item.id} className="fake-service-card">
                <div className="fake-service-card__header">
                  <div>
                    <span className="fake-service-card__eyebrow">برگ اجرا</span>
                    <strong>{item.executionPaperCode || `#${item.executionPaperId}`}</strong>
                  </div>
                  <span className={`fake-service-badge fake-service-badge--${item.status === 'CONFIRMED' ? 'confirmed' : 'sent'}`}>
                    {statusLabel(item.status)}
                  </span>
                </div>

                <div className="fake-service-meta">
                  <Info label="کد رهگیری" value={item.externalTrackingCode || '-'} ltr />
                  <Info label="زمان ارسال" value={formatJalaliDateTime(item.sentAt)} />
                  <Info label="تایید خودکار" value={formatJalaliDateTime(item.autoConfirmAt)} />
                  <Info label="تاریخ ابلاغ" value={formatJalaliDateTime(item.informDate)} />
                  <Info label="زمان تایید" value={formatJalaliDateTime(item.confirmedAt)} />
                  <Info label="ارسال پیام به Camunda" value={formatJalaliDateTime(item.callbackDeliveredAt)} />
                </div>

                {item.status !== 'CONFIRMED' && (
                  <div className="fake-service-action-bar">
                    <label>
                      <span>تاریخ و زمان ابلاغ</span>
                      <input
                        type="datetime-local"
                        value={informDates[item.id] || currentDateTimeLocal()}
                        onChange={event => setInformDates(current => ({ ...current, [item.id]: event.target.value }))}
                      />
                    </label>
                    <button className="fake-service-primary" onClick={() => confirm(item)}>تایید دریافت ابلاغ</button>
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </main>
    </AppShell>
  )
}

function Info({ label, value, ltr = false }) {
  return (
    <div className="fake-service-info">
      <span>{label}</span>
      <strong className={ltr ? 'fake-service-ltr' : ''}>{value}</strong>
    </div>
  )
}
