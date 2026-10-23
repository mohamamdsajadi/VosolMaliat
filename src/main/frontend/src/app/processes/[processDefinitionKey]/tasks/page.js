'use client';
import { businessRoleLabel } from '../../../lib/business-roles';

import Link from 'next/link';
import { useParams, useSearchParams } from 'next/navigation';
import { useCallback, useEffect, useRef, useState } from 'react';
import AppShell from '../../../components/app-shell';
import RefreshButton from '../../../components/refresh-button';
import TaskReferralAction from '../../../components/task-referral/task-referral-action';
import { fetchCurrentUser } from '../../../lib/security';
import {
  formatCompactId,
  formatFaNumber,
  formatJalaliDateTime,
  getDisplayTaskTitle,
  getProcessTitle
} from '../../../lib/display';
import {
  fetchProcessDefinitions,
  fetchTasks,
  startProcessInstance
} from '../../../lib/tasks';

const TABLE_HEADERS = [
  '#',
  'برگ اجرا',
  'مرحله فعلی',
  'مسئول فعلی',
  'نقش مجاز',
  'تاریخ ایجاد',
  'عملیات',
];

export default function ProcessTasksPage() {
  const params = useParams();
  const searchParams = useSearchParams();
  const processDefinitionKey = params?.processDefinitionKey
    ? decodeURIComponent(params.processDefinitionKey)
    : "";
  const startedInstanceKey = searchParams.get('startedInstanceKey');
  const startedInstanceHandled = useRef("");
  const [processDefinition, setProcessDefinition] = useState();
  const [tasks, setTasks] = useState([]);
  const [currentUser, setCurrentUser] = useState(null);
  const [loadingProcess, setLoadingProcess] = useState(true);
  const [loadingTasks, setLoadingTasks] = useState(true);
  const [starting, setStarting] = useState(false);
  const [message, setMessage] = useState("");

  const loadProcess = useCallback(async () => {
    setLoadingProcess(true);

    try {
      const items = await fetchProcessDefinitions();
      setProcessDefinition(
        items.find((item) => item.processDefinitionKey === processDefinitionKey)
      );
    } catch (error) {
      setMessage(error.message || "دریافت اطلاعات فرایند با خطا روبه‌رو شد.");
    } finally {
      setLoadingProcess(false);
    }
  }, [processDefinitionKey]);

  const loadTasks = useCallback(async (options = {}) => {
    const { silent = false } = options;
    if (!silent) {
      setLoadingTasks(true);
    }
    setMessage("");

    try {
      const [items, me] = await Promise.all([fetchTasks(processDefinitionKey), fetchCurrentUser()]);
      setCurrentUser(me);
      setTasks(items);
      return items;
    } catch (error) {
      setMessage(error.message || "دریافت تسک‌های فرایند با خطا روبه‌رو شد.");
      setTasks([]);
      return [];
    } finally {
      if (!silent) {
        setLoadingTasks(false);
      }
    }
  }, [processDefinitionKey]);

  const waitForStartedTask = useCallback(async (processInstanceKey) => {
    setLoadingTasks(true);
    setMessage("");

    try {
      for (let attempt = 0; attempt < 10; attempt += 1) {
        if (attempt > 0) {
          await delay(1000);
        }

        const items = await fetchTasks(processDefinitionKey);
        setTasks(items);

        const hasStartedTask = processInstanceKey
          ? items.some((task) => String(task.processInstanceKey) === String(processInstanceKey))
          : items.length > 0;

        if (hasStartedTask) {
          return items;
        }
      }

      setMessage("فرایند شروع شد، اما کار جدید هنوز در صف عملیاتی آماده نشده است. چند لحظه دیگر بروزرسانی کنید.");
      return [];
    } catch (error) {
      setMessage(error.message || "دریافت کار جدید با خطا روبه‌رو شد.");
      return [];
    } finally {
      setLoadingTasks(false);
    }
  }, [processDefinitionKey]);

  const startProcess = async () => {
    setStarting(true);
    setMessage("");

    try {
      const result = await startProcessInstance(processDefinitionKey);
      await waitForStartedTask(result?.processInstanceKey);
    } catch (error) {
      setMessage(error.message || "شروع فرایند با خطا روبه‌رو شد.");
    } finally {
      setStarting(false);
    }
  };

  useEffect(() => {
    if (processDefinitionKey) {
      loadProcess();
      loadTasks();
    }
  }, [loadProcess, loadTasks, processDefinitionKey]);

  useEffect(() => {
    if (
      processDefinitionKey &&
      startedInstanceKey &&
      startedInstanceHandled.current !== startedInstanceKey
    ) {
      startedInstanceHandled.current = startedInstanceKey;
      waitForStartedTask(startedInstanceKey);
    }
  }, [processDefinitionKey, startedInstanceKey, waitForStartedTask]);

  const title = loadingProcess
    ? "در حال دریافت فرایند"
    : getProcessTitle(processDefinition, "صف کار فرایند");

  return (
    <AppShell
      mobileDrawer
      active="processes"
      title={title}
      eyebrow="صف کار فرایند"
      contextItems={[
        `کلید اجرا: ${processDefinitionKey || '-'}`,
        `نسخه: ${processDefinition?.version ? Number(processDefinition.version).toLocaleString('fa-IR') : '-'}`,
        `کارهای فعال: ${formatFaNumber(tasks.length)}`
      ]}
      actions={(
        <div className="process-page-actions">
          <button className="primary-action process-start-action" onClick={startProcess} disabled={starting || !processDefinitionKey}>
            <span aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <path d="M8 5.5v13l10-6.5Z" />
              </svg>
            </span>
            {starting ? 'در حال شروع...' : 'شروع پرونده جدید'}
          </button>
          <RefreshButton onClick={loadTasks} loading={loadingTasks} disabled={!processDefinitionKey}>بروزرسانی صف</RefreshButton>
        </div>
      )}
    >
      <section className="workflow-panel">
        <div className="workflow-step workflow-step--done">
          <span>۱</span>
          <strong>فرایند انتخاب شد</strong>
          <small>پرونده از تعریف فعال شروع می‌شود.</small>
        </div>
        <div className="workflow-step workflow-step--active">
          <span>۲</span>
          <strong>کارها در صف</strong>
          <small>کارهای انسانی همین فرایند دریافت می‌شوند.</small>
        </div>
        <div className="workflow-step">
          <span>۳</span>
          <strong>فرم مرحله</strong>
          <small>هر کار با فرم خودش تکمیل و ثبت می‌شود.</small>
        </div>
      </section>

      <section className="panel-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">صف کارشناسان</p>
            <h2>کارهای فعال این فرایند</h2>
          </div>
          <Link className="process-back-action" href="/">
            <span aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <path d="M15 6l-6 6 6 6" />
                <path d="M9 12h11" />
              </svg>
            </span>
            بازگشت به داشبورد
          </Link>
        </div>

        {message && <div className="notice error">{message}</div>}

        {loadingTasks ? (
          <div className="task-table-card process-task-table-card" aria-hidden="true">
            <div className="task-table-header">
              <div>
                <p className="eyebrow">در حال دریافت</p>
                <h2>صف کارهای فرایند</h2>
              </div>
              <span>...</span>
            </div>
            <div className="process-task-loading-list">
              {[1, 2, 3].map((item) => (
                <div className="process-task-loading-row" key={item}>
                  <span className="skeleton-line tiny" />
                  <span className="skeleton-line title" />
                  <span className="skeleton-line short" />
                </div>
              ))}
            </div>
          </div>
        ) : tasks.length === 0 ? (
          <div className="empty-state">
            <strong>کاری برای این فرایند وجود ندارد.</strong>
            <span>یک پرونده جدید شروع کنید یا چند لحظه بعد صف کار را بروزرسانی کنید.</span>
          </div>
        ) : (
          <div className="task-table-card process-task-table-card">
            <div className="task-table-header">
              <div>
                <p className="eyebrow">صف فرایند</p>
                <h2>کارهای فعال این فرایند</h2>
              </div>
              <span>{formatFaNumber(tasks.length)} مورد</span>
            </div>
            <div className="task-table-scroll">
              <table className="task-grid-table process-task-grid-table">
                <thead>
                  <tr>
                    {TABLE_HEADERS.map((header) => (
                      <th key={header}>{header}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {tasks.map((task, index) => (
                    <ProcessTaskRow
                      key={task.id}
                      task={task}
                      currentUser={currentUser}
                      index={index}
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

function delay(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

function ProcessTaskRow({ task, index, zebra, currentUser }) {
  const variables = variableMap(task.variables);
  const executionPaperId = firstValue(variables, ['executionPaperId', 'execution_paper_id']);
  const executionPaperCode = firstValue(variables, ['executionPaperCode', 'executionPaperUniqueCode', 'execution_paper_code']);
  const taskId = task?.id || task?.taskKey;
  const processDefinitionKey = task?.processDefinitionKey;
  const taskHref = processDefinitionKey
    ? `/tasks/${taskId}?processDefinitionKey=${encodeURIComponent(processDefinitionKey)}`
    : `/tasks/${taskId}`;
  const candidateLabel = task.candidateGroups?.length
    ? task.candidateGroups.map(roleLabel).join('، ')
    : 'بدون گروه کاندید';

  return (
    <tr className={`task-grid-row${zebra ? ' task-grid-row--zebra' : ''}`}>
      <td className="task-grid-cell task-grid-cell--index">{formatFaNumber(index + 1)}</td>
      <td className="task-grid-cell task-grid-cell--paper">
        <span>برگ اجرا</span>
        <strong>{executionPaperCode || 'در جریان'}</strong>
      </td>
      <td className="task-grid-cell task-grid-cell--step">
        <strong>{getDisplayTaskTitle(task, 'بررسی سازمانی')}</strong>
        <small>کار {formatCompactId(taskId)}</small>
      </td>
      <td className="task-grid-cell">
        <span className={task.assignee ? 'task-assignee-pill task-assignee-pill--assigned' : 'task-assignee-pill'}>
          {task.assigneeFullName || task.assignee || 'آماده دریافت'}
        </span>
      </td>
      <td className="task-grid-cell">
        <span className="task-role-pill">{candidateLabel}</span>
      </td>
      <td className="task-grid-cell">{formatJalaliDateTime(task.createdAt)}</td>
      <td className="task-grid-cell task-grid-cell--actions">
        <div className="task-table-actions process-task-actions">
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
          <TaskReferralAction task={task} user={currentUser} processDefinitionKey={processDefinitionKey} />
          <Link className="task-table-secondary-link" href={taskHref}>
            باز کردن کار
          </Link>
        </div>
      </td>
    </tr>
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

function roleLabel(value) { return businessRoleLabel(value); }
