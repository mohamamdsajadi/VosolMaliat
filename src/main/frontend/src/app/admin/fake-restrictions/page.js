'use client'

import Link from 'next/link'
import { useCallback, useEffect, useMemo, useState } from 'react'
import AppShell from '../../components/app-shell'
import RefreshButton from '../../components/refresh-button'
import { apiJson } from '../../lib/api'
import { formatJalaliDateTime } from '../../lib/display'

const statusLabel = status => ({ SENT: 'در انتظار تایید', CONFIRMED: 'اعمال محدودیت تایید شد' }[status] || status || 'نامشخص')

export default function FakeRestrictionsPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')
  const [modal, setModal] = useState(null)
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setItems(await apiJson('/api/v1/fake-restrictions/requests'))
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
    confirmed: items.filter(item => item.status === 'CONFIRMED').length,
    people: items.reduce((sum, item) => sum + Number(item.relatedPersonCount || 0), 0)
  }), [items])

  const openPreview = item => {
    let parsed
    try {
      parsed = JSON.parse(item.payload || '[]')
    } catch {
      parsed = []
    }
    setModal({ item, text: JSON.stringify(parsed, null, 2), error: '' })
  }

  const confirm = async () => {
    let payload
    try {
      payload = JSON.parse(modal.text)
      if (!Array.isArray(payload)) throw new Error('ساختار درخواست باید آرایه JSON باشد.')
    } catch (error) {
      setModal(current => ({ ...current, error: error.message || 'JSON نامعتبر است.' }))
      return
    }
    setBusy(true)
    try {
      await apiJson(`/api/v1/fake-restrictions/requests/${modal.item.id}/confirm`, {
        method: 'POST',
        body: JSON.stringify(payload)
      })
      setModal(null)
      setMessage('دریافت و اعمال محدودیت تایید و در تاریخچه برگ اجرا ثبت شد.')
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
      title="سامانه اعمال محدودیت آزمایشی"
      eyebrow="Fake Restriction System"
      actions={<div className="row-actions"><Link className="text-action" href="/test-front">بازگشت به Test Front</Link><RefreshButton onClick={load} loading={loading} /></div>}
    >
      <main className="fake-service-page">
        <section className="fake-service-hero fake-service-hero--restriction">
          <div><p className="eyebrow">کنترل اعمال محدودیت</p><h2>فهرست اشخاص مرتبط ارسالی برای محدودیت</h2><p className="fake-service-hero__copy">محتوای هر درخواست را قبل از تایید مشاهده و در صورت نیاز اصلاح کنید.</p></div>
          <div className="fake-service-stats">
            <span>کل: {stats.total.toLocaleString('fa-IR')}</span>
            <span className="fake-service-stat--warning">در انتظار: {stats.pending.toLocaleString('fa-IR')}</span>
            <span className="fake-service-stat--success">تاییدشده: {stats.confirmed.toLocaleString('fa-IR')}</span>
            <span>اشخاص: {stats.people.toLocaleString('fa-IR')}</span>
          </div>
        </section>
        {message && <div className="notice">{message}</div>}
        <section className="task-table-card fake-service-table-card">
          <div className="task-table-header"><div><p className="eyebrow">درخواست‌های دریافتی</p><h2>اعمال محدودیت اشخاص مرتبط</h2></div><span>{items.length.toLocaleString('fa-IR')} مورد</span></div>
          {loading ? <div className="empty-state compact">در حال دریافت درخواست‌ها...</div> : items.length === 0 ? (
            <div className="empty-state">هنوز فهرستی برای اعمال محدودیت دریافت نشده است.</div>
          ) : (
            <div className="task-table-scroll">
              <table className="task-grid-table fake-service-table">
                <thead><tr><th>#</th><th>برگ اجرا</th><th>درخواست روزنامه رسمی</th><th>تعداد اشخاص</th><th>زمان دریافت</th><th>زمان تایید</th><th>وضعیت</th><th>عملیات</th></tr></thead>
                <tbody>{items.map((item, index) => (
                  <tr key={item.id} className={`task-grid-row ${index % 2 ? 'task-grid-row--zebra' : ''}`}>
                    <td className="task-grid-cell task-grid-cell--index">{(index + 1).toLocaleString('fa-IR')}</td>
                    <td className="task-grid-cell"><Link className="text-action fake-service-paper-code" href={`/execution-papers/${item.executionPaperId}`}>{item.executionPaperCode || `#${item.executionPaperId}`}</Link></td>
                    <td className="task-grid-cell"><small className="fake-service-request-id">{item.officialGazetteRequestId}</small></td>
                    <td className="task-grid-cell"><strong>{Number(item.relatedPersonCount || 0).toLocaleString('fa-IR')}</strong></td>
                    <td className="task-grid-cell">{formatJalaliDateTime(item.sentAt)}</td>
                    <td className="task-grid-cell">{formatJalaliDateTime(item.confirmedAt)}</td>
                    <td className="task-grid-cell"><span className={`fake-service-badge fake-service-badge--${item.status === 'CONFIRMED' ? 'confirmed' : 'sent'}`}>{statusLabel(item.status)}</span></td>
                    <td className="task-grid-cell task-grid-cell--actions">{item.status === 'CONFIRMED' ? <span className="task-assignee-pill task-assignee-pill--assigned">تکمیل شده</span> : <button className="task-table-primary-action" type="button" onClick={() => openPreview(item)}>بازبینی و تایید</button>}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          )}
        </section>
      </main>

      {modal && (
        <div className="fake-sanim-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="restriction-preview-title">
          <section className="fake-sanim-modal fake-payload-modal">
            <div className="fake-sanim-modal__header">
              <div><p className="eyebrow">بازبینی درخواست</p><h2 id="restriction-preview-title">تایید اعمال محدودیت</h2></div>
              <button type="button" className="fake-sanim-modal__close" onClick={() => setModal(null)} aria-label="بستن">×</button>
            </div>
            <div className="fake-payload-modal__context">
              <span>برگ اجرا: <strong>{modal.item.executionPaperCode || `#${modal.item.executionPaperId}`}</strong></span>
              <span>تعداد اشخاص: <strong>{modal.item.relatedPersonCount}</strong></span>
            </div>
            <label className="fake-payload-editor">
              <span>فهرست اشخاص مشمول محدودیت (JSON قابل ویرایش)</span>
              <textarea value={modal.text} onChange={event => setModal(current => ({ ...current, text: event.target.value, error: '' }))} spellCheck="false" />
              <small>نسخه نهایی پس از تایید در سابقه برگ اجرا ذخیره می‌شود.</small>
            </label>
            {modal.error && <div className="notice error">{modal.error}</div>}
            <div className="fake-sanim-modal__actions">
              <button type="button" className="ghost-action" onClick={() => setModal(null)} disabled={busy}>انصراف</button>
              <button type="button" className="fake-service-primary" onClick={confirm} disabled={busy}>{busy ? 'در حال تایید...' : 'تایید اعمال محدودیت'}</button>
            </div>
          </section>
        </div>
      )}
    </AppShell>
  )
}
