'use client'

import { useCallback, useEffect, useState } from 'react';
import AppShell from '../../components/app-shell';
import {
  createFinalizedDebtPaper,
  createManualExecutionPaper,
  fetchAdminDataSummary,
  sendFinalizedDebtPaper
} from '../../lib/admin-data-entry';
import { fetchGeneralDirectorates, fetchTaxAffairsOffices, fetchTaxUnits } from '../../lib/security';

const initialDebtPaper = {
  debtPaperUniqueCode: '',
  debtPaperIssueDate: '',
  notificationId: '',
  finalizationReason: '',
  taxSource: '',
  performanceYear: '',
  performancePeriod: '',
  activityTypeCode: '',
  mainTaxBureauId: '',
  taxBureauId: '',
  taxUnitId: '',
  sendingUnitId: '',
  taxpayerType: 'NATURAL',
  nationalCode: '',
  firstName: '',
  lastName: '',
  fatherName: '',
  birthDate: '',
  nationalId: '',
  registrationNumber: '',
  registrationDate: '',
  companyName: '',
  taxAmount: '',
  dutyAmount: '',
  penaltyArticle22: '',
  penaltyArticle23: '',
  deductiblePenalty: '',
  nonDeductiblePenalty: '',
  taxPaidAmount: '',
  penaltyPaidAmount: '',
  taxCredit: '',
  remainingTax: '',
  municipalityShare: '',
  guildShare: '',
  complianceBonus: ''
};

const initialExecutionPaper = {
  executionPaperUniqueCode: '',
  finalizedDebtPaperId: '',
  taxpayerId: '',
  mainTaxBureauId: '',
  taxBureauId: '',
  taxUnitId: '',
  sendingUnitId: '',
  finalizationReason: '',
  taxSource: '',
  currentState: 'UNASSIGNED',
  taxAmount: '',
  dutyAmount: '',
  penaltyArticle22: '',
  penaltyArticle23: '',
  deductiblePenalty: '',
  nonDeductiblePenalty: '',
  taxPaidAmount: '',
  penaltyPaidAmount: '',
  taxCredit: '',
  remainingTax: ''
};

export default function AdminDataEntryPage() {
  const [debtForm, setDebtForm] = useState(initialDebtPaper);
  const [executionForm, setExecutionForm] = useState(initialExecutionPaper);
  const [summary, setSummary] = useState({ finalizedDebtPapers: [], taxpayers: [], executionPapers: [] });
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

  const updateDebt = (event) => {
    setDebtForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  };

  const updateExecution = (event) => {
    setExecutionForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  };

  const submitDebt = async (sendToWorkflow) => {
    setMessage('');
    try {
      const payload = toDebtPaperPayload(debtForm);
      const result = sendToWorkflow
        ? await sendFinalizedDebtPaper(payload)
        : await createFinalizedDebtPaper(payload);
      setMessage(sendToWorkflow ? `ارسال شد: ${result.message || 'فرایند آغاز شد'}` : 'برگ بدهی قطعی و برگ اجرا ثبت شد.');
      setDebtForm(initialDebtPaper);
      await load();
    } catch (error) {
      setMessage(error.message || 'ثبت اطلاعات ناموفق بود.');
    }
  };

  const submitExecution = async (event) => {
    event.preventDefault();
    setMessage('');
    try {
      await createManualExecutionPaper(toExecutionPayload(executionForm));
      setMessage('برگ اجرا به صورت دستی ثبت شد.');
      setExecutionForm(initialExecutionPaper);
      await load();
    } catch (error) {
      setMessage(error.message || 'ثبت برگ اجرا ناموفق بود.');
    }
  };

  return (
    <AppShell
      active="data-entry"
      title="داشبورد ورود و مدیریت داده"
      eyebrow="ثبت دستی"
      subtitle="ثبت دستی برگ بدهی قطعی، ارسال به فرایند، ایجاد برگ اجرا و مشاهده موجودیت‌های اصلی سیستم."
      actions={<button className="ghost-action" onClick={load} disabled={loading}>بروزرسانی</button>}
      contextItems={[
        `برگ بدهی: ${(summary.finalizedDebtPapers?.length || 0).toLocaleString('fa-IR')}`,
        `مودیان: ${(summary.taxpayers?.length || 0).toLocaleString('fa-IR')}`,
        `برگ اجرا: ${(summary.executionPapers?.length || 0).toLocaleString('fa-IR')}`
      ]}
    >
      {message && <div className={message.includes('ناموفق') || message.includes('خطا') ? 'notice error' : 'notice success'}>{message}</div>}

      <section className="panel-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">برگ بدهی قطعی</p>
            <h2>ثبت و ارسال دستی برگ بدهی قطعی</h2>
          </div>
        </div>
        <form className="admin-form wide-form" onSubmit={(event) => { event.preventDefault(); submitDebt(false); }}>
          <FieldGrid>
            <Input label="شناسه یکتا" name="debtPaperUniqueCode" value={debtForm.debtPaperUniqueCode} onChange={updateDebt} required />
            <Input label="تاریخ صدور/قطعیت" name="debtPaperIssueDate" value={debtForm.debtPaperIssueDate} onChange={updateDebt} required />
            <Input label="شماره ابلاغ" name="notificationId" value={debtForm.notificationId} onChange={updateDebt} />
            <Input label="علت قطعیت" name="finalizationReason" value={debtForm.finalizationReason} onChange={updateDebt} required />
            <Input label="منبع مالیاتی" name="taxSource" value={debtForm.taxSource} onChange={updateDebt} required />
            <Input label="سال عملکرد" name="performanceYear" value={debtForm.performanceYear} onChange={updateDebt} />
            <Input label="دوره عملکرد" name="performancePeriod" value={debtForm.performancePeriod} onChange={updateDebt} />
            <Input label="کد فعالیت" name="activityTypeCode" value={debtForm.activityTypeCode} onChange={updateDebt} />
          </FieldGrid>

          <FieldGrid>
            <Select label="اداره کل" name="mainTaxBureauId" value={debtForm.mainTaxBureauId} onChange={updateDebt} options={masterdata.generals.map((item) => ({ value: item.id, label: item.name }))} />
            <Select label="اداره مالیات" name="taxBureauId" value={debtForm.taxBureauId} onChange={updateDebt} options={masterdata.offices.map((item) => ({ value: item.id, label: item.officeName }))} />
            <Select label="واحد مالیاتی" name="taxUnitId" value={debtForm.taxUnitId} onChange={updateDebt} options={masterdata.units.map((item) => ({ value: item.id, label: item.taxUnitName }))} />
            <Select label="واحد ارسال‌کننده" name="sendingUnitId" value={debtForm.sendingUnitId} onChange={updateDebt} options={masterdata.units.map((item) => ({ value: item.id, label: item.taxUnitName }))} />
          </FieldGrid>

          <div className="form-subtitle">اطلاعات مودی</div>
          <FieldGrid>
            <label>
              <span>نوع مودی</span>
              <select name="taxpayerType" value={debtForm.taxpayerType} onChange={updateDebt}>
                <option value="NATURAL">حقیقی</option>
                <option value="LEGAL">حقوقی</option>
              </select>
            </label>
            {debtForm.taxpayerType === 'NATURAL' ? (
              <>
                <Input label="کد ملی" name="nationalCode" value={debtForm.nationalCode} onChange={updateDebt} />
                <Input label="نام" name="firstName" value={debtForm.firstName} onChange={updateDebt} />
                <Input label="نام خانوادگی" name="lastName" value={debtForm.lastName} onChange={updateDebt} />
                <Input label="نام پدر" name="fatherName" value={debtForm.fatherName} onChange={updateDebt} />
                <Input label="تاریخ تولد" name="birthDate" value={debtForm.birthDate} onChange={updateDebt} />
              </>
            ) : (
              <>
                <Input label="شناسه ملی" name="nationalId" value={debtForm.nationalId} onChange={updateDebt} />
                <Input label="نام شرکت" name="companyName" value={debtForm.companyName} onChange={updateDebt} />
                <Input label="شماره ثبت" name="registrationNumber" value={debtForm.registrationNumber} onChange={updateDebt} />
                <Input label="تاریخ ثبت" name="registrationDate" value={debtForm.registrationDate} onChange={updateDebt} />
              </>
            )}
          </FieldGrid>

          <div className="form-subtitle">ریز بدهی</div>
          <MoneyFields form={debtForm} onChange={updateDebt} />

          <div className="row-actions">
            <button className="primary-action">ثبت بدون ارسال به فرایند</button>
            <button className="ghost-action" type="button" onClick={() => submitDebt(true)}>ثبت و ارسال به Camunda</button>
          </div>
        </form>
      </section>

      <section className="panel-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">برگ اجرا</p>
            <h2>ایجاد دستی Execution Paper</h2>
          </div>
        </div>
        <form className="admin-form wide-form" onSubmit={submitExecution}>
          <FieldGrid>
            <Input label="شناسه برگ اجرا" name="executionPaperUniqueCode" value={executionForm.executionPaperUniqueCode} onChange={updateExecution} />
            <Select label="برگ بدهی قطعی" name="finalizedDebtPaperId" value={executionForm.finalizedDebtPaperId} onChange={updateExecution} required options={(summary.finalizedDebtPapers || []).map((item) => ({ value: item.id, label: `${item.debtPaperUniqueCode} (${item.id})` }))} />
            <Select label="مودی" name="taxpayerId" value={executionForm.taxpayerId} onChange={updateExecution} required options={(summary.taxpayers || []).map((item) => ({ value: item.id, label: `${item.debtPaperUniqueCode} (${item.id})` }))} />
            <Input label="وضعیت" name="currentState" value={executionForm.currentState} onChange={updateExecution} />
            <Input label="علت قطعیت" name="finalizationReason" value={executionForm.finalizationReason} onChange={updateExecution} required />
            <Input label="منبع مالیاتی" name="taxSource" value={executionForm.taxSource} onChange={updateExecution} required />
          </FieldGrid>
          <FieldGrid>
            <Select label="اداره کل" name="mainTaxBureauId" value={executionForm.mainTaxBureauId} onChange={updateExecution} options={masterdata.generals.map((item) => ({ value: item.id, label: item.name }))} />
            <Select label="اداره مالیات" name="taxBureauId" value={executionForm.taxBureauId} onChange={updateExecution} options={masterdata.offices.map((item) => ({ value: item.id, label: item.officeName }))} />
            <Select label="واحد مالیاتی" name="taxUnitId" value={executionForm.taxUnitId} onChange={updateExecution} options={masterdata.units.map((item) => ({ value: item.id, label: item.taxUnitName }))} />
            <Select label="واحد ارسال‌کننده" name="sendingUnitId" value={executionForm.sendingUnitId} onChange={updateExecution} options={masterdata.units.map((item) => ({ value: item.id, label: item.taxUnitName }))} />
          </FieldGrid>
          <MoneyFields form={executionForm} onChange={updateExecution} />
          <button className="primary-action">ثبت برگ اجرا</button>
        </form>
      </section>

      <section className="admin-grid">
        <DataList title="برگ‌های بدهی قطعی" items={summary.finalizedDebtPapers || []} render={(item) => `${item.debtPaperUniqueCode} · ${item.taxSource || '-'}`} />
        <DataList title="مودیان" items={summary.taxpayers || []} render={(item) => `${item.debtPaperUniqueCode} · ${item.id}`} />
        <DataList title="برگ‌های اجرا" items={summary.executionPapers || []} render={(item) => `${item.executionPaperUniqueCode} · ${item.currentState}`} />
      </section>
    </AppShell>
  );
}

function toDebtPaperPayload(form) {
  return {
    debtPaperUniqueCode: form.debtPaperUniqueCode,
    debtPaperIssueDate: form.debtPaperIssueDate,
    notificationId: form.notificationId,
    finalizationReason: form.finalizationReason,
    taxSource: form.taxSource,
    performanceYear: form.performanceYear,
    performancePeriod: form.performancePeriod,
    activityTypeCode: form.activityTypeCode,
    mainTaxBureauId: numberOrNull(form.mainTaxBureauId),
    taxBureauId: numberOrNull(form.taxBureauId),
    taxUnitId: numberOrNull(form.taxUnitId),
    sendingUnitId: numberOrNull(form.sendingUnitId),
    taxpayer: {
      type: form.taxpayerType,
      nationalCode: form.nationalCode,
      firstName: form.firstName,
      lastName: form.lastName,
      fatherName: form.fatherName,
      birthDate: form.birthDate,
      nationalId: form.nationalId,
      registrationNumber: form.registrationNumber,
      registrationDate: form.registrationDate,
      companyName: form.companyName
    },
    debtInfo: debtInfoPayload(form),
    relatedPersons: []
  };
}

function toExecutionPayload(form) {
  return {
    executionPaperUniqueCode: form.executionPaperUniqueCode || null,
    finalizedDebtPaperId: numberOrNull(form.finalizedDebtPaperId),
    taxpayerId: numberOrNull(form.taxpayerId),
    mainTaxBureauId: numberOrNull(form.mainTaxBureauId),
    taxBureauId: numberOrNull(form.taxBureauId),
    taxUnitId: numberOrNull(form.taxUnitId),
    sendingUnitId: numberOrNull(form.sendingUnitId),
    finalizationReason: form.finalizationReason,
    taxSource: form.taxSource,
    currentState: form.currentState,
    debtInfo: debtInfoPayload(form)
  };
}

function debtInfoPayload(form) {
  return {
    taxAmount: numberOrZero(form.taxAmount),
    dutyAmount: numberOrZero(form.dutyAmount),
    penaltyArticle22: numberOrZero(form.penaltyArticle22),
    penaltyArticle23: numberOrZero(form.penaltyArticle23),
    deductiblePenalty: numberOrZero(form.deductiblePenalty),
    nonDeductiblePenalty: numberOrZero(form.nonDeductiblePenalty),
    taxPaidAmount: numberOrZero(form.taxPaidAmount),
    penaltyPaidAmount: numberOrZero(form.penaltyPaidAmount),
    taxCredit: numberOrZero(form.taxCredit),
    remainingTax: numberOrZero(form.remainingTax),
    municipalityShare: numberOrZero(form.municipalityShare),
    guildShare: numberOrZero(form.guildShare),
    complianceBonus: numberOrZero(form.complianceBonus)
  };
}

function numberOrNull(value) {
  return value === '' || value == null ? null : Number(value);
}

function numberOrZero(value) {
  return value === '' || value == null ? 0 : Number(value);
}

function FieldGrid({ children }) {
  return <div className="form-grid wide-grid">{children}</div>;
}

function Input({ label, ...props }) {
  return (
    <label>
      <span>{label}</span>
      <input {...props} />
    </label>
  );
}

function Select({ label, options = [], ...props }) {
  return (
    <label>
      <span>{label}</span>
      <select {...props}>
        <option value="">انتخاب کنید</option>
        {options.map((item) => <option value={item.value} key={item.value}>{item.label}</option>)}
      </select>
    </label>
  );
}

function MoneyFields({ form, onChange }) {
  return (
    <FieldGrid>
      <Input label="اصل مالیات" name="taxAmount" type="number" value={form.taxAmount} onChange={onChange} />
      <Input label="عوارض" name="dutyAmount" type="number" value={form.dutyAmount} onChange={onChange} />
      <Input label="جریمه ماده ۲۲" name="penaltyArticle22" type="number" value={form.penaltyArticle22} onChange={onChange} />
      <Input label="جریمه ماده ۲۳" name="penaltyArticle23" type="number" value={form.penaltyArticle23} onChange={onChange} />
      <Input label="جریمه قابل بخشش" name="deductiblePenalty" type="number" value={form.deductiblePenalty} onChange={onChange} />
      <Input label="جریمه غیرقابل بخشش" name="nonDeductiblePenalty" type="number" value={form.nonDeductiblePenalty} onChange={onChange} />
      <Input label="پرداخت اصل" name="taxPaidAmount" type="number" value={form.taxPaidAmount} onChange={onChange} />
      <Input label="پرداخت جریمه" name="penaltyPaidAmount" type="number" value={form.penaltyPaidAmount} onChange={onChange} />
      <Input label="اعتبار مالیاتی" name="taxCredit" type="number" value={form.taxCredit} onChange={onChange} />
      <Input label="مانده بدهی" name="remainingTax" type="number" value={form.remainingTax} onChange={onChange} />
    </FieldGrid>
  );
}

function DataList({ title, items, render }) {
  return (
    <section className="panel-section data-list-panel">
      <div className="section-heading compact-heading">
        <div>
          <p className="eyebrow">CRUD / List</p>
          <h2>{title}</h2>
        </div>
      </div>
      <div className="admin-table">
        {items.slice(0, 12).map((item) => (
          <div className="admin-row" key={item.id}>
            <div>
              <strong>{render(item)}</strong>
              <span>شناسه داخلی: {item.id}</span>
            </div>
          </div>
        ))}
        {items.length === 0 && <div className="empty-state compact">داده‌ای ثبت نشده است.</div>}
      </div>
    </section>
  );
}
