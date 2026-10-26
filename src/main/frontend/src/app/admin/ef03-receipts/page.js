'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import AppShell from '../../components/app-shell';
import { apiFetch, apiJson, clearAuthTokens } from '../../lib/api';

const initialForm = {
  nationalID: '', xml: '', hash: '', produceDate: '', referenceID: '', followCode: '',
  secondParameter: '', year: '', period: '1', taxOfficeID: '', taxAmount: '',
  taxSource: 'TAXB', announcementType: 'LTR_EF03'
};

const now = () => new Date().toISOString();

function Field({ label, hint, children, wide = false }) {
  return <label className={wide ? 'ef03-field ef03-field--wide' : 'ef03-field'}>
    <span>{label}</span>
    {children}
    {hint && <small>{hint}</small>}
  </label>;
}

export default function Ef03Receipts() {
  const [form, setForm] = useState(initialForm);
  const [samples, setSamples] = useState([]);
  const [selectedSample, setSelectedSample] = useState('');
  const [samplesBusy, setSamplesBusy] = useState(true);
  const [samplesError, setSamplesError] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  const loadSamples = useCallback(async () => {
    setSamplesBusy(true);
    setSamplesError('');
    try {
      setSamples(await apiJson('/api/v1/admin/ef03/samples'));
    } catch (failure) {
      setSamplesError(failure.message);
    } finally {
      setSamplesBusy(false);
    }
  }, []);

  useEffect(() => {
    setForm(current => ({ ...current, produceDate: current.produceDate || now() }));
    loadSamples();
  }, [loadSamples]);

  const change = event => setForm(current => ({
    ...current, [event.target.name]: event.target.value
  }));

  function chooseSample(event) {
    const key = event.target.value;
    setSelectedSample(key);
    const sample = samples.find(item => item.key === key);
    if (!sample) return;
    const { key: ignoredKey, label: ignoredLabel, ...sampleFields } = sample;
    setForm({ ...initialForm, ...sampleFields, year: String(sample.year),
      period: String(sample.period), produceDate: now() });
    setError('');
    setResult(null);
  }

  function clearForm() {
    setSelectedSample('');
    setForm({ ...initialForm, produceDate: now() });
    setError('');
    setResult(null);
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError('');
    setResult(null);
    try {
      const payload = { ...form, year: Number(form.year), period: Number(form.period) };
      if (!payload.hash) delete payload.hash;
      if (!payload.secondParameter) delete payload.secondParameter;
      if (!payload.taxAmount) delete payload.taxAmount;
      const response = await apiFetch('/api/v1/debt-papers/push', {
        method: 'POST', body: JSON.stringify(payload)
      });
      const responseBody = await response.json();
      if (response.status === 401) {
        clearAuthTokens();
        window.location.replace(`/login?next=${encodeURIComponent(window.location.pathname)}`);
        return;
      }
      if (!response.ok && response.status !== 422) {
        throw new Error(responseBody.message || responseBody.error
          || responseBody.apiGatewayMessages?.join('، ') || `درخواست ناموفق بود: ${response.status}`);
      }
      setResult(responseBody);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setBusy(false);
    }
  }

  const executionPaperId = result?.apiGatewayData?.executionPaperID;
  const rejection = result?.apiGatewayData;
  const activeSample = samples.find(item => item.key === selectedSample);

  return <AppShell active="admin" title="ارسال برگه بدهی قطعی EF03">
    <div className="ef03-page">
      <section className="ef03-hero">
        <div>
          <p className="eyebrow">دریافت اطلاعات سنیم</p>
          <h2>صدور برگه اجرا با داده‌های EF03</h2>
          <p>یک نمونه واقعی را انتخاب کنید یا فیلدها و XML را دستی وارد کنید. درخواست مستقیماً به API دریافت برگه بدهی قطعی ارسال می‌شود.</p>
        </div>
        <div className="ef03-hero__stat">
          <strong>{samplesBusy ? '…' : samples.length.toLocaleString('fa-IR')}</strong>
          <span>نمونه آماده</span>
        </div>
      </section>

      <section className="ef03-sample-panel">
        <div className="ef03-panel-heading">
          <div>
            <h3>پرکردن خودکار از EF03-samples.ods</h3>
            <p>انتخاب هر ردیف، XML و فیلدهای متناظر قرارداد را جای‌گذاری می‌کند.</p>
          </div>
          <button className="debt-entry-action debt-entry-action--refresh" type="button"
            onClick={loadSamples} disabled={samplesBusy}>
            {samplesBusy ? 'در حال خواندن…' : 'بارگذاری مجدد'}
          </button>
        </div>
        {samplesError && <div className="notice error">{samplesError}</div>}
        <div className="ef03-sample-picker">
          <label>
            <span>نمونه واقعی</span>
            <select value={selectedSample} onChange={chooseSample} disabled={samplesBusy || samples.length === 0}>
              <option value="">یک نمونه را انتخاب کنید</option>
              {samples.map(sample => <option value={sample.key} key={sample.key}>{sample.label}</option>)}
            </select>
          </label>
          {activeSample && <div className="ef03-sample-summary">
            <span>کد ملی: <b dir="ltr">{activeSample.nationalID}</b></span>
            <span>سال: <b>{activeSample.year}</b></span>
            <span>اداره: <b>{activeSample.taxOfficeID || 'نیازمند ورود دستی'}</b></span>
            <span>مبلغ مالیات: <b dir="ltr">{activeSample.taxAmount}</b></span>
          </div>}
        </div>
      </section>

      {error && <div className="notice error" role="alert">{error}</div>}

      <form className="ef03-form" onSubmit={submit}>
        <section className="ef03-form-section">
          <div className="ef03-section-heading">
            <span>۱</span>
            <div><h3>شناسه‌های درخواست</h3><p>اطلاعات شناسایی مؤدی و درخواست ورودی</p></div>
          </div>
          <div className="ef03-field-grid">
            <Field label="شناسه ملی یا کد ملی" hint="nationalID">
              <input dir="ltr" name="nationalID" value={form.nationalID} onChange={change} required />
            </Field>
            <Field label="شناسه درخواست" hint="referenceID">
              <input dir="ltr" name="referenceID" value={form.referenceID} onChange={change} required />
            </Field>
            <Field label="کد رهگیری ثبت‌نام" hint="followCode">
              <input dir="ltr" name="followCode" value={form.followCode} onChange={change} required />
            </Field>
            <Field label="تاریخ ایجاد" hint="به‌صورت پیش‌فرض زمان فعلی است">
              <input dir="ltr" name="produceDate" value={form.produceDate} onChange={change} required />
            </Field>
          </div>
        </section>

        <section className="ef03-form-section">
          <div className="ef03-section-heading ef03-section-heading--green">
            <span>۲</span>
            <div><h3>اطلاعات مالیاتی</h3><p>سال، دوره، اداره، منبع و مبلغ گزارش‌شده</p></div>
          </div>
          <div className="ef03-field-grid ef03-field-grid--three">
            <Field label="سال"><input dir="ltr" type="number" name="year" value={form.year} onChange={change} required /></Field>
            <Field label="دوره">
              <select name="period" value={form.period} onChange={change} required>
                {[1, 2, 3, 4].map(period => <option value={period} key={period}>{period}</option>)}
              </select>
            </Field>
            <Field label="کد اداره مالیاتی" hint="taxOfficeID">
              <input dir="ltr" name="taxOfficeID" value={form.taxOfficeID} onChange={change} required />
            </Field>
            <Field label="مبلغ مالیات" hint="taxAmount">
              <input dir="ltr" name="taxAmount" value={form.taxAmount} onChange={change} inputMode="decimal" />
            </Field>
            <Field label="منبع مالیاتی" hint="taxSource">
              <input dir="ltr" name="taxSource" value={form.taxSource} onChange={change} required />
            </Field>
            <Field label="نوع برگه" hint="برای این صفحه ثابت است">
              <input dir="ltr" name="announcementType" value={form.announcementType} readOnly />
            </Field>
          </div>
        </section>

        <section className="ef03-form-section">
          <div className="ef03-section-heading ef03-section-heading--indigo">
            <span>۳</span>
            <div><h3>اطلاعات فنی</h3><p>مقادیر تکمیلی پاکت درخواست</p></div>
          </div>
          <div className="ef03-field-grid">
            <Field label="هش XML" hint="برای نمونه‌ها به‌صورت SHA-256 محاسبه می‌شود">
              <input dir="ltr" name="hash" value={form.hash} onChange={change} />
            </Field>
            <Field label="پارامتر دوم" hint="در نمونه‌ها از شناسه ردیف منبع پر می‌شود">
              <input dir="ltr" name="secondParameter" value={form.secondParameter} onChange={change} />
            </Field>
          </div>
        </section>

        <section className="ef03-form-section ef03-xml-section">
          <div className="ef03-section-heading ef03-section-heading--amber">
            <span>۴</span>
            <div><h3>محتوای XML</h3><p>XML کامل برگه بدهی قطعی EF03</p></div>
          </div>
          <Field label="XML درخواست" wide>
            <textarea dir="ltr" name="xml" value={form.xml} onChange={change}
              rows={18} spellCheck={false} required />
          </Field>
        </section>

        <div className="ef03-command-bar">
          <button className="debt-entry-action debt-entry-action--back" type="button" onClick={clearForm}>پاک‌کردن فرم</button>
          <button className="debt-entry-submit debt-entry-submit--workflow" disabled={busy} type="submit">
            {busy ? 'در حال ارسال درخواست…' : 'ارسال درخواست به سیستم'}
          </button>
        </div>
      </form>

      {result && <section className={executionPaperId ? 'ef03-result ef03-result--success' : 'ef03-result'} role="status">
        <div>
          <span>{executionPaperId ? 'صدور موفق' : 'نتیجه فرایند'}</span>
          <h2>{executionPaperId ? 'برگه اجرا صادر شد' : 'برگه اجرا صادر نشد'}</h2>
          <p>{result.apiGatewayMessages?.join('، ')}</p>
          <small>شناسه داخلی برگه اجرا: <b dir="ltr">{executionPaperId || 'ایجاد نشده'}</b></small>
        </div>
        {executionPaperId
          ? <Link className="debt-entry-action debt-entry-action--refresh"
              href={`/execution-papers/${executionPaperId}`}>مشاهده برگه اجرا</Link>
          : <div>
              <span>هنوز شناسه برگه اجرا ایجاد نشده است.</span>
              {rejection?.rejectionReasonCode
                && <p>کد رد: <b dir="ltr">{rejection.rejectionReasonCode}</b></p>}
              {rejection?.rejectionReasonDescription
                && <p>علت رد: {rejection.rejectionReasonDescription}</p>}
              {rejection?.nextRetryDate
                && <p>تاریخ تلاش بعدی: <b dir="ltr">{rejection.nextRetryDate}</b></p>}
            </div>}
      </section>}
    </div>
  </AppShell>;
}
