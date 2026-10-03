'use client'

import Link from 'next/link';
import { apiJson } from '../../../lib/api';
import { useCallback, useEffect, useState } from 'react';
import AppShell from '../../../components/app-shell';
import RefreshButton from '../../../components/refresh-button';
import {
  createFinalizedDebtPaper,
  fetchAdminDataSummary,
  sendFinalizedDebtPaper
} from '../../../lib/admin-data-entry';
import { fetchGeneralDirectorates, fetchTaxAffairsOffices } from '../../../lib/security';
import {
  FieldGrid,
  Input,
  MoneyFields,
  Select,
  initialDebtPaper,
  randomDebtPaperSample,
  toDebtPaperPayload
} from '../form-parts';

export default function FinalizedDebtPaperEntryPage() {
  const [form, setForm] = useState(initialDebtPaper);
  const [summary, setSummary] = useState({ finalizedDebtPapers: [] });
  const [masterdata, setMasterdata] = useState({ generals: [], offices: [], activities: [] });
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setMessage('');
    try {
      const [summaryResult, generalResult, officeResult, activities] = await Promise.all([
        fetchAdminDataSummary(),
        fetchGeneralDirectorates(),
        fetchTaxAffairsOffices(),
        apiJson('/api/v1/masterdata/activity-types')
      ]);
      setSummary(summaryResult);
      setMasterdata({ generals: generalResult, offices: officeResult, activities });
    } catch (error) {
      setMessage(error.message || 'دریافت اطلاعات اولیه ناموفق بود.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const updateForm = (event) => {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  };

  const submit = async (sendToWorkflow) => {
    setMessage('');
    try {
      const result = sendToWorkflow
        ? await sendFinalizedDebtPaper(toDebtPaperPayload(form))
        : await createFinalizedDebtPaper(toDebtPaperPayload(form));
      setMessage(sendToWorkflow ? `ارسال شد: ${result.message || 'فرایند آغاز شد'}` : 'برگ بدهی قطعی و برگ اجرا ثبت شد.');
      setForm(initialDebtPaper);
      await load();
    } catch (error) {
      setMessage(error.message || 'ثبت اطلاعات ناموفق بود.');
    }
  };

  const fillRandomSample = () => {
    setForm(randomDebtPaperSample(masterdata));
    setMessage('فرم با اطلاعات نمونه پر شد. قبل از ثبت، مقادیر را بررسی کنید.');
  };

  return (
    <AppShell
      active="data-entry"
      title="ثبت دستی برگ بدهی قطعی"
      eyebrow="Finalized Debt Paper"
      actions={(
        <div className="row-actions debt-entry-top-actions">
          <button className="debt-entry-action debt-entry-action--sample" type="button" onClick={fillRandomSample}>
            <IconBox tone="sample"><SparkIcon /></IconBox>
            پر کردن نمونه تصادفی
          </button>
          <Link className="debt-entry-action debt-entry-action--back" href="/admin/data-entry">
            <IconBox tone="back"><BackIcon /></IconBox>
            بازگشت به داشبورد داده
          </Link>
        </div>
      )}
      contextItems={[
        `برگ بدهی ثبت‌شده: ${(summary.finalizedDebtPapers?.length || 0).toLocaleString('fa-IR')}`
      ]}
    >
      {message && <div className={message.includes('ناموفق') || message.includes('خطا') ? 'notice error' : 'notice success'}>{message}</div>}

      <section className="debt-entry-hero">
        <div className="debt-entry-hero__icon" aria-hidden="true">
          <DocumentIcon />
        </div>
        <div>
          <p className="eyebrow">فرم عملیاتی</p>
          <h2>ثبت برگ بدهی قطعی و آماده‌سازی صدور برگ اجرا</h2>
          <span>اطلاعات پرونده، مودی، ساختار مالیاتی و ریز بدهی را در یک مسیر منظم ثبت کنید.</span>
        </div>
        <div className="debt-entry-hero__stats">
          <span>برگ‌های ثبت‌شده</span>
          <strong>{(summary.finalizedDebtPapers?.length || 0).toLocaleString('fa-IR')}</strong>
        </div>
      </section>

      <section className="panel-section debt-entry-panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">اطلاعات اصلی</p>
            <h2>برگ بدهی قطعی</h2>
          </div>
          <RefreshButton onClick={load} loading={loading}>بروزرسانی داده‌های مرجع</RefreshButton>
        </div>

        <form className="admin-form wide-form" onSubmit={(event) => { event.preventDefault(); submit(false); }}>
          <FormSectionTitle tone="blue" icon={<DocumentIcon />} title="مشخصات برگ بدهی" />
          <FieldGrid>
            <Input label="شناسه یکتا" name="debtPaperUniqueCode" value={form.debtPaperUniqueCode} onChange={updateForm} required />
            <Input label="تاریخ صدور سند" name="issueDateRaw" value={form.issueDateRaw} onChange={updateForm} required />
            <Input label="شماره سند" name="documentNumber" value={form.documentNumber} onChange={updateForm} required />
            <Input label="تاریخ مبنای صدور (برگ قطعی / اظهارنامه)" name="basisDocumentDateRaw" value={form.basisDocumentDateRaw} onChange={updateForm} required />
            <Input label="شروع دوره مالی" name="taxPeriodStartRaw" value={form.taxPeriodStartRaw} onChange={updateForm} />
            <Input label="پایان دوره مالی" name="taxPeriodEndRaw" value={form.taxPeriodEndRaw} onChange={updateForm} />
            <Input label="شناسه مؤدی (TIN)" name="taxpayerIdentifier" value={form.taxpayerIdentifier} onChange={updateForm} required />
            <Input label="کد رهگیری ثبت‌نام" name="registrationTraceCode" value={form.registrationTraceCode} onChange={updateForm} required />
            <Select label="علت قطعیت / صدور" name="issuanceReason" value={form.issuanceReason} onChange={updateForm} required options={[{value:'SELF_DECLARATION',label:'خوداظهاری'},{value:'FINAL_NOTICE',label:'برگه بدهی قطعی'}]} />
            <Select label="نوع برگه" name="paperType" value={form.paperType} onChange={updateForm} required options={['EF03','EF23','EF23_1','EF23_2','EF104'].map(value=>({value,label:value}))} />
            <Input label="منبع مالیاتی" name="taxSource" value={form.taxSource} onChange={updateForm} required />
            <Input label="سال عملکرد" name="taxYear" value={form.taxYear} onChange={updateForm} required />
            <Input label="دوره عملکرد" name="taxPeriod" value={form.taxPeriod} onChange={updateForm} />
            <Select label="نوع فعالیت" name="activityTypeId" value={form.activityTypeId} onChange={updateForm} required options={masterdata.activities.map(item => ({ value: item.id, label: `${item.intaCode} — ${item.title}` }))} />
            <Link href="/admin/activity-types">مدیریت انواع فعالیت</Link>
          </FieldGrid>

          <FormSectionTitle tone="green" icon={<OrganizationIcon />} title="ساختار سازمانی پرونده" />
          <FieldGrid>
            <Select label="اداره کل" name="mainTaxBureauId" value={form.mainTaxBureauId} onChange={updateForm} options={masterdata.generals.map((item) => ({ value: item.id, label: item.name }))} />
            <Select label="اداره مالیات" name="taxBureauId" value={form.taxBureauId} onChange={updateForm} required options={masterdata.offices.map((item) => ({ value: item.id, label: item.officeName }))} />
          </FieldGrid>

          <FormSectionTitle tone="indigo" icon={<UserIcon />} title="اطلاعات مودی" />
          <FieldGrid>
            <label>
              <span>نوع مودی</span>
              <select name="taxpayerType" value={form.taxpayerType} onChange={updateForm}>
                <option value="NATURAL">حقیقی</option>
                <option value="LEGAL">حقوقی</option>
              </select>
            </label>
            <Input label="شماره تلفن" name="phone" value={form.phone} onChange={updateForm} required />
            <Input label="شماره همراه" name="mobile" value={form.mobile} onChange={updateForm} required />
            {form.taxpayerType === 'NATURAL' ? (
              <>
                <Input label="کد ملی" name="nationalCode" value={form.nationalCode} onChange={updateForm} />
                <Input label="نام" name="firstName" value={form.firstName} onChange={updateForm} />
                <Input label="نام خانوادگی" name="lastName" value={form.lastName} onChange={updateForm} />
                <Input label="نام پدر" name="fatherName" value={form.fatherName} onChange={updateForm} required />
                <Input label="تاریخ تولد" name="birthDate" value={form.birthDate} onChange={updateForm} required />
                <Input label="کد پستی مودی" name="postalCode" value={form.postalCode} onChange={updateForm} required />
                <Input label="اقامتگاه قانونی مودی" name="address" value={form.address} onChange={updateForm} required />
              </>
            ) : (
              <>
                <Input label="شناسه ملی" name="nationalId" value={form.nationalId} onChange={updateForm} required />
                <Input label="نام تجاری" name="tradeName" value={form.tradeName} onChange={updateForm} required />
                <Input label="نام شرکت" name="companyName" value={form.companyName} onChange={updateForm} required />
                <Input label="شماره ثبت" name="registrationNumber" value={form.registrationNumber} onChange={updateForm} required />
                <Input label="تاریخ ثبت" name="registrationDate" value={form.registrationDate} onChange={updateForm} required />
                <Input label="کد پستی مودی" name="postalCode" value={form.postalCode} onChange={updateForm} required />
                <Input label="اقامتگاه قانونی مودی" name="address" value={form.address} onChange={updateForm} required />
              </>
            )}
          </FieldGrid>

          <FormSectionTitle tone="amber" icon={<MoneyIcon />} title="ریز بدهی" />
          <MoneyFields form={form} onChange={updateForm} />

          <div className="debt-entry-command-bar">
            <button className="debt-entry-submit debt-entry-submit--save">
              <IconBox><CheckIcon /></IconBox>
              ثبت بدون ارسال به فرایند
            </button>
            <button className="debt-entry-submit debt-entry-submit--workflow" type="button" onClick={() => submit(true)}>
              <IconBox><SendIcon /></IconBox>
              ثبت و ارسال به Camunda
            </button>
          </div>
        </form>
      </section>

    </AppShell>
  );
}

function FormSectionTitle({ tone, icon, title }) {
  return (
    <div className={`debt-form-section-title debt-form-section-title--${tone}`}>
      <span aria-hidden="true">{icon}</span>
      <strong>{title}</strong>
    </div>
  );
}

function IconBox({ children, tone = '' }) {
  return <span className={tone ? `debt-entry-icon debt-entry-icon--${tone}` : 'debt-entry-icon'} aria-hidden="true">{children}</span>;
}

function DocumentIcon() {
  return (
    <svg viewBox="0 0 24 24">
      <path d="M7 3.75h6.2L18.25 8.8V20A1.25 1.25 0 0 1 17 21.25H7A1.25 1.25 0 0 1 5.75 20V5A1.25 1.25 0 0 1 7 3.75Z" />
      <path d="M13 4v5h5M9 13h6M9 16h4" />
    </svg>
  );
}

function OrganizationIcon() {
  return (
    <svg viewBox="0 0 24 24">
      <path d="M4 20h16M6 20V8l6-4 6 4v12" />
      <path d="M9 20v-6h6v6M9 10h.01M12 10h.01M15 10h.01" />
    </svg>
  );
}

function UserIcon() {
  return (
    <svg viewBox="0 0 24 24">
      <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" />
      <path d="M4.5 20c1-4.2 3.6-6.2 7.5-6.2s6.5 2 7.5 6.2" />
    </svg>
  );
}

function MoneyIcon() {
  return (
    <svg viewBox="0 0 24 24">
      <path d="M4 7h16v10H4z" />
      <path d="M8 7V5h8v2M8 17v2h8v-2M12 10v4M9.5 12h5" />
    </svg>
  );
}

function SparkIcon() {
  return (
    <svg viewBox="0 0 24 24">
      <path d="M12 3l1.9 5.2L19 10l-5.1 1.8L12 17l-1.9-5.2L5 10l5.1-1.8Z" />
      <path d="M18 15l.8 2.2L21 18l-2.2.8L18 21l-.8-2.2L15 18l2.2-.8Z" />
    </svg>
  );
}

function BackIcon() {
  return (
    <svg viewBox="0 0 24 24">
      <path d="M15 6l-6 6 6 6" />
      <path d="M9 12h11" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 24 24">
      <path d="M5 12.5l4.2 4.2L19 7" />
    </svg>
  );
}

function SendIcon() {
  return (
    <svg viewBox="0 0 24 24">
      <path d="M4 12 20 4l-4 16-4-7Z" />
      <path d="M20 4 12 13" />
    </svg>
  );
}
