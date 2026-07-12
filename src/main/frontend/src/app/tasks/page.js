'use client'

import { useCallback, useEffect, useState } from 'react';
import AppShell from '../components/app-shell';
import { formatFaNumber, formatCompactId } from '../lib/display';
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
            <p className="eyebrow">Inbox</p>
            <h2>{isAdmin ? 'همه کارها' : 'کارهای اختصاص‌یافته به من'}</h2>
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
            {tasks.map((task, index) => (
              <CartableTaskCard key={task.taskKey} task={task} order={index + 1} onChanged={loadTasks} readOnly={isAdmin} />
            ))}
          </div>
        )}
      </section>
    </AppShell>
  );
}

function CartableTaskCard({ task, order, onChanged, readOnly }) {
  const [targets, setTargets] = useState([]);
  const [target, setTarget] = useState('');
  const [loadingTargets, setLoadingTargets] = useState(false);
  const [message, setMessage] = useState('');
  const [claiming, setClaiming] = useState(false);
  const canClaim = task.allowedActions?.claim && !readOnly;
  const canDelegate = task.allowedActions?.delegate && !readOnly;

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
    <article className="task-summary-card">
      <div className="task-summary-main">
        <span className="task-index">{Number(order || 1).toLocaleString('fa-IR')}</span>
        <div>
          <p className="eyebrow">{task.processDefinitionId || 'فرایند'}</p>
          <h3>{task.name || 'کار فعال'}</h3>
          <p className="summary-copy">
            شناسه کار: {formatCompactId(task.taskKey)} · مسئول فعلی: {task.assignee || 'بدون مسئول'}
          </p>
          {task.candidateGroups?.length > 0 && <p className="summary-copy">گروه‌های کاندید: {task.candidateGroups.join('، ')}</p>}
        </div>
      </div>
      <div className="task-summary-side">
        <span className="status-pill status-pill--ready">{readOnly ? 'فقط مشاهده' : 'آماده اقدام'}</span>
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
            {message && <span className="delegate-message">{message}</span>}
          </div>
        )}
      </div>
    </article>
  );
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
