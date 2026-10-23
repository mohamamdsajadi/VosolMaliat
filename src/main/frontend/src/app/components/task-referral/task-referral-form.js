'use client';
import dynamic from 'next/dynamic';
import { useState } from 'react';
import { claimCartableTask, hasRenderableSchema } from '../../lib/tasks';
import { isReferralTask, canShowReferral } from '../../lib/task-referral';
import styles from './task-referral.module.css';
import '@bpmn-io/form-js-viewer/dist/assets/form-js.css';

const ProcessForm = dynamic(() => import('../embedded-form'), { ssr: false, loading: () => <p role="status">در حال بارگذاری فرم ارجاع…</p> });

export default function TaskReferralForm({ task, user, onComplete, onReload, submitting }) {
  const [claiming, setClaiming] = useState(false);
  const [error, setError] = useState('');
  const complete = task?.allowedActions?.complete === true;
  const claim = async () => {
    setClaiming(true); setError('');
    try { await claimCartableTask(task.taskKey || task.id); onReload(); }
    catch (failure) { setError(failure.message || 'دریافت کار ناموفق بود.'); }
    finally { setClaiming(false); }
  };
  return <section className={styles.form} aria-label="ارجاع برگ اجرا">
    <h4>ارجاع برگ اجرا</h4>
    {error && <p role="alert" className={styles.error}>{error}</p>}
    {!task ? <p>اطلاعات کار در دسترس نیست.</p> : !canShowReferral(task, user) ? <p>شما مجوز ارجاع این برگ اجرا را ندارید.</p> : !isReferralTask(task) ? <p>مرحله جاری، مرحله انتخاب مسئول و ارجاع نیست. ارجاع پس از رسیدن پرونده به مرحله مربوط در دسترس قرار می‌گیرد.</p>
      : !complete ? <><p>{task.allowedActions?.claim ? 'برای تکمیل فرم ارجاع، ابتدا کار را دریافت کنید.' : 'در وضعیت فعلی مجوز تکمیل ارجاع این کار را ندارید.'}</p>{task.allowedActions?.claim && <button type="button" className={styles.action} disabled={claiming} onClick={claim}>{claiming ? 'در حال دریافت…' : 'دریافت کار'}</button>}</>
        : hasRenderableSchema(task) ? <ProcessForm schema={task.schema} data={task.data} onComplete={onComplete} submitting={submitting} />
          : <p role="status">فرم ارجاع این مرحله هنوز در دسترس نیست. پس از آماده شدن فرایند و فرم مربوط، انتخاب مسئول و ثبت ارجاع از همین بخش انجام می‌شود.</p>}
    <button type="button" className={styles.refresh} onClick={onReload} disabled={submitting || claiming}>بروزرسانی وضعیت کار</button>
  </section>;
}
