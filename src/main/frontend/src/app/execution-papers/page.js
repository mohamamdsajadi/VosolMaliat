'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import AppShell from '../components/app-shell'
import RefreshButton from '../components/refresh-button'
import { apiFetch } from '../lib/api'
import { formatJalaliDateTime } from '../lib/display'

function toPersianDigits(str) {
  const persianDigits = '۰۱۲۳۴۵۶۷۸۹'
  return String(str).replace(/\d/g, d => persianDigits[d])
}

function formatDebt(amount) {
  if (amount == null) return '—'
  return `${toPersianDigits(Number(amount).toLocaleString('en-US'))} ریال`
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

const TABS = [
  { label: 'همه برگ‌های اجرا', endpoint: '/api/v1/execution-papers' },
  { label: 'تخصیص یافته', endpoint: '/api/v1/execution-papers/assigned' },
  { label: 'تخصیص نیافته', endpoint: '/api/v1/execution-papers/unassigned' },
]

const PAYMENT_STATUS_MAP = {
  AWAITING_PAYMENT: { label: 'در انتظار پرداخت', tone: 'warning' },
  DEADLINE_EXPIRED: { label: 'اتمام مهلت', tone: 'danger' },
}

const NOTIFICATION_STATUS_MAP = {
  NOTIFIED: { label: 'ابلاغ شده', tone: 'success' },
  NOT_NOTIFIED: { label: 'عدم ابلاغ', tone: 'danger' },
}

const TABLE_HEADERS = [
  '#',
  'شناسه یکتا',
  'نام مودی',
  'کد / شناسه ملی',
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

function StatusPill({ status, map }) {
  if (!status) return <span className="status-pill status-pill--muted">—</span>
  const config = map[status]
  if (!config) return <span className="status-pill status-pill--muted">{status}</span>
  return <span className={`status-pill status-pill--${config.tone}`}>{config.label}</span>
}

function Pagination({ page, totalPages, onPageChange }) {
  if (!totalPages || totalPages <= 1) return null
  const pages = buildPageList(page, totalPages)

  return (
    <div className="table-pagination">
      <button onClick={() => onPageChange(0)} disabled={page === 0} aria-label="اولین صفحه">«</button>
      <button onClick={() => onPageChange(page - 1)} disabled={page === 0} aria-label="صفحه قبل">‹</button>
      {pages.map((p, i) =>
        p === '...' ? (
          <span key={`dot-${i}`}>…</span>
        ) : (
          <button
            className={page === p ? 'is-active' : ''}
            key={p}
            onClick={() => onPageChange(p)}
          >
            {toPersianDigits(p + 1)}
          </button>
        )
      )}
      <button onClick={() => onPageChange(page + 1)} disabled={page >= totalPages - 1} aria-label="صفحه بعد">›</button>
      <button onClick={() => onPageChange(totalPages - 1)} disabled={page >= totalPages - 1} aria-label="آخرین صفحه">»</button>
    </div>
  )
}

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
    setLoading(true)
    setError(null)
    apiFetch(`${TABS[activeTab].endpoint}?${buildQuery(page, filters)}`)
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

  const reload = () => {
    setData(null)
    setReloadKey(current => current + 1)
  }

  return (
    <AppShell
      active="execution-papers"
      title="فهرست برگ‌های اجرا"
      eyebrow="برگ‌های اجرا"
      contextItems={[`تعداد نتایج: ${toPersianDigits(totalElements.toLocaleString('en-US'))}`]}
      actions={<RefreshButton onClick={reload} loading={loading}>بروزرسانی فهرست</RefreshButton>}
    >
      <div className="execution-list-page">
        <form className="execution-filter-panel" onSubmit={applyFilters}>
          <div className="task-filter-header">
            <div className="task-filter-title">
              <span aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <circle cx="10.5" cy="10.5" r="5.5" />
                  <path d="M15 15l4 4" />
                </svg>
              </span>
              <div>
                <p className="eyebrow">جستجو</p>
                <h3>فیلتر برگ‌های اجرا</h3>
              </div>
            </div>
            <div className="task-filter-actions">
              <button type="button" className="task-filter-secondary" onClick={clearFilters}>پاک کردن</button>
              <button type="submit" className="task-filter-primary">اعمال فیلتر</button>
            </div>
          </div>

          <div className="execution-filter-grid">
            <label>
              <span>شناسه یکتای برگ اجرا</span>
              <input name="uniqueCode" value={filterForm.uniqueCode} onChange={updateFilter} placeholder="مثلاً EXEC-..." />
            </label>
            <label>
              <span>نام مودی</span>
              <input name="taxpayerName" value={filterForm.taxpayerName} onChange={updateFilter} placeholder="نام، نام خانوادگی یا شرکت" />
            </label>
            <label>
              <span>کد / شناسه ملی</span>
              <input name="taxpayerNationalId" value={filterForm.taxpayerNationalId} onChange={updateFilter} placeholder="کد ملی یا شناسه ملی" />
            </label>
            <label>
              <span>وضعیت برگ اجرا</span>
              <select name="currentState" value={filterForm.currentState} onChange={updateFilter}>
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
            <label>
              <span>وضعیت ابلاغ</span>
              <select name="notificationStatus" value={filterForm.notificationStatus} onChange={updateFilter}>
                <option value="">همه</option>
                <option value="NOTIFIED">ابلاغ شده</option>
                <option value="NOT_NOTIFIED">عدم ابلاغ</option>
              </select>
            </label>
            <label>
              <span>کد واحد مالیاتی</span>
              <input name="taxUnitId" type="number" value={filterForm.taxUnitId} onChange={updateFilter} placeholder="مثلاً ۱۰۱" />
            </label>
            <label>
              <span>حداقل مبلغ بدهی</span>
              <input name="minDebt" type="number" value={filterForm.minDebt} onChange={updateFilter} placeholder="ریال" />
            </label>
            <label>
              <span>حداکثر مبلغ بدهی</span>
              <input name="maxDebt" type="number" value={filterForm.maxDebt} onChange={updateFilter} placeholder="ریال" />
            </label>
          </div>
        </form>

        <div className="task-table-card execution-table-card">
          <div className="execution-tabs">
            {TABS.map((tab, idx) => (
              <button
                className={activeTab === idx ? 'is-active' : ''}
                key={tab.endpoint}
                onClick={() => handleTabChange(idx)}
                type="button"
              >
                {tab.label}
              </button>
            ))}
          </div>

          {loading && (
            <div className="table-loading">
              <div className="spinner" />
              <span>در حال دریافت اطلاعات...</span>
            </div>
          )}

          {!loading && error && (
            <div className="dashboard-error table-error">
              <strong>دریافت فهرست برگ‌های اجرا ناموفق بود.</strong>
              <span>{error}</span>
              <button className="primary-action" type="button" onClick={reload}>تلاش مجدد</button>
            </div>
          )}

          {!loading && !error && (
            <>
              <div className="task-table-scroll">
                <table className="task-grid-table execution-grid-table">
                  <thead>
                    <tr>
                      {TABLE_HEADERS.map((header) => (
                        <th key={header}>{header}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {papers.length === 0 ? (
                      <tr>
                        <td colSpan={TABLE_HEADERS.length}>
                          <div className="empty-state compact execution-empty-state">داده‌ای برای نمایش وجود ندارد.</div>
                        </td>
                      </tr>
                    ) : (
                      papers.map((paper, idx) => (
                        <tr className={`task-grid-row${idx % 2 === 1 ? ' task-grid-row--zebra' : ''}`} key={paper.id ?? idx}>
                          <td className="task-grid-cell task-grid-cell--index">{toPersianDigits(page * 10 + idx + 1)}</td>
                          <td className="task-grid-cell execution-code-cell">{paper.uniqueCode ? `…${String(paper.uniqueCode).slice(-12)}` : '—'}</td>
                          <td className="task-grid-cell execution-name-cell">{paper.taxpayerName || '—'}</td>
                          <td className="task-grid-cell execution-code-cell">{paper.taxpayerNationalId || '—'}</td>
                          <td className="task-grid-cell">{formatJalaliDateTime(paper.notificationDate)}</td>
                          <td className="task-grid-cell"><StatusPill status={paper.paymentDeadlineStatus} map={PAYMENT_STATUS_MAP} /></td>
                          <td className="task-grid-cell execution-debt-cell">{formatDebt(paper.totalDebt)}</td>
                          <td className="task-grid-cell">{taxpayerTypeLabel(paper.taxpayerType)}</td>
                          <td className="task-grid-cell"><StatusPill status={paper.notificationStatus} map={NOTIFICATION_STATUS_MAP} /></td>
                          <td className="task-grid-cell task-grid-cell--actions execution-detail-cell">
                            <button
                              className="task-table-detail-action"
                              type="button"
                              onClick={() => router.push(`/execution-papers/${paper.id ?? paper.uniqueCode}`)}
                            >
                              <span className="task-table-detail-action__icon" aria-hidden="true">
                                <svg viewBox="0 0 24 24">
                                  <path d="M7 3.75h6.2L18.25 8.8V20A1.25 1.25 0 0 1 17 21.25H7A1.25 1.25 0 0 1 5.75 20V5A1.25 1.25 0 0 1 7 3.75Z" />
                                  <path d="M13 4v5h5" />
                                  <path d="M9 13h6M9 16h4" />
                                </svg>
                              </span>
                              <span>مشاهده جزئیات</span>
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
