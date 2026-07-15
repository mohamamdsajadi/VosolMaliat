'use client'

import { useEffect, useState } from 'react'
import AppShell from '../../components/app-shell'
import { apiJson } from '../../lib/api'
import { formatJalaliDateTime } from '../../lib/display'

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
      actions={<button className="ghost-action" onClick={load} disabled={loading}>بروزرسانی</button>}
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
                  <Info label="تاریخ ابلاغ ارسال‌شده" value={formatJalaliDateTime(item.informDate)} />
                  <Info label="زمان ارسال" value={formatJalaliDateTime(item.sentAt)} />
                  <Info label="زمان تایید" value={formatJalaliDateTime(item.confirmedAt)} />
                </div>
                {item.status !== 'CONFIRMED' && (
                  <div className="fake-service-action-bar">
                    <button className="fake-service-primary" onClick={() => confirm(item.id)}>تایید دریافت در سنیم</button>
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

function Info({ label, value }) {
  return (
    <div className="fake-service-info">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}
