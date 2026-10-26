'use client';
import Link from 'next/link';
import { useEffect, useRef, useState } from 'react';
import { apiJson } from '../../lib/api';
import { LEGAL_RESTRICTION_GROUPS, RESTRICTION_STATUSES, canViewRestrictions } from '../../lib/legal-restrictions';
import { identifier, normalizeDigits } from '../../lib/execution-papers';
import { arrestDate } from '../../lib/arrest-orders';
import FigmaShell from '../manager-dashboard/figma-shell';
import PersianDateFilter from '../execution-papers/persian-date-filter';
import Icon from './restriction-icon';
import styles from './legal-restrictions.module.css';

function useResource(path, enabled = true, revision = 0) {
  const [state, setState] = useState({ key: '', data: null, error: '' });
  const key = `${path}:${revision}`;
  useEffect(() => {
    if (!enabled) return;
    const controller = new AbortController();
    apiJson(path, { signal: controller.signal }).then(data => setState({ key, data, error: '' }))
      .catch(error => { if (!controller.signal.aborted) setState({ key, data: null, error: error.message }); });
    return () => controller.abort();
  }, [path, enabled, key]);
  return !enabled ? { data: null, error: '', loading: false } : state.key === key ? { ...state, loading: false } : { data: null, error: '', loading: true };
}
function initialFilters() {
  const params = new URLSearchParams(typeof window === 'undefined' ? '' : window.location.search);
  return { search: params.get('search') || '', status: params.get('status') || '', from: params.get('from') || '', to: params.get('to') || '', page: 0 };
}
export default function LegalRestrictionsWorkspace() {
  const [filters, setFilters] = useState(initialFilters);
  const [searchText, setSearchText] = useState(filters.search);
  const [revision, setRevision] = useState(0);
  const [requestId, setRequestId] = useState(() => typeof window === 'undefined' ? null : new URLSearchParams(window.location.search).get('request'));
  const user = useResource('/api/v1/auth/me', true, revision);
  const canView = canViewRestrictions(user.data);
  const params = new URLSearchParams({ ...filters, size: 20 });
  const list = useResource(`/api/v1/legal-restrictions/article-186?${params}`, canView, revision);
  const detail = useResource(`/api/v1/legal-restrictions/article-186/${encodeURIComponent(requestId || '')}`, canView && !!requestId, revision);
  useEffect(() => {
    const search = normalizeDigits(searchText.trim());
    if (search === filters.search) return;
    const timer = setTimeout(() => setFilters(current => ({ ...current, search, page: 0 })), 300);
    return () => clearTimeout(timer);
  }, [searchText, filters.search]);
  useEffect(() => {
    const navigate = () => setRequestId(new URLSearchParams(window.location.search).get('request'));
    window.addEventListener('popstate', navigate); return () => window.removeEventListener('popstate', navigate);
  }, []);
  const changeFilter = (key, value) => setFilters(current => ({ ...current, [key]: value, page: 0 }));
  const openRequest = id => { setRequestId(String(id)); window.history.pushState(null, '', `/legal-restrictions?${new URLSearchParams({ ...filters, request: id })}`); };
  const closeRequest = () => { setRequestId(null); window.history.replaceState(null, '', `/legal-restrictions?${params}`); };
  const reset = () => { setSearchText(''); setFilters({ search: '', status: '', from: '', to: '', page: 0 }); };
  const counts = list.data?.counts;
  const rows = list.data?.content || [];
  const canOpenPaper = user.data?.businessRole === 'ADMIN' || ['REPORT_VIEW', 'EXECUTION_PAPER_DETAIL_VIEW'].some(code => [...(user.data?.allowedActions || []), ...(user.data?.localBusinessPermissions || [])].includes(code));
  const metrics = [
    { key: 'total', label: 'کل درخواست‌ها', icon: 'documents', tone: 'blue', status: '' },
    { key: 'pending', label: 'در انتظار پاسخ', icon: 'clock', tone: 'amber', status: 'SENT' },
    { key: 'confirmed', label: 'تأیید شده', icon: 'check', tone: 'green', status: 'CONFIRMED' },
    { key: 'failed', label: 'ناموفق', icon: 'alert', tone: 'red', status: 'FAILED' },
  ];
  return <FigmaShell user={user.data} active="legal-restrictions" toolbar={<nav className={styles.breadcrumbs} aria-label="مسیر صفحه"><Link href="/">سامانه جامع وصول و اجرا</Link><span>/</span><span>محدودیت‌های قانونی</span></nav>}>
    <section className={styles.workspace} aria-busy={user.loading || list.loading}>
      <header className={styles.heading}><div className={styles.title}><span className={styles.heroIcon}><Icon name="shield" size={27} /></span><div><h1>محدودیت‌های قانونی</h1><p>وضعیت درخواست‌ها، اشخاص مشمول و سوابق</p></div></div><button className={styles.secondary} onClick={() => setRevision(value => value + 1)} disabled={user.loading || list.loading}><Icon name="refresh" size={18} />بروزرسانی</button></header>
      <div className={styles.groups} aria-label="گروه‌های محدودیت قانونی">{LEGAL_RESTRICTION_GROUPS.map(group => <div key={group.key} className={`${styles.group} ${group.href ? styles.groupActive : styles.groupDisabled}`} aria-disabled={!group.href || undefined} aria-current={group.href ? 'page' : undefined}><span className={styles.groupIcon}><Icon name={group.icon} size={25} /></span><div><h2>{group.label}</h2><p>{group.subtitle}</p></div><span className={group.href ? styles.activeTag : styles.disabledTag}>{group.href ? 'فعال' : 'غیرفعال'}</span></div>)}</div>
      {user.error ? <ErrorBox message={user.error} retry={() => setRevision(value => value + 1)} /> : user.loading ? <Loading /> : !canView ? <div className={styles.empty}><Icon name="shield" size={40} /><h2>دسترسی مشاهده این بخش فعال نیست</h2></div> : <>
        <div className={styles.metrics} aria-label="خلاصه درخواست‌های تبصره ۳ ماده ۱۸۶">{metrics.map(metric => <button key={metric.key} className={`${styles.metric} ${styles[metric.tone]}`} aria-pressed={filters.status === metric.status} onClick={() => changeFilter('status', metric.status)}><span className={styles.metricIcon}><Icon name={metric.icon} /></span><span><span className={styles.metricLabel}>{metric.label}</span><strong>{list.loading ? '…' : identifier(counts?.[metric.key])}</strong></span></button>)}</div>
        <section className={styles.card} aria-labelledby="restriction-list-title">
          <header className={styles.cardHeader}><div><span className={styles.eyebrow}>تبصره ۳ ماده ۱۸۶</span><h2 id="restriction-list-title">درخواست‌های اعمال محدودیت</h2></div><span className={styles.resultCount}>{list.loading ? 'در حال دریافت…' : `${identifier(list.data?.totalElements ?? '—')} درخواست`}</span></header>
          <div className={styles.filters}>
            <label className={styles.search}><Icon name="search" size={20} /><input aria-label="جستجوی درخواست محدودیت" placeholder="شماره برگ، نام مؤدی، شناسه ملی یا پیگیری…" value={searchText} onChange={event => setSearchText(event.target.value)} /></label>
            <label className={styles.field}><span>وضعیت درخواست</span><select value={filters.status} onChange={event => changeFilter('status', event.target.value)}><option value="">همه وضعیت‌ها</option>{Object.entries(RESTRICTION_STATUSES).map(([status, item]) => <option key={status} value={status}>{item.label}</option>)}</select></label>
            <div className={styles.dates}><PersianDateFilter label="ارسال از تاریخ" value={filters.from} onChange={value => changeFilter('from', value)} /><PersianDateFilter label="تا تاریخ" value={filters.to} min={filters.from} onChange={value => changeFilter('to', value)} /></div>
            {(filters.search || filters.status || filters.from || filters.to) && <button className={styles.clear} onClick={reset}>پاک کردن فیلترها</button>}
          </div>
          {list.error ? <ErrorBox message={list.error} retry={() => setRevision(value => value + 1)} /> : list.loading ? <Loading /> : !rows.length ? <div className={styles.empty}><span className={styles.emptyIcon}><Icon name="documents" size={34} /></span><h3>{filters.search || filters.status || filters.from || filters.to ? 'درخواستی با این مشخصات یافت نشد' : 'درخواستی در محدوده دسترسی شما ثبت نشده است'}</h3>{(filters.search || filters.status || filters.from || filters.to) && <button className={styles.secondary} onClick={reset}>نمایش همه درخواست‌ها</button>}</div> : <div className={styles.tableScroll} role="region" tabIndex={0} aria-label="فهرست درخواست‌های محدودیت قانونی"><table className={styles.table}><thead><tr><th>برگ اجرا / مؤدی حقوقی</th><th>اشخاص ارسالی</th><th>تاریخ ارسال</th><th>وضعیت درخواست</th><th>تاریخ پاسخ</th><th>عملیات</th></tr></thead><tbody>{rows.map(row => <tr key={row.id}><td><strong>{row.taxpayerName || '—'}</strong>{canOpenPaper ? <Link className={styles.paperLink} href={`/execution-papers/${row.executionPaperId}`}>برگ {identifier(row.executionPaperCode)}</Link> : <small>برگ {identifier(row.executionPaperCode)}</small>}<small>شناسه ملی: {identifier(row.taxpayerNationalId)}</small></td><td><span className={styles.personCount}><Icon name="people" size={17} />{identifier(row.subjectCount)}</span></td><td><time>{arrestDate(row.sentAt)}</time></td><td><Status status={row.status} /></td><td><time>{arrestDate(row.completedAt)}</time></td><td><button className={styles.detailButton} onClick={() => openRequest(row.id)} aria-label={`جزئیات و سوابق درخواست ${row.id}`}>جزئیات و سوابق<Icon name="arrow" size={16} /></button></td></tr>)}</tbody></table></div>}
          <footer className={styles.pagination}><span>صفحه {identifier(filters.page + 1)} از {identifier(Math.max(1, list.data?.totalPages || 1))}</span><div><button className={styles.secondary} disabled={!filters.page || list.loading} onClick={() => setFilters(current => ({ ...current, page: current.page - 1 }))}>قبلی</button><button className={styles.secondary} disabled={list.loading || !list.data || filters.page + 1 >= list.data.totalPages} onClick={() => setFilters(current => ({ ...current, page: current.page + 1 }))}>بعدی</button></div></footer>
        </section>
      </>}
    </section>
    {!!requestId && canView && <RequestDrawer key={requestId} resource={detail} onClose={closeRequest} canOpenPaper={canOpenPaper} retry={() => setRevision(value => value + 1)} />}
  </FigmaShell>;
}

function RequestDrawer({ resource, onClose, canOpenPaper, retry }) {
  const dialog = useRef(null);
  const [tab, setTab] = useState('info');
  const tabs = useRef([]);
  useEffect(() => { dialog.current.showModal(); }, []);
  const data = resource.data;
  const row = data?.request;
  const items = [['info', 'اطلاعات درخواست', 'documents'], ['subjects', 'اشخاص مشمول', 'people'], ['history', 'سوابق درخواست', 'history']];
  return <dialog ref={dialog} className={styles.detailDialog} aria-labelledby="restriction-detail-title" onCancel={event => { event.preventDefault(); onClose(); }} onClick={event => { if (event.target === event.currentTarget) onClose(); }}>
    <header className={styles.drawerHeader}><div><span className={styles.eyebrow}>تبصره ۳ ماده ۱۸۶</span><h2 id="restriction-detail-title">{row ? `درخواست شماره ${identifier(row.id)}` : 'جزئیات درخواست'}</h2></div><button className={styles.close} aria-label="بستن جزئیات درخواست" onClick={onClose}><Icon name="close" size={21} /></button></header>
    {row && <div className={styles.drawerSummary}><div><strong>{row.taxpayerName}</strong><span>برگ اجرا {identifier(row.executionPaperCode)}</span></div><Status status={row.status} /></div>}
    <div className={styles.drawerTabs} role="tablist" aria-label="بخش‌های جزئیات درخواست">{items.map(([key, label, icon], index) => <button key={key} ref={element => { tabs.current[index] = element; }} id={`restriction-tab-${key}`} role="tab" aria-selected={tab === key} tabIndex={tab === key ? 0 : -1} aria-controls="restriction-detail-panel" onClick={() => setTab(key)} onKeyDown={event => {
      const next = event.key === 'ArrowLeft' ? (index + 1) % 3 : event.key === 'ArrowRight' ? (index + 2) % 3 : event.key === 'Home' ? 0 : event.key === 'End' ? 2 : null;
      if (next != null) { event.preventDefault(); setTab(items[next][0]); tabs.current[next]?.focus(); }
    }}><Icon name={icon} size={18} />{label}</button>)}</div>
    <div className={styles.drawerBody} id="restriction-detail-panel" role="tabpanel" aria-labelledby={`restriction-tab-${tab}`} aria-busy={resource.loading} tabIndex={0}>
      {resource.loading ? <Loading /> : resource.error ? <ErrorBox message={resource.error} retry={retry} /> : data && <>
        {tab === 'info' && <><h3 className={styles.sectionTitle}>مشخصات و وضعیت درخواست</h3><dl className={styles.facts}><Fact label="نوع درخواست" value="اعمال محدودیت" /><Fact label="سامانه مقصد" value="ثبت شرکت‌ها" /><Fact label="مؤدی حقوقی" value={row.taxpayerName} /><Fact label="شناسه ملی مؤدی" value={identifier(row.taxpayerNationalId)} /><Fact label="اداره کل" value={row.directorateName} wide /><Fact label="تاریخ ارسال" value={arrestDate(row.sentAt)} /><Fact label="تاریخ پاسخ" value={arrestDate(row.completedAt)} /><Fact label="تعداد اشخاص ارسالی" value={`${identifier(row.subjectCount)} شخص`} /><Fact label="تاریخ سند بدهی" value={identifier(data.debtPaperDate?.replaceAll('-', '/'))} /><Fact label="شناسه پیگیری" value={row.trackingCode} wide /></dl>{data.errorMessage && <div className={styles.failure}><Icon name="alert" size={20} /><div><strong>جزئیات خطای درخواست</strong><p>{data.errorMessage}</p></div></div>}</>}
        {tab === 'subjects' && <><h3 className={styles.sectionTitle}>اشخاص ارسالی در این درخواست <span>{identifier(data.subjects.length)} شخص</span></h3>{data.subjects.length ? <div className={styles.subjects}>{data.subjects.map((person, index) => <article className={styles.subject} key={`${person.identifier}-${index}`}><span className={styles.personAvatar}><Icon name={person.personType === 'LEGAL' ? 'company' : 'people'} size={22} /></span><div><h4>{person.name || 'نام ثبت نشده'}</h4><span className={styles.personPost}>{person.postTitle || 'سمت ثبت نشده'}</span><p>{person.personType === 'LEGAL' ? 'شناسه ملی' : 'کد ملی'}: <bdi>{identifier(person.identifier)}</bdi></p><small>شروع سمت: {identifier(person.startDate?.replaceAll('-', '/'))} {person.endDate && ` · پایان سمت: ${identifier(person.endDate.replaceAll('-', '/'))}`}</small>{person.representative && <small>نماینده: {person.representative}</small>}</div></article>)}</div> : <p className={styles.emptyText}>شخصی در اطلاعات این درخواست ثبت نشده است.</p>}
          {!!data.paperRestrictions.length && <section className={styles.paperRestrictions}><h3 className={styles.sectionTitle}>محدودیت‌های ثبت‌شده روی این برگ اجرا</h3><div className={styles.linkedList}>{data.paperRestrictions.map((restriction, index) => <article key={`${restriction.identifier}-${index}`}><div><strong>{restriction.name || '—'}</strong><small>کد ملی: {identifier(restriction.identifier)}</small></div><span className={styles.restrictionState}>{restriction.stateTitle}</span><dl><Fact label="تاریخ ثبت" value={arrestDate(restriction.appliedAt)} /><Fact label="شناسه ثبت" value={restriction.trackingCode} />{restriction.releasedAt && <><Fact label="تاریخ رفع محدودیت" value={arrestDate(restriction.releasedAt)} /><Fact label="شناسه رفع محدودیت" value={restriction.releaseTrackingCode} /></>}</dl></article>)}</div></section>}
        </>}
        {tab === 'history' && <><h3 className={styles.sectionTitle}>خط زمانی درخواست <span>{identifier(data.history.length)} رویداد</span></h3>{data.history.length ? <ol className={styles.timeline}>{data.history.map((event, index) => <li key={event.id}><span className={styles.timelineMarker}>{index === data.history.length - 1 && row.status === 'CONFIRMED' ? <Icon name="check" size={17} /> : <span />}</span><div><time>{arrestDate(event.occurredAt)}</time><h4>{event.title}</h4><p>{event.actor || 'انجام‌دهنده ثبت نشده'}</p>{event.note && <small>{identifier(event.note)}</small>}</div></li>)}</ol> : <p className={styles.emptyText}>سابقه‌ای برای این درخواست ثبت نشده است.</p>}</>}
      </>}
    </div>
    <footer className={styles.drawerFooter}><button className={styles.secondary} onClick={onClose}>بستن</button>{row && canOpenPaper && <Link className={styles.primary} href={`/execution-papers/${row.executionPaperId}`}>مشاهده برگ اجرا<Icon name="arrow" size={17} /></Link>}</footer>
  </dialog>;
}
function Status({ status }) { const item = RESTRICTION_STATUSES[status]; return <span className={`${styles.status} ${styles[item?.tone || 'unknown']}`}><Icon name={item?.icon || 'documents'} size={15} />{item?.label || status || 'ثبت نشده'}</span>; }
function Fact({ label, value, wide }) { return <div className={wide ? styles.wideFact : ''}><dt>{label}</dt><dd>{value || '—'}</dd></div>; }
function ErrorBox({ message, retry }) { return <div className={styles.error} role="alert"><Icon name="alert" size={22} /><p>{message}</p><button className={styles.secondary} onClick={retry}>تلاش مجدد</button></div>; }
function Loading() { return <div className={styles.loading} role="status"><span className={styles.srOnly}>در حال دریافت اطلاعات…</span>{[1, 2, 3, 4].map(item => <div key={item} />)}</div>; }
