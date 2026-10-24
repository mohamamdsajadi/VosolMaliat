'use client';
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { apiJson } from '../../lib/api';
import { STAGES, arrestDate, digits } from '../../lib/arrest-orders';
import styles from './arrest-orders.module.css';

export default function PaperArrestAttachment({ paperId }) {
  const [state, setState] = useState({ loading: true });
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const abort = new AbortController();
    apiJson(`/api/v1/arrest-orders?executionPaperId=${encodeURIComponent(paperId)}`, { signal: abort.signal })
      .then(data => setState({ data: data.content[0] || null, loading: false }))
      .catch(error => { if (!abort.signal.aborted) setState({ error: error.message, loading: false }); });
    return () => abort.abort();
  }, [paperId, retry]);
  const order = state.data;
  if (!order) return <div className={styles.attachmentNotice} role={state.error ? 'alert' : 'status'}>
    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M12 11v6M12 7v1" /></svg>
    <span>{state.loading ? 'در حال دریافت دستور مرتبط…' : state.error ? <>{state.error} <button type="button" onClick={() => setRetry(value => value + 1)}>تلاش مجدد</button></> : <>دستور بازداشتی برای این برگ ثبت نشده است. <Link href="/arrest-orders">بررسی برگ‌های آماده صدور ←</Link></>}</span>
  </div>;
  return <section className={styles.attachment} aria-label="دستور بازداشت پیوست برگ اجرا"><h2>دستور بازداشت اموال</h2>
    <div><span>{order.issuanceStatus === 'ISSUED' ? `شماره دستور: ${digits(order.letterNumber)}` : 'پیش‌نویس در انتظار صدور'}</span><span>{STAGES[order.stage]}</span><span>تاریخ ارجاع: {arrestDate(order.referredAt)}</span><span>مقصد: {order.referralDestination}</span></div><Link className={styles.paperLink} href={`/arrest-orders?order=${order.id}`}>مشاهده نامه و گردش ارجاع ←</Link>
  </section>;
}
