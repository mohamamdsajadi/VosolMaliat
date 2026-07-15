'use client'

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import AppShell from '../components/app-shell';
import RefreshButton from '../components/refresh-button';
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

const TABLE_HEADERS = ['#', 'فرایند', 'شناسه فرایند', 'کار باز', 'وضعیت', 'عملیات'];

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
      contextItems={[
        `فرایندها: ${formatFaNumber(processDefinitions.length)}`,
        `کارهای باز: ${formatFaNumber(tasks.length)}`,
        `فرایندهای درگیر: ${formatFaNumber(busyProcesses)}`
      ]}
      actions={(
        <RefreshButton onClick={loadOperations} loading={loading}>بروزرسانی دید عملیاتی</RefreshButton>
      )}
    >
      <section className="ops-summary-grid">
        <article className="metric-card">
          <div className="metric-card__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path d="M4 6h16M4 12h10M4 18h7" /></svg>
          </div>
          <div>
            <span>فرایندهای قابل اجرا</span>
            <strong>{formatFaNumber(processDefinitions.length)}</strong>
          </div>
        </article>
        <article className="metric-card metric-card--success">
          <div className="metric-card__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path d="M9 12l2 2 4-5" /><path d="M5 4h14v16H5z" /></svg>
          </div>
          <div>
            <span>کارهای منتظر اقدام</span>
            <strong>{formatFaNumber(tasks.length)}</strong>
          </div>
        </article>
        <article className="metric-card metric-card--warning">
          <div className="metric-card__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path d="M12 8v5l3 2" /><path d="M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg>
          </div>
          <div>
            <span>فرایندهای دارای صف</span>
            <strong>{formatFaNumber(busyProcesses)}</strong>
          </div>
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
          <div className="task-table-card" aria-hidden="true">
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
          <div className="task-table-card">
            <div className="task-table-header">
              <div>
                <p className="eyebrow">فرایندها</p>
                <h2>وضعیت صف‌های عملیاتی</h2>
              </div>
              <span>{formatFaNumber(processDefinitions.length)} مورد</span>
            </div>
            <div className="task-table-scroll">
              <table className="task-grid-table ops-grid-table">
                <thead>
                  <tr>{TABLE_HEADERS.map(header => <th key={header}>{header}</th>)}</tr>
                </thead>
                <tbody>
                  {processDefinitions.map((processDefinition, index) => {
                    const key = processDefinition.processDefinitionKey;
                    const openTasks = tasksByProcess[key] || 0;

                    return (
                      <tr key={key} className={`task-grid-row ${index % 2 ? 'task-grid-row--zebra' : ''}`}>
                        <td className="task-grid-cell task-grid-cell--index">{formatFaNumber(index + 1)}</td>
                        <td className="task-grid-cell task-grid-cell--step">
                          <strong>{getProcessTitle(processDefinition)}</strong>
                          <small>{getProcessSubtitle(processDefinition)}</small>
                        </td>
                        <td className="task-grid-cell fake-service-ltr">{key}</td>
                        <td className="task-grid-cell"><b className="ops-count-pill">{formatFaNumber(openTasks)}</b></td>
                        <td className="task-grid-cell">
                          <span className={`status-pill ${openTasks > 0 ? "status-pill--ready" : "status-pill--submitting"}`}>
                            {openTasks > 0 ? "در جریان" : "بدون صف"}
                          </span>
                        </td>
                        <td className="task-grid-cell task-grid-cell--actions">
                          <Link className="task-table-detail-action" href={`/processes/${encodeURIComponent(key)}/tasks`}>
                            <span className="task-table-detail-action__icon" aria-hidden="true">
                              <svg viewBox="0 0 24 24">
                                <path d="M8 6h13" />
                                <path d="M8 12h13" />
                                <path d="M8 18h13" />
                                <path d="M3 6h.01" />
                                <path d="M3 12h.01" />
                                <path d="M3 18h.01" />
                              </svg>
                            </span>
                            مشاهده صف
                          </Link>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </section>
    </AppShell>
  );
}
