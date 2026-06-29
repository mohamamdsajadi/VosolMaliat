'use client'

import { useEffect, useMemo, useState } from 'react';
import { formatCompactId } from '../lib/display';

const DECISION_APPROVED = 'approved';
const DECISION_NEEDS_REVIEW = 'needsReview';

export default function EvaluationReviewForm({ data = {}, onComplete, submitting }) {
  const fields = useMemo(() => buildReviewFields(data), [data]);
  const [decisions, setDecisions] = useState({});
  const [comments, setComments] = useState({});
  const remainingCount = fields.filter((field) => !decisions[field.key]).length;

  useEffect(() => {
    setDecisions({});
    setComments({});
  }, [fields]);

  const setDecision = (key, decision) => {
    setDecisions((current) => ({
      ...current,
      [key]: current[key] === decision ? undefined : decision
    }));
  };

  const setComment = (key, comment) => {
    setComments((current) => ({
      ...current,
      [key]: comment
    }));
  };

  const submit = () => {
    if (remainingCount > 0) {
      return;
    }

    const evaluationResults = fields.reduce((result, field) => {
      const decision = decisions[field.key];
      result[field.key] = {
        value: field.rawValue,
        approved: decision === DECISION_APPROVED,
        needsInspection: decision === DECISION_NEEDS_REVIEW,
        needsReview: decision === DECISION_NEEDS_REVIEW,
        decision,
        comment: decision === DECISION_NEEDS_REVIEW ? comments[field.key]?.trim() || "" : ""
      };
      return result;
    }, {});

    const NEED_REVIEW = fields.some((field) => decisions[field.key] === DECISION_NEEDS_REVIEW);

    onComplete?.({ evaluationResults, NEED_REVIEW });
  };

  if (fields.length === 0) {
    return (
      <div className="empty-state compact">
        داده‌ای از مرحله ارزیابی برای بازبینی پیدا نشد.
      </div>
    );
  }

  return (
    <div className="evaluation-review-form">
      <div className="review-intro">
        <div>
          <p className="eyebrow">فرم اختصاصی بازبینی</p>
          <h4>برای هر مقدار ثبت‌شده وضعیت تایید یا نیاز به بررسی را مشخص کنید.</h4>
        </div>
        <span className="status-pill status-pill--ready">
          {remainingCount > 0 ? `${remainingCount.toLocaleString('fa-IR')} مورد بدون تصمیم` : 'همه موارد بررسی شد'}
        </span>
      </div>

      <div className="review-field-list">
        {fields.map((field) => (
          <article className="review-field-row" key={field.key}>
            <div className="review-field-main">
              <span className="review-field-key">{field.label}</span>
              <strong>{field.displayValue}</strong>
            </div>
            <div className="review-field-controls">
              <div className="review-actions" role="group" aria-label={`بازبینی ${field.label}`}>
                <label className={decisions[field.key] === DECISION_APPROVED ? 'review-choice review-choice--selected' : 'review-choice'}>
                  <input
                    type="checkbox"
                    checked={decisions[field.key] === DECISION_APPROVED}
                    onChange={() => setDecision(field.key, DECISION_APPROVED)}
                    disabled={submitting}
                  />
                  تایید
                </label>
                <label className={decisions[field.key] === DECISION_NEEDS_REVIEW ? 'review-choice review-choice--selected' : 'review-choice'}>
                  <input
                    type="checkbox"
                    checked={decisions[field.key] === DECISION_NEEDS_REVIEW}
                    onChange={() => setDecision(field.key, DECISION_NEEDS_REVIEW)}
                    disabled={submitting}
                  />
                  نیاز به بررسی
                </label>
              </div>
              <label className="review-comment-field">
                <span>نظر کارشناس</span>
                <textarea
                  value={comments[field.key] || ''}
                  onChange={(event) => setComment(field.key, event.target.value)}
                  placeholder="توضیح یا دلیل تصمیم را بنویسید..."
                  disabled={submitting || decisions[field.key] !== DECISION_NEEDS_REVIEW}
                  rows={2}
                />
              </label>
            </div>
          </article>
        ))}
      </div>

      <div className="cf-footer">
        <button className="cf-submit-btn" onClick={submit} disabled={submitting || remainingCount > 0}>
          {submitting ? 'در حال ثبت بازبینی...' : 'ثبت نتیجه بازبینی'}
        </button>
      </div>
    </div>
  );
}

function buildReviewFields(data) {
  return Object.entries(data || {})
    .filter(([key]) => !key.startsWith('_') && key !== 'evaluationResults')
    .map(([key, value]) => ({
      key,
      label: humanizeKey(key),
      rawValue: value,
      displayValue: formatValue(value)
    }));
}

function humanizeKey(key) {
  return String(key || '')
    .replace(/[_-]+/g, ' ')
    .trim() || 'بدون عنوان';
}

function formatValue(value) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  if (typeof value === 'boolean') {
    return value ? 'بله' : 'خیر';
  }
  if (typeof value === 'object') {
    return JSON.stringify(value);
  }
  return formatCompactId(value);
}
