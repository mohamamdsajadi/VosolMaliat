'use client'

import { useEffect, useState } from 'react'
import AppShell from '../../components/app-shell'
import { apiJson } from '../../lib/api'

function statusLabel(status) {
  return {
    SENT: 'ارسال شده به سنیم',
    CONFIRMED: 'دریافت توسط سنیم تایید شد'
  }[status] || status || 'نامشخص'
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
      <main style={{ maxWidth: 1280, margin: '0 auto' }}>
        {message && <div className="notice">{message}</div>}
        {loading ? <p>در حال دریافت درخواست‌ها...</p> : items.length === 0 ? (
          <section className="empty-state">هنوز تاریخ ابلاغی به سنیم آزمایشی ارسال نشده است.</section>
        ) : (
          <div style={gridStyle}>
            {items.map(item => (
              <article key={item.id} style={cardStyle}>
                <div style={cardHeaderStyle}>
                  <div>
                    <strong style={codeStyle}>{item.executionPaperCode || `#${item.executionPaperId}`}</strong>
                    <p style={mutedStyle}>تاریخ ابلاغ ارسال‌شده: {item.informDate || '-'}</p>
                  </div>
                  <span style={badgeStyle(item.status)}>{statusLabel(item.status)}</span>
                </div>
                <div style={metaGridStyle}>
                  <span>زمان ارسال: {item.sentAt || '-'}</span>
                  <span>زمان تایید: {item.confirmedAt || '-'}</span>
                </div>
                {item.status !== 'CONFIRMED' && (
                  <button style={primaryButtonStyle} onClick={() => confirm(item.id)}>تایید دریافت در سنیم</button>
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
const primaryButtonStyle = { border: 0, borderRadius: 10, background: '#0f4c81', color: '#fff', padding: '11px 16px', fontWeight: 800, cursor: 'pointer' }

function badgeStyle(status) {
  return {
    borderRadius: 999,
    padding: '6px 10px',
    fontSize: 12,
    fontWeight: 800,
    color: status === 'CONFIRMED' ? '#047857' : '#1d4ed8',
    background: status === 'CONFIRMED' ? '#ecfdf5' : '#eff6ff',
    border: `1px solid ${status === 'CONFIRMED' ? '#a7f3d0' : '#bfdbfe'}`
  }
}
