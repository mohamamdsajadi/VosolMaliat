'use client';
import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { apiJson } from '../../lib/api';
import { STAGES, EVENT_LABELS, canStartArrest, hasArrestPermission, arrestDate, arrestTargets, actOnArrestOrder, digits } from '../../lib/arrest-orders';
import { formatDashboardNumber as money } from '../../lib/manager-dashboard';
import FigmaShell from '../manager-dashboard/figma-shell';
import DashboardIcon from '../manager-dashboard/dashboard-icon';
import PaperIcon from '../execution-papers/paper-icon';
import ArrestStartDialog from './arrest-dialog';
import ArrestLetter, { printArrestLetter } from './arrest-letter';
import styles from './arrest-orders.module.css';

function Icon({ name }) {
  return ['search', 'refresh', 'plus'].includes(name) ? <PaperIcon name={name === 'plus' ? 'add' : name} /> : <DashboardIcon name={name} />;
}

function useResource(path, enabled = true, revision = 0) {
  const [state, setState] = useState({ path: '', revision: -1, data: null, error: '', loading: true });
  useEffect(() => {
    if (!enabled) return;
    const abort = new AbortController();
    apiJson(path, { signal: abort.signal }).then(data => setState({ path, revision, data, error: '', loading: false }))
      .catch(error => { if (!abort.signal.aborted) setState({ path, revision, data: null, error: error.message, loading: false }); });
    return () => abort.abort();
  }, [path, enabled, revision]);
  return !enabled ? { data: null, error: '', loading: false } : state.path === path && state.revision === revision ? state : { data: null, error: '', loading: true };
}

export default function ArrestOrdersWorkspace() {
  const [selectedTab, setTab] = useState(null);
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [stage, setStage] = useState('');
  const [revision, setRevision] = useState(0);
  const [selectedIds, setSelectedIds] = useState([]);
  const [dialogPapers, setDialogPapers] = useState(null);
  const [selectedOrder, setSelectedOrder] = useState(() => typeof window !== 'undefined' ? new URLSearchParams(window.location.search).get('order') : null);
  const [notice, setNotice] = useState('');
  const user = useResource('/api/v1/auth/me');
  const canView = hasArrestPermission(user.data, 'ARREST_ORDER_VIEW');
  const canStart = canStartArrest(user.data);
  const tab = selectedTab || (canStart ? 'candidates' : 'mine');
  const tabs = [...(canStart ? [['candidates', 'آماده صدور']] : []), ['mine', 'ارجاع‌های من'], ['all', 'پیگیری دستورها']];
  const params = new URLSearchParams({ page, size: 20, search: query, ...(tab !== 'candidates' ? { stage, mine: tab === 'mine', history: tab === 'all' } : {}) });
  const list = useResource(`/api/v1/arrest-orders${tab === 'candidates' ? '/candidates' : ''}?${params}`, canView && !selectedOrder, revision);
  const detail = useResource(`/api/v1/arrest-orders/${encodeURIComponent(selectedOrder || '')}`, canView && !!selectedOrder, revision);
  const refresh = useCallback(() => { setRevision(value => value + 1); setSelectedIds([]); }, []);
  useEffect(() => {
    const normalized = search.trim().replace(/[۰-۹]/g, n => String('۰۱۲۳۴۵۶۷۸۹'.indexOf(n)));
    if (normalized === query) return;
    const timer = setTimeout(() => { setQuery(normalized); setPage(0); setSelectedIds([]); }, 300);
    return () => clearTimeout(timer);
  }, [search, query]);
  useEffect(() => {
    const navigate = () => setSelectedOrder(new URLSearchParams(window.location.search).get('order'));
    window.addEventListener('popstate', navigate); return () => window.removeEventListener('popstate', navigate);
  }, []);
  const openOrder = id => { setSelectedOrder(String(id)); window.history.pushState(null, '', `/arrest-orders?order=${encodeURIComponent(id)}`); };
  const closeOrder = () => { setSelectedOrder(null); window.history.pushState(null, '', '/arrest-orders'); refresh(); };
  const rows = list.data?.content || [];
  const papers = tab === 'candidates' ? rows.map(row => row.paper) : [];
  const selected = papers.filter(paper => selectedIds.includes(paper.id));
  const changeTab = value => { setTab(value); setPage(0); setStage(''); setSelectedIds([]); setNotice(''); };
  const toggle = id => setSelectedIds(ids => ids.includes(id) ? ids.filter(value => value !== id) : [...ids, id]);
  const error = user.error || list.error || detail.error;
  return <FigmaShell user={user.data} active="arrest" toolbar={<nav aria-label="مسیر صفحه"><Link href="/">سامانه جامع وصول و اجرا</Link> / دستور بازداشت اموال</nav>}>
    <section className={styles.workspace} aria-busy={user.loading || (selectedOrder ? detail.loading : list.loading)}>
      <header className={styles.heading}><div><p className={styles.eyebrow}>وصول و اجرای مالیاتی</p><h1>دستور بازداشت اموال</h1><p>صدور، ارجاع و پیگیری دستورهای مرتبط با برگ‌های اجرا</p></div><button className={styles.secondary} onClick={refresh} disabled={list.loading && !selectedOrder || detail.loading && !!selectedOrder}><PaperIcon name="refresh" />بروزرسانی</button></header>
      {error && <div className={styles.error} role="alert">{error}<button className={styles.secondary} onClick={refresh}>تلاش مجدد</button></div>}
      {notice && <p className={styles.success} role="status">{notice}</p>}
      {user.loading ? <div className={styles.empty} role="status">در حال دریافت اطلاعات کاربر…</div> : !canView ? <div className={styles.empty}>شما مجوز مشاهده دستورهای بازداشت را ندارید.</div> : selectedOrder ? <>
        <button className={styles.back} onClick={closeOrder}>← بازگشت به فهرست دستورها</button>
        {detail.loading ? <div className={styles.empty} role="status">در حال دریافت دستور و سوابق ارجاع…</div> : detail.data && <OrderDetail detail={detail.data} onChanged={refresh} />}
      </> : <>
        <div className={styles.tabs} role="tablist" aria-label="فهرست دستورهای بازداشت">{tabs.map(([value, label]) => <button key={value} role="tab" id={`arrest-tab-${value}`} aria-controls="arrest-list-panel" aria-selected={tab === value} tabIndex={tab === value ? 0 : -1} onClick={() => changeTab(value)} onKeyDown={event => { const keys = tabs.map(([key]) => key); if (['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) { event.preventDefault(); const index = event.key === 'Home' ? 0 : event.key === 'End' ? keys.length - 1 : (keys.indexOf(tab) + (event.key === 'ArrowLeft' ? 1 : keys.length - 1)) % keys.length; changeTab(keys[index]); document.getElementById(`arrest-tab-${keys[index]}`)?.focus(); } }}>{label}</button>)}</div>
        <section id="arrest-list-panel" role="tabpanel" aria-labelledby={`arrest-tab-${tab}`} className={styles.card}>
          <div className={styles.listToolbar}><label className={styles.search}><Icon name="search" /><input aria-label={tab === 'candidates' ? 'جستجوی برگ اجرا، مؤدی یا شناسه ملی' : 'جستجوی شماره برگ اجرا'} placeholder={tab === 'candidates' ? 'شماره برگ اجرا، نام مؤدی یا شناسه ملی…' : 'جستجوی شماره برگ اجرا…'} value={search} onChange={event => setSearch(event.target.value)} /></label>{tab !== 'candidates' && <select aria-label="فیلتر مرحله فرایند" value={stage} onChange={event => { setStage(event.target.value); setPage(0); }}><option value="">همه مراحل</option>{Object.entries(STAGES).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select>}<span>{list.loading ? 'در حال دریافت…' : `${digits(list.data?.totalElements || 0)} مورد`}</span></div>
          {tab === 'candidates' && canStart && <div className={styles.bulkBar}><span>{selected.length ? `${digits(selected.length)} برگ انتخاب شده` : '۰ برگ انتخاب شده'}</span><button className={styles.primary} disabled={!selected.length || list.loading} onClick={() => setDialogPapers(selected)}><Icon name="plus" />صدور گروهی{selected.length ? ` (${digits(selected.length)})` : ''}</button></div>}
          {list.loading ? <div className={styles.loading} role="status">{[1, 2, 3, 4].map(n => <div key={n} />)}<span>در حال دریافت فهرست…</span></div> : list.error ? <div className={styles.empty}>فهرست دریافت نشد. دوباره تلاش کنید.</div> : !rows.length ? <div className={styles.empty}><Icon name="document" /><h2>{tab === 'candidates' ? 'برگ آماده صدور یافت نشد' : tab === 'mine' ? 'دستوری در انتظار اقدام شما نیست' : 'سابقه دستوری برای شما یافت نشد'}</h2></div> : <div className={styles.tableScroll}><table className={styles.table}><thead><tr>{tab === 'candidates' && canStart && <th><input type="checkbox" aria-label="انتخاب همه برگ‌های این صفحه" checked={!!papers.length && selected.length === papers.length} ref={el => { if (el) el.indeterminate = selected.length > 0 && selected.length < papers.length; }} onChange={event => setSelectedIds(event.target.checked ? papers.map(paper => paper.id) : [])} /></th>}<th>برگ اجرا / مؤدی</th><th>بدهی باقیمانده (ریال)</th>{tab === 'candidates' ? <><th>تاریخ ابلاغ</th><th>پایان مهلت پرداخت</th><th>وضعیت</th></> : <><th>تاریخ ارجاع</th><th>مقصد ارجاع</th><th>وضعیت صدور</th><th>مرحله جاری</th></>}<th>عملیات</th></tr></thead><tbody>{rows.map(item => {
            const paper = tab === 'candidates' ? item.paper : item;
            return <tr key={paper.id}>{tab === 'candidates' && canStart && <td><input type="checkbox" aria-label={`انتخاب برگ ${paper.uniqueCode}`} checked={selectedIds.includes(paper.id)} onChange={() => toggle(paper.id)} /></td>}<td><Link className={styles.paperLink} href={`/execution-papers/${tab === 'candidates' ? paper.id : paper.executionPaperId}`}>{digits(paper.uniqueCode || paper.executionPaperCode)}</Link><strong>{paper.taxpayerName || '—'}</strong><small>شناسه ملی: {digits(paper.taxpayerNationalId)}</small></td><td className={styles.amount}>{money(paper.totalDebt ?? paper.debtAmount)}</td>{tab === 'candidates' ? <><td>{arrestDate(paper.notificationDate, false)}</td><td>{arrestDate(item.paymentDeadline, false)}</td><td><span className={styles.ready}>آماده صدور</span></td></> : <><td>{arrestDate(item.referredAt)}</td><td><strong>{item.referralDestination}</strong><small>{item.assignee?.roleTitle || 'صف رئیس اداره اجرائیات ویژه'}</small></td><td><span className={item.issuanceStatus === 'ISSUED' ? styles.ready : styles.pending}>{item.issuanceStatus === 'ISSUED' ? 'صادر شده' : 'در انتظار صدور'}</span>{item.letterNumber && <small>{digits(item.letterNumber)}</small>}</td><td>{STAGES[item.stage] || item.stage}</td></>}<td>{tab === 'candidates' ? canStart ? <button className={styles.rowAction} onClick={() => setDialogPapers([paper])}>آغاز فرایند</button> : <small>شروع توسط رئیس واحد اجرائیات</small> : <button className={styles.rowAction} onClick={() => openOrder(item.id)}>{item.allowedActions.length ? 'بررسی و اقدام' : 'جزئیات و نامه'}</button>}</td></tr>;
          })}</tbody></table></div>}
          <footer className={styles.pagination}><span>صفحه {digits(page + 1)} از {digits(Math.max(1, list.data?.totalPages || 1))}</span><div><button className={styles.secondary} disabled={page === 0 || list.loading} onClick={() => { setPage(value => value - 1); setSelectedIds([]); }}>قبلی</button><button className={styles.secondary} disabled={!list.data || page + 1 >= list.data.totalPages || list.loading} onClick={() => { setPage(value => value + 1); setSelectedIds([]); }}>بعدی</button></div></footer>
        </section>
      </>}
    </section>
    {dialogPapers && <ArrestStartDialog papers={dialogPapers} onClose={() => setDialogPapers(null)} onCompleted={ids => { if (ids.length) { refresh(); setNotice(`${digits(ids.length)} فرایند آغاز شد. مراحل نیازمند اقدام شما در «ارجاع‌های من» در دسترس است.`); } }} />}
  </FigmaShell>;
}

function OrderDetail({ detail, onChanged }) {
  const { order, history } = detail;
  const [panel, setPanel] = useState('letter');
  return <>
    <div className={styles.orderHeading}><div><h2>برگ اجرای {digits(order.executionPaperCode)}</h2><p>{order.taxpayerName} • شناسه ملی {digits(order.taxpayerNationalId)}</p></div><span className={order.issuanceStatus === 'ISSUED' ? styles.ready : styles.pending}>{order.issuanceStatus === 'ISSUED' ? 'دستور صادر شده' : 'در انتظار صدور'}</span></div>
    <div className={styles.summary}><div><small>تاریخ ارجاع</small><strong>{arrestDate(order.referredAt)}</strong></div><div><small>مقصد ارجاع</small><strong>{order.referralDestination}</strong><small>{order.assignee?.roleTitle}</small></div><div><small>مرحله جاری</small><strong>{STAGES[order.stage]}</strong></div><div><small>شماره دستور</small><strong>{digits(order.letterNumber)}</strong></div></div>
    <Progress order={order} history={history} />
    <div className={styles.detailGrid}><section className={styles.card}><div className={styles.detailTabs}><div><button aria-pressed={panel === 'letter'} onClick={() => setPanel('letter')}>نامه دستور بازداشت</button><button aria-pressed={panel === 'history'} onClick={() => setPanel('history')}>سوابق گردش و ارجاع ({digits(history.length)})</button></div>{panel === 'letter' && !!detail.letter?.paper && <button className={styles.secondary} onClick={printArrestLetter}><Icon name="document" />{order.issuanceStatus === 'ISSUED' ? 'چاپ / ذخیره PDF' : 'چاپ پیش‌نویس'}</button>}</div>{panel === 'letter' ? <div className={styles.letterPreview}><ArrestLetter detail={detail} /></div> : <ol className={styles.timeline}>{history.map(event => <li key={event.id}><span className={styles.timelineDot} /><div><strong>{EVENT_LABELS[event.action] || event.action}</strong><time>{arrestDate(event.occurredAt)}</time><p>{event.actorName} ← {event.destination}</p>{event.note && <small>{event.note}</small>}</div></li>)}</ol>}</section><aside><ActionPanel key={`${order.id}:${order.version}`} order={order} onChanged={onChanged} /><div><Link className={styles.paperLink} href={`/execution-papers/${order.executionPaperId}`}>مشاهده برگ اجرا و مشخصات مؤدی ←</Link></div></aside></div>
  </>;
}

function ActionPanel({ order, onChanged }) {
  const action = order.allowedActions[0];
  const [targets, setTargets] = useState([]);
  const [userId, setUserId] = useState('');
  const [loading, setLoading] = useState(!!action && action !== 'APPROVE');
  const [saving, setSaving] = useState(false);
  const [note, setNote] = useState('');
  const [error, setError] = useState('');
  useEffect(() => {
    if (!action || action === 'APPROVE') return;
    let active = true;
    arrestTargets({ orderId: order.id, role: action === 'PREPARE' ? 'DIRECTORATE_EXECUTION_TEAM_SUPERVISOR' : 'EXECUTION_OFFICER' })
      .then(data => { if (active) setTargets(data); }).catch(failure => { if (active) setError(failure.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [action, order.id]);
  const submit = async event => {
    event.preventDefault(); if (saving) return; setSaving(true); setError('');
    try { await actOnArrestOrder(order.id, { PREPARE: 'prepare', APPROVE: 'approve', ASSIGN_OFFICER: 'assign-officer' }[action], { version: order.version, userId: userId ? Number(userId) : null, note }); onChanged(); }
    catch (failure) { setError(failure.message); setSaving(false); }
  };
  const titles = { PREPARE: 'انتخاب سرپرست و تهیه پیش‌نویس', APPROVE: 'تأیید صدور و ارجاع', ASSIGN_OFFICER: 'ارجاع به مأمور اجرا' };
  return <section className={styles.actionCard}><span className={styles.eyebrow}>اقدام در مرحله جاری</span><h3>{titles[action] || (order.stage === 'ASSIGNED_TO_OFFICER' ? 'تحویل به مأمور اجرا' : 'در انتظار اقدام مسئول')}</h3>{error && <p className={styles.error} role="alert">{error}</p>}
    {action ? <form onSubmit={submit}>
      {action !== 'APPROVE' && <label className={styles.field}>{action === 'PREPARE' ? 'سرپرست هیئت اجرا' : 'مأمور اجرا'}<select required aria-label="کاربر مقصد مرحله بعد" disabled={loading || saving} value={userId} onChange={event => setUserId(event.target.value)}><option value="">{loading ? 'در حال دریافت کاربران…' : 'انتخاب کاربر مقصد'}</option>{targets.map(user => <option key={user.id} value={user.id}>{user.fullName}</option>)}</select>{!loading && !targets.length && <small className={styles.warning}>کاربر فعال با نقش موردنیاز در اداره کل این برگ اجرا موجود نیست.</small>}</label>}
      <label className={styles.field}>توضیحات <small>(اختیاری)</small><textarea rows={3} maxLength={1000} disabled={saving} value={note} onChange={event => setNote(event.target.value)} /></label>
      {action === 'APPROVE' && <><button type="button" className={styles.secondary} disabled={saving} onClick={async () => { setSaving(true); setError(''); try { await actOnArrestOrder(order.id, 'refresh-draft', { version: order.version }); onChanged(); } catch (failure) { setError(failure.message); setSaving(false); } }}>بروزرسانی پیش‌نویس</button><label className={styles.confirm}><input type="checkbox" required disabled={saving} />پیش‌نویس، مشخصات مؤدی و مبلغ بدهی را بررسی و تأیید کردم.</label></>}
      <button type="submit" className={styles.primary} disabled={saving || loading || action !== 'APPROVE' && !userId}>{saving ? 'در حال ثبت…' : titles[action]}</button>
    </form> : <p>{order.stage === 'ASSIGNED_TO_OFFICER' ? `این دستور و برگ اجرا به ${order.referralDestination} تحویل داده شده‌اند. گردش صدور و ارجاع تکمیل شده است.` : `انجام این مرحله بر عهده ${order.referralDestination} است.`}</p>}
  </section>;
}

function Progress({ order, history }) {
  const actions = new Set(history.map(event => event.action));
  const steps = [['STARTED', 'آغاز و انتخاب مقصد'], ['DEBT_INQUIRY_MOCKED', 'استعلام بدهی'], ['DRAFT_PREPARED', 'تهیه پیش‌نویس'], ['APPROVED_AND_ISSUED', 'تأیید و صدور'], ...(order.route === 'OFFICER' ? [] : [['REFERRED_TO_SUPERVISOR', 'ارجاع به سرپرست']]), ['ASSIGNED_TO_OFFICER', 'ارجاع به مأمور']];
  return <ol className={styles.progress} aria-label="مراحل گردش دستور بازداشت">{steps.map(([action, label], i) => <li key={action} className={actions.has(action) ? styles.stepDone : ''} aria-current={!actions.has(action) && (i === 0 || actions.has(steps[i - 1][0])) ? 'step' : undefined}><span>{actions.has(action) ? '✓' : digits(i + 1)}</span><strong>{label}</strong>{action === 'DEBT_INQUIRY_MOCKED' && <small>آزمایشی</small>}</li>)}</ol>;
}
