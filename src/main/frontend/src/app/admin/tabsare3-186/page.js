'use client'

import Link from 'next/link';
import { useState } from 'react';
import AppShell from '../../components/app-shell';
import { apiJson } from '../../lib/api';

const INITIAL_FORM = {
  executionPaperId: '',
  legalPersonNationalId: '14001234567',
  debtPaperDate: '1405-01-01'
};

export default function Tabsare3186TestPage() {
  const [form, setForm] = useState(INITIAL_FORM);
  const [result, setResult] = useState(null);
  const [message, setMessage] = useState('');
  const [starting, setStarting] = useState(false);

  const updateForm = (event) => {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  };

  const startProcess = async (event) => {
    event.preventDefault();
    setStarting(true);
    setMessage('');
    setResult(null);
    try {
      const response = await apiJson('/api/v1/test/tabsare3-186/start', {
        method: 'POST',
        body: JSON.stringify({
          executionPaperId: Number(form.executionPaperId),
          legalPersonNationalId: form.legalPersonNationalId.trim(),
          debtPaperDate: form.debtPaperDate.trim()
        })
      });
      setResult(response);
      setMessage(response.message || 'فرایند آزمایشی آغاز شد.');
    } catch (error) {
      setMessage(error.message || 'شروع فرایند آزمایشی ناموفق بود.');
    } finally {
      setStarting(false);
    }
  };

  return (
    <AppShell
      active="test-front"
      title="آزمایش فرایند تبصره ۳ ماده ۱۸۶"
      eyebrow="راه‌اندازی نمونه فرایند"
      contextItems={[
        'شناسه BPMN: Process_0eqvh8j',
        'پاسخ روزنامه رسمی: غیرهمزمان'
      ]}
      actions={<Link className="text-action" href="/operations">بازگشت به دید عملیاتی</Link>}
    >
      {message && (
        <div className={result ? 'notice success' : 'notice error'}>{message}</div>
      )}

      <section className="panel-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">داده آزمایشی</p>
            <h2>شروع Tabsare3-186</h2>
          </div>
        </div>

        <form className="admin-form wide-form" onSubmit={startProcess}>
          <div className="form-grid">
            <label>
              <span>شناسه برگ اجرا</span>
              <input
                inputMode="numeric"
                name="executionPaperId"
                value={form.executionPaperId}
                onChange={updateForm}
                placeholder="برای نمونه: 100"
                required
              />
            </label>
            <label>
              <span>شناسه ملی شخص حقوقی</span>
              <input
                name="legalPersonNationalId"
                value={form.legalPersonNationalId}
                onChange={updateForm}
                required
              />
            </label>
            <label>
              <span>تاریخ برگ بدهی</span>
              <input
                name="debtPaperDate"
                value={form.debtPaperDate}
                onChange={updateForm}
                placeholder="1405-01-01"
                required
              />
            </label>
          </div>

          <div className="row-actions">
            <button className="primary-action" type="submit" disabled={starting}>
              {starting ? 'در حال راه‌اندازی...' : 'شروع فرایند آزمایشی'}
            </button>
          </div>
        </form>
      </section>

      {result && (
        <section className="panel-section">
          <div className="section-heading">
            <div>
              <p className="eyebrow">نتیجه راه‌اندازی</p>
              <h2>شناسه‌های پیگیری</h2>
            </div>
          </div>
          <div className="admin-control-stats">
            <article>
              <span>کلید نمونه فرایند</span>
              <strong>{result.processInstanceKey || '-'}</strong>
            </article>
            <article>
              <span>شناسه درخواست روزنامه رسمی</span>
              <strong className="fake-service-ltr">{result.officialGazetteRequestId || '-'}</strong>
            </article>
          </div>
        </section>
      )}
    </AppShell>
  );
}
