'use client';
import { useRef, useState } from 'react';
import Link from 'next/link';
import { identifier, money, paperDate, compareExact, workflowSteps } from '../../lib/execution-papers';
import { SanimDocumentSection, RelatedPersonsSection, TraceSection } from './paper-document';
import PaperTable from './paper-table';
import { ViewIcon } from './paper-view-tools';
import Icon from './paper-icon';
import styles from './paper-details.module.css';
import { DEBT_GROUPS, parseDebtDetails, isDebtAmount } from '../../lib/debt-details';
import PaperArrestAttachment from '../arrest-orders/paper-arrest-attachment';

const TABS = ['اطلاعات بدهی', 'اقدامات انجام شده', 'اطلاعات مؤدی', 'اطلاعات پرونده'];

export default function PaperDetailPage({ resource, trace, related, external, canTrace, canViewArrest, onPrint, onRefreshRelated, refreshingRelated, taskHref, hrefFor, embedded = false, onClose, paymentDeadline }) {
  const [tab, setTab] = useState(embedded ? 1 : 0);
  const [stage, setStage] = useState(embedded ? null : 'issue');
  const tabs = useRef([]);
  const documentDialog = useRef(null);
  const historyDialog = useRef(null);
  const data = resource.data;
  const steps = workflowSteps(data, trace.data);
  const selectedStage = stage || steps.find(step => step.status === 'active')?.key || steps.findLast(step => step.status === 'done')?.key || 'issue';
  const openDocument = () => documentDialog.current.showModal();
  const openHistory = () => historyDialog.current.showModal();
  const tabItems = embedded ? [{ key: 1, label: 'فرآیند اجرایی' }, { key: 2, label: 'اطلاعات هویتی' }, { key: 0, label: 'اطلاعات مالیاتی' }, { key: 4, label: 'اقدامات انجام شده' }] : TABS.map((label, key) => ({ key, label }));
  const idPrefix = embedded ? 'paper-summary' : 'paper';
  return <section className={`${styles.pageCard} ${embedded ? styles.embedded : ''}`} aria-labelledby={`${idPrefix}-heading`} aria-busy={resource.loading}>
    {embedded ? <header className={styles.previewHeader}><h2 id={`${idPrefix}-heading`}><Icon name="document" />جزئیات برگ اجرا — <bdi>{identifier(data?.summary?.uniqueCode)}</bdi></h2><div>{data && <Link className={styles.textAction} href={hrefFor(data.summary.id)}>مشاهده جزئیات کامل</Link>}<button className={styles.previewClose} onClick={onClose} aria-label="بستن خلاصه برگ اجرا">×</button></div></header>
      : <h1 className={styles.srOnly} id={`${idPrefix}-heading`}>جزئیات برگ اجرا {identifier(data?.summary?.uniqueCode)}</h1>}
    {resource.loading ? <p className={styles.message} role="status">در حال دریافت جزئیات برگ اجرا…</p>
      : resource.error ? <Retry message={resource.error} onRetry={resource.retry} /> : !data ? <p className={styles.message}>اطلاعات این برگ اجرا در دسترس نیست.</p> : <>
        {!embedded && <PaperTable papers={[data.summary]} summary />}
        <dl className={styles.paperSource}><dt><Icon name="document" />سامانه صادرکننده برگ اجرا:</dt><dd>{data.executionPaper?.issuerSystemLabel || data.executionPaper?.issuerSystem || 'ثبت نشده'}</dd></dl>
        {!embedded && canViewArrest && <PaperArrestAttachment paperId={data.summary.id} />}
        <div className={embedded ? styles.previewLayout : undefined}>
        {embedded && <DebtSummary data={data} paymentDeadline={paymentDeadline} />}
        <div className={embedded ? styles.previewContent : undefined}>
        <div className={styles.tabs} role="tablist" aria-label="بخش‌های جزئیات برگ اجرا">{tabItems.map(({ label, key }, index) => <button role="tab" id={`${idPrefix}-tab-${key}`} key={key} ref={element => { tabs.current[index] = element; }}
          aria-selected={tab === key} aria-controls={`${idPrefix}-panel-${key}`} tabIndex={tab === key ? 0 : -1} onClick={() => setTab(key)} onKeyDown={event => {
            const next = event.key === 'ArrowLeft' ? (index + 1) % 4 : event.key === 'ArrowRight' ? (index + 3) % 4 : event.key === 'Home' ? 0 : event.key === 'End' ? 3 : null;
            if (next != null) { event.preventDefault(); setTab(tabItems[next].key); tabs.current[next]?.focus(); }
          }}>{label}</button>)}</div>
        <div id={`${idPrefix}-panel-${tab}`} role="tabpanel" aria-labelledby={`${idPrefix}-tab-${tab}`} tabIndex={0} className={styles.panel}>
          {tab === 0 && <><div className={styles.statementLink}><Link href={`/debt-statement?paper=${encodeURIComponent(data.summary.id)}`}>صورت وضعیت بدهی و پرداخت‌ها ←</Link></div><DebtHistory data={data} hrefFor={hrefFor} /></>}
          {tab === 1 && <>
            <div className={styles.workflowScroll} role="region" aria-label="مراحل فرآیند اجرایی" tabIndex={0}><ol className={styles.workflow}>{steps.map((step, index) => <li key={step.key}>
              <button className={styles.step} aria-pressed={selectedStage === step.key} onClick={() => setStage(step.key)}>
                <span className={styles.stepIcon}><Icon name={step.status === 'done' ? 'doneOuter' : step.status === 'active' ? 'activeOuter' : 'pendingOuter'} />
                  <span className={styles.stepInner}><Icon name={step.status === 'done' ? 'doneInner' : step.status === 'active' ? 'activeInner' : 'pendingInner'} /></span>
                  <span className={styles.stepSymbol}><Icon name={step.status === 'done' ? 'done' : step.icon} /></span></span>
                <span className={styles.stepName}>{step.label}</span><span className={`${styles.stepStatus} ${styles[step.status] || ''}`}><Icon name={step.status === 'done' ? 'doneDot' : step.status === 'active' ? 'activeDot' : 'pendingDot'} />{step.status === 'done' ? 'انجام شده' : step.status === 'active' ? 'در حال انجام' : 'ثبت نشده'}</span>
                {embedded && <time className={styles.stepDate}>{paperDate(step.date)}</time>}
              </button>{index < steps.length - 1 && <span className={styles.stepArrow}><Icon name="stepArrow" /></span>}
            </li>)}</ol></div>
            <StageDetails data={data} step={steps.find(step => step.key === selectedStage) || steps[0]} onView={openDocument} onHistory={openHistory} onPrint={onPrint} canTrace={canTrace} taskHref={taskHref} />
            {trace.loading && <p className={styles.muted} role="status">در حال دریافت سوابق مراحل…</p>}
            {trace.error && <Retry message="دریافت تاریخچه ناموفق بود؛ وضعیت مراحل ممکن است کامل نباشد." onRetry={trace.retry} />}
            {!canTrace && <p className={styles.muted}>دسترسی مشاهده سوابق فرایند برای این حساب فعال نیست.</p>}
          </>}
          {tab === 2 && <TaxpayerInfo data={data} related={related} refreshing={refreshingRelated} onRefresh={onRefreshRelated} />}
          {tab === 3 && <FileInfo data={data} onView={openDocument} onPrint={onPrint} />}
          {tab === 4 && (canTrace ? <><TraceSection trace={trace.data} loading={trace.loading} error={trace.error} externalRequests={external.data || []} externalRequestsLoading={external.loading} externalRequestsError={external.error} />{(trace.error || external.error) && <Retry message="دریافت سوابق ناموفق بود." onRetry={() => { trace.retry(); external.retry(); }} />}</> : <p className={styles.muted}>دسترسی مشاهده سوابق فرایند برای این حساب فعال نیست.</p>)}
        </div>
        </div></div>
        <DetailDialog dialogRef={documentDialog} title="سند کامل برگ اجرا" closeLabel="بستن سند کامل">{onPrint && <div className={styles.documentTools}><button className={styles.outlineAction} onClick={onPrint}><ViewIcon name="print" />چاپ برگ اجرا</button></div>}<SanimDocumentSection data={data} /></DetailDialog>
        <DetailDialog dialogRef={historyDialog} title="سوابق و استعلام‌های برگ اجرا" closeLabel="بستن سوابق">
          {canTrace ? <><TraceSection trace={trace.data} loading={trace.loading} error={trace.error} externalRequests={external.data || []} externalRequestsLoading={external.loading} externalRequestsError={external.error} />
            {(trace.error || external.error) && <button onClick={() => { trace.retry(); external.retry(); }}>تلاش مجدد</button>}</> : <p>شما مجوز مشاهده تاریخچه این برگ اجرا را ندارید.</p>}
        </DetailDialog>
      </>}
  </section>;
}

function DebtSummary({ data, paymentDeadline }) {
  const current = data.debtInformation?.history?.find(row => row.current);
  return <aside className={styles.previewDebt} aria-label="خلاصه اطلاعات بدهی"><h3>اطلاعات بدهی</h3><dl>
    <div><dt>مبلغ اصل بدهی</dt><dd><bdi>{money(current?.principalAmount)}</bdi> <span>ریال</span></dd></div>
    <div><dt>مبلغ جرایم و خسارات</dt><dd><bdi>{money(current?.penaltyAmount)}</bdi> <span>ریال</span></dd></div>
    <div className={styles.previewTotal}><dt>جمع بدهی باقیمانده</dt><dd><bdi>{money(data.debtInformation?.outstandingDebtAmount ?? data.summary?.totalDebt)}</bdi> <span>ریال</span></dd></div>
  </dl><dl className={styles.previewDates}><div><dt><Icon name="timer" />تاریخ صدور</dt><dd><bdi>{paperDate(data.executionPaper?.issuanceDate)}</bdi></dd></div><div><dt><ViewIcon name="alert" />مهلت پرداخت</dt><dd><bdi>{paperDate(data.notification?.paymentDeadline || paymentDeadline)}</bdi></dd></div></dl></aside>;
}

export function DebtHistory({ data, hrefFor }) {
  const otherPapers = data.executionPaper?.otherActivePapers || [];
  const otherDialog = useRef(null);
  const breakdownDialog = useRef(null);
  const [breakdown, setBreakdown] = useState(null);
  const rows = data.debtInformation?.history || [];
  const showBreakdown = (row, group) => {
    setBreakdown({ row, group, ...parseDebtDetails(row.financialDetails?.[group.detail], data.finalTaxDebtPaper?.paperType) });
    breakdownDialog.current.showModal();
  };
  return <>
    <div className={styles.debtScroll} role="region" tabIndex={0} aria-label="جدول تاریخچه بدهی"><table className={styles.debtTable}>
      <caption className={styles.srOnly}>رکورد اولیه و وضعیت جاری بدهی؛ همه مبالغ به ریال است. برای تفصیل، دکمه هر مبلغ را انتخاب کنید.</caption><thead><tr><th scope="col">تاریخ</th>{DEBT_GROUPS.map(group => <th scope="col" key={group.key}>{group.label}{group.key === 'outstandingAmount' ? '(ریال)' : ''}</th>)}</tr></thead>
      <tbody>{rows.length ? rows.map((row, index) => <tr key={`${row.current ? 'current' : 'initial'}-${row.id || index}`}>
        <td><span className={styles.snapshotDate}>{row.current && <span title="وضعیت جاری بدهی" aria-label="وضعیت جاری بدهی"><Icon name="timer" /></span>}<bdi>{paperDate(row.receivedAt)}</bdi></span><small className={styles.snapshotLabel}>{row.current ? 'وضعیت جاری' : 'رکورد اولیه'}</small></td>
        {DEBT_GROUPS.map(group => <td key={group.key} className={group.key === 'outstandingAmount' && index > 0 && compareExact(row.outstandingAmount, rows[0].outstandingAmount) === 1 ? styles.increased : ''}>
          <button className={styles.amountButton} disabled={row.unavailable} onClick={() => showBreakdown(row, group)} aria-label={`جزئیات ${group.label} — ${row.current ? 'وضعیت جاری' : 'رکورد اولیه'}`}><bdi>{money(row[group.key])}</bdi><ViewIcon name="eye" /></button>
        </td>)}
      </tr>) : <tr><td colSpan={7} className={styles.message}>سابقه‌ای از مبالغ بدهی ثبت نشده است.</td></tr>}</tbody>
    </table></div>
    {rows.some(row => row.unavailable) && <p className={styles.aggregateHelp}>رکورد اولیه بدهی ثبت نشده است؛ مبالغ جاری از جدول اطلاعات بدهی نمایش داده می‌شوند.</p>}
    <DetailDialog dialogRef={breakdownDialog} title={breakdown ? `جزئیات ${breakdown.group.label}` : 'جزئیات مبلغ'} closeLabel="بستن جزئیات مبلغ" hideHeading>
      {breakdown && <section className={styles.breakdownCard}><h3><Icon name="receipt" />جزئیات {breakdown.group.label}</h3><p className={styles.breakdownDate}>{breakdown.row.current ? 'وضعیت جاری' : 'رکورد اولیه'} — <bdi>{paperDate(breakdown.row.receivedAt)}</bdi></p>
        <div className={styles.breakdownScroll} role="region" tabIndex={0} aria-label={`جدول جزئیات ${breakdown.group.label}؛ برای مشاهده همه ستون‌ها پیمایش کنید`}><table className={styles.breakdownTable}><caption className={styles.srOnly}>جزئیات {breakdown.group.label} به ریال</caption>
          <thead><tr><th scope="col">{breakdown.group.heading}</th><th scope="col">مبلغ (ریال)</th></tr></thead>
          <tbody>{breakdown.rows.length ? breakdown.rows.map((item, index) => <tr key={index}><td className={/برگشت|تخفیف|تخفيف/.test(item.label) ? styles.increased : ''}>{item.label}{!item.reported && <small className={styles.snapshotLabel}>در منبع گزارش نشده</small>}</td><td><bdi>{item.reported ? isDebtAmount(item.amount) ? money(item.amount) : item.amount || '—' : '—'}</bdi></td></tr>)
            : <tr><td colSpan={2} className={styles.message}>{breakdown.error ? 'جزئیات ثبت‌شده قابل خواندن نیست.' : 'جزئیاتی برای این گروه ثبت نشده است.'}</td></tr>}</tbody>
          <tfoot><tr><th scope="row">{breakdown.group.total}</th><td><bdi>{money(breakdown.row[breakdown.group.key])}</bdi></td></tr></tfoot>
        </table></div>
      </section>}
    </DetailDialog>
    {otherPapers.length > 0 && <aside className={styles.otherPapersAlert} aria-label="برگ‌های اجرای فعال دیگر این مؤدی"><ViewIcon name="alert" /><p>{identifier(otherPapers.length)} برگ اجرای فعال دیگر برای این مؤدی وجود دارد</p><button onClick={() => otherDialog.current.showModal()}>مشاهده</button></aside>}
    <DetailDialog dialogRef={otherDialog} title="برگ‌های اجرای فعال دیگر این مؤدی" closeLabel="بستن برگ‌های مرتبط"><ul className={styles.otherPapersList}>{otherPapers.map(paper => <li key={paper.id}><Link href={hrefFor(paper.id)} onClick={() => otherDialog.current.close()}>برگ اجرا <bdi>{identifier(paper.uniqueCode)}</bdi><ViewIcon name="eye" /></Link></li>)}</ul></DetailDialog>
  </>;
}

function StageDetails({ data, step, onView, onHistory, onPrint, canTrace, taskHref }) {
  const issue = step.key === 'issue';
  const source = data.finalTaxDebtPaper || {};
  const reason = { SELF_DECLARATION: 'خوداظهاری', FINAL_NOTICE: 'برگ بدهی قطعی' }[source.finalizationReason] || source.finalizationReason;
  return <section className={styles.stageCard} aria-label="جزئیات مرحله انتخاب‌شده"><h2><Icon name="receipt" />{issue ? 'جزئیات صدور' : `جزئیات ${step.label}`}</h2>
    <dl className={styles.stageFacts}><Info label={issue ? 'تاریخ صدور:' : 'تاریخ ثبت:'} value={paperDate(step.date)} /><Info label={issue ? 'مبنای صدور:' : 'انجام‌دهنده:'} value={issue ? reason : step.event?.actorUsername} />
      <div className={styles.stageSubject}><dt>موضوع:</dt><dd>{issue ? <>{reason || 'سند مبنا'} به شماره <bdi>{identifier(source.debtPaperUniqueCode || source.uniqueFinalTaxDebtNoticeId)}</bdi> و تاریخ <bdi>{paperDate(source.finalTaxDebtNoticeIssueDate || source.sourceDocument?.issueDateRaw)}</bdi></> : step.event?.label || 'اطلاعاتی برای این مرحله ثبت نشده است.'}</dd></div></dl>
    <div className={styles.actions}>{issue && <><button className={styles.primaryAction} onClick={onView}><ViewIcon name="eye" />مشاهده</button>{onPrint && <button className={styles.outlineAction} onClick={onPrint}><ViewIcon name="print" />چاپ</button>}</>}
      {canTrace && <button className={styles.textAction} onClick={onHistory}>سوابق و استعلام‌ها</button>}{taskHref && <Link className={styles.textAction} href={taskHref}>مشاهده کارتابل</Link>}</div>
  </section>;
}

function TaxpayerInfo({ data, related, refreshing, onRefresh }) {
  const taxpayer = data.taxpayer || {}, source = data.finalTaxDebtPaper?.sourceDocument || {};
  const legal = taxpayer.type === 'LEGAL';
  return <><section className={styles.infoCard} aria-label="اطلاعات مؤدی"><h2>{legal ? 'مؤدی حقوقی' : taxpayer.type === 'NATURAL' ? 'مؤدی حقیقی' : 'اطلاعات مؤدی'}</h2>
    <dl className={styles.infoGrid}>
      <Info label="شناسه مودی:" value={taxpayer.tin || data.summary?.taxpayerCode} /><Info label="شماره شناسایی ملی/شماره ملی:" value={taxpayer.nationalId || taxpayer.nationalCode || data.summary?.taxpayerNationalId} />
      <Info label="نام شخص حقوقی/نام و نام‌خانوادگی:" value={data.summary?.taxpayerName} /><Info label="نام تجاری/شهرت کسبی:" value={taxpayer.tradeName || source.tradeName} />
      <Info label="نوع فعالیت:" value={taxpayer.activityType || source.activityTitle} /><Info label="تلفن:" value={taxpayer.phone || source.phone} />
      <Info label="نشانی اقامتگاه قانونی:" value={taxpayer.address || source.address} wide /><Info label="کد پستی:" value={taxpayer.postalCode || source.postalCode} wide />
      {!legal && <><Info label="نام پدر:" value={taxpayer.fatherName} /><Info label="تاریخ تولد:" value={paperDate(taxpayer.birthDate)} /></>}
    </dl></section>
    {legal && <details className={styles.supplemental}><summary>اطلاعات ثبت و اشخاص مرتبط</summary><dl className={styles.infoGrid}><Info label="شماره ثبت:" value={taxpayer.registrationNumber} /><Info label="تاریخ ثبت:" value={paperDate(taxpayer.registrationDate)} /></dl><RelatedPersonsSection data={related.data} loading={related.loading} refreshing={refreshing} error={related.error} onRefresh={onRefresh} /></details>}
  </>;
}

function FileInfo({ data, onView, onPrint }) {
  const source = data.finalTaxDebtPaper || {}, execution = data.executionPaper || {};
  return <section className={styles.infoCard} aria-label="اطلاعات پرونده"><h2>{data.summary?.taxType || source.paperTypeTitle || 'برگ اجرا'} - <bdi>{identifier(data.summary?.uniqueCode)}</bdi></h2>
    <dl className={styles.infoGrid}><Info label="اداره کل امور مالیاتی:" value={source.generalDirectorateName || execution.generalDirectorateName} /><Info label="اداره امور مالیاتی:" value={source.taxAffairsOfficeName || execution.taxAffairsOfficeName} />
      <Info label="تلفن:" value={source.taxAffairsOfficePhone || execution.taxAffairsOfficePhone} /><Info label="کد پستی:" value={source.taxAffairsOfficePostalCode || execution.taxAffairsOfficePostalCode} /><Info label="نشانی:" value={source.taxAffairsOfficeAddress || execution.taxAffairsOfficeAddress} wide /></dl>
    <div className={styles.fileActions}><button className={styles.textAction} onClick={onView}>مشاهده سند کامل</button><button className={styles.textAction} onClick={onPrint}>چاپ برگ اجرا</button></div>
  </section>;
}

function Info({ label, value, wide }) { return <div className={wide ? styles.wideInfo : ''}><dt>{label}</dt><dd><bdi>{identifier(value)}</bdi></dd></div>; }
function DetailDialog({ dialogRef, title, closeLabel, children, hideHeading = false }) { return <dialog ref={dialogRef} className={styles.dialog} aria-label={title} onClick={event => { if (event.target === event.currentTarget) dialogRef.current.close(); }}><div><form method="dialog"><button className={styles.dialogClose} aria-label={closeLabel}>×</button></form><h2 className={hideHeading ? styles.srOnly : undefined}>{title}</h2>{children}</div></dialog>; }
export function Retry({ message, onRetry }) { return <div className={styles.error} role="alert"><p>{message}</p>{onRetry && <button onClick={onRetry}>تلاش مجدد</button>}</div>; }
