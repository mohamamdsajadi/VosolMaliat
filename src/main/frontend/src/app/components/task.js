'use client'

import dynamic from 'next/dynamic'
import { useCallback, useEffect, useState } from "react";
import {
  formatCompactId,
  getDisplayTaskTitle
} from '../lib/display';
import EvaluationReviewForm from './evaluation-review-form';
import IncompleteInformationForm from './incomplete-information-form';
import { completeTask, fetchTask, hasRenderableSchema, isEvaluationReviewTask, isIncompleteInformationTask } from '../lib/tasks';

const ProcessForm = dynamic(
  () => import('./embedded-form'),
  { ssr: false, loading: () => <TaskSkeleton /> }
)

export default function Task({ taskId, order, onDone }) {
  const [task, setTask] = useState();
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
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
        setTask(await fetchTask(taskId));

      } catch (err) {
        setError(err.message || "خطایی در دریافت اطلاعات رخ داد.");
      } finally {
        setLoading(false);
      }
    };

    if (taskId) {
      loadTask();
    }
  }, [taskId]);

  return (
    <article className={`task-card task-card--${state}`}>
      <div className="task-card-header">
        <div className="task-title-block">
          <span className="task-index">{Number(order || 1).toLocaleString('fa-IR')}</span>
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

      <div className="task-workflow-note">
        <span>۱. دریافت اطلاعات</span>
        <span>۲. تکمیل فرم مرحله</span>
        <span>۳. ثبت نتیجه و بازگشت به صف</span>
      </div>

      {error && <div className="notice error">{error}</div>}

      {loading ? (
        <TaskSkeleton />
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
