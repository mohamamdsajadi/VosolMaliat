'use client';
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { apiJson } from '../../lib/api';
import { hasArrestPermission } from '../../lib/arrest-orders';
import { identifier, money } from '../../lib/execution-papers';
import Icon from './dashboard-icon';
import styles from './manager-dashboard.module.css';

export default function PendingArrestPapers({ user, refreshKey, size = 5 }) {
  const [page, setPage] = useState(0);
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState(null);
  const canView = hasArrestPermission(user, 'ARREST_ORDER_VIEW');
  const path = `/api/v1/dashboard/pending-arrest-papers?page=${page}&size=${size}`;
  useEffect(() => {
    if (!canView) return;
    const controller = new AbortController();
    apiJson(path, { signal: controller.signal }).then(data => {
      if (controller.signal.aborted) return;
      if (page > 0 && page >= data.totalPages) { setPage(Math.max(0, data.totalPages - 1)); return; }
      setResult({ path, refreshKey, attempt, data });
    }).catch(error => {
      if (!controller.signal.aborted) setResult({ path, refreshKey, attempt, error: error.message });
    });
    return () => controller.abort();
  }, [path, page, canView, refreshKey, attempt]);
  const current = result?.path === path && result?.refreshKey === refreshKey && result?.attempt === attempt ? result : null;
  const loading = canView && !current;
  const data = current?.data;
  const rows = data?.content || [];
  const canOpenPaper = ['REPORT_VIEW', 'EXECUTION_PAPER_DETAIL_VIEW'].some(permission => hasArrestPermission(user, permission));
  return <section className={`${styles.card} ${styles.pendingCard}`} aria-labelledby="pending-heading" aria-busy={loading}>
    <div className={styles.cardHeading}><h2 id="pending-heading" className={styles.dangerHeading}><Icon name="info" />نیازمند اقدام شما</h2>{canView && <Link className={styles.viewAll} href="/arrest-orders">دستورهای بازداشت ←</Link>}</div>
    <p className={styles.pendingDescription}>برگ‌های آماده صدور دستور بازداشت و پیش‌نویس‌های در انتظار تأیید شما</p>
    {current?.error && <div className={styles.error} role="alert"><span>دریافت برگ‌های نیازمند اقدام ناموفق بود.</span><button onClick={() => setAttempt(value => value + 1)}>تلاش مجدد</button></div>}
    <div className={styles.tableScroll} tabIndex={0} role="region" aria-label="برگ‌های نیازمند صدور یا تأیید دستور بازداشت">
      <table className={`${styles.tasksTable} ${styles.pendingPapersTable}`}><thead><tr>{['ردیف', 'شماره برگ اجرا', 'مشخصات مؤدی', 'مانده بدهی (ریال)', 'وضعیت دستور بازداشت', 'عملیات'].map(label => <th key={label} scope="col">{label}</th>)}</tr></thead>
        <tbody>{loading ? <tr><td colSpan={6} className={styles.tableEmpty}>در حال دریافت برگ‌های نیازمند اقدام…</td></tr> : current?.error ? <tr><td colSpan={6} className={styles.tableEmpty}>فهرست دریافت نشد.</td></tr> : !rows.length ? <tr><td colSpan={6} className={styles.tableEmpty}>برگی برای صدور یا تأیید دستور بازداشت در محدوده مسئولیت شما وجود ندارد.</td></tr> : rows.map((row, index) => <tr key={row.paper.id}>
          <td>{identifier(page * size + index + 1)}</td>
          <td>{canOpenPaper ? <Link className={styles.paperCodeLink} href={`/execution-papers/${row.paper.id}`}><bdi>{identifier(row.paper.uniqueCode)}</bdi></Link> : <bdi>{identifier(row.paper.uniqueCode)}</bdi>}</td>
          <td><strong>{row.paper.taxpayerName || '—'}</strong><small className={styles.pendingTaxpayerId}>شناسه ملی: <bdi>{identifier(row.paper.taxpayerNationalId)}</bdi></small></td>
          <td><bdi>{money(row.paper.totalDebt)}</bdi></td>
          <td><span className={`${styles.pendingStatus} ${row.status === 'AWAITING_APPROVAL' ? styles.awaitingApproval : styles.readyForIssuance}`}>{row.status === 'AWAITING_APPROVAL' ? 'پیش‌نویس در انتظار تأیید' : 'در انتظار صدور'}</span></td>
          <td><Link className={styles.quickAction} href={row.arrestOrderId ? `/arrest-orders?order=${row.arrestOrderId}` : `/arrest-orders?search=${encodeURIComponent(row.paper.uniqueCode)}`}>{row.arrestOrderId ? 'بررسی و تأیید' : 'صدور دستور'}</Link></td>
        </tr>)}</tbody>
      </table>
    </div>
    {canView && <div className={styles.pendingPagination}><span>{identifier(data?.totalElements)} برگ · صفحه {identifier(page + 1)} از {identifier(Math.max(1, data?.totalPages || 1))}</span><div><button disabled={loading || !page} onClick={() => setPage(value => value - 1)}>قبلی</button><button disabled={loading || !data || page + 1 >= data.totalPages} onClick={() => setPage(value => value + 1)}>بعدی</button></div></div>}
  </section>;
}
