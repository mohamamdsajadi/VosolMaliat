'use client'

import Link from 'next/link';
import { useCallback, useEffect, useMemo, useState } from 'react';
import AppShell from '../../../components/app-shell';
import RefreshButton from '../../../components/refresh-button';
import { createManualExecutionPaper, fetchAdminDataSummary } from '../../../lib/admin-data-entry';
import { fetchGeneralDirectorates, fetchTaxAffairsOffices } from '../../../lib/security';
import {
  DataList,
  FieldGrid,
  Input,
  MoneyFields,
  Select,
  initialExecutionPaper,
  randomExecutionPaperSample,
  toExecutionPayload
} from '../form-parts';

export default function ExecutionPaperEntryPage() {
  const [form, setForm] = useState(initialExecutionPaper);
  const [summary, setSummary] = useState({ finalizedDebtPapers: [], taxpayers: [], executionPapers: [] });
  const [masterdata, setMasterdata] = useState({ generals: [], offices: [] });
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setMessage('');
    try {
      const [summaryResult, generalResult, officeResult] = await Promise.all([
        fetchAdminDataSummary(),
        fetchGeneralDirectorates(),
        fetchTaxAffairsOffices()
      ]);
      setSummary(summaryResult);
      setMasterdata({ generals: generalResult, offices: officeResult });
    } catch (error) {
      setMessage(error.message || 'دریافت اطلاعات اولیه ناموفق بود.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const filteredOffices = useMemo(() => {
    if (!form.mainTaxBureauId) {
      return [];
    }
    return masterdata.offices.filter((item) => String(item.generalDirectorateOfTaxAffairsId) === String(form.mainTaxBureauId));
  }, [form.mainTaxBureauId, masterdata.offices]);

  const updateForm = (event) => {
    const { name, value } = event.target;
    setForm((current) => {
      if (name === 'mainTaxBureauId') {
        return { ...current, mainTaxBureauId: value, taxBureauId: '' };
      }
      return { ...current, [name]: value };
    });
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

  const fillRandomSample = () => {
    setForm(randomExecutionPaperSample(summary, masterdata));
    setMessage('فرم با اطلاعات نمونه پر شد. قبل از ثبت، مقادیر را بررسی کنید.');
  };

  return (
    <AppShell
      active="data-entry"
      title="ثبت دستی برگ اجرا"
      eyebrow="Execution Paper"
      actions={(
        <div className="row-actions">
          <button className="ghost-action" type="button" onClick={fillRandomSample}>پر کردن نمونه تصادفی</button>
          <Link className="ghost-action" href="/admin/data-entry">بازگشت به داشبورد داده</Link>
        </div>
      )}
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
          <RefreshButton onClick={load} loading={loading}>بروزرسانی داده‌های مرجع</RefreshButton>
        </div>

        <form className="admin-form wide-form" onSubmit={submit}>
          <FieldGrid>
            <p>شماره برگ اجرا هنگام ثبت، با سال جاری و شمارنده هفت‌رقمی تخصیص می‌یابد.</p>
            <Select label="برگ بدهی قطعی" name="finalizedDebtPaperId" value={form.finalizedDebtPaperId} onChange={updateForm} required options={(summary.finalizedDebtPapers || []).map((item) => ({ value: item.id, label: `${item.debtPaperUniqueCode} (${item.id})` }))} />
            <Select label="مودی" name="taxpayerId" value={form.taxpayerId} onChange={updateForm} required options={(summary.taxpayers || []).map((item) => ({ value: item.id, label: `${item.tin} (${item.id})` }))} />
            <Input label="وضعیت" name="currentState" value={form.currentState} onChange={updateForm} />
            <Input label="منبع مالیاتی" name="taxSource" value={form.taxSource} onChange={updateForm} required />
          </FieldGrid>

          <FieldGrid>
            <Select label="اداره کل امور مالیاتی" name="mainTaxBureauId" value={form.mainTaxBureauId} onChange={updateForm} required options={masterdata.generals.map((item) => ({ value: item.id, label: item.name }))} />
            <Select label="اداره مالیات" name="taxBureauId" value={form.taxBureauId} onChange={updateForm} required disabled={!form.mainTaxBureauId} options={filteredOffices.map((item) => ({ value: item.id, label: item.officeName }))} />
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
