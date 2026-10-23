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
  return <section className={styles.attachment} aria-label="دستور بازداشت پیوست برگ اجرا"><h2>دستور بازداشت اموال</h2>
    {state.loading ? <p role="status">در حال دریافت دستور مرتبط…</p> : state.error ? <p role="alert">{state.error} <button className={styles.secondary} onClick={() => setRetry(value => value + 1)}>تلاش مجدد</button></p> : order ? <><div><span>{order.issuanceStatus === 'ISSUED' ? `شماره دستور: ${digits(order.letterNumber)}` : 'پیش‌نویس در انتظار صدور'}</span><span>{STAGES[order.stage]}</span><span>تاریخ ارجاع: {arrestDate(order.referredAt)}</span><span>مقصد: {order.referralDestination}</span></div><Link className={styles.paperLink} href={`/arrest-orders?order=${order.id}`}>مشاهده نامه و گردش ارجاع ←</Link></> : <p>دستور بازداشتی برای این برگ ثبت نشده است. <Link className={styles.paperLink} href="/arrest-orders">بررسی برگ‌های آماده صدور ←</Link></p>}
  </section>;
}
