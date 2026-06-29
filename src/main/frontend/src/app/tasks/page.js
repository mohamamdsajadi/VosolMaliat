'use client'

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import AppShell from '../components/app-shell';
import TaskSummaryCard from '../components/task-summary-card';
import {
  formatFaNumber,
  groupTasksByProcess
} from '../lib/display';
import { fetchAllTasks } from '../lib/tasks';

export default function AllTasksPage() {
  const [tasks, setTasks] = useState([]);
  const [loadingTasks, setLoadingTasks] = useState(true);
  const [message, setMessage] = useState("");

  const loadTasks = useCallback(async () => {
    setLoadingTasks(true);
    setMessage("");

    try {
      setTasks(await fetchAllTasks());
    } catch (error) {
      setMessage(error.message || "دریافت کارهای فعال با خطا روبه‌رو شد.");
      setTasks([]);
    } finally {
      setLoadingTasks(false);
    }
  }, []);

  useEffect(() => {
    loadTasks();
  }, [loadTasks]);

  const processGroups = groupTasksByProcess(tasks);
  const processCount = Object.keys(processGroups).length;

  return (
    <AppShell
      active="tasks"
      title="همه کارهای فعال"
      eyebrow="صف مشترک"
      subtitle="دید سراسری از کارهایی که در فرایندهای مختلف منتظر اقدام کارشناس هستند."
      contextItems={[
        `کارهای فعال: ${formatFaNumber(tasks.length)}`,
        `فرایندهای دارای کار: ${formatFaNumber(processCount)}`
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
            <p className="eyebrow">کارتابل کارشناسان</p>
            <h2>کارها به ترتیب آخرین ایجاد</h2>
          </div>
          <Link className="text-action" href="/">شروع پرونده جدید</Link>
        </div>

        {message && <div className="notice error">{message}</div>}

        {loadingTasks ? (
          <div className="task-list" aria-hidden="true">
            {[1, 2, 3, 4].map((item) => (
              <article className="task-summary-card task-summary-card--loading" key={item}>
                <div className="task-summary-main">
                  <span className="task-index skeleton-index" />
                  <div className="task-heading-skeleton">
                    <span className="skeleton-line tiny" />
                    <span className="skeleton-line title" />
                  </div>
                </div>
                <div className="task-summary-side">
                  <span className="status-pill status-pill--loading">در حال دریافت</span>
                  <span className="skeleton-line tiny" />
                </div>
              </article>
            ))}
          </div>
        ) : tasks.length === 0 ? (
          <div className="empty-state">
            <strong>فعلا کاری در صف مشترک نیست.</strong>
            <span>از داشبورد فرایندها یک پرونده شروع کنید یا چند لحظه بعد بروزرسانی کنید.</span>
          </div>
        ) : (
          <div className="task-list">
            {tasks.map((task, index) => (
              <TaskSummaryCard key={task.id} task={task} order={index + 1} />
            ))}
          </div>
        )}
      </section>
    </AppShell>
  );
}
