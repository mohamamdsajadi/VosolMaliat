'use client'

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import AppShell from '../components/app-shell';
import { formatFaNumber } from '../lib/display';
import { claimCartableTask, delegateTask, fetchCartableTasks, fetchDelegationTargets } from '../lib/tasks';
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

export default function AllTasksPage() {
  const [tasks, setTasks] = useState([]);
  const [currentUser, setCurrentUser] = useState(null);
  const [loadingTasks, setLoadingTasks] = useState(true);
  const [message, setMessage] = useState("");

  const loadTasks = useCallback(async () => {
    setLoadingTasks(true);
    setMessage("");
    try {
      const [me, taskResult] = await Promise.all([fetchCurrentUser(), fetchCartableTasks()]);
      setCurrentUser(me);
      setTasks(taskResult);
    } catch (error) {
      setMessage(error.message || "دریافت کارتابل با خطا روبه‌رو شد.");
      setTasks([]);
    } finally {
      setLoadingTasks(false);
    }
  }, []);

  useEffect(() => {
    loadTasks();
  }, [loadTasks]);

  const isAdmin = currentUser?.businessRole === 'ADMIN' || currentUser?.allowedActions?.includes('ADMIN');

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
        <button className="ghost-action" onClick={loadTasks} disabled={loadingTasks}>
          {loadingTasks ? 'در حال بروزرسانی' : 'بروزرسانی صف'}
        </button>
      )}
    >
      <section className="panel-section">
        {message && <div className="notice error">{message}</div>}

        {loadingTasks ? (
          <div className="empty-state compact">در حال دریافت کارتابل...</div>
        ) : tasks.length === 0 ? (
          <div className="empty-state">
            <strong>فعلا کاری در کارتابل وجود ندارد.</strong>
            <span>بعد از ایجاد یا ارجاع کار، این فهرست را بروزرسانی کنید.</span>
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

function CartableTaskRow({ task, index, onChanged, readOnly, zebra }) {
  const [targets, setTargets] = useState([]);
  const [target, setTarget] = useState('');
  const [loadingTargets, setLoadingTargets] = useState(false);
  const [message, setMessage] = useState('');
  const [claiming, setClaiming] = useState(false);
  const canClaim = task.allowedActions?.claim && !readOnly;
  const canDelegate = task.allowedActions?.delegate && !readOnly;
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
    await delegateTask(task.taskKey, { assignee: target });
    await onChanged();
  };

  const delegateToGroup = async () => {
    await delegateTask(task.taskKey, {});
    await onChanged();
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
            {task.assignee || 'آماده دریافت'}
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
                    <button className="task-table-primary-action" type="button" onClick={delegateToUser}>ارجاع به کاربر</button>
                  </>
                )}
                <button className="task-table-secondary-action" type="button" onClick={delegateToGroup}>ارجاع به نقش بعدی</button>
              </div>
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
  if (!value) {
    return '-';
  }
  try {
    return new Intl.DateTimeFormat('fa-IR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(value));
  } catch {
    return value;
  }
}

function roleLabel(value) {
  return {
    GENERAL_OFFICE_HEAD: 'رئیس اداره کل',
    TAX_OFFICE_HEAD: 'رئیس اداره مالیات',
    TAX_UNIT_HEAD: 'رئیس واحد مالیاتی',
    TAX_EXPERT: 'کارشناس مالیاتی',
    ADMIN: 'مدیر سامانه'
  }[value] || value;
}
