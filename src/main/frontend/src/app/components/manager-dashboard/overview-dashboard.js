'use client';

import dynamic from 'next/dynamic';
import Link from 'next/link';
import { useEffect, useRef, useState, useSyncExternalStore } from 'react';
import { apiJson } from '../../lib/api';
import { formatDashboardNumber as number } from '../../lib/manager-dashboard';
import { ENFORCEMENT_LABELS, FINANCIAL_LABELS } from '../../lib/dashboard-reporting';
import { identifier, paperDate } from '../../lib/execution-papers';
import { hasArrestPermission } from '../../lib/arrest-orders';
import Icon from './overview-icon';
import { REPORT_COLORS as COLORS } from './overview-colors';
import PendingArrestPapers from './pending-arrest-papers';
import styles from './overview-dashboard.module.css';

const ReportPieChart = dynamic(() => import('./overview-charts').then(module => module.ReportPieChart), { ssr: false, loading: () => <p className={styles.chartEmpty}>در حال بارگذاری نمودار…</p> });
const OfficePieChart = dynamic(() => import('./overview-charts').then(module => module.OfficePieChart), { ssr: false, loading: () => <p className={styles.chartEmpty}>در حال بارگذاری نمودار…</p> });
const subscribeViewport = callback => { window.addEventListener('resize', callback); return () => window.removeEventListener('resize', callback); };
const shortViewport = () => window.innerHeight < 740;
const serverViewport = () => false;

export default function DashboardOverview({ user, data, loading, onRefresh }) {
  const report = data?.overview;
  const [selection, setSelection] = useState(null);
  const [workOpen, setWorkOpen] = useState(false);
  const smallHeight = useSyncExternalStore(subscribeViewport, shortViewport, serverViewport);
  const pageSize = smallHeight ? 3 : 5;
  const canView = Boolean(report?.canViewDetails);
  const canOpen = ['REPORT_VIEW', 'EXECUTION_PAPER_DETAIL_VIEW'].some(permission => hasArrestPermission(user, permission));
  const select = canView ? (group, key, label) => setSelection({ group, key, label }) : null;
  const balances = report?.balances;
  const financialItems = ['liability', 'penalties'].map((key, index) => ({ key, label: FINANCIAL_LABELS[key], value: balances?.[key], color: COLORS[index] }));
  const adjustmentItems = ['remissions', 'credits'].map((key, index) => ({ key, label: FINANCIAL_LABELS[key], value: balances?.[key], color: index ? '#8b68d5' : '#10a38f' }));
  const cards = [
    { key: 'taxpayers', label: 'مؤدیان دارای برگ اجرا', value: report?.taxpayers, unit: 'مؤدی یکتا', icon: 'user', tone: 'amber', group: 'all', filter: 'TAXPAYERS' },
    { key: 'papers', label: 'تعداد برگ‌های اجرا', value: report?.papers, unit: 'برگ اجرا', icon: 'document', tone: 'violet', group: 'all', filter: 'PAPERS' },
    { key: 'remaining', label: 'مانده بدهی', value: balances?.remaining, unit: 'ریال', icon: 'receipt', tone: 'blue', group: 'finance', filter: 'remaining' },
    { key: 'paid', label: 'پرداخت‌های ثبت‌شده', value: balances?.paid, unit: 'ریال', icon: 'check', tone: 'teal', group: 'finance', filter: 'paid' },
  ];
  const stateItems = (report?.states || []).map((item, index) => ({ ...item, value: item.count, color: COLORS[index % COLORS.length] })).filter(item => item.count > 0);
  const notificationItems = (report?.notifications || []).map((item, index) => ({ ...item, value: item.count, color: index ? '#efa52d' : '#10a38f' }));
  const issuedCount = report?.enforcement?.find(item => item.key === 'ISSUED')?.count;
  const hasEnforcementCounts = Number.isSafeInteger(report?.papers) && Number.isSafeInteger(issuedCount) && issuedCount >= 0 && issuedCount <= report.papers;
  const enforcementItems = [
    { key: 'ISSUED', label: ENFORCEMENT_LABELS.ISSUED, value: hasEnforcementCounts ? issuedCount : null, color: '#8b68d5' },
    { key: 'NOT_ISSUED', label: ENFORCEMENT_LABELS.NOT_ISSUED, value: hasEnforcementCounts ? report.papers - issuedCount : null, color: '#2779dc' },
  ];
  const paymentItems = ['paid', 'remaining'].map((key, index) => ({ key, label: FINANCIAL_LABELS[key], value: balances?.[key], color: index ? '#abc4e2' : '#10a38f' }));
  return <div className={styles.overview} aria-busy={loading} data-dashboard-overview>
    <div className={styles.intro}><div><span className={styles.liveDot} /><span>{report?.scopeLabel || 'در حال دریافت محدوده گزارش…'}</span><span className={styles.scopeDot} /><span>آخرین وضعیت مالی ذخیره‌شده</span></div><button className={styles.refresh} onClick={onRefresh} disabled={loading}><Icon name="refresh" />{loading ? 'در حال دریافت…' : 'بروزرسانی'}</button></div>
    <section className={styles.metrics} aria-label="آمار کلی">
      {cards.map(card => <button key={card.key} className={`${styles.metric} ${styles[card.tone]}`} data-money={card.unit === 'ریال'} disabled={!select || loading} onClick={() => select?.(card.group, card.filter, card.label)} aria-label={`${card.label}: ${loading ? 'در حال دریافت' : number(card.value)}؛ مشاهده برگ‌های مرتبط`} title={`${number(card.value)} ${card.unit}`}>
        <span className={styles.metricLabel}><span className={styles.metricIcon}><Icon name={card.icon} /></span><span>{card.label}</span></span><span className={styles.metricValue}><strong><bdi dir="ltr">{loading ? '…' : number(card.value)}</bdi></strong><small>{card.unit}</small></span>
      </button>)}
    </section>
    {!report ? <div className={styles.placeholder} role={data?.overviewError ? 'alert' : 'status'}>{data?.overviewError ? <><span>دریافت گزارش داشبورد ناموفق بود.</span><button className={styles.refresh} onClick={onRefresh}>تلاش مجدد</button></> : 'در حال آماده‌سازی گزارش‌ها…'}</div> : <>
      <div className={styles.financialGrid}>
        <Panel title="ترکیب اصل بدهی و جرایم" icon="chart" id="composition">
          <div className={styles.composition}><ReportPieChart items={financialItems} money onSelect={select ? item => select('finance', item.key, item.label) : null} label="ترکیب اصل مالیات و عوارض و جرایم" /><div className={styles.legend}>
            {financialItems.map(item => <LegendItem key={item.key} item={item} money onClick={select ? () => select('finance', item.key, item.label) : null} />)}
          </div></div>
        </Panel>
        <Panel title="بخشودگی و اعتبارات" icon="shield" id="adjustments">
          <div className={styles.adjustmentPieLayout}><ReportPieChart items={adjustmentItems} money onSelect={select ? item => select('finance', item.key, item.label) : null} label="سهم بخشودگی و اعتبارات از کاهش بدهی" /><div className={styles.legend}>
            {adjustmentItems.map(item => <LegendItem key={item.key} item={item} money onClick={select ? () => select('finance', item.key, item.label) : null} />)}
          </div></div>
        </Panel>
        <Panel title="وضعیت پرداخت و مانده بدهی" icon="receipt" id="payment">
          <div className={styles.paymentOverview}>
            <ReportPieChart items={paymentItems} onSelect={select ? item => select('finance', item.key, item.label) : null} money label="سهم پرداخت و مانده از مجموع این دو مبلغ" />
            <div className={styles.paymentFacts}>{paymentItems.map(item => <LegendItem key={item.key} item={item} money onClick={select ? () => select('finance', item.key, item.label) : null} />)}</div>
          </div>
        </Panel>
      </div>
      <div className={styles.analysisGrid}>
        <Panel title="وضعیت برگ‌های اجرا" icon="document" id="status">
          <div className={styles.statusPieLayout}><ReportPieChart items={stateItems} summaryCount={report.papers} secondaryItems={notificationItems} onSecondarySelect={select ? item => select('notification', item.key, item.label) : null} onSelect={select ? item => select('state', item.key, item.label) : null} label="توزیع مراحل برگ اجرا و وضعیت ابلاغ در حلقه بیرونی" />
          <div className={styles.stateLegend}>{stateItems.length ? stateItems.map(item => <LegendItem key={item.key} item={item} onClick={select ? () => select('state', item.key, item.label) : null} />) : <p className={styles.note}>برگ اجرایی در این محدوده وجود ندارد.</p>}</div>
          </div>
          <div className={styles.notificationSummary}>{notificationItems.map(item => <button key={item.key} disabled={!select} onClick={() => select?.('notification', item.key, item.label)}><span className={styles.dot} style={{ background: item.color }} /><span>{item.label}</span><strong>{number(item.count)}</strong></button>)}</div>
        </Panel>
        <Panel title="مقایسه عملکرد واحدها" icon="reports" id="offices">
          <OfficePieChart items={report.offices} onSelect={select ? item => select('office', item.key, item.label) : null} />
        </Panel>
        <Panel title="مسیر اقدامات اجرایی" icon="lock" id="enforcement">
          <div className={styles.enforcementPieLayout}><ReportPieChart items={enforcementItems} summaryCount={report.papers} onSelect={select ? item => select('enforcement', item.key, item.label) : null} label="برگ‌های دارای دستور بازداشت صادرشده و بدون دستور صادرشده" /><div className={styles.enforcementLegend}>
            {enforcementItems.map(item => <LegendItem key={item.key} item={item} onClick={select ? () => select('enforcement', item.key, item.label) : null} />)}
          </div></div>
          <div className={styles.unavailableStages}>{report.enforcement.filter(stage => !stage.available).map(stage => <button key={stage.key} disabled><span>{ENFORCEMENT_LABELS[stage.key] || stage.label}</span><small>اطلاعات در دسترس نیست</small></button>)}</div>
        </Panel>
      </div>
    </>}
    <footer className={styles.overviewFooter}><span className={styles.hint} title="هر بدهی قطعی در جمع مبالغ یک‌بار محاسبه می‌شود؛ تعداد برگ‌های اجرا مستقل است."><Icon name="info" />{canView ? 'انتخاب نمودار برای مشاهده جزئیات' : 'نمایش جزئیات به مجوز مشاهده فهرست برگ‌های اجرا نیاز دارد.'}</span><button className={styles.workButton} onClick={() => setWorkOpen(true)}><span>میزکار شخصی</span><strong>{number(data?.stats?.myActionsAwaitingCompletion)}</strong><small>در انتظار</small><span>←</span></button></footer>
    {selection && <Modal label="dashboard-details-heading" onClose={() => setSelection(null)}><Drilldown key={`${selection.group}:${selection.key}:${pageSize}`} selection={selection} refreshKey={data} size={pageSize} canOpen={canOpen} onClose={() => setSelection(null)} /></Modal>}
    {workOpen && <Modal label="dashboard-work-heading" onClose={() => setWorkOpen(false)} work><header className={styles.detailsHeading}><h2 id="dashboard-work-heading">میزکار شخصی</h2><button className={styles.close} onClick={() => setWorkOpen(false)}>بستن میزکار ×</button></header><div className={styles.workFacts}><span>اقدام‌های انجام‌شده: <strong>{number(data?.stats?.myActionsCompleted)}</strong></span><span>عملکرد دستور بازداشت: <strong>{data?.performance ? `${number(data.performance.percent)}٪` : '—'}</strong></span></div>{data?.statsError && <p className={styles.error}>دریافت آمار میزکار شخصی ناموفق بود.</p>}<PendingArrestPapers user={user} refreshKey={data} size={pageSize} /></Modal>}
  </div>;
}

function Panel({ title, icon, id, children }) {
  return <section className={styles.panel} data-report-panel={id} aria-labelledby={`dashboard-panel-${id}`}><header className={styles.panelHeading}><span className={styles.panelIcon}><Icon name={icon} /></span><h2 id={`dashboard-panel-${id}`}>{title}</h2></header>{children}</section>;
}

function LegendItem({ item, money = false, onClick }) {
  return <button className={styles.legendItem} disabled={!onClick} onClick={onClick} data-money={money} title={`${item.label} · ${number(item.value)} ${money ? 'ریال' : 'برگ'}`}><span className={styles.dot} style={{ background: item.color }} /><span>{item.label}</span><strong><bdi dir="ltr">{number(item.value)}</bdi><small>{money ? 'ریال' : 'برگ'}</small></strong></button>;
}

function Modal({ label, onClose, work = false, children }) {
  const dialog = useRef(null);
  useEffect(() => {
    const element = dialog.current;
    const overflow = document.body.style.overflow;
    element.showModal();
    document.body.style.overflow = 'hidden';
    return () => { element.close(); document.body.style.overflow = overflow; };
  }, []);
  return <dialog ref={dialog} className={`${styles.reportDialog} ${work ? styles.workDialog : ''}`} aria-labelledby={label} onCancel={event => { event.preventDefault(); onClose(); }} onClick={event => { if (event.target === event.currentTarget) onClose(); }}>{children}</dialog>;
}

function Drilldown({ selection, refreshKey, size, canOpen, onClose }) {
  const [page, setPage] = useState(0);
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState(null);
  const path = `/api/v1/dashboard/details?${new URLSearchParams({ group: selection.group, key: selection.key, page: String(page), size: String(size) })}`;
  useEffect(() => {
    const controller = new AbortController();
    apiJson(path, { signal: controller.signal }).then(data => {
      if (controller.signal.aborted) return;
      if (page > 0 && page >= data.totalPages) { setPage(Math.max(0, data.totalPages - 1)); return; }
      setResult({ path, refreshKey, attempt, data });
    }).catch(error => { if (!controller.signal.aborted) setResult({ path, refreshKey, attempt, error: error.message }); });
    return () => controller.abort();
  }, [path, page, refreshKey, attempt]);
  const current = result?.path === path && result?.refreshKey === refreshKey && result?.attempt === attempt ? result : null;
  const data = current?.data;
  const financial = selection.group === 'finance';
  return <section className={styles.panel} aria-labelledby="dashboard-details-heading" aria-busy={!current}>
    <header className={styles.detailsHeading}><div><h2 id="dashboard-details-heading">برگ‌های مرتبط · {selection.label}</h2><p>{data ? `${number(data.totalElements)} برگ · مجموع ${financial ? selection.label : 'مانده بدهی'}: ${number(data.amount)} ریال` : 'در حال دریافت جزئیات…'}</p></div><button className={styles.close} onClick={onClose}>بستن جزئیات ×</button></header>
    {current?.error && <div className={styles.error} role="alert"><span>دریافت جزئیات ناموفق بود.</span><button onClick={() => setAttempt(value => value + 1)}>تلاش مجدد</button></div>}
    {financial && <p className={styles.note}>ستون «سهم در جمع» از تکرار مبلغ بدهی مشترک بین چند برگ اجرا جلوگیری می‌کند.</p>}
    <div className={styles.tableScroll} tabIndex={0} role="region" aria-label={`جزئیات ${selection.label}`}><table><thead><tr>{['شماره برگ اجرا', 'مشخصات مؤدی', 'وضعیت', 'واحد مالیاتی', 'تاریخ صدور', 'مانده بدهی (ریال)', ...(financial ? ['سهم در جمع (ریال)'] : [])].map(label => <th key={label} scope="col">{label}</th>)}</tr></thead><tbody>
      {!current || current.error || !data?.content.length ? <tr><td colSpan={financial ? 7 : 6} className={styles.tableEmpty}>{!current ? 'در حال دریافت…' : current.error ? 'فهرست دریافت نشد.' : 'برگ اجرایی در این گروه وجود ندارد.'}</td></tr> : data.content.map(row => <tr key={row.id}>
        <td>{canOpen ? <Link href={`/execution-papers/${row.id}`}>{identifier(row.uniqueCode)}</Link> : identifier(row.uniqueCode)}</td>
        <td><strong>{row.taxpayerName || '—'}</strong><small>شناسه ملی: <bdi>{identifier(row.nationalId)}</bdi></small><small>شناسه مؤدی: <bdi>{identifier(row.taxpayerCode)}</bdi></small></td>
        <td>{row.stateLabel || '—'}</td><td>{row.officeName || '—'}</td><td>{paperDate(row.issuanceDate)}</td><td><bdi>{number(row.remaining)}</bdi></td>{financial && <td><bdi>{number(row.contribution)}</bdi></td>}
      </tr>)}
    </tbody></table></div>
    <footer className={styles.pagination}><span>صفحه {number(page + 1)} از {number(Math.max(1, data?.totalPages || 1))}</span><div><button disabled={!current || !page} onClick={() => setPage(value => value - 1)}>قبلی</button><button disabled={!data || page + 1 >= data.totalPages} onClick={() => setPage(value => value + 1)}>بعدی</button></div></footer>
  </section>;
}
