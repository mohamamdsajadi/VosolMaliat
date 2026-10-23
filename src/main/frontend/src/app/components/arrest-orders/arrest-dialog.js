'use client';
import { useEffect, useRef, useState } from 'react';
import { arrestTargets, startArrestOrders, digits } from '../../lib/arrest-orders';
import styles from './arrest-orders.module.css';
import PageHelp, { ArrestGuide } from '../page-help';

export default function ArrestStartDialog({ papers, onClose, onCompleted }) {
  const dialog = useRef(null);
  const [destination, setDestination] = useState('USER');
  const [targets, setTargets] = useState([]);
  const [supervisorId, setSupervisorId] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [note, setNote] = useState('');
  const [result, setResult] = useState(null);
  useEffect(() => { dialog.current.showModal(); }, []);
  useEffect(() => {
    if (destination === 'SPECIAL_DEPARTMENT') return;
    let active = true;
    setLoading(true); setSupervisorId(''); setTargets([]); setError('');
    Promise.all(papers.map(paper => arrestTargets({ executionPaperId: paper.id, role: destination === 'OFFICER' ? 'EXECUTION_OFFICER' : 'DIRECTORATE_EXECUTION_TEAM_SUPERVISOR' })))
      .then(lists => { if (active) setTargets((lists[0] || []).filter(user => lists.every(list => list.some(item => item.id === user.id)))); })
      .catch(failure => { if (active) setError(failure.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [papers, destination]);
  const pending = result ? papers.filter(paper => result.results.some(item => item.executionPaperId === paper.id && !item.success)) : papers;
  const submit = async event => {
    event.preventDefault(); if (saving || !pending.length) return;
    setSaving(true); setError('');
    try {
      const response = await startArrestOrders({ executionPaperIds: pending.map(paper => paper.id), destination,
        supervisorId: destination === 'USER' ? Number(supervisorId) : null,
        officerId: destination === 'OFFICER' ? Number(supervisorId) : null, note });
      setResult(response);
      onCompleted(response.results.filter(item => item.success).map(item => item.executionPaperId));
    } catch (failure) { setError(failure.message); }
    finally { setSaving(false); }
  };
  return <dialog ref={dialog} className={styles.dialog} aria-labelledby="arrest-start-title" onCancel={event => { event.preventDefault(); if (!saving) onClose(); }} onClick={event => { if (event.target === event.currentTarget && !saving) onClose(); }}>
    <form onSubmit={submit}>
      <header className={styles.dialogHeader}><div><span>آغاز فرایند</span><h2 id="arrest-start-title">صدور دستور بازداشت اموال</h2></div><button type="button" aria-label="بستن پنجره صدور" disabled={saving} onClick={onClose}>×</button></header>
      <div className={styles.listToolbar}><span>{digits(papers.length)} برگ اجرا انتخاب شده است.</span><PageHelp title="راهنمای آغاز صدور"><ArrestGuide /></PageHelp></div>
      <div className={styles.selectedPapers}>{papers.map(paper => <span key={paper.id}>{digits(paper.uniqueCode)} • {paper.taxpayerName}</span>)}</div>
      {error && <p className={styles.error} role="alert">{error}</p>}
      {result && <div role="status" className={result.failed ? styles.warning : styles.success}><b>{digits(result.succeeded)} مورد موفق، {digits(result.failed)} مورد ناموفق</b>{result.results.map(item => <p key={item.executionPaperId}>{digits(papers.find(paper => paper.id === item.executionPaperId)?.uniqueCode)}: {item.message}</p>)}</div>}
      {!!pending.length && <>
        <fieldset className={styles.destination}><legend>مقصد ارجاع</legend>
          <label className={destination === 'USER' ? styles.choiceActive : ''}><input type="radio" name="destination" value="USER" checked={destination === 'USER'} disabled={saving} onChange={() => setDestination('USER')} /><span><b>انتخاب کاربر با نقش سرپرست هیئت اجرا</b></span></label>
          <label className={destination === 'OFFICER' ? styles.choiceActive : ''}><input type="radio" name="destination" value="OFFICER" checked={destination === 'OFFICER'} disabled={saving} onChange={() => setDestination('OFFICER')} /><span><b>ارجاع مستقیم به مأمور اجرا</b></span></label>
          <label className={destination === 'SPECIAL_DEPARTMENT' ? styles.choiceActive : ''}><input type="radio" name="destination" value="SPECIAL_DEPARTMENT" checked={destination === 'SPECIAL_DEPARTMENT'} disabled={saving} onChange={() => setDestination('SPECIAL_DEPARTMENT')} /><span><b>ارجاع به اداره وصول و اجرای ویژه</b></span></label>
        </fieldset>
        {destination !== 'SPECIAL_DEPARTMENT' && <label className={styles.field}>{destination === 'OFFICER' ? 'مأمور اجرا' : 'سرپرست هیئت اجرایی'}<select id={destination === 'OFFICER' ? 'arrest-officer' : 'arrest-supervisor'} required value={supervisorId} disabled={loading || saving} onChange={event => setSupervisorId(event.target.value)}><option value="">{loading ? 'در حال دریافت کاربران…' : 'انتخاب کاربر مقصد'}</option>{targets.map(user => <option key={user.id} value={user.id}>{user.fullName} — {user.roleTitle}</option>)}</select>{!loading && !targets.length && <small className={styles.warning}>کاربر فعال مشترکی با این نقش برای اداره کل برگ‌های منتخب موجود نیست. برگ‌ها را بر اساس اداره کل انتخاب کنید یا از ارجاع به اداره ویژه استفاده کنید.</small>}</label>}
        <label className={styles.field}>توضیح ارجاع <small>(اختیاری)</small><textarea maxLength={1000} rows={3} value={note} disabled={saving} onChange={event => setNote(event.target.value)} /></label>
      </>}
      <footer className={styles.dialogFooter}><button type="button" className={styles.secondary} onClick={onClose} disabled={saving}>{pending.length ? 'انصراف' : 'بستن'}</button>{!!pending.length && <button type="submit" className={styles.primary} disabled={saving || destination !== 'SPECIAL_DEPARTMENT' && (!supervisorId || loading)}>{saving ? 'در حال ثبت…' : result ? 'تلاش مجدد برای موارد ناموفق' : `آغاز فرایند و ثبت ارجاع (${digits(pending.length)})`}</button>}</footer>
    </form>
  </dialog>;
}
