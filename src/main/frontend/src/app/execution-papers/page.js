'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import AppShell from '../components/app-shell'
import { apiFetch } from '../lib/api'
import { formatJalaliDateTime } from '../lib/display'

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

const EMPTY_FILTERS = {
  uniqueCode: '',
  taxpayerName: '',
  taxpayerNationalId: '',
  currentState: '',
  notificationStatus: '',
  taxUnitId: '',
  minDebt: '',
  maxDebt: '',
}

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

function buildQuery(page, filters) {
  const params = new URLSearchParams({ page: String(page), size: '10' })
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== '' && value != null) {
      params.set(key, value)
    }
  })
  return params.toString()
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function ExecutionPapersPage() {
  const router = useRouter()
  const [activeTab, setActiveTab] = useState(0)
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [filterForm, setFilterForm] = useState(EMPTY_FILTERS)
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const endpoints = [
      '/api/v1/execution-papers',
      '/api/v1/execution-papers/assigned',
      '/api/v1/execution-papers/unassigned',
    ]

    setLoading(true)
    setError(null)

    apiFetch(`${endpoints[activeTab]}?${buildQuery(page, filters)}`)
      .then(r => {
        if (!r.ok) {
          if (r.status === 403) throw new Error('شما مجوز مشاهده فهرست برگ‌های اجرا را ندارید.')
          throw new Error(`HTTP ${r.status}`)
        }
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
  }, [activeTab, page, filters, reloadKey])

  const papers = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  const handleTabChange = idx => {
    setActiveTab(idx)
    setPage(0)
    setData(null)
  }

  const updateFilter = event => {
    const { name, value } = event.target
    setFilterForm(current => ({ ...current, [name]: value }))
  }

  const applyFilters = event => {
    event.preventDefault()
    setFilters(filterForm)
    setPage(0)
    setData(null)
  }

  const clearFilters = () => {
    setFilterForm(EMPTY_FILTERS)
    setFilters(EMPTY_FILTERS)
    setPage(0)
    setData(null)
  }

  return (
    <AppShell
      active="execution-papers"
      title="دستیار هوشمند حسابرسی"
      eyebrow="برگ‌های اجرا"
    >
      <div style={{ maxWidth: 1600, margin: '0 auto' }}>
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
        <form onSubmit={applyFilters} style={filterPanelStyle}>
          <div style={filterHeaderStyle}>
            <div>
              <p style={filterEyebrowStyle}>جستجو</p>
              <h3 style={filterTitleStyle}>فیلتر برگ‌های اجرا</h3>
            </div>
            <div style={filterActionStyle}>
              <button type="button" onClick={clearFilters} style={secondaryButtonStyle}>پاک کردن</button>
              <button type="submit" style={primaryButtonStyle}>اعمال فیلتر</button>
            </div>
          </div>
          <div style={filterGridStyle}>
            <label style={filterFieldStyle}>
              <span>شناسه یکتای برگ اجرا</span>
              <input name="uniqueCode" value={filterForm.uniqueCode} onChange={updateFilter} placeholder="مثلاً EXEC-..." style={filterInputStyle} />
            </label>
            <label style={filterFieldStyle}>
              <span>نام مودی</span>
              <input name="taxpayerName" value={filterForm.taxpayerName} onChange={updateFilter} placeholder="نام، نام خانوادگی یا شرکت" style={filterInputStyle} />
            </label>
            <label style={filterFieldStyle}>
              <span>کد / شناسه ملی</span>
              <input name="taxpayerNationalId" value={filterForm.taxpayerNationalId} onChange={updateFilter} placeholder="کد ملی یا شناسه ملی" style={filterInputStyle} />
            </label>
            <label style={filterFieldStyle}>
              <span>وضعیت برگ اجرا</span>
              <select name="currentState" value={filterForm.currentState} onChange={updateFilter} style={filterInputStyle}>
                <option value="">همه وضعیت‌ها</option>
                <option value="UNASSIGNED">تخصیص نیافته</option>
                <option value="ASSIGNED">تخصیص یافته</option>
                <option value="NOTIFIED">ابلاغ شده</option>
                <option value="NOTIFICATION_FAILED">عدم ابلاغ</option>
                <option value="AWAITING_ARREST_ORDER">در انتظار صدور دستور بازداشت</option>
                <option value="ARREST_ORDER_ISSUED">دستور بازداشت صادر شده</option>
                <option value="ARREST_ORDER_NOT_ISSUED">دستور بازداشت صادر نشده</option>
                <option value="COMPLETED">تکمیل شده</option>
                <option value="CANCELLED">لغو شده</option>
              </select>
            </label>
            <label style={filterFieldStyle}>
              <span>وضعیت ابلاغ</span>
              <select name="notificationStatus" value={filterForm.notificationStatus} onChange={updateFilter} style={filterInputStyle}>
                <option value="">همه</option>
                <option value="NOTIFIED">ابلاغ شده</option>
                <option value="NOT_NOTIFIED">عدم ابلاغ</option>
              </select>
            </label>
            <label style={filterFieldStyle}>
              <span>کد واحد مالیاتی</span>
              <input name="taxUnitId" type="number" value={filterForm.taxUnitId} onChange={updateFilter} placeholder="مثلاً 101" style={filterInputStyle} />
            </label>
            <label style={filterFieldStyle}>
              <span>حداقل مبلغ بدهی</span>
              <input name="minDebt" type="number" value={filterForm.minDebt} onChange={updateFilter} placeholder="ریال" style={filterInputStyle} />
            </label>
            <label style={filterFieldStyle}>
              <span>حداکثر مبلغ بدهی</span>
              <input name="maxDebt" type="number" value={filterForm.maxDebt} onChange={updateFilter} placeholder="ریال" style={filterInputStyle} />
            </label>
          </div>
        </form>

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
                onClick={() => { setData(null); setLoading(true); setError(null); setReloadKey(current => current + 1) }}
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
                            {formatJalaliDateTime(paper.notificationDate)}
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
    </AppShell>
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

const filterPanelStyle = {
  background: 'white',
  borderRadius: 12,
  boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
  border: '1px solid #e5e7eb',
  padding: 18,
  marginBottom: 18,
  direction: 'rtl',
}

const filterHeaderStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: 16,
  marginBottom: 14,
}

const filterEyebrowStyle = {
  margin: 0,
  color: '#2563eb',
  fontSize: 12,
  fontWeight: 800,
}

const filterTitleStyle = {
  margin: '4px 0 0',
  color: '#1e3a5f',
  fontSize: 18,
}

const filterActionStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: 8,
  flexWrap: 'wrap',
}

const filterGridStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))',
  gap: 12,
}

const filterFieldStyle = {
  display: 'grid',
  gap: 6,
  color: '#374151',
  fontSize: 13,
  fontWeight: 700,
}

const filterInputStyle = {
  width: '100%',
  boxSizing: 'border-box',
  border: '1px solid #d1d5db',
  borderRadius: 8,
  padding: '10px 12px',
  color: '#111827',
  background: 'white',
  fontSize: 14,
}

const primaryButtonStyle = {
  background: '#2563eb',
  color: 'white',
  border: 'none',
  borderRadius: 8,
  padding: '10px 16px',
  cursor: 'pointer',
  fontSize: 13,
  fontWeight: 800,
}

const secondaryButtonStyle = {
  background: '#f8fafc',
  color: '#475569',
  border: '1px solid #cbd5e1',
  borderRadius: 8,
  padding: '9px 14px',
  cursor: 'pointer',
  fontSize: 13,
  fontWeight: 800,
}
