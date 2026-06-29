'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import AppHeader from '../components/AppHeader'

// ─── Helpers ──────────────────────────────────────────────────────────────────

function toPersianDigits(str) {
  const persianDigits = '۰۱۲۳۴۵۶۷۸۹'
  return String(str).replace(/\d/g, d => persianDigits[d])
}

function formatDebt(amount) {
  if (amount == null) return '—'
  return toPersianDigits(Number(amount).toLocaleString('en-US'))
}

function taxpayerTypeLabel(type) {
  if (!type) return '—'
  const map = {
    NATURAL: 'حقیقی',
    LEGAL: 'حقوقی',
    REAL: 'حقیقی',
    LEGAL_PERSON: 'حقوقی',
  }
  return map[type] ?? type
}

// ─── Constants ────────────────────────────────────────────────────────────────

const TABS = [
  { label: 'لیست همه برگ‌های اجرا', endpoint: '/api/v1/execution-papers' },
  { label: 'برگ‌های اجرای تخصیص یافته', endpoint: '/api/v1/execution-papers/assigned' },
  { label: 'برگ‌های اجرای تخصیص نیافته', endpoint: '/api/v1/execution-papers/unassigned' },
]

const PAYMENT_STATUS_MAP = {
  AWAITING_PAYMENT: { label: 'در انتظار پرداخت', bg: '#ffedd5', color: '#c2410c' },
  DEADLINE_EXPIRED: { label: 'اتمام مهلت', bg: '#fee2e2', color: '#dc2626' },
}

const NOTIFICATION_STATUS_MAP = {
  NOTIFIED: { label: 'ابلاغ شده', bg: '#dcfce7', color: '#15803d' },
  NOT_NOTIFIED: { label: 'عدم ابلاغ', bg: '#fee2e2', color: '#dc2626' },
}

const TABLE_HEADERS = [
  '#',
  'شناسه یکتا 🔍',
  'نام مودی 🔍',
  'کد / شناسه ملی 🔍',
  'تاریخ ابلاغ',
  'وضعیت مهلت پرداخت',
  'مبلغ بدهی',
  'نوع مودی',
  'وضعیت ابلاغ',
  'جزئیات',
]

// ─── Sub-components ───────────────────────────────────────────────────────────

function StatusPill({ status, map }) {
  if (!status) return <span style={{ color: '#9ca3af' }}>—</span>
  const config = map[status]
  if (!config) return <span style={{ color: '#9ca3af', fontSize: 12 }}>{status}</span>
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: '3px 10px',
        borderRadius: 9999,
        background: config.bg,
        color: config.color,
        fontSize: 12,
        fontWeight: 600,
        whiteSpace: 'nowrap',
      }}
    >
      {config.label}
    </span>
  )
}

function Pagination({ page, totalPages, onPageChange }) {
  if (!totalPages || totalPages <= 1) return null

  const pages = buildPageList(page, totalPages)

  const btnBase = {
    padding: '6px 11px',
    borderRadius: 6,
    border: '1px solid #e5e7eb',
    cursor: 'pointer',
    fontSize: 13,
    fontWeight: 400,
    lineHeight: 1.4,
  }
  const btnActive = { ...btnBase, background: '#2563eb', color: 'white', fontWeight: 700, border: '1px solid #2563eb' }
  const btnNormal = { ...btnBase, background: 'white', color: '#374151' }
  const btnDisabled = { ...btnBase, background: '#f9fafb', color: '#d1d5db', cursor: 'not-allowed' }

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        gap: 4,
        padding: '16px 0',
        direction: 'rtl',
      }}
    >
      <button
        style={page === 0 ? btnDisabled : btnNormal}
        onClick={() => onPageChange(0)}
        disabled={page === 0}
        aria-label="اولین صفحه"
      >
        «
      </button>
      <button
        style={page === 0 ? btnDisabled : btnNormal}
        onClick={() => onPageChange(page - 1)}
        disabled={page === 0}
        aria-label="صفحه قبل"
      >
        ‹
      </button>

      {pages.map((p, i) =>
        p === '...' ? (
          <span key={`dot-${i}`} style={{ padding: '6px 4px', color: '#9ca3af', fontSize: 14 }}>
            …
          </span>
        ) : (
          <button
            key={p}
            style={page === p ? btnActive : btnNormal}
            onClick={() => onPageChange(p)}
          >
            {toPersianDigits(p + 1)}
          </button>
        )
      )}

      <button
        style={page >= totalPages - 1 ? btnDisabled : btnNormal}
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
        aria-label="صفحه بعد"
      >
        ›
      </button>
      <button
        style={page >= totalPages - 1 ? btnDisabled : btnNormal}
        onClick={() => onPageChange(totalPages - 1)}
        disabled={page >= totalPages - 1}
        aria-label="آخرین صفحه"
      >
        »
      </button>
    </div>
  )
}

/** Returns a list of page indices with '...' ellipsis placeholders. */
function buildPageList(current, total) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i)

  const pages = []
  const left = Math.max(0, current - 2)
  const right = Math.min(total - 1, current + 2)

  if (left > 0) {
    pages.push(0)
    if (left > 1) pages.push('...')
  }

  for (let i = left; i <= right; i++) pages.push(i)

  if (right < total - 1) {
    if (right < total - 2) pages.push('...')
    pages.push(total - 1)
  }

  return pages
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function ExecutionPapersPage() {
  const router = useRouter()
  const [activeTab, setActiveTab] = useState(0)
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    const endpoints = [
      '/api/v1/execution-papers',
      '/api/v1/execution-papers/assigned',
      '/api/v1/execution-papers/unassigned',
    ]

    setLoading(true)
    setError(null)

    fetch(`${endpoints[activeTab]}?page=${page}&size=10`)
      .then(r => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`)
        return r.json()
      })
      .then(json => {
        setData(json)
        setLoading(false)
      })
      .catch(err => {
        setError(err.message)
        setLoading(false)
      })
  }, [activeTab, page])

  const papers = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  const handleTabChange = idx => {
    setActiveTab(idx)
    setPage(0)
    setData(null)
  }

  return (
    <div style={{ minHeight: '100vh', background: '#f9fafb' }}>
      <AppHeader title="دستیار هوشمند حسابرسی" subtitle="میزکار حسابرس" />

      <div style={{ padding: '28px 24px', maxWidth: 1600, margin: '0 auto' }}>
        {/* Page heading */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            marginBottom: 24,
            direction: 'rtl',
          }}
        >
          <button
            onClick={() => router.push('/')}
            style={{
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              color: '#2563eb',
              fontSize: 20,
              lineHeight: 1,
              padding: '4px',
              transform: 'rotate(180deg)',
            }}
            aria-label="بازگشت"
          >
            →
          </button>
          <h2
            style={{
              fontSize: 22,
              fontWeight: 800,
              color: '#1e3a5f',
              margin: 0,
            }}
          >
            لیست برگ‌های اجرا
          </h2>
          {totalElements > 0 && (
            <span
              style={{
                fontSize: 13,
                color: '#6b7280',
                background: '#f3f4f6',
                padding: '2px 10px',
                borderRadius: 9999,
              }}
            >
              {toPersianDigits(totalElements.toLocaleString('en-US'))} مورد
            </span>
          )}
        </div>

        {/* Card */}
        <div
          style={{
            background: 'white',
            borderRadius: 12,
            boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
            border: '1px solid #e5e7eb',
            overflow: 'hidden',
          }}
        >
          {/* Tabs */}
          <div
            style={{
              display: 'flex',
              borderBottom: '1px solid #e5e7eb',
              direction: 'rtl',
              background: '#fafafa',
            }}
          >
            {TABS.map((tab, idx) => (
              <button
                key={idx}
                onClick={() => handleTabChange(idx)}
                style={{
                  padding: '14px 20px',
                  background: 'none',
                  border: 'none',
                  borderBottom: activeTab === idx ? '2px solid #2563eb' : '2px solid transparent',
                  color: activeTab === idx ? '#2563eb' : '#6b7280',
                  fontWeight: activeTab === idx ? 700 : 400,
                  cursor: 'pointer',
                  fontSize: 14,
                  marginBottom: '-1px',
                  whiteSpace: 'nowrap',
                  transition: 'color 0.15s',
                }}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Loading */}
          {loading && (
            <div
              style={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                height: 240,
              }}
            >
              <div className="spinner" />
            </div>
          )}

          {/* Error */}
          {!loading && error && (
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 16,
                padding: '60px 40px',
              }}
            >
              <div style={{ fontSize: 36 }}>⚠️</div>
              <p style={{ color: '#dc2626', fontSize: 15, textAlign: 'center', direction: 'rtl' }}>
                خطا در دریافت اطلاعات: {error}
              </p>
              <button
                onClick={() => { setData(null); setLoading(true); setError(null) }}
                style={{
                  padding: '10px 28px',
                  background: '#2563eb',
                  color: 'white',
                  borderRadius: 8,
                  border: 'none',
                  cursor: 'pointer',
                  fontSize: 14,
                  fontWeight: 600,
                }}
              >
                تلاش مجدد
              </button>
            </div>
          )}

          {/* Table */}
          {!loading && !error && (
            <>
              <div style={{ overflowX: 'auto' }}>
                <table
                  style={{
                    width: '100%',
                    borderCollapse: 'collapse',
                    direction: 'rtl',
                    minWidth: 1000,
                  }}
                >
                  <thead>
                    <tr style={{ background: '#eff6ff' }}>
                      {TABLE_HEADERS.map((h, i) => (
                        <th
                          key={i}
                          style={{
                            padding: '13px 10px',
                            fontSize: 12,
                            fontWeight: 700,
                            color: '#4b5563',
                            textAlign: 'center',
                            borderBottom: '2px solid #dbeafe',
                            whiteSpace: 'nowrap',
                          }}
                        >
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {papers.length === 0 ? (
                      <tr>
                        <td
                          colSpan={TABLE_HEADERS.length}
                          style={{
                            textAlign: 'center',
                            padding: '60px 40px',
                            color: '#9ca3af',
                            fontSize: 15,
                          }}
                        >
                          داده‌ای برای نمایش وجود ندارد
                        </td>
                      </tr>
                    ) : (
                      papers.map((paper, idx) => (
                        <tr
                          key={paper.id ?? idx}
                          style={{
                            background: idx % 2 === 0 ? 'white' : '#fafafa',
                            borderBottom: '1px solid #f3f4f6',
                            transition: 'background 0.1s',
                          }}
                          onMouseEnter={e =>
                            (e.currentTarget.style.background = '#eff6ff')
                          }
                          onMouseLeave={e =>
                            (e.currentTarget.style.background =
                              idx % 2 === 0 ? 'white' : '#fafafa')
                          }
                        >
                          {/* # */}
                          <td style={tdStyle}>
                            <span style={{ color: '#9ca3af', fontSize: 13 }}>
                              {toPersianDigits(page * 10 + idx + 1)}
                            </span>
                          </td>

                          {/* شناسه یکتا */}
                          <td style={{ ...tdStyle, fontFamily: 'monospace, monospace', fontSize: 12 }}>
                            {paper.uniqueCode
                              ? `…${String(paper.uniqueCode).slice(-12)}`
                              : '—'}
                          </td>

                          {/* نام مودی */}
                          <td style={{ ...tdStyle, textAlign: 'right', fontWeight: 500, color: '#1f2937' }}>
                            {paper.taxpayerName || '—'}
                          </td>

                          {/* کد/شناسه ملی */}
                          <td style={{ ...tdStyle, fontFamily: 'monospace, monospace' }}>
                            {paper.taxpayerNationalId || '—'}
                          </td>

                          {/* تاریخ ابلاغ */}
                          <td style={tdStyle}>
                            {paper.notificationDate || '—'}
                          </td>

                          {/* وضعیت مهلت پرداخت */}
                          <td style={tdStyle}>
                            <StatusPill
                              status={paper.paymentDeadlineStatus}
                              map={PAYMENT_STATUS_MAP}
                            />
                          </td>

                          {/* مبلغ بدهی */}
                          <td style={{ ...tdStyle, fontWeight: 600, color: '#1f2937' }}>
                            {formatDebt(paper.totalDebt)}
                          </td>

                          {/* نوع مودی */}
                          <td style={tdStyle}>
                            {taxpayerTypeLabel(paper.taxpayerType)}
                          </td>

                          {/* وضعیت ابلاغ */}
                          <td style={tdStyle}>
                            <StatusPill
                              status={paper.notificationStatus}
                              map={NOTIFICATION_STATUS_MAP}
                            />
                          </td>

                          {/* جزئیات */}
                          <td style={tdStyle}>
                            <button
                              onClick={() =>
                                router.push(`/execution-papers/${paper.id ?? paper.uniqueCode}`)
                              }
                              style={{
                                color: '#2563eb',
                                background: 'none',
                                border: '1px solid #bfdbfe',
                                borderRadius: 6,
                                cursor: 'pointer',
                                fontSize: 12,
                                fontWeight: 600,
                                padding: '4px 10px',
                                whiteSpace: 'nowrap',
                              }}
                              onMouseEnter={e =>
                                (e.currentTarget.style.background = '#eff6ff')
                              }
                              onMouseLeave={e =>
                                (e.currentTarget.style.background = 'none')
                              }
                            >
                              مشاهده جزئیات
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>

              <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
            </>
          )}
        </div>
      </div>
    </div>
  )
}

// Shared table data cell style
const tdStyle = {
  padding: '12px 10px',
  fontSize: 13,
  color: '#374151',
  textAlign: 'center',
  verticalAlign: 'middle',
}
