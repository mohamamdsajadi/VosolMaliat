'use client'

import { useEffect, useMemo, useState } from 'react';
import { formatCompactId } from '../lib/display';

export default function IncompleteInformationForm({ data = {}, onComplete, submitting }) {
  const fields = useMemo(() => buildCorrectionFields(data), [data]);
  const editableFields = useMemo(
    () => fields.filter((field) => field.needsReview),
    [fields]
  );
  const [values, setValues] = useState({});

  useEffect(() => {
    setValues(
      editableFields.reduce((result, field) => {
        result[field.key] = formatEditableValue(field.currentValue);
        return result;
      }, {})
    );
  }, [editableFields]);

  const setValue = (key, value) => {
    setValues((current) => ({
      ...current,
      [key]: value
    }));
  };

  const submit = () => {
    const correctedValues = editableFields.reduce((result, field) => {
      result[field.key] = coerceValue(values[field.key], field.currentValue);
      return result;
    }, {});

    onComplete?.(correctedValues);
  };

  if (fields.length === 0) {
    return (
      <div className="empty-state compact">
        اطلاعات قبلی برای تکمیل مجدد پیدا نشد.
      </div>
    );
  }

  if (editableFields.length === 0) {
    return (
      <div className="empty-state compact">
        موردی برای اصلاح توسط کاربر مشخص نشده است.
      </div>
    );
  }

  return (
    <div className="correction-form">
      <div className="review-intro">
        <div>
          <p className="eyebrow">تکمیل مجدد اطلاعات ناقص</p>
          <h4>همه پاسخ‌های قبلی نمایش داده می‌شود، اما فقط موارد نیازمند بررسی قابل ویرایش هستند.</h4>
        </div>
        <span className="status-pill status-pill--ready">
          {`${editableFields.length.toLocaleString('fa-IR')} مورد قابل اصلاح`}
        </span>
      </div>

      <div className="review-field-list">
        {fields.map((field) => (
          <article className={field.needsReview ? 'review-field-row correction-row correction-row--editable' : 'review-field-row correction-row'} key={field.key}>
            <div className="review-field-main">
              <span className="review-field-key">{field.label}</span>
              <strong>{field.displayValue}</strong>
              {!field.needsReview && <small className="correction-readonly-note">تایید شده و غیرقابل ویرایش</small>}
            </div>

            <div className="review-field-controls">
              {field.needsReview ? (
                <>
                  <label className="review-comment-field">
                    <span>پاسخ اصلاح‌شده</span>
                    <textarea
                      value={values[field.key] || ''}
                      onChange={(event) => setValue(field.key, event.target.value)}
                      placeholder="پاسخ جدید را وارد کنید..."
                      disabled={submitting}
                      rows={2}
                    />
                  </label>
                  <div className="admin-comment-box">
                    <span>نظر کارشناس</span>
                    <p>{field.comment || 'کارشناس نظری ثبت نکرده است.'}</p>
                  </div>
                </>
              ) : (
                <div className="status-pill status-pill--submitting">بدون نیاز به اصلاح</div>
              )}
            </div>
          </article>
        ))}
      </div>

      <div className="cf-footer">
        <button className="cf-submit-btn" onClick={submit} disabled={submitting}>
          {submitting ? 'در حال ثبت اصلاحات...' : 'ثبت اصلاحات'}
        </button>
      </div>
    </div>
  );
}

function buildCorrectionFields(data) {
  const evaluationResults = isPlainObject(data.evaluationResults) ? data.evaluationResults : {};
  return Object.entries(data || {})
    .filter(([key]) => !key.startsWith('_') && key !== 'evaluationResults' && key !== 'NEED_REVIEW')
    .map(([key, value]) => {
      const review = isPlainObject(evaluationResults[key]) ? evaluationResults[key] : {};
      return {
        key,
        label: humanizeKey(key),
        currentValue: value,
        displayValue: formatDisplayValue(value),
        needsReview: Boolean(review.needsReview || review.needsInspection),
        comment: typeof review.comment === 'string' ? review.comment.trim() : ''
      };
    });
}

function humanizeKey(key) {
  return String(key || '')
    .replace(/[_-]+/g, ' ')
    .trim() || 'بدون عنوان';
}

function formatEditableValue(value) {
  if (value === null || value === undefined) {
    return '';
  }
  if (typeof value === 'object') {
    return JSON.stringify(value);
  }
  return String(value);
}

function formatDisplayValue(value) {
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

function coerceValue(value, previousValue) {
  if (typeof previousValue === 'number') {
    const numberValue = Number(value);
    return Number.isNaN(numberValue) ? value : numberValue;
  }
  if (typeof previousValue === 'boolean') {
    return value === true || String(value).toLowerCase() === 'true' || value === 'بله';
  }
  if (previousValue && typeof previousValue === 'object') {
    try {
      return JSON.parse(value);
    } catch {
      return value;
    }
  }
  return value;
}

function isPlainObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}
