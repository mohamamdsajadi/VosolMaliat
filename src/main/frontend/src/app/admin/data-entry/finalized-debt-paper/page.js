'use client'

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import AppShell from '../../../components/app-shell';
import RefreshButton from '../../../components/refresh-button';
import {
  createFinalizedDebtPaper,
  fetchAdminDataSummary,
  sendFinalizedDebtPaper
} from '../../../lib/admin-data-entry';
import { fetchGeneralDirectorates, fetchTaxAffairsOffices, fetchTaxUnits } from '../../../lib/security';
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
  const [masterdata, setMasterdata] = useState({ generals: [], offices: [], units: [] });
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setMessage('');
    try {
      const [summaryResult, generalResult, officeResult, unitResult] = await Promise.all([
        fetchAdminDataSummary(),
        fetchGeneralDirectorates(),
        fetchTaxAffairsOffices(),
        fetchTaxUnits()
      ]);
      setSummary(summaryResult);
      setMasterdata({ generals: generalResult, offices: officeResult, units: unitResult });
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
            <span aria-hidden="true">✦</span>
            پر کردن نمونه تصادفی
          </button>
          <Link className="debt-entry-action debt-entry-action--back" href="/admin/data-entry">
            <span aria-hidden="true">↩</span>
            بازگشت به داشبورد داده
          </Link>
        </div>
      )}
      contextItems={[
        `برگ بدهی ثبت‌شده: ${(summary.finalizedDebtPapers?.length || 0).toLocaleString('fa-IR')}`
      ]}
    >
      {message && <div className={message.includes('ناموفق') || message.includes('خطا') ? 'notice error' : 'notice success'}>{message}</div>}

      <section className="panel-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">اطلاعات اصلی</p>
            <h2>برگ بدهی قطعی</h2>
          </div>
          <RefreshButton onClick={load} loading={loading}>بروزرسانی داده‌های مرجع</RefreshButton>
        </div>

        <form className="admin-form wide-form" onSubmit={(event) => { event.preventDefault(); submit(false); }}>
          <FieldGrid>
            <Input label="شناسه یکتا" name="debtPaperUniqueCode" value={form.debtPaperUniqueCode} onChange={updateForm} required />
            <Input label="تاریخ صدور/قطعیت" name="debtPaperIssueDate" value={form.debtPaperIssueDate} onChange={updateForm} required />
            <Input label="شماره ابلاغ" name="notificationId" value={form.notificationId} onChange={updateForm} />
            <Input label="علت قطعیت" name="finalizationReason" value={form.finalizationReason} onChange={updateForm} required />
            <Input label="منبع مالیاتی" name="taxSource" value={form.taxSource} onChange={updateForm} required />
            <Input label="سال عملکرد" name="performanceYear" value={form.performanceYear} onChange={updateForm} />
            <Input label="دوره عملکرد" name="performancePeriod" value={form.performancePeriod} onChange={updateForm} />
            <Input label="کد فعالیت" name="activityTypeCode" value={form.activityTypeCode} onChange={updateForm} />
            <Input label="عنوان فعالیت" name="activityTypeTitle" value={form.activityTypeTitle} onChange={updateForm} />
          </FieldGrid>

          <FieldGrid>
            <Select label="اداره کل" name="mainTaxBureauId" value={form.mainTaxBureauId} onChange={updateForm} options={masterdata.generals.map((item) => ({ value: item.id, label: item.name }))} />
            <Select label="اداره مالیات" name="taxBureauId" value={form.taxBureauId} onChange={updateForm} options={masterdata.offices.map((item) => ({ value: item.id, label: item.officeName }))} />
            <Select label="واحد مالیاتی" name="taxUnitId" value={form.taxUnitId} onChange={updateForm} options={masterdata.units.map((item) => ({ value: item.id, label: item.taxUnitName }))} />
            <Select label="واحد ارسال‌کننده" name="sendingUnitId" value={form.sendingUnitId} onChange={updateForm} options={masterdata.units.map((item) => ({ value: item.id, label: item.taxUnitName }))} />
          </FieldGrid>

          <div className="form-subtitle">اطلاعات مودی</div>
          <FieldGrid>
            <label>
              <span>نوع مودی</span>
              <select name="taxpayerType" value={form.taxpayerType} onChange={updateForm}>
                <option value="NATURAL">حقیقی</option>
                <option value="LEGAL">حقوقی</option>
              </select>
            </label>
            {form.taxpayerType === 'NATURAL' ? (
              <>
                <Input label="کد ملی" name="nationalCode" value={form.nationalCode} onChange={updateForm} />
                <Input label="نام" name="firstName" value={form.firstName} onChange={updateForm} />
                <Input label="نام خانوادگی" name="lastName" value={form.lastName} onChange={updateForm} />
                <Input label="نام پدر" name="fatherName" value={form.fatherName} onChange={updateForm} />
                <Input label="تاریخ تولد" name="birthDate" value={form.birthDate} onChange={updateForm} />
                <Input label="کد پستی مودی" name="postalCode" value={form.postalCode} onChange={updateForm} />
                <Input label="اقامتگاه قانونی مودی" name="address" value={form.address} onChange={updateForm} />
              </>
            ) : (
              <>
                <Input label="شناسه ملی" name="nationalId" value={form.nationalId} onChange={updateForm} />
                <Input label="نام شرکت" name="companyName" value={form.companyName} onChange={updateForm} />
                <Input label="شماره ثبت" name="registrationNumber" value={form.registrationNumber} onChange={updateForm} />
                <Input label="تاریخ ثبت" name="registrationDate" value={form.registrationDate} onChange={updateForm} />
                <Input label="کد پستی مودی" name="postalCode" value={form.postalCode} onChange={updateForm} />
                <Input label="اقامتگاه قانونی مودی" name="address" value={form.address} onChange={updateForm} />
              </>
            )}
          </FieldGrid>

          <div className="form-subtitle">ریز بدهی</div>
          <MoneyFields form={form} onChange={updateForm} />

          <div className="debt-entry-command-bar">
            <button className="debt-entry-submit debt-entry-submit--save">
              <span aria-hidden="true">✓</span>
              ثبت بدون ارسال به فرایند
            </button>
            <button className="debt-entry-submit debt-entry-submit--workflow" type="button" onClick={() => submit(true)}>
              <span aria-hidden="true">↗</span>
              ثبت و ارسال به Camunda
            </button>
          </div>
        </form>
      </section>

    </AppShell>
  );
}
