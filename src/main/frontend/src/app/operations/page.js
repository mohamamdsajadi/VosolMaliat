'use client'

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import AppShell from '../components/app-shell';
import {
  formatFaNumber,
  getProcessSubtitle,
  getProcessTitle,
  groupTasksByProcess
} from '../lib/display';
import {
  fetchAllTasks,
  fetchProcessDefinitions
} from '../lib/tasks';

export default function OperationsPage() {
  const [processDefinitions, setProcessDefinitions] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  const loadOperations = useCallback(async () => {
    setLoading(true);
    setMessage("");

    const [processResult, taskResult] = await Promise.allSettled([
      fetchProcessDefinitions(),
      fetchAllTasks()
    ]);

    if (processResult.status === "fulfilled") {
      setProcessDefinitions(processResult.value);
    } else {
      setProcessDefinitions([]);
      setMessage(processResult.reason?.message || "دریافت فرایندها با خطا روبه‌رو شد.");
    }

    if (taskResult.status === "fulfilled") {
      setTasks(taskResult.value);
    } else {
      setTasks([]);
      setMessage((current) => current || taskResult.reason?.message || "دریافت کارهای فعال با خطا روبه‌رو شد.");
    }

    setLoading(false);
  }, []);

  useEffect(() => {
    loadOperations();
  }, [loadOperations]);

  const tasksByProcess = groupTasksByProcess(tasks);
  const busyProcesses = Object.values(tasksByProcess).filter(Boolean).length;

  return (
    <AppShell
      active="operations"
      title="دید عملیاتی کارتابل"
      eyebrow="پایش سبک"
      subtitle="نمای مدیریتی برای فهمیدن اینکه کدام فرایندها آماده اجرا هستند و کدام‌یک کار باز در صف دارند."
      contextItems={[
        `فرایندها: ${formatFaNumber(processDefinitions.length)}`,
        `کارهای باز: ${formatFaNumber(tasks.length)}`,
        `فرایندهای درگیر: ${formatFaNumber(busyProcesses)}`
      ]}
      actions={(
        <button className="ghost-action" onClick={loadOperations} disabled={loading}>
          {loading ? 'در حال بروزرسانی' : 'بروزرسانی دید عملیاتی'}
        </button>
      )}
    >
      <section className="ops-summary-grid">
        <article className="metric-card">
          <span>فرایندهای قابل اجرا</span>
          <strong>{formatFaNumber(processDefinitions.length)}</strong>
        </article>
        <article className="metric-card metric-card--success">
          <span>کارهای منتظر اقدام</span>
          <strong>{formatFaNumber(tasks.length)}</strong>
        </article>
        <article className="metric-card metric-card--warning">
          <span>فرایندهای دارای صف</span>
          <strong>{formatFaNumber(busyProcesses)}</strong>
        </article>
      </section>

      <section className="panel-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">ظرفیت فرایندها</p>
            <h2>نمای عملیاتی بر اساس صف کار</h2>
          </div>
          <Link className="text-action" href="/tasks">رفتن به همه کارها</Link>
        </div>

        {message && <div className="notice error">{message}</div>}

        {loading ? (
          <div className="operation-table" aria-hidden="true">
            {[1, 2, 3].map((item) => (
              <div className="operation-row operation-row--loading" key={item}>
                <span className="skeleton-line title" />
                <span className="skeleton-line short" />
                <span className="skeleton-line tiny" />
              </div>
            ))}
          </div>
        ) : processDefinitions.length === 0 ? (
          <div className="empty-state">
            <strong>اطلاعات عملیاتی هنوز آماده نیست.</strong>
            <span>اتصال API را بررسی کنید یا صفحه را بروزرسانی کنید.</span>
          </div>
        ) : (
          <div className="operation-table">
            {processDefinitions.map((processDefinition) => {
              const key = processDefinition.processDefinitionKey;
              const openTasks = tasksByProcess[key] || 0;

              return (
                <Link
                  className="operation-row"
                  href={`/processes/${encodeURIComponent(key)}/tasks`}
                  key={key}
                >
                  <div>
                    <p className="eyebrow">فرایند</p>
                    <strong>{getProcessTitle(processDefinition)}</strong>
                    <span>{getProcessSubtitle(processDefinition)}</span>
                  </div>
                  <div className="operation-stat">
                    <small>کار باز</small>
                    <b>{formatFaNumber(openTasks)}</b>
                  </div>
                  <span className={`status-pill ${openTasks > 0 ? "status-pill--ready" : "status-pill--submitting"}`}>
                    {openTasks > 0 ? "در جریان" : "بدون صف"}
                  </span>
                </Link>
              );
            })}
          </div>
        )}
      </section>
    </AppShell>
  );
}
