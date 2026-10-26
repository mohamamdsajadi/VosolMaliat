'use client'

import Link from 'next/link'
import { useCallback, useEffect, useMemo, useState } from 'react'
import AppShell from '../../components/app-shell'
import RefreshButton from '../../components/refresh-button'
import { apiJson } from '../../lib/api'
import { formatJalaliDateTime } from '../../lib/display'

const statusLabel = status => ({
  SENT: 'در انتظار پاسخ',
  RESPONDED: 'پاسخ ارسال شده'
}[status] || status || 'نامشخص')

function PayloadModal({ modal, busy, onChange, onClose, onSubmit }) {
  if (!modal) return null
  return (
    <div className="fake-sanim-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="gazette-preview-title">
      <section className="fake-sanim-modal fake-payload-modal">
        <div className="fake-sanim-modal__header">
          <div>
            <p className="eyebrow">بازبینی و ویرایش قبل از ارسال</p>
            <h2 id="gazette-preview-title">پاسخ روزنامه رسمی</h2>
          </div>
          <button type="button" className="fake-sanim-modal__close" onClick={onClose} aria-label="بستن">×</button>
        </div>
        <div className="fake-payload-modal__context">
          <span>برگ اجرا: <strong>{modal.item.executionPaperCode || `#${modal.item.executionPaperId}`}</strong></span>
          <span>شناسه ملی: <strong>{modal.item.legalPersonNationalId}</strong></span>
          <span>تاریخ بدهی: <strong>{modal.item.debtPaperDate}</strong></span>
        </div>
        <label className="fake-payload-editor">
          <span>محتوای پاسخ (JSON قابل ویرایش)</span>
          <textarea value={modal.text} onChange={event => onChange(event.target.value)} spellCheck="false" />
          <small>پاسخ باید یک آرایه JSON از اشخاص حقیقی یا حقوقی مرتبط باشد.</small>
        </label>
        {modal.error && <div className="notice error">{modal.error}</div>}
        <div className="fake-sanim-modal__actions">
          <button type="button" className="ghost-action" onClick={onClose} disabled={busy}>انصراف</button>
          <button type="button" className="fake-service-primary" onClick={onSubmit} disabled={busy}>
            {busy ? 'در حال ارسال...' : 'تایید و ارسال پاسخ'}
          </button>
        </div>
      </section>
    </div>
  )
}

export default function FakeOfficialGazettePage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')
  const [modal, setModal] = useState(null)
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setItems(await apiJson('/api/v1/fake-official-gazette/requests'))
    } catch (error) {
      setMessage(error.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const stats = useMemo(() => ({
    total: items.length,
    pending: items.filter(item => item.status === 'SENT').length,
    responded: items.filter(item => item.status === 'RESPONDED').length
  }), [items])

  const openPreview = async item => {
    setMessage('')
    setBusy(true)
    try {
      const payload = await apiJson(`/api/v1/fake-official-gazette/requests/${item.id}/preview`)
      setModal({ item, text: JSON.stringify(payload, null, 2), error: '' })
    } catch (error) {
      setMessage(error.message)
    } finally {
      setBusy(false)
    }
  }

  const submitResponse = async () => {
    let payload
    try {
      payload = JSON.parse(modal.text)
      if (!Array.isArray(payload)) throw new Error('ساختار پاسخ باید آرایه JSON باشد.')
    } catch (error) {
      setModal(current => ({ ...current, error: error.message || 'JSON نامعتبر است.' }))
      return
    }
    setBusy(true)
    try {
      await apiJson(`/api/v1/fake-official-gazette/requests/${modal.item.id}/respond`, {
        method: 'POST',
        body: JSON.stringify(payload)
      })
      setModal(null)
      setMessage('پاسخ ویرایش‌شده به Camunda ارسال و در تاریخچه برگ اجرا ثبت شد.')
      await load()
    } catch (error) {
      setModal(current => ({ ...current, error: error.message }))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AppShell
      active="test-front"
      title="روزنامه رسمی"
      eyebrow="Fake Official Gazette"
      actions={<div className="row-actions"><Link className="text-action" href="/test-front">بازگشت به Test Front</Link><RefreshButton onClick={load} loading={loading} /></div>}
    >
      <main className="fake-service-page">
        <section className="fake-service-hero fake-service-hero--gazette">
          <div><p className="eyebrow">پاسخ غیرهمزمان</p><h2>استخراج اعضای هیئت‌مدیره و اشخاص مرتبط</h2><p className="fake-service-hero__copy">درخواست را بازبینی کنید، پاسخ تولیدشده را ببینید و پیش از ارسال به فرایند ویرایش کنید.</p></div>
          <div className="fake-service-stats">
            <span>کل: {stats.total.toLocaleString('fa-IR')}</span>
            <span className="fake-service-stat--warning">در انتظار: {stats.pending.toLocaleString('fa-IR')}</span>
            <span className="fake-service-stat--success">پاسخ‌شده: {stats.responded.toLocaleString('fa-IR')}</span>
          </div>
        </section>
        {message && <div className="notice">{message}</div>}
        <section className="task-table-card fake-service-table-card">
          <div className="task-table-header"><div><p className="eyebrow">صف درخواست‌ها</p><h2>درخواست‌های روزنامه رسمی</h2></div><span>{items.length.toLocaleString('fa-IR')} مورد</span></div>
          {loading ? <div className="empty-state compact">در حال دریافت درخواست‌ها...</div> : items.length === 0 ? (
            <div className="empty-state">درخواستی از فرایند صدور برگ اجرا دریافت نشده است.</div>
          ) : (
            <div className="task-table-scroll">
              <table className="task-grid-table fake-service-table">
                <thead><tr><th>#</th><th>برگ اجرا</th><th>مودی حقوقی</th><th>تاریخ بدهی</th><th>زمان دریافت</th><th>وضعیت</th><th>تعداد اشخاص</th><th>عملیات</th></tr></thead>
                <tbody>{items.map((item, index) => (
                  <tr key={item.id} className={`task-grid-row ${index % 2 ? 'task-grid-row--zebra' : ''}`}>
                    <td className="task-grid-cell task-grid-cell--index">{(index + 1).toLocaleString('fa-IR')}</td>
                    <td className="task-grid-cell"><Link className="text-action fake-service-paper-code" href={`/execution-papers/${item.executionPaperId}`}>{item.executionPaperCode || `#${item.executionPaperId}`}</Link></td>
                    <td className="task-grid-cell"><strong>{item.legalPersonNationalId}</strong><small className="fake-service-request-id">{item.requestId}</small></td>
                    <td className="task-grid-cell">{item.debtPaperDate || '-'}</td>
                    <td className="task-grid-cell">{formatJalaliDateTime(item.sentAt)}</td>
                    <td className="task-grid-cell"><span className={`fake-service-badge fake-service-badge--${item.status === 'RESPONDED' ? 'confirmed' : 'sent'}`}>{statusLabel(item.status)}</span></td>
                    <td className="task-grid-cell">{Number(item.relatedPersonCount || 0).toLocaleString('fa-IR')}</td>
                    <td className="task-grid-cell task-grid-cell--actions">{item.status === 'RESPONDED' ? <span className="task-assignee-pill task-assignee-pill--assigned">تکمیل شده</span> : <button className="task-table-primary-action" type="button" onClick={() => openPreview(item)} disabled={busy}>پیش‌نمایش پاسخ</button>}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          )}
        </section>
      </main>
      <PayloadModal modal={modal} busy={busy} onChange={text => setModal(current => ({ ...current, text, error: '' }))} onClose={() => setModal(null)} onSubmit={submitResponse} />
    </AppShell>
  )
}
