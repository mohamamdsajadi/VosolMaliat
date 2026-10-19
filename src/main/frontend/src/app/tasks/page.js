'use client'

import Link from 'next/link';
import { useCallback, useEffect, useRef, useState } from 'react';
import AppShell from '../components/app-shell';
import JalaliInput from '../components/jalali-input';
import RefreshButton from '../components/refresh-button';
import { formatFaNumber, formatJalaliDateTime } from '../lib/display';
import { claimCartableTask, completeTask, delegateTask, fetchCartableTasks, fetchDelegationTargets, isExecutionPaperReviewTask } from '../lib/tasks';
import { fetchCurrentUser } from '../lib/security';

const TABLE_HEADERS = [
  '#',
  'برگ اجرا',
  'مرحله فعلی',
  'مسئول فعلی',
  'نقش مجاز',
  'تاریخ ایجاد',
  'عملیات',
];

const EMPTY_FILTERS = {
  executionPaper: '',
  taskName: '',
  assignee: '',
  candidateGroup: '',
  createdFrom: '',
  createdTo: '',
};

export default function AllTasksPage() {
  const [tasks, setTasks] = useState([]);
  const [currentUser, setCurrentUser] = useState(null);
  const [loadingTasks, setLoadingTasks] = useState(true);
  const [message, setMessage] = useState("");
  const [filterForm, setFilterForm] = useState(EMPTY_FILTERS);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [filtersReady, setFiltersReady] = useState(false);
  const handledTaskKeys = useRef(new Set());

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const initialFilters = { ...EMPTY_FILTERS };
    Object.keys(initialFilters).forEach(key => { initialFilters[key] = params.get(key) || ''; });
    setFilterForm(initialFilters);
    setFilters(initialFilters);
    setFiltersReady(true);
  }, []);

  const loadTasks = useCallback(async () => {
    setLoadingTasks(true);
    setMessage("");
    try {
      const [me, taskResult] = await Promise.all([fetchCurrentUser(), fetchCartableTasks(filters)]);
      setCurrentUser(me);
      setTasks(taskResult.filter((task) => !handledTaskKeys.current.has(String(task.taskKey))));
    } catch (error) {
      setMessage(error.message || "دریافت کارتابل با خطا روبه‌رو شد.");
      setTasks([]);
    } finally {
      setLoadingTasks(false);
    }
  }, [filters]);

  const removeHandledTask = useCallback((taskKey) => {
    const normalizedTaskKey = String(taskKey);
    handledTaskKeys.current.add(normalizedTaskKey);
    setTasks((current) => current.filter((task) => String(task.taskKey) !== normalizedTaskKey));
  }, []);

  useEffect(() => {
    if (filtersReady) loadTasks();
  }, [loadTasks, filtersReady]);

  const isAdmin = currentUser?.businessRole === 'ADMIN' || currentUser?.allowedActions?.includes('ADMIN');

  const updateFilter = (event) => {
    const { name, value } = event.target;
    setFilterForm((current) => ({ ...current, [name]: value }));
  };

  const applyFilters = (event) => {
    event.preventDefault();
    setFilters(filterForm);
  };

  const clearFilters = () => {
    setFilterForm(EMPTY_FILTERS);
    setFilters(EMPTY_FILTERS);
  };

  return (
    <AppShell
      active="tasks"
      title={isAdmin ? "مشاهده همه کارهای فعال" : "کارتابل شخصی"}
      eyebrow="کارتابل"
      contextItems={[
        `کارهای فعال: ${formatFaNumber(tasks.length)}`,
        currentUser?.businessRole ? `نقش: ${roleLabel(currentUser.businessRole)}` : 'نقش: نامشخص'
      ]}
      actions={(
        <RefreshButton onClick={loadTasks} loading={loadingTasks}>بروزرسانی صف</RefreshButton>
      )}
    >
      <section className="panel-section">
        {message && <div className="notice error">{message}</div>}

        <form className="task-filter-panel" onSubmit={applyFilters}>
          <div className="task-filter-header">
            <div className="task-filter-title">
              <span aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <circle cx="10.5" cy="10.5" r="5.5" />
                  <path d="M15 15l4 4" />
                </svg>
              </span>
              <div>
                <p className="eyebrow">جستجو</p>
                <h3>فیلتر کارهای فعال</h3>
              </div>
            </div>
            <div className="task-filter-actions">
              <button type="button" className="task-filter-secondary" onClick={clearFilters}>پاک کردن</button>
              <button type="submit" className="task-filter-primary">اعمال فیلتر</button>
            </div>
          </div>
          <div className="task-filter-grid">
            <label>
              <span>شناسه یا کد برگ اجرا</span>
              <input name="executionPaper" value={filterForm.executionPaper} onChange={updateFilter} placeholder="مثلاً EXEC یا شناسه عددی" />
            </label>
            <label>
              <span>مرحله فعلی</span>
              <input name="taskName" value={filterForm.taskName} onChange={updateFilter} placeholder="نام مرحله" />
            </label>
            <label>
              <span>مسئول فعلی</span>
              <input name="assignee" value={filterForm.assignee} onChange={updateFilter} placeholder="نام کاربری مسئول" />
            </label>
            <label>
              <span>نقش مجاز</span>
              <select name="candidateGroup" value={filterForm.candidateGroup} onChange={updateFilter}>
                <option value="">همه نقش‌ها</option>
                <option value="GENERAL_OFFICE_HEAD">رئیس اداره کل</option>
                <option value="TAX_OFFICE_HEAD">رئیس اداره مالیات</option>
                <option value="TAX_EXPERT">کارشناس مالیاتی</option>
              </select>
            </label>
            <label>
              <span>از تاریخ ایجاد</span>
              <JalaliInput
                name="createdFrom"
                value={filterForm.createdFrom}
                onChangeValue={value => setFilterForm(current => ({ ...current, createdFrom: value }))}
              />
            </label>
            <label>
              <span>تا تاریخ ایجاد</span>
              <JalaliInput
                name="createdTo"
                value={filterForm.createdTo}
                onChangeValue={value => setFilterForm(current => ({ ...current, createdTo: value }))}
              />
            </label>
          </div>
        </form>

        {loadingTasks ? (
          <div className="empty-state compact">در حال دریافت کارتابل...</div>
        ) : tasks.length === 0 ? (
          <div className="empty-state task-empty-state">
            <strong>فعلا کاری در کارتابل وجود ندارد.</strong>
            <span>اگر فیلتر فعال است، آن را پاک کنید یا بازه جستجو را تغییر دهید.</span>
          </div>
        ) : (
          <div className="task-table-card">
            <div className="task-table-header">
              <div>
                <p className="eyebrow">کارتابل</p>
                <h2>{isAdmin ? 'کارهای فعال' : 'کارهای اختصاص‌یافته به من'}</h2>
              </div>
              <span>{formatFaNumber(tasks.length)} مورد</span>
            </div>
            <div className="task-table-scroll">
              <table className="task-grid-table">
                <thead>
                  <tr>
                    {TABLE_HEADERS.map((header) => (
                      <th key={header}>{header}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {tasks.map((task, index) => (
                    <CartableTaskRow
                      key={task.taskKey}
                      index={index}
                      task={task}
                      onChanged={loadTasks}
                      onHandled={removeHandledTask}
                      readOnly={isAdmin}
                      zebra={index % 2 === 1}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </section>
    </AppShell>
  );
}

function CartableTaskRow({ task, index, onChanged, onHandled, readOnly, zebra }) {
  const [targets, setTargets] = useState([]);
  const [target, setTarget] = useState('');
  const [loadingTargets, setLoadingTargets] = useState(false);
  const [message, setMessage] = useState('');
  const [claiming, setClaiming] = useState(false);
  const [delegating, setDelegating] = useState(false);
  const [finishingReview, setFinishingReview] = useState(false);
  const canClaim = task.allowedActions?.claim && !readOnly;
  const canDelegate = task.allowedActions?.delegate && !readOnly;
  const canFinishReview = isExecutionPaperReviewTask(task) && task.allowedActions?.complete && !readOnly;
  const variables = variableMap(task.variables);
  const executionPaperId = firstValue(variables, ['executionPaperId', 'execution_paper_id']);
  const executionPaperCode = firstValue(variables, ['executionPaperCode', 'executionPaperUniqueCode', 'execution_paper_code']);
  const candidateLabel = task.candidateGroups?.length
    ? task.candidateGroups.map(roleLabel).join('، ')
    : 'بدون گروه کاندید';
  const claimTask = async () => {
    setClaiming(true);
    setMessage('');
    try {
      await claimCartableTask(task.taskKey);
      await onChanged();
    } catch (error) {
      setMessage(error.message || 'دریافت کار ناموفق بود.');
    } finally {
      setClaiming(false);
    }
  };

  const loadTargets = async () => {
    setLoadingTargets(true);
    setMessage('');
    try {
      const result = await fetchDelegationTargets(task.taskKey);
      setTargets(result);
      setTarget(result[0]?.username || '');
    } catch (error) {
      setMessage(error.message || 'دریافت کاربران مقصد ناموفق بود.');
    } finally {
      setLoadingTargets(false);
    }
  };

  const delegateToUser = async () => {
    if (!target) {
      setMessage('ابتدا کاربر مقصد را انتخاب کنید.');
      return;
    }
    setDelegating(true);
    setMessage('');
    try {
      await delegateTask(task.taskKey, { assignee: target });
      onHandled(task.taskKey);
    } catch (error) {
      setMessage(error.message || 'ارجاع کاربر ناموفق بود.');
    } finally {
      setDelegating(false);
    }
  };

  const finishExecutionPaperReview = async () => {
    if (!window.confirm('آیا از پایان بررسی برگ اجرا و رد کردن مراحل سازمانی باقی‌مانده مطمئن هستید؟')) {
      return;
    }
    setFinishingReview(true);
    setMessage('');
    try {
      await completeTask(task.taskKey, { finishExecutionPaperCheck: true });
      onHandled(task.taskKey);
    } catch (error) {
      setMessage(error.message || 'پایان بررسی برگ اجرا ناموفق بود.');
    } finally {
      setFinishingReview(false);
    }
  };

  return (
    <>
      <tr className={`task-grid-row${zebra ? ' task-grid-row--zebra' : ''}`}>
        <td className="task-grid-cell task-grid-cell--index">{formatFaNumber(index + 1)}</td>
        <td className="task-grid-cell task-grid-cell--paper">
          <span>برگ اجرا</span>
          <strong>{executionPaperCode || 'در جریان'}</strong>
        </td>
        <td className="task-grid-cell task-grid-cell--step">
          <strong>{task.name || 'بررسی سازمانی'}</strong>
        </td>
        <td className="task-grid-cell">
          <span className={task.assignee ? 'task-assignee-pill task-assignee-pill--assigned' : 'task-assignee-pill'}>
            {task.assigneeFullName || task.assignee || 'آماده دریافت'}
          </span>
        </td>
        <td className="task-grid-cell">
          <span className="task-role-pill">{candidateLabel}</span>
        </td>
        <td className="task-grid-cell">{formatDate(task.createdAt)}</td>
        <td className="task-grid-cell task-grid-cell--actions">
          <div className="task-table-actions">
            {executionPaperId && (
              <Link className="task-table-detail-action" href={`/execution-papers/${executionPaperId}`}>
                <span className="task-table-detail-action__icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24" role="img">
                    <path d="M7 3.75h6.2L18.25 8.8V20A1.25 1.25 0 0 1 17 21.25H7A1.25 1.25 0 0 1 5.75 20V5A1.25 1.25 0 0 1 7 3.75Z" />
                    <path d="M13 4v5h5" />
                    <path d="M9 13h6M9 16h4" />
                  </svg>
                </span>
                <span>جزئیات برگ اجرا</span>
              </Link>
            )}
            {canClaim && (
              <button className="task-table-primary-action" type="button" onClick={claimTask} disabled={claiming}>
                {claiming ? 'در حال دریافت' : 'دریافت کار'}
              </button>
            )}
            {canDelegate && (
              <div className="task-table-delegate">
                <button className="task-table-secondary-action" type="button" onClick={loadTargets} disabled={loadingTargets}>
                  {loadingTargets ? 'در حال دریافت' : 'نمایش مقصدها'}
                </button>
                {targets.length > 0 && (
                  <>
                    <select value={target} onChange={(event) => setTarget(event.target.value)}>
                      {targets.map((item) => <option value={item.username} key={item.username}>{item.fullName} - {roleLabel(item.businessRole)}</option>)}
                    </select>
                    <button className="task-table-primary-action" type="button" onClick={delegateToUser} disabled={delegating}>
                      {delegating ? 'در حال ارجاع' : 'ارجاع به کاربر'}
                    </button>
                  </>
                )}
              </div>
            )}
            {canFinishReview && (
              <button
                className="task-table-primary-action task-table-primary-action--danger"
                type="button"
                onClick={finishExecutionPaperReview}
                disabled={finishingReview}
              >
                {finishingReview ? 'در حال پایان بررسی' : 'پایان بررسی برگ اجرا'}
              </button>
            )}
          </div>
        </td>
      </tr>
      {message && (
        <tr className="task-grid-message-row">
          <td colSpan={TABLE_HEADERS.length}>
            <div className="delegate-message execution-task-message">{message}</div>
          </td>
        </tr>
      )}
    </>
  );
}

function variableMap(variables = []) {
  return variables.reduce((acc, item) => {
    if (item?.name) {
      acc[item.name] = item.value;
    }
    return acc;
  }, {});
}

function firstValue(values, keys) {
  for (const key of keys) {
    const value = values[key];
    if (value !== undefined && value !== null && value !== '') {
      return value;
    }
  }
  return null;
}

function formatDate(value) {
  return formatJalaliDateTime(value);
}

function roleLabel(value) {
  return {
    GENERAL_OFFICE_HEAD: 'رئیس اداره کل',
    TAX_OFFICE_HEAD: 'رئیس اداره مالیات',
    TAX_EXPERT: 'کارشناس مالیاتی',
    ADMIN: 'مدیر سامانه'
  }[value] || value;
}
