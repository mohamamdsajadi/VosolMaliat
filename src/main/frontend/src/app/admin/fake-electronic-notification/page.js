'use client'

import { useEffect, useState } from 'react'
import AppShell from '../../components/app-shell'
import { apiJson } from '../../lib/api'

function statusLabel(status) {
  return {
    SENT: 'ارسال شده و در انتظار تایید',
    CONFIRMED: 'تایید شده'
  }[status] || status || 'نامشخص'
}

function today() {
  return new Date().toISOString().slice(0, 10)
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
    setInformDates(Object.fromEntries(data.map(item => [item.id, item.informDate || today()])))
    setLoading(false)
  }

  useEffect(() => {
    load().catch(error => { setMessage(error.message); setLoading(false) })
  }, [])

  const confirm = async item => {
    setMessage('')
    await apiJson(`/api/v1/fake-electronic-notification/requests/${item.id}/confirm`, {
      method: 'POST',
      body: JSON.stringify({ InformDate: informDates[item.id] || today() })
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
      <main style={{ maxWidth: 1280, margin: '0 auto' }}>
        {message && <div className="notice">{message}</div>}
        {loading ? <p>در حال دریافت درخواست‌ها...</p> : items.length === 0 ? (
          <section className="empty-state">درخواستی برای ابلاغ الکترونیک ثبت نشده است.</section>
        ) : (
          <div style={gridStyle}>
            {items.map(item => (
              <article key={item.id} style={cardStyle}>
                <div style={cardHeaderStyle}>
                  <div>
                    <strong style={codeStyle}>{item.executionPaperCode || `#${item.executionPaperId}`}</strong>
                    <p style={mutedStyle}>کد رهگیری: {item.externalTrackingCode || '-'}</p>
                  </div>
                  <span style={badgeStyle(item.status)}>{statusLabel(item.status)}</span>
                </div>
                <div style={metaGridStyle}>
                  <span>زمان ارسال: {item.sentAt || '-'}</span>
                  <span>تایید خودکار: {item.autoConfirmAt || '-'}</span>
                  <span>تاریخ ابلاغ: {item.informDate || '-'}</span>
                  <span>زمان تایید: {item.confirmedAt || '-'}</span>
                </div>
                {item.status !== 'CONFIRMED' && (
                  <div style={actionRowStyle}>
                    <input
                      type="date"
                      value={informDates[item.id] || today()}
                      onChange={event => setInformDates(current => ({ ...current, [item.id]: event.target.value }))}
                      style={inputStyle}
                    />
                    <button style={primaryButtonStyle} onClick={() => confirm(item)}>تایید دریافت ابلاغ</button>
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

const gridStyle = { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 16 }
const cardStyle = { background: '#fff', border: '1px solid #dbe4ee', borderRadius: 14, padding: 18, boxShadow: '0 14px 34px rgba(15, 35, 52, .08)' }
const cardHeaderStyle = { display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'flex-start', marginBottom: 14 }
const codeStyle = { color: '#123b63', fontSize: 18 }
const mutedStyle = { margin: '6px 0 0', color: '#64748b' }
const metaGridStyle = { display: 'grid', gap: 8, color: '#334155', lineHeight: 1.8, marginBottom: 14 }
const actionRowStyle = { display: 'flex', gap: 10, alignItems: 'center', borderTop: '1px solid #e2e8f0', paddingTop: 14 }
const inputStyle = { border: '1px solid #cbd5e1', borderRadius: 10, padding: '10px 12px', font: 'inherit' }
const primaryButtonStyle = { border: 0, borderRadius: 10, background: '#0f4c81', color: '#fff', padding: '11px 16px', fontWeight: 800, cursor: 'pointer' }

function badgeStyle(status) {
  return {
    borderRadius: 999,
    padding: '6px 10px',
    fontSize: 12,
    fontWeight: 800,
    color: status === 'CONFIRMED' ? '#047857' : '#92400e',
    background: status === 'CONFIRMED' ? '#ecfdf5' : '#fffbeb',
    border: `1px solid ${status === 'CONFIRMED' ? '#a7f3d0' : '#fde68a'}`
  }
}
