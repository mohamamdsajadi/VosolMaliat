'use client'

import { useEffect, useMemo, useState } from 'react'
import AppShell from '../../components/app-shell'
import { apiJson } from '../../lib/api'

const TYPE_LABELS = { DEBT: 'بدهی', PAYMENT: 'پرداخت', PENALTY: 'جریمه', CREDIT: 'اعتبار' }
const money = value => `${Number(value || 0).toLocaleString('fa-IR')} ریال`

export default function FakeSanimAuditPage() {
  const [taxpayers, setTaxpayers] = useState([])
  const [taxpayerId, setTaxpayerId] = useState('')
  const [history, setHistory] = useState([])
  const [status, setStatus] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [form, setForm] = useState({ entryType: 'DEBT', amountRials: '', referenceNo: '', description: '' })
  const selected = useMemo(() => taxpayers.find(item => String(item.taxpayerId) === String(taxpayerId)), [taxpayers, taxpayerId])

  useEffect(() => {
    apiJson('/api/v1/fake-sanim/audit/taxpayers').then(items => {
      setTaxpayers(items)
      if (items.length) setTaxpayerId(String(items[0].taxpayerId))
    }).catch(err => setError(err.message))
  }, [])

  useEffect(() => { if (taxpayerId) refresh() }, [taxpayerId]) // eslint-disable-line react-hooks/exhaustive-deps

  async function refresh() {
    setBusy(true); setError('')
    try {
      const entries = await apiJson(`/api/v1/fake-sanim/audit/taxpayers/${taxpayerId}/history`)
      setHistory(entries)
      try { setStatus(await apiJson(`/api/v1/fake-sanim/audit/taxpayers/${taxpayerId}/inquiry`)) }
      catch { setStatus(null) }
    } catch (err) { setError(err.message) }
    finally { setBusy(false) }
  }

  async function addEntry(event) {
    event.preventDefault(); setBusy(true); setError('')
    try {
      await apiJson(`/api/v1/fake-sanim/audit/taxpayers/${taxpayerId}/entries`, {
        method: 'POST', body: JSON.stringify({ ...form, amountRials: Number(form.amountRials) })
      })
      setForm({ entryType: 'DEBT', amountRials: '', referenceNo: '', description: '' })
      await refresh()
    } catch (err) { setError(err.message); setBusy(false) }
  }

  async function randomize() {
    setBusy(true); setError('')
    try {
      await apiJson(`/api/v1/fake-sanim/audit/taxpayers/${taxpayerId}/random`, { method: 'POST', body: JSON.stringify({ count: 12 }) })
      await refresh()
    } catch (err) { setError(err.message); setBusy(false) }
  }

  return <AppShell active="test-front" title="حسابرسی بدهی سنیم" eyebrow="Fake SANIM Audit">
    <section className="panel-section">
      <div className="section-heading"><div><p className="eyebrow">انتخاب مؤدی</p><h2>آخرین وضعیت بدهی</h2></div></div>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="form-grid">
        <label><span>مؤدی موجود</span><select value={taxpayerId} onChange={e => setTaxpayerId(e.target.value)}>
          {taxpayers.map(item => <option key={`${item.taxpayerType}-${item.identifier}`} value={item.taxpayerId}>
            {item.displayName} — {item.identifier} ({item.taxpayerType === 'LEGAL' ? 'حقوقی' : 'حقیقی'})
          </option>)}
        </select></label>
        <div style={{ alignSelf: 'end', display: 'flex', gap: 8 }}>
          <button className="primary-action" disabled={!taxpayerId || busy} onClick={refresh}>استعلام</button>
          <button className="ghost-action" disabled={!taxpayerId || busy} onClick={randomize}>تولید ۱۲ رویداد تصادفی</button>
        </div>
      </div>
      {status && <div className="dashboard-stat-grid" style={{ marginTop: 18 }}>
        <article className="dashboard-stat-card"><span>بدهی</span><strong>{money(status.totalDebtRials)}</strong></article>
        <article className="dashboard-stat-card dashboard-stat-card--danger"><span>جریمه</span><strong>{money(status.totalPenaltyRials)}</strong></article>
        <article className="dashboard-stat-card dashboard-stat-card--success"><span>پرداخت</span><strong>{money(status.totalPaymentRials)}</strong></article>
        <article className="dashboard-stat-card dashboard-stat-card--success"><span>اعتبار</span><strong>{money(status.totalCreditRials)}</strong></article>
        <article className="dashboard-stat-card dashboard-stat-card--warning"><span>مانده قابل مطالبه</span><strong>{money(status.outstandingDebtRials)}</strong></article>
      </div>}
    </section>

    <section className="panel-section">
      <div className="section-heading"><div><p className="eyebrow">ثبت غیرقابل‌ویرایش</p><h2>رویداد مالی جدید برای {selected?.displayName || 'مؤدی'}</h2></div></div>
      <form className="form-grid" onSubmit={addEntry}>
        <label><span>نوع رویداد</span><select value={form.entryType} onChange={e => setForm({ ...form, entryType: e.target.value })}>
          {Object.entries(TYPE_LABELS).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
        </select></label>
        <label><span>مبلغ (ریال)</span><input type="number" min="1" required value={form.amountRials} onChange={e => setForm({ ...form, amountRials: e.target.value })} /></label>
        <label><span>شماره مرجع</span><input value={form.referenceNo} onChange={e => setForm({ ...form, referenceNo: e.target.value })} /></label>
        <label><span>شرح</span><input value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} /></label>
        <button className="primary-action" disabled={!taxpayerId || busy}>ثبت رویداد</button>
      </form>
    </section>

    <section className="panel-section">
      <div className="section-heading"><div><p className="eyebrow">Audit Trail</p><h2>تاریخچه تغییرناپذیر</h2></div><span>{history.length.toLocaleString('fa-IR')} رویداد</span></div>
      <div className="task-grid-wrap"><table className="task-grid-table"><thead><tr><th>نوع</th><th>مبلغ</th><th>تاریخ رویداد</th><th>مرجع</th><th>شرح</th></tr></thead>
        <tbody>{history.map(item => <tr key={item.id}><td>{TYPE_LABELS[item.entryType]}</td><td>{money(item.amountRials)}</td><td>{new Date(item.occurredAt).toLocaleString('fa-IR')}</td><td>{item.referenceNo || '—'}</td><td>{item.description || '—'}</td></tr>)}</tbody>
      </table>{!history.length && <div className="empty-state">هنوز سابقه‌ای برای این مؤدی ثبت نشده است.</div>}</div>
    </section>
  </AppShell>
}
