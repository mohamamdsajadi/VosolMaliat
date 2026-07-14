'use client'

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import AppShell from '../components/app-shell';
import { formatFaNumber } from '../lib/display';
import { claimCartableTask, delegateTask, fetchCartableTasks, fetchDelegationTargets } from '../lib/tasks';
import { fetchCurrentUser } from '../lib/security';

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
      subtitle={isAdmin ? "مدیر سامانه کارها را فقط به صورت خواندنی مشاهده می‌کند." : "فقط کارهای اختصاص‌یافته و مجاز در محدوده سازمانی شما نمایش داده می‌شود."}
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
        <div className="section-heading">
          <div>
            <h2>{isAdmin ? 'کار های فعال' : 'کارهای اختصاص‌یافته به من'}</h2>
          </div>
        </div>

        {message && <div className="notice error">{message}</div>}

        {loadingTasks ? (
          <div className="empty-state compact">در حال دریافت کارتابل...</div>
        ) : tasks.length === 0 ? (
          <div className="empty-state">
            <strong>فعلا کاری در کارتابل وجود ندارد.</strong>
            <span>بعد از ایجاد یا ارجاع کار، این فهرست را بروزرسانی کنید.</span>
          </div>
        ) : (
          <div className="task-list">
            {tasks.map((task) => (
              <CartableTaskCard key={task.taskKey} task={task} onChanged={loadTasks} readOnly={isAdmin} />
            ))}
          </div>
        )}
      </section>
    </AppShell>
  );
}

function CartableTaskCard({ task, onChanged, readOnly }) {
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
  const statusLabel = readOnly
    ? 'مشاهده مدیریتی'
    : task.assignee
      ? 'اختصاص‌یافته'
      : 'در انتظار دریافت';

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
    <article className="execution-task-card">
      <div className="execution-task-card__content">
        <div className="execution-task-card__header">
          <div>
            <p className="eyebrow">برگ اجرا</p>
            <h3>{executionPaperCode ? `برگ اجرا ${executionPaperCode}` : task.name || 'برگ اجرای در جریان'}</h3>
          </div>
          <span className={`task-state-badge ${task.assignee ? 'task-state-badge--assigned' : 'task-state-badge--candidate'}`}>
            {statusLabel}
          </span>
        </div>

        <div className="execution-task-meta">
          <MetaItem label="مرحله فعلی" value={task.name || 'بررسی سازمانی'} />
          <MetaItem label="مسئول فعلی" value={task.assignee || 'آماده دریافت توسط نقش مجاز'} />
          <MetaItem label="نقش مجاز" value={candidateLabel} />
          <MetaItem label="تاریخ ایجاد" value={formatDate(task.createdAt)} />
        </div>

        {message && <div className="delegate-message execution-task-message">{message}</div>}
      </div>

      <div className="execution-task-actions">
        {executionPaperId && (
          <Link className="task-detail-action" href={`/execution-papers/${executionPaperId}`}>
            <span className="task-detail-action__icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" role="img">
                <path d="M7 3.75h6.2L18.25 8.8V20A1.25 1.25 0 0 1 17 21.25H7A1.25 1.25 0 0 1 5.75 20V5A1.25 1.25 0 0 1 7 3.75Z" />
                <path d="M13 4v5h5" />
                <path d="M9 13h6M9 16h4" />
              </svg>
            </span>
            <span>مشاهده جزئیات برگ اجرا</span>
          </Link>
        )}
        {canClaim && (
          <button className="primary-action" type="button" onClick={claimTask} disabled={claiming}>
            {claiming ? 'در حال دریافت' : 'دریافت کار'}
          </button>
        )}
        {canDelegate && (
          <div className="delegate-box">
            <button className="ghost-action" type="button" onClick={loadTargets} disabled={loadingTargets}>
              {loadingTargets ? 'در حال دریافت' : 'نمایش مقصدها'}
            </button>
            {targets.length > 0 && (
              <>
                <select value={target} onChange={(event) => setTarget(event.target.value)}>
                  {targets.map((item) => <option value={item.username} key={item.username}>{item.fullName} - {roleLabel(item.businessRole)}</option>)}
                </select>
                <button className="primary-action" type="button" onClick={delegateToUser}>ارجاع به کاربر</button>
              </>
            )}
            <button className="ghost-action" type="button" onClick={delegateToGroup}>ارجاع به نقش بعدی</button>
          </div>
        )}
      </div>
    </article>
  );
}

function MetaItem({ label, value }) {
  return (
    <div className="execution-task-meta__item">
      <span>{label}</span>
      <strong>{value || '-'}</strong>
    </div>
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
