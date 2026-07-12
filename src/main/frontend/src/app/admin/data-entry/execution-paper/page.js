'use client'

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import AppShell from '../../../components/app-shell';
import { createManualExecutionPaper, fetchAdminDataSummary } from '../../../lib/admin-data-entry';
import { fetchGeneralDirectorates, fetchTaxAffairsOffices, fetchTaxUnits } from '../../../lib/security';
import {
  DataList,
  FieldGrid,
  Input,
  MoneyFields,
  Select,
  initialExecutionPaper,
  toExecutionPayload
} from '../form-parts';

export default function ExecutionPaperEntryPage() {
  const [form, setForm] = useState(initialExecutionPaper);
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

  const updateForm = (event) => {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  };

  const submit = async (event) => {
    event.preventDefault();
    setMessage('');
    try {
      await createManualExecutionPaper(toExecutionPayload(form));
      setMessage('برگ اجرا به صورت دستی ثبت شد.');
      setForm(initialExecutionPaper);
      await load();
    } catch (error) {
      setMessage(error.message || 'ثبت برگ اجرا ناموفق بود.');
    }
  };

  return (
    <AppShell
      active="data-entry"
      title="ثبت دستی برگ اجرا"
      eyebrow="Execution Paper"
      subtitle="ایجاد مستقیم برگ اجرا از برگ بدهی قطعی و مودی موجود، همراه با وضعیت و ریز بدهی."
      actions={<Link className="ghost-action" href="/admin/data-entry">بازگشت به داشبورد داده</Link>}
      contextItems={[
        `برگ اجرا: ${(summary.executionPapers?.length || 0).toLocaleString('fa-IR')}`,
        `برگ بدهی موجود: ${(summary.finalizedDebtPapers?.length || 0).toLocaleString('fa-IR')}`,
        `مودی موجود: ${(summary.taxpayers?.length || 0).toLocaleString('fa-IR')}`
      ]}
    >
      {message && <div className={message.includes('ناموفق') || message.includes('خطا') ? 'notice error' : 'notice success'}>{message}</div>}

      <section className="panel-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">اطلاعات اصلی</p>
            <h2>برگ اجرا</h2>
          </div>
          <button className="ghost-action" onClick={load} disabled={loading}>بروزرسانی داده‌های مرجع</button>
        </div>

        <form className="admin-form wide-form" onSubmit={submit}>
          <FieldGrid>
            <Input label="شناسه برگ اجرا" name="executionPaperUniqueCode" value={form.executionPaperUniqueCode} onChange={updateForm} />
            <Select label="برگ بدهی قطعی" name="finalizedDebtPaperId" value={form.finalizedDebtPaperId} onChange={updateForm} required options={(summary.finalizedDebtPapers || []).map((item) => ({ value: item.id, label: `${item.debtPaperUniqueCode} (${item.id})` }))} />
            <Select label="مودی" name="taxpayerId" value={form.taxpayerId} onChange={updateForm} required options={(summary.taxpayers || []).map((item) => ({ value: item.id, label: `${item.debtPaperUniqueCode} (${item.id})` }))} />
            <Input label="وضعیت" name="currentState" value={form.currentState} onChange={updateForm} />
            <Input label="علت قطعیت" name="finalizationReason" value={form.finalizationReason} onChange={updateForm} required />
            <Input label="منبع مالیاتی" name="taxSource" value={form.taxSource} onChange={updateForm} required />
          </FieldGrid>

          <FieldGrid>
            <Select label="اداره کل" name="mainTaxBureauId" value={form.mainTaxBureauId} onChange={updateForm} options={masterdata.generals.map((item) => ({ value: item.id, label: item.name }))} />
            <Select label="اداره مالیات" name="taxBureauId" value={form.taxBureauId} onChange={updateForm} options={masterdata.offices.map((item) => ({ value: item.id, label: item.officeName }))} />
            <Select label="واحد مالیاتی" name="taxUnitId" value={form.taxUnitId} onChange={updateForm} options={masterdata.units.map((item) => ({ value: item.id, label: item.taxUnitName }))} />
            <Select label="واحد ارسال‌کننده" name="sendingUnitId" value={form.sendingUnitId} onChange={updateForm} options={masterdata.units.map((item) => ({ value: item.id, label: item.taxUnitName }))} />
          </FieldGrid>

          <div className="form-subtitle">ریز بدهی اختیاری</div>
          <MoneyFields form={form} onChange={updateForm} />

          <button className="primary-action">ثبت برگ اجرا</button>
        </form>
      </section>

      <section className="admin-grid">
        <DataList title="برگ‌های اجرا" items={summary.executionPapers || []} render={(item) => `${item.executionPaperUniqueCode} · ${item.currentState}`} />
        <DataList title="برگ‌های بدهی قطعی" items={summary.finalizedDebtPapers || []} render={(item) => `${item.debtPaperUniqueCode} · ${item.taxSource || '-'}`} />
        <DataList title="مودیان" items={summary.taxpayers || []} render={(item) => `${item.debtPaperUniqueCode} · ${item.id}`} />
      </section>
    </AppShell>
  );
}
