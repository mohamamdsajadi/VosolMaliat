'use client'

import dynamic from 'next/dynamic'
import { useCallback, useEffect, useState } from "react";
import {
  formatCompactId,
  getDisplayTaskTitle
} from '../lib/display';
import EvaluationReviewForm from './evaluation-review-form';
import IncompleteInformationForm from './incomplete-information-form';
import TaskReferralForm from './task-referral/task-referral-form';
import { fetchCurrentUser } from '../lib/security';
import { completeTask, fetchTask, hasRenderableSchema, isEvaluationReviewTask, isExecutionPaperReviewTask, isIncompleteInformationTask } from '../lib/tasks';

const ProcessForm = dynamic(
  () => import('./embedded-form'),
  { ssr: false, loading: () => <TaskSkeleton /> }
)

export default function Task({ taskId, order, onDone, intent }) {
  const [task, setTask] = useState();
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [currentUser, setCurrentUser] = useState(null);
  const [revision, setRevision] = useState(0);
  const hasError = Boolean(error);
  const isUnavailable = !loading && !task;
  const state = loading
    ? "loading"
    : hasError || isUnavailable
      ? "error"
      : submitting
        ? "submitting"
        : "ready";
  const statusLabel = {
    loading: "در حال دریافت",
    ready: "آماده اقدام",
    submitting: "در حال ارسال",
    error: "نیازمند بررسی"
  }[state];
  const fallbackTitle = "اطلاعات درخواست در دسترس نیست";
  const title = loading
    ? "در حال دریافت اطلاعات"
    : getDisplayTaskTitle(task, fallbackTitle);

  const submitTask = useCallback(async (data) => {
    setSubmitting(true);
    setError("");

    try {
      await completeTask(taskId, data);
      await onDone?.();
    } catch (err) {
      setError(err.message || "در ارسال اطلاعات خطایی رخ داد.");
    } finally {
      setSubmitting(false);
    }
  }, [onDone, taskId]);

  useEffect(() => {
    const loadTask = async () => {
      setLoading(true);
      setError("");
      setTask(undefined);

      try {
        const [result, user] = await Promise.all([fetchTask(taskId), intent === 'referral' ? fetchCurrentUser() : Promise.resolve(null)]);
        setTask(result); setCurrentUser(user);

      } catch (err) {
        setError(err.message || "خطایی در دریافت اطلاعات رخ داد.");
      } finally {
        setLoading(false);
      }
    };

    if (taskId) {
      loadTask();
    }
  }, [taskId, intent, revision]);

  return (
    <article className={`task-card task-card--${state}`}>
      <div className="task-card-header">
        <div className="task-title-block">
          <span className="task-index" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path d="M8 6h8" />
              <path d="M8 12h8" />
              <path d="M8 18h5" />
              <path d="M5 6h.01" />
              <path d="M5 12h.01" />
              <path d="M5 18h.01" />
            </svg>
          </span>
          <div>
            <p className="eyebrow">{state === "error" ? "وضعیت درخواست" : "مرحله فعال"}</p>
            <h3>{title}</h3>
            <p className="task-card-copy">
              {taskId ? `شناسه کار: ${formatCompactId(taskId)}` : "شناسه کار در دسترس نیست"}
            </p>
          </div>
        </div>
        <span className={`status-pill status-pill--${state}`}>{statusLabel}</span>
      </div>

      {error && <div className="notice error">{error}</div>}

      {loading ? (
        <TaskSkeleton />
      ) : intent === 'referral' ? (
        <TaskReferralForm task={task} user={currentUser} onComplete={submitTask} onReload={() => setRevision(value => value + 1)} submitting={submitting} />
      ) : isEvaluationReviewTask(task) ? (
        <div className="form-stage">
          <EvaluationReviewForm
            data={task.data}
            onComplete={submitTask}
            submitting={submitting}
          />
        </div>
      ) : isIncompleteInformationTask(task) ? (
        <div className="form-stage">
          <IncompleteInformationForm
            data={task.data}
            onComplete={submitTask}
            submitting={submitting}
          />
        </div>
      ) : isExecutionPaperReviewTask(task) ? (
        <div className="form-stage">
          <div className="form-stage-header">
            <div>
              <p className="eyebrow">تصمیم نهایی این مرحله</p>
              <h4>پایان بررسی سازمانی برگ اجرا</h4>
            </div>
            <span className={`status-pill ${task.allowedActions?.complete ? 'status-pill--ready' : 'status-pill--muted'}`}>
              {task.allowedActions?.complete ? 'قابل اقدام' : 'ابتدا کار را دریافت کنید'}
            </span>
          </div>
          <p className="task-card-copy">
            این اقدام مراحل سازمانی باقی‌مانده را رد می‌کند و شاخه بررسی را مستقیماً به محل اتصال فرایند می‌فرستد. سایر شاخه‌های موازی باید طبق روال خود تکمیل شوند.
          </p>
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 18 }}>
            <button
              className="primary-action primary-action--danger"
              type="button"
              disabled={submitting || !task.allowedActions?.complete}
              onClick={() => {
                if (window.confirm('آیا از پایان بررسی برگ اجرا و رد کردن مراحل سازمانی باقی‌مانده مطمئن هستید؟')) {
                  submitTask({ finishExecutionPaperCheck: true });
                }
              }}
            >
              {submitting ? 'در حال ثبت تصمیم...' : 'پایان بررسی برگ اجرا'}
            </button>
          </div>
        </div>
      ) : hasRenderableSchema(task) ? (
        <div className="form-stage">
          <div className="form-stage-header">
            <div>
              <p className="eyebrow">فرم عملیاتی</p>
              <h4>اطلاعات لازم برای تکمیل این مرحله</h4>
            </div>
            <span className="status-pill status-pill--ready">
              {submitting ? "در حال ثبت" : "قابل ثبت"}
            </span>
          </div>
          <ProcessForm
            schema={task.schema}
            data={task.data}
            onComplete={submitTask}
            submitting={submitting}
          />
        </div>
      ) : (
        <div className="empty-state compact">این درخواست در صف قابل نمایش نیست.</div>
      )}
    </article>
  )
}

function TaskSkeleton() {
  return (
    <div className="task-skeleton" aria-hidden="true">
      <div className="skeleton-grid">
        <span className="skeleton-line medium" />
        <span className="skeleton-line short" />
      </div>
      <span className="skeleton-input" />
      <span className="skeleton-input" />
      <span className="skeleton-area" />
      <span className="skeleton-button" />
    </div>
  );
}
