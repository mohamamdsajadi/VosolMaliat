'use client';
import dynamic from 'next/dynamic';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Link from 'next/link';
import { fetchCartableTasks, fetchTask, completeTask, claimCartableTask, hasRenderableSchema } from '../../lib/tasks';
import { canOpenReferral, referralTasksForPaper, referralHref } from '../../lib/task-referral';
import { identifier } from '../../lib/execution-papers';
import actionStyles from '../task-referral/task-referral.module.css';
import styles from './execution-papers.module.css';
import '@bpmn-io/form-js-viewer/dist/assets/form-js.css';

const ProcessForm = dynamic(() => import('../embedded-form'), { ssr: false, loading: () => <p role="status">در حال بارگذاری فرم انتخاب مقصد…</p> });
const taskKey = task => task.taskKey || task.id;
// Share a form only when its field values agree. Never copy paper process variables.
function formValues(task) {
  const values = {};
  const visit = components => (components || []).forEach(field => {
    if (field.key) values[field.key] = task.data?.[field.key] ?? field.defaultValue ?? null;
    visit(field.components);
  });
  visit(task.schema?.components);
  return values;
}
function formSignature(task) { return JSON.stringify({ stage: task.taskDefinitionId, schema: task.schema, data: formValues(task) }); }

export default function PaperReferralDialog({ papers, user, onClose, onSuccess }) {
  const dialog = useRef(null);
  const busy = useRef(false);
  const succeeded = useRef(new Set());
  const [attempt, setAttempt] = useState(0);
  const [prepared, setPrepared] = useState({ loading: true, rows: [], error: '' });
  const [submitting, setSubmitting] = useState(false);
  const [results, setResults] = useState([]);
  const [error, setError] = useState('');
  const [confirmed, setConfirmed] = useState(false);
  const refresh = () => { setConfirmed(false); setAttempt(value => value + 1); };
  useEffect(() => { dialog.current.showModal(); }, []);
  useEffect(() => {
    let active = true;
    (async () => {
      setPrepared({ loading: true, rows: [], error: '' });
      try {
        const tasks = await fetchCartableTasks();
        const rows = await Promise.all(papers.filter(paper => !succeeded.current.has(paper.id)).map(async paper => {
          const matches = referralTasksForPaper(tasks, paper, user);
          if (matches.length !== 1) return { paper, matches, error: matches.length ? 'چند وظیفه ارجاع وجود دارد؛ مرحله موردنظر را جداگانه انتخاب کنید.' : 'وظیفه ارجاعی برای این برگ در کارتابل شما وجود ندارد.' };
          try {
            const task = await fetchTask(taskKey(matches[0]));
            return { paper, matches, task, error: referralTasksForPaper([task], paper, user).length !== 1 ? 'وضعیت وظیفه تغییر کرده است.' : !canOpenReferral(task) ? 'مجوز ارجاع در وضعیت فعلی وجود ندارد.' : !hasRenderableSchema(task) ? 'فرم انتخاب مقصد این مرحله هنوز در دسترس نیست.' : '' };
          } catch (failure) { return { paper, matches, error: failure.message || 'دریافت وظیفه ناموفق بود.' }; }
        }));
        if (active) setPrepared({ loading: false, rows, error: '' });
      } catch (failure) { if (active) setPrepared({ loading: false, rows: [], error: failure.message || 'دریافت وظایف ارجاع ناموفق بود.' }); }
    })();
    return () => { active = false; };
  }, [papers, user, attempt]);
  const rows = prepared.rows;
  const first = rows[0]?.task;
  const data = useMemo(() => first ? formValues(first) : {}, [first]);
  const compatible = Boolean(first && rows.every(row => !row.error && formSignature(row.task) === formSignature(first)));
  const ready = !prepared.loading && !prepared.error && rows.length > 0 && compatible;
  const submit = useCallback(async values => {
    if (busy.current || !ready || !confirmed) return;
    busy.current = true; setSubmitting(true); setError('');
    const newResults = [];
    // Independent completions: stop on failure and retain successful paper IDs.
    for (const row of rows) {
      if (succeeded.current.has(row.paper.id)) continue;
      try {
        let current = await fetchTask(taskKey(row.task));
        if (referralTasksForPaper([current], row.paper, user).length !== 1 || !canOpenReferral(current) || formSignature(current) !== formSignature(row.task)) throw Error('وضعیت یا فرم ارجاع تغییر کرده است؛ اطلاعات را به‌روزرسانی کنید.');
        if (!current.allowedActions?.complete) {
          await claimCartableTask(taskKey(current));
          current = await fetchTask(taskKey(current));
        }
        if (!current.allowedActions?.complete || referralTasksForPaper([current], row.paper, user).length !== 1 || formSignature(current) !== formSignature(row.task)) throw Error('مجوز یا فرم تکمیل ارجاع تغییر کرده است.');
        const fieldKeys = Object.keys(formValues(current));
        await completeTask(taskKey(current), Object.fromEntries(Object.entries(values).filter(([key]) => fieldKeys.includes(key))));
        succeeded.current.add(row.paper.id);
        newResults.push({ paper: row.paper, success: true });
      } catch (failure) {
        newResults.push({ paper: row.paper, success: false, message: failure.message });
        setError('عملیات متوقف شد. ارجاع‌های موفق حفظ شده‌اند؛ پیش از ادامه، وضعیت باقی برگ‌ها را به‌روزرسانی کنید.');
        break;
      }
      setResults(previous => [...previous.filter(result => result.success && !newResults.some(item => item.paper.id === result.paper.id)), ...newResults]);
    }
    setResults(previous => [...previous.filter(result => result.success && !newResults.some(item => item.paper.id === result.paper.id)), ...newResults]);
    onSuccess(newResults.filter(result => result.success).map(result => result.paper.id));
    setSubmitting(false); setConfirmed(false); busy.current = false;
    setPrepared(previous => ({ ...previous, rows: [] }));
  }, [ready, confirmed, rows, user, onSuccess]);
  return <dialog ref={dialog} className={`${styles.viewDialog} ${styles.referralDialog}`} aria-label={papers.length > 1 ? 'ارجاع گروهی برگ‌های اجرا' : 'ارجاع برگ اجرا'}
    onCancel={event => { if (busy.current) event.preventDefault(); }} onClose={() => { if (!busy.current) onClose(); }}>
    <div><button type="button" className={styles.dialogClose} aria-label="بستن ارجاع" disabled={submitting} onClick={() => dialog.current.close()}>×</button>
      <h2>{papers.length > 1 ? `ارجاع گروهی ${identifier(papers.length)} برگ اجرا` : `ارجاع برگ اجرا ${identifier(papers[0].uniqueCode)}`}</h2>
      <p>مقصد ارجاع را در فرم زیر انتخاب کنید. مقصدهای مجاز از فرم مرحله جاری فرایند تعیین می‌شوند.</p>
      {papers.length > 1 && <p>هر برگ جداگانه ثبت می‌شود؛ در صورت خطا، ادامه عملیات متوقف و نتیجه هر برگ نمایش داده می‌شود.</p>}
      <details><summary>برگ‌های انتخاب‌شده</summary><ul className={styles.referralResults}>{papers.map(paper => <li key={paper.id}><bdi>{identifier(paper.uniqueCode)}</bdi></li>)}</ul></details>
      {prepared.loading && <p role="status">در حال دریافت وظایف و فرم انتخاب مقصد…</p>}
      {prepared.error && <p role="alert">دریافت وظایف ارجاع ناموفق بود. {prepared.error}</p>}
      {error && <p role="alert">{error}</p>}
      {results.length > 0 && <ul className={styles.referralResults} aria-live="polite">{results.map(result => <li key={result.paper.id}><bdi>{identifier(result.paper.uniqueCode)}</bdi>: {result.success ? 'ارجاع شد' : `ثبت ارجاع تأیید نشد؛ ${result.message || 'خطای سرویس'}`}</li>)}</ul>}
      {rows.some(row => row.error) && <ul className={styles.referralResults}>{rows.map(row => <li key={row.paper.id}><bdi>{identifier(row.paper.uniqueCode)}</bdi>: {row.error || 'آماده ارجاع'}{row.matches?.length > 1 && row.matches.map(task => <Link key={taskKey(task)} className={styles.entryLink} href={referralHref(task)}>{task.name}</Link>)}</li>)}</ul>}
      {!prepared.loading && !prepared.error && rows.length > 1 && rows.every(row => !row.error) && !compatible && <p role="alert">فرم یا اطلاعات ارجاع این برگ‌ها یکسان نیست. برگ‌های دارای مرحله و فرم یکسان را در یک گروه انتخاب کنید.</p>}
      {ready && <><label className={styles.referralConfirmation}><input type="checkbox" checked={confirmed} disabled={submitting} onChange={event => setConfirmed(event.target.checked)} />{identifier(rows.length)} برگ به مقصدی که در فرم انتخاب می‌کنم ارجاع شود{rows.some(row => !row.task.allowedActions.complete) ? '؛ وظایف موردنیاز نیز دریافت شوند.' : '.'}</label>
        <fieldset className={actionStyles.form} disabled={submitting || !confirmed}><legend>انتخاب مقصد و ثبت ارجاع</legend><ProcessForm schema={first.schema} data={data} onComplete={submit} submitting={submitting || !confirmed} /></fieldset></>}
      {submitting && <p role="status">در حال ثبت ارجاع… این پنجره را باز نگه دارید.</p>}
      <button type="button" className={actionStyles.refresh} onClick={refresh} disabled={submitting || prepared.loading}>به‌روزرسانی وظایف ارجاع</button>
    </div>
  </dialog>;
}
