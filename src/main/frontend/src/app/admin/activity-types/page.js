'use client'

import { useEffect, useState } from 'react'
import AppShell from '../../components/app-shell'
import { apiJson } from '../../lib/api'

export default function ActivityTypesPage() {
  const [rows, setRows] = useState([])
  const [form, setForm] = useState({ intaCode: '', title: '' })
  const [editing, setEditing] = useState(null)
  const [message, setMessage] = useState('')
  const load = () => apiJson('/api/v1/masterdata/activity-types').then(setRows)
  useEffect(() => { load().catch(e => setMessage(e.message)) }, [])
  async function save(event) {
    event.preventDefault()
    try {
      await apiJson(`/api/v1/masterdata/activity-types${editing ? `/${editing}` : ''}`, {
        method: editing ? 'PUT' : 'POST', body: JSON.stringify(form)
      })
      setEditing(null); setForm({ intaCode: '', title: '' }); setMessage('نوع فعالیت ثبت شد.'); await load()
    } catch (error) { setMessage(error.message) }
  }
  async function remove(id) {
    try { await apiJson(`/api/v1/masterdata/activity-types/${id}`, { method: 'DELETE' }); await load() }
    catch (error) { setMessage(error.message) }
  }
  return <AppShell active="data-entry" title="انواع فعالیت">
    {message && <p role="status">{message}</p>}
    <form onSubmit={save} className="admin-form">
      <label>اینتاکد<input required value={form.intaCode} onChange={e => setForm({ ...form, intaCode: e.target.value })} /></label>
      <label>عنوان<input required value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} /></label>
      <button type="submit">{editing ? 'ذخیره تغییرات' : 'افزودن'}</button>
    </form>
    <table><thead><tr><th>اینتاکد</th><th>عنوان</th><th>عملیات</th></tr></thead>
      <tbody>{rows.map(row => <tr key={row.id}><td>{row.intaCode}</td><td>{row.title}</td><td>
        <button onClick={() => { setEditing(row.id); setForm({ intaCode: row.intaCode, title: row.title }) }}>ویرایش</button>
        <button onClick={() => remove(row.id)}>حذف</button>
      </td></tr>)}</tbody></table>
  </AppShell>
}
