'use client';
import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { apiJson } from '../../lib/api';
import { isAdminUser } from '../../lib/navigation';
import { DEBT_SETTLEMENT_ENTRIES } from '../../lib/demo-navigation';
import { identifier, money, normalizeDigits } from '../../lib/execution-papers';
import { arrestDate } from '../../lib/arrest-orders';
import { DEBT_GROUPS, isDebtAmount, parseDebtDetails } from '../../lib/debt-details';
import FigmaShell from '../manager-dashboard/figma-shell';
import { DebtHistory } from '../execution-papers/paper-detail-page';
import PersianDateFilter from '../execution-papers/persian-date-filter';
import Icon from '../business-feature-icon';
import styles from './debt-statement.module.css';

function useResource(path, enabled = true, revision = 0) {
  const [result, setResult] = useState({ key: '', data: null, error: '' });
  const key = `${path}:${revision}`;
  useEffect(() => {
    if (!enabled) return;
    const controller = new AbortController();
    apiJson(path, { signal: controller.signal })
      .then(data => { if (!controller.signal.aborted) setResult({ key, data, error: '' }); })
      .catch(error => { if (!controller.signal.aborted) setResult({ key, data: null, error: error.message }); });
    return () => controller.abort();
  }, [path, enabled, key]);
  return !enabled ? { data: null, error: '', loading: false } : result.key === key ? { ...result, loading: false } : { data: null, error: '', loading: true };
}

export default function DebtStatementWorkspace() {
  const router = useRouter();
  const params = useSearchParams();
  const paperId = params.get('paper') || '';
  const [searchText, setSearchText] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [revision, setRevision] = useState(0);
  const user = useResource('/api/v1/auth/me', true, revision);
  const allowed = permission => isAdminUser(user.data) || [...(user.data?.allowedActions || []), ...(user.data?.localBusinessPermissions || [])].includes(permission);
  const canList = ['REPORT_VIEW', 'EXECUTION_PAPER_LIST_VIEW'].some(allowed);
  const canView = ['REPORT_VIEW', 'EXECUTION_PAPER_DETAIL_VIEW'].some(allowed);
  const list = useResource(`/api/v1/execution-papers?${new URLSearchParams({ page, size: 20, search })}`, canList, revision);
  const report = useResource(`/api/v1/execution-papers/${encodeURIComponent(paperId)}/financial-statement`, canView && !!paperId, revision);
  const papers = list.data?.content || [];
  const selected = report.data?.detail?.summary;
  const refresh = () => setRevision(value => value + 1);
  useEffect(() => {
    const normalized = normalizeDigits(searchText.trim());
    if (normalized === search) return;
    const timer = setTimeout(() => { setSearch(normalized); setPage(0); }, 300);
    return () => clearTimeout(timer);
  }, [searchText, search]);
  const choosePaper = id => router.push(id ? `/debt-statement?paper=${encodeURIComponent(id)}` : '/debt-statement', { scroll: false });

  return <FigmaShell user={user.data} active="debt-settlement" toolbar={<nav className={styles.breadcrumbs} aria-label="مسیر صفحه"><Link href="/">سامانه جامع وصول و اجرا</Link><span>/</span><span>تعیین تکلیف بدهی</span><span>/</span><span>صورت وضعیت بدهی</span></nav>}>
    <section className={styles.workspace} aria-busy={user.loading || report.loading}>
      <header className={styles.heading}><div className={styles.title}><span className={styles.heroIcon}><Icon name="statement" size={28} /></span><div><h1>صورت وضعیت بدهی</h1><p>گزارش بدهی، پرداختی و سوابق مالی برگ اجرا</p></div></div><button className={styles.secondary} onClick={refresh} disabled={user.loading || list.loading || report.loading}><Icon name="refresh" size={17} />بروزرسانی</button></header>
      <div className={styles.services} aria-label="خدمات تعیین تکلیف بدهی">{DEBT_SETTLEMENT_ENTRIES.map(entry => entry.href ? <Link key={entry.key} href={paperId ? `${entry.href}?paper=${encodeURIComponent(paperId)}` : entry.href} className={`${styles.service} ${styles.activeService}`} aria-current="page"><Icon name={entry.icon} /><span>{entry.label}</span></Link> : <div key={entry.key} className={`${styles.service} ${styles.disabledService}`} aria-disabled="true"><Icon name={entry.icon} /><span>{entry.label}</span><small>غیرفعال</small></div>)}</div>
      {user.error ? <ErrorBox message={user.error} retry={refresh} /> : user.loading ? <Loading /> : <>
        <section className={styles.picker} aria-labelledby="statement-picker-title">
          <div className={styles.pickerHeading}><h2 id="statement-picker-title">انتخاب برگ اجرا</h2><span>{list.loading ? 'در حال دریافت…' : `${identifier(list.data?.totalElements ?? '—')} برگ در محدوده دسترسی`}</span></div>
          <div className={styles.pickerFields}><label className={styles.search}><Icon name="search" size={19} /><input aria-label="جستجوی برگ اجرا برای صورت وضعیت" placeholder="شماره برگ، نام مؤدی یا شناسه ملی…" disabled={!canList} value={searchText} onChange={event => setSearchText(event.target.value)} /></label>
            <label className={styles.paperSelect}><span className={styles.srOnly}>برگ اجرای گزارش</span><select aria-label="برگ اجرای گزارش" disabled={!canList || list.loading} value={paperId} onChange={event => choosePaper(event.target.value)}><option value="">انتخاب برگ اجرا…</option>{paperId && !papers.some(paper => String(paper.id) === paperId) && <option value={paperId}>{selected ? `${identifier(selected.uniqueCode)} — ${selected.taxpayerName}` : `برگ ${identifier(paperId)}`}</option>}{papers.map(paper => <option key={paper.id} value={paper.id}>{identifier(paper.uniqueCode)} — {paper.taxpayerName || 'نام ثبت نشده'}</option>)}</select></label>
          </div>
          {list.error && <ErrorBox message={list.error} retry={refresh} />}
          {!canList && <p className={styles.note}>دسترسی جستجوی برگ‌های اجرا برای این حساب فعال نیست.</p>}
          {canList && !list.loading && !list.error && !papers.length && <p className={styles.note}>برگی با این مشخصات در محدوده دسترسی شما یافت نشد.</p>}
          {canList && <footer className={styles.pickerFooter}><span>صفحه {identifier(page + 1)} از {identifier(Math.max(1, list.data?.totalPages || 1))}</span><div><button className={styles.secondary} disabled={!page || list.loading} onClick={() => setPage(value => value - 1)}>قبلی</button><button className={styles.secondary} disabled={list.loading || !list.data || page + 1 >= list.data.totalPages} onClick={() => setPage(value => value + 1)}>بعدی</button></div></footer>}
        </section>
        {!canView ? <div className={styles.empty}><Icon name="guarantee" size={38} /><p>دسترسی مشاهده اطلاعات مالی برگ اجرا برای این حساب فعال نیست.</p></div> : !paperId ? <div className={styles.empty}><Icon name="statement" size={45} /><h2>برگ اجرای گزارش را انتخاب کنید</h2><p>صورت وضعیت بدهی و پرداختی همان برگ در این بخش نمایش داده می‌شود.</p></div> : report.loading ? <Loading /> : report.error ? <ErrorBox message={report.error} retry={refresh} /> : report.data && <StatementReport key={paperId} report={report.data} />}
      </>}
    </section>
  </FigmaShell>;
}

function StatementReport({ report }) {
  const { detail, history } = report;
  const paper = detail.summary;
  const current = detail.debtInformation?.history?.find(row => row.current);
  const metrics = [
    ['principalAmount', 'اصل مالیات و عوارض', 'statement', 'blue'],
    ['penaltyAmount', 'جرائم', 'scales', 'amber'],
    ['paidAmount', 'مجموع پرداختی گزارش‌شده', 'payment', 'green'],
    ['outstandingAmount', 'مانده بدهی', 'history', 'navy'],
  ];
  return <div className={styles.report} aria-label="گزارش مالی برگ اجرا">
    <header className={styles.paperHeading}><div><span className={styles.eyebrow}>صورت وضعیت برگ اجرا</span><h2><bdi>{identifier(paper.uniqueCode)}</bdi><span>{paper.taxpayerName || 'نام ثبت نشده'}</span></h2><p>شناسه ملی: {identifier(paper.taxpayerNationalId)}<span>سال: {identifier(paper.taxYear)} · دوره: {identifier(paper.taxPeriod)}</span></p></div><Link className={styles.secondary} href={`/execution-papers/${paper.id}`}>مشاهده جزئیات برگ اجرا ←</Link></header>
    <div className={styles.metrics}>{metrics.map(([key, label, icon, tone]) => <article key={key} className={`${styles.metric} ${styles[tone]}`}><span className={styles.metricIcon}><Icon name={icon} size={24} /></span><div><h3>{label}</h3><strong><bdi>{money(current?.[key] ?? (key === 'outstandingAmount' ? paper.totalDebt : null))}</bdi><small>ریال</small></strong></div></article>)}</div>
    <section className={styles.card} aria-labelledby="statement-debt-title"><header className={styles.cardHeading}><div><span className={styles.eyebrow}>اطلاعات مالی برگ اجرا</span><h2 id="statement-debt-title"><Icon name="statement" size={20} />وضعیت بدهی</h2></div><span>مبالغ به ریال</span></header><div className={styles.debtBody}><DebtHistory data={detail} hrefFor={id => `/execution-papers/${id}`} /></div></section>
    <Payments current={current} paperType={detail.finalTaxDebtPaper?.paperType} />
    <FinancialHistory history={history} />
  </div>;
}

function Payments({ current, paperType }) {
  const payments = useMemo(() => parseDebtDetails(current?.financialDetails?.totalPaymentsDetails, paperType), [current?.financialDetails?.totalPaymentsDetails, paperType]);
  return <section className={styles.card} aria-labelledby="statement-payments-title"><header className={styles.cardHeading}><div><span className={styles.eyebrow}>بر اساس منبع اطلاعات مالی</span><h2 id="statement-payments-title"><Icon name="payment" size={21} />پرداختی</h2></div><div className={styles.paidTotal}><span>جمع پرداختی گزارش‌شده</span><strong><bdi>{money(current?.paidAmount)}</bdi> <small>ریال</small></strong></div></header>
    {payments.error ? <p className={styles.note} role="alert">تفصیل پرداخت ذخیره‌شده قابل خواندن نیست.</p> : !payments.rows.length ? <p className={styles.note}>تفصیل پرداختی برای این برگ در منبع ثبت نشده است.</p> : <div className={styles.tableScroll} role="region" aria-label="تفصیل پرداختی برگ اجرا" tabIndex={0}><table className={styles.paymentTable}><thead><tr><th scope="col">شرح پرداخت</th><th scope="col">مبلغ (ریال)</th></tr></thead><tbody>{payments.rows.map((row, index) => <tr key={index}><td>{row.label}{!row.reported && <small>در منبع گزارش نشده</small>}</td><td><bdi>{row.reported ? isDebtAmount(row.amount) ? money(row.amount) : identifier(row.amount) : '—'}</bdi></td></tr>)}</tbody></table></div>}
  </section>;
}

function FinancialHistory({ history }) {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);
  const selected = history.filter(row => (!from || row.receivedAt?.slice(0, 10) >= from) && (!to || row.receivedAt?.slice(0, 10) <= to)).toReversed();
  const pages = Math.max(1, Math.ceil(selected.length / 10));
  const rows = selected.slice(page * 10, page * 10 + 10);
  return <section className={styles.card} aria-labelledby="statement-history-title"><header className={styles.cardHeading}><div><span className={styles.eyebrow}>تاریخچه مرتبط با همین برگ اجرا</span><h2 id="statement-history-title"><Icon name="history" size={20} />سوابق مالی ثبت‌شده</h2></div><span>{identifier(selected.length)} سابقه</span></header>
    <div className={styles.historyFilters}><PersianDateFilter label="ثبت از تاریخ" value={from} onChange={value => { setFrom(value); setPage(0); }} /><PersianDateFilter label="تا تاریخ" min={from} value={to} onChange={value => { setTo(value); setPage(0); }} />{(from || to) && <button className={styles.clear} onClick={() => { setFrom(''); setTo(''); setPage(0); }}>پاک کردن فیلتر</button>}</div>
    <p className={styles.historyNote}>مبالغ هر ردیف، وضعیت تجمیعی در زمان ثبت هستند.</p>
    {!rows.length ? <p className={styles.note}>{from || to ? 'سابقه مالی در این بازه ثبت نشده است.' : 'سابقه مالی برای این برگ ذخیره نشده است.'}</p> : <div className={styles.tableScroll} role="region" aria-label="سوابق مالی برگ اجرا" tabIndex={0}><table className={styles.historyTable}><caption className={styles.srOnly}>سوابق ذخیره‌شده اطلاعات مالی؛ مبالغ به ریال</caption><thead><tr><th scope="col">تاریخ ثبت</th>{DEBT_GROUPS.map(group => <th scope="col" key={group.key}>{group.label} (ریال)</th>)}</tr></thead><tbody>{rows.map(row => <tr key={row.id}><td><time>{arrestDate(row.receivedAt)}</time></td>{DEBT_GROUPS.map(group => <td key={group.key}><bdi>{money(row[group.key])}</bdi></td>)}</tr>)}</tbody></table></div>}
    <footer className={styles.pickerFooter}><span>صفحه {identifier(page + 1)} از {identifier(pages)}</span><div><button className={styles.secondary} disabled={!page} onClick={() => setPage(value => value - 1)}>قبلی</button><button className={styles.secondary} disabled={page + 1 >= pages} onClick={() => setPage(value => value + 1)}>بعدی</button></div></footer>
  </section>;
}

function ErrorBox({ message, retry }) { return <div className={styles.error} role="alert"><p>{message}</p><button className={styles.secondary} onClick={retry}>تلاش مجدد</button></div>; }
function Loading() { return <div className={styles.loading} role="status"><span className={styles.srOnly}>در حال دریافت اطلاعات…</span>{[1, 2, 3].map(item => <div key={item} />)}</div>; }
