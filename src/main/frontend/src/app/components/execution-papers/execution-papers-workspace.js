'use client';
import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { apiJson } from '../../lib/api';
import { isAdminUser } from '../../lib/navigation';
import { PAPER_FILTER_DEFAULTS, PAPER_STATES, paperQuery, identifier } from '../../lib/execution-papers';
import FigmaShell from '../manager-dashboard/figma-shell';
import PaperDetailPanel, { Retry } from './paper-detail-page';
import PersianDateFilter from './persian-date-filter';
import { PrintExecutionPaper } from './paper-document';
import Icon from './paper-icon';
import PaperViewTools, { ViewIcon } from './paper-view-tools';
import PaperTable, { PAPER_LIST_COLUMNS } from './paper-table';
import PaperReferralAction from './paper-referral-action';
import PaperReferralDialog from './paper-referral-dialog';
import { canReferExecutionPaper } from '../../lib/task-referral';
import styles from './execution-papers.module.css';

const PAPER_NAVIGATION = [
  { key: 'referral', label: 'ارجاع به گروه اجرائیات', icon: 'referral' },
  { key: 'execution', label: 'اقدامات اجرایی', icon: 'list' },
  { key: 'auction', label: 'مزایده و فروش', icon: 'gavel' },
  { key: 'debt', label: 'تعیین تکلیف بدهی', icon: 'receipt' },
];

function useResource(path, enabled = true) {
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState({ key: '', data: null, loading: true, error: '' });
  const key = `${path}:${attempt}`;
  useEffect(() => {
    if (!enabled) return;
    const abort = new AbortController();
    let active = true;
    Promise.resolve().then(() => {
      if (active) setResult({ key, data: null, loading: true, error: '' });
      return apiJson(path, { signal: abort.signal });
    }).then(data => {
      if (active) setResult({ key, data, loading: false, error: '', updatedAt: Date.now() });
    }).catch(error => {
      if (active && error.name !== 'AbortError') setResult({ key, data: null, loading: false, error: error.message });
    });
    return () => { active = false; abort.abort(); };
  }, [path, enabled, key]);
  const retry = useCallback(() => setAttempt(value => value + 1), []);
  if (!enabled) return { data: null, loading: false, error: '', retry };
  return { ...(result.key === key ? result : { data: null, loading: true, error: '' }), retry };
}

function initialQuery() {
  const params = new URLSearchParams(typeof window === 'undefined' ? '' : window.location.search);
  const filters = Object.fromEntries(Object.keys(PAPER_FILTER_DEFAULTS).map(key => [key, params.get(key) || '']));
  return { ...filters, page: Math.max(0, Number(params.get('page')) || 0), size: 50,
    scope: ['assigned', 'unassigned'].includes(params.get('scope')) ? params.get('scope') : '',
    sortBy: ['totalDebt', 'issuanceDate', 'taxYear', 'taxPeriod'].includes(params.get('sortBy')) ? params.get('sortBy') : 'id',
    sortDirection: params.get('sortDirection') === 'asc' ? 'asc' : 'desc' };
}

export default function ExecutionPapersWorkspace({ selectedPaperId }) {
  const router = useRouter();
  const [query, setQuery] = useState(initialQuery);
  const [searchText, setSearchText] = useState(query.search);
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [hiddenColumns, setHiddenColumns] = useState([]);
  const [dense, setDense] = useState(true);
  const [refreshingRelated, setRefreshingRelated] = useState(false);
  const [relatedRefreshError, setRelatedRefreshError] = useState('');
  const [selection, setSelection] = useState({ key: '', ids: new Set() });
  const [referralPapers, setReferralPapers] = useState(null);
  const user = useResource('/api/v1/auth/me');
  const stats = useResource('/api/v1/dashboard/stats');
  const hasPermission = permission => isAdminUser(user.data) || (user.data?.allowedActions || []).includes(permission) || (user.data?.localBusinessPermissions || []).includes(permission);
  const canList = hasPermission('REPORT_VIEW') || hasPermission('EXECUTION_PAPER_LIST_VIEW');
  const canViewDetail = hasPermission('REPORT_VIEW') || hasPermission('EXECUTION_PAPER_DETAIL_VIEW');
  const canTrace = ['REPORT_VIEW', 'AUDIT_VIEW', 'EXECUTION_PAPER_TRACE_VIEW'].some(hasPermission);
  const tasks = useResource('/api/v1/cartable/my-tasks', hasPermission('TASK_VIEW_OWN'));
  const params = paperQuery(query);
  // Discard the previous view's selection, including when returning to that view.
  if (selection.key !== params) setSelection({ key: params, ids: new Set() });
  const requestParams = new URLSearchParams(params);
  requestParams.delete('scope');
  const list = useResource(`/api/v1/execution-papers${query.scope ? `/${query.scope}` : ''}?${requestParams}`, canList && !selectedPaperId);
  const paperPath = `/api/v1/execution-papers/${encodeURIComponent(selectedPaperId || '')}`;
  const detail = useResource(paperPath, Boolean(selectedPaperId && user.data));
  const trace = useResource(`${paperPath}/trace`, Boolean(selectedPaperId && user.data && canTrace));
  const related = useResource(`${paperPath}/related-persons`, Boolean(selectedPaperId && detail.data?.taxpayer?.type === 'LEGAL'));
  const external = useResource(`${paperPath}/external-requests`, Boolean(selectedPaperId && detail.data));
  const papers = list.data?.content || [];
  const total = list.data?.totalElements ?? 0;
  const pages = list.data?.totalPages ?? 0;
  const canRefer = canReferExecutionPaper(user.data);
  const selectedIds = new Set(papers.filter(paper => selection.key === params && selection.ids.has(paper.id)).map(paper => paper.id));
  const columns = [...(canRefer ? [{ key: 'selection', label: 'انتخاب', width: 38 }] : []), ...PAPER_LIST_COLUMNS.filter(column => !hiddenColumns.includes(column.key)).map(column => column.key === 'details' && canRefer
    ? { ...column, label: 'عملیات', width: 80 } : column)];
  const togglePaper = paper => setSelection({ key: params, ids: selectedIds.has(paper.id) ? new Set([...selectedIds].filter(id => id !== paper.id)) : new Set([...selectedIds, paper.id]) });
  const retryList = list.retry;
  const retryTasks = tasks.retry;
  const referralCompleted = useCallback(ids => {
    setSelection(current => ({ ...current, ids: new Set([...current.ids].filter(id => !ids.includes(id))) }));
    if (ids.length) { retryList(); retryTasks(); }
  }, [retryList, retryTasks]);
  const filterCount = Object.entries(PAPER_FILTER_DEFAULTS).filter(([key]) => key !== 'search' && query[key]).length + Number(Boolean(query.scope));

  useEffect(() => { window.history.replaceState(null, '', `${window.location.pathname}?${params}`); }, [params]);
  useEffect(() => {
    const timer = setTimeout(() => setQuery(current => current.search === searchText.trim() ? current : { ...current, search: searchText.trim(), page: 0 }), 350);
    return () => clearTimeout(timer);
  }, [searchText]);

  const changeFilter = (name, value) => setQuery(current => ({ ...current, [name]: value, page: 0 }));
  const changeSort = field => setQuery(current => ({ ...current, page: 0, sortBy: field, sortDirection: current.sortBy === field && current.sortDirection === 'desc' ? 'asc' : 'desc' }));
  const paperHref = id => `/execution-papers/${encodeURIComponent(id)}?${params}`;
  const print = () => {
    document.body.classList.add('execution-printing');
    const cleanup = () => { document.body.classList.remove('execution-printing'); window.removeEventListener('afterprint', cleanup); };
    window.addEventListener('afterprint', cleanup);
    window.requestAnimationFrame(() => { window.print(); setTimeout(cleanup, 500); });
  };
  const refreshRelated = async () => {
    setRefreshingRelated(true); setRelatedRefreshError('');
    try { await apiJson(`${paperPath}/related-persons/refresh`, { method: 'POST' }); related.retry(); trace.retry(); external.retry(); }
    catch (error) { setRelatedRefreshError(error.message); }
    finally { setRefreshingRelated(false); }
  };
  const createAllowed = hasPermission('USER_PROFILE_CREATE');
  const toolbar = <div className={styles.searchToolbar}>
    <form className={styles.searchBox} role="search" aria-label="جستجوی برگ‌های اجرا" onSubmit={event => { event.preventDefault(); changeFilter('search', searchText.trim()); }}>
      <label className={styles.srOnly} htmlFor="paper-search">جستجوی مؤدی، شماره برگ اجرا، شناسه مودی یا شناسه ملی</label>
      <input id="paper-search" value={searchText} onChange={event => setSearchText(event.target.value)} placeholder="جستجو مؤدی، شماره برگ اجرا، شناسه مودی و ..." />
      <button type="submit" aria-label="جستجو"><Icon name="search" /></button>
    </form>
    <button className={styles.filterToggle} aria-expanded={advancedOpen} aria-controls="paper-filters" onClick={() => setAdvancedOpen(!advancedOpen)}><span>فیلترها{filterCount > 0 && <small>{identifier(filterCount)}</small>}</span><ViewIcon name="filter" /></button>
    <PaperViewTools columns={PAPER_LIST_COLUMNS} hiddenColumns={hiddenColumns} onToggleColumn={key => setHiddenColumns(current => current.includes(key) ? current.filter(item => item !== key) : [...current, key])} dense={dense} onDenseChange={setDense} createAllowed={createAllowed} />
  </div>;
  const breadcrumbs = <nav className={styles.breadcrumbs} aria-label="مسیر صفحه"><Link href="/">سامانه جامع وصول و اجرا</Link><span aria-hidden="true">/</span><Link href={`/execution-papers?${params}`}>برگ‌های اجرا</Link><span aria-hidden="true">/</span><span aria-current="page">برگ اجرا <bdi>{identifier(detail.data?.summary?.uniqueCode || selectedPaperId)}</bdi></span></nav>;

  return <>
    <FigmaShell user={user.data} active="execution-papers" data={{ stats: stats.data, tasks: tasks.data || [], tasksError: tasks.error }} loading={stats.loading}
      toolbar={selectedPaperId ? breadcrumbs : toolbar} toolbarClassName={selectedPaperId ? styles.detailToolbar : styles.toolbar} mainClassName={styles.workspace} navigationItems={PAPER_NAVIGATION} papersLabel="برگ‌های اجرا">
      {user.error ? <Retry message="دریافت حساب کاربری ناموفق بود." onRetry={user.retry} /> : selectedPaperId ?
        <PaperDetailPanel key={selectedPaperId} resource={detail} trace={trace} canTrace={canTrace} canViewArrest={hasPermission('ARREST_ORDER_VIEW')} related={{ ...related, error: relatedRefreshError || related.error }} external={external}
          onPrint={print} onRefreshRelated={refreshRelated} refreshingRelated={refreshingRelated} hrefFor={paperHref}
          taskHref={hasPermission('TASK_VIEW_OWN') && detail.data ? `/tasks?executionPaper=${encodeURIComponent(detail.data.summary.uniqueCode)}` : null} /> : <>
        {advancedOpen && <div id="paper-filters" className={styles.filterPanel}>
          <h2 className={styles.srOnly}>فیلترهای برگ اجرا</h2>
          <div className={styles.filters}>
          <label className={styles.selectField}><span>نوع مؤدی</span><select value={query.taxpayerType} onChange={event => changeFilter('taxpayerType', event.target.value)}>
            <option value="">همه</option><option value="NATURAL">حقیقی</option><option value="LEGAL">حقوقی</option>
          </select><Icon name="down" /></label>
          <label className={styles.selectField}><span>وضعیت</span><select value={query.scope ? `@${query.scope}` : query.currentState} onChange={event => {
            const value = event.target.value;
            setQuery(current => ({ ...current, page: 0, scope: value.startsWith('@') ? value.slice(1) : '', currentState: value.startsWith('@') ? '' : value }));
          }}><option value="">همه</option><optgroup label="تخصیص"><option value="@assigned">تخصیص یافته</option><option value="@unassigned">تخصیص نیافته</option></optgroup><optgroup label="وضعیت برگ اجرا">{Object.entries(PAPER_STATES).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</optgroup></select><Icon name="down" /></label>
          <div className={styles.dateRange}><PersianDateFilter label="تاریخ صدور از" value={query.issuedFrom} onChange={value => changeFilter('issuedFrom', value)} /><PersianDateFilter label="تا" value={query.issuedTo} min={query.issuedFrom} onChange={value => changeFilter('issuedTo', value)} /></div>
          </div>
          <div className={styles.advancedFilters}>
            <label>حداقل بدهی (ریال)<input inputMode="decimal" value={query.minDebt} onChange={event => changeFilter('minDebt', event.target.value)} /></label>
            <label>حداکثر بدهی (ریال)<input inputMode="decimal" value={query.maxDebt} onChange={event => changeFilter('maxDebt', event.target.value)} /></label>
            <label>وضعیت ابلاغ<select value={query.notificationStatus} onChange={event => changeFilter('notificationStatus', event.target.value)}><option value="">همه</option><option value="NOTIFIED">ابلاغ شده</option><option value="NOT_NOTIFIED">عدم ابلاغ</option></select></label>
            <button onClick={() => { setQuery({ ...PAPER_FILTER_DEFAULTS, page: 0, size: 50, scope: '', sortBy: 'id', sortDirection: 'desc' }); setSearchText(''); }}>پاک کردن فیلترها</button>
          </div>
        </div>}

        <section className={styles.listPanel} aria-label="فهرست برگ‌های اجرا" aria-busy={list.loading || user.loading}>
          {canRefer && <div className={styles.bulkToolbar}>
            <span aria-live="polite">{identifier(selectedIds.size)} برگ انتخاب شده <small>انتخاب در همین صفحه</small></span>
            <div><button type="button" className={styles.bulkReferral} disabled={!selectedIds.size || list.loading} onClick={() => setReferralPapers(papers.filter(paper => selectedIds.has(paper.id)))}><ViewIcon name="referral" />ارجاع گروهی</button>
              <button type="button" disabled={!selectedIds.size} onClick={() => setSelection({ key: params, ids: new Set() })}>لغو انتخاب</button></div>
          </div>}
          <div className={styles.tableToolbar}>
            <div className={styles.tableTools}><button className={styles.iconButton} aria-label="بروزرسانی فهرست" onClick={list.retry} disabled={list.loading || !canList}><Icon name="refresh" /></button><RefreshStatus updatedAt={list.updatedAt} loading={list.loading || user.loading} error={list.error} /></div>
            <div className={styles.pagination}>
              <span aria-live="polite">{list.loading ? '…' : total ? `${identifier(query.page * query.size + 1)} تا ${identifier(Math.min(query.page * query.size + papers.length, total))} از ${identifier(total)} مورد` : '۰ نتیجه'}</span>
              <button aria-label="صفحه قبل" disabled={query.page === 0 || list.loading} onClick={() => setQuery(current => ({ ...current, page: current.page - 1 }))}><Icon name="previous" /></button>
              <button aria-label="صفحه بعد" disabled={query.page + 1 >= pages || list.loading} onClick={() => setQuery(current => ({ ...current, page: current.page + 1 }))}><Icon name="next" /></button>
            </div>
          </div>
          <PaperTable papers={papers} columns={columns} query={query} onSort={changeSort} onOpen={id => router.push(paperHref(id))} hrefFor={paperHref}
            selection={canRefer ? { ids: selectedIds, toggle: togglePaper, toggleAll: () => setSelection({ key: params, ids: selectedIds.size === papers.length ? new Set() : new Set(papers.map(paper => paper.id)) }) } : null}
            renderReferral={canRefer ? paper => <PaperReferralAction paper={paper} onOpen={setReferralPapers} /> : undefined}
            dense={dense} loading={user.loading || list.loading} canView={canList} canViewDetail={canViewDetail} error={list.error} onRetry={list.retry} />
        </section>
      </>}
      {referralPapers && <PaperReferralDialog papers={referralPapers} user={user.data} onClose={() => setReferralPapers(null)} onSuccess={referralCompleted} />}
    </FigmaShell>
    {selectedPaperId && detail.data && <PrintExecutionPaper data={detail.data} />}
  </>;
}

function RefreshStatus({ updatedAt, loading, error }) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => { const timer = setInterval(() => setNow(Date.now()), 60000); return () => clearInterval(timer); }, []);
  const minutes = Math.max(0, Math.floor((Math.max(now, updatedAt || 0) - (updatedAt || now)) / 60000));
  return <span className={styles.refreshStatus} role="status" title={updatedAt ? new Intl.DateTimeFormat('fa-IR', { dateStyle: 'medium', timeStyle: 'short' }).format(updatedAt) : undefined}>
    {loading ? 'در حال به‌روزرسانی…' : error ? 'به‌روزرسانی ناموفق بود' : updatedAt ? `آخرین به‌روزرسانی ${minutes ? `${identifier(minutes)} دقیقه قبل` : 'اکنون'}` : 'فهرست به‌روزرسانی نشده است'}
  </span>;
}
