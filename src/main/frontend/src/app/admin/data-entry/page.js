'use client'

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import AppShell from '../../components/app-shell';
import RefreshButton from '../../components/refresh-button';
import { fetchAdminDataSummary } from '../../lib/admin-data-entry';
import { DataList } from './form-parts';

export default function AdminDataEntryHubPage() {
  const [summary, setSummary] = useState({ finalizedDebtPapers: [], taxpayers: [], executionPapers: [] });
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setMessage('');
    try {
      setSummary(await fetchAdminDataSummary());
    } catch (error) {
      setMessage(error.message || 'دریافت اطلاعات اولیه ناموفق بود.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <AppShell
      active="data-entry"
      title="داشبورد ورود دستی داده"
      eyebrow="ثبت دستی"
      actions={<RefreshButton onClick={load} loading={loading} />}
      contextItems={[
        `برگ بدهی: ${(summary.finalizedDebtPapers?.length || 0).toLocaleString('fa-IR')}`,
        `مودیان: ${(summary.taxpayers?.length || 0).toLocaleString('fa-IR')}`,
        `برگ اجرا: ${(summary.executionPapers?.length || 0).toLocaleString('fa-IR')}`
      ]}
    >
      {message && <div className="notice error">{message}</div>}

      <section className="data-entry-choice-grid">
        <Link className="data-entry-choice" href="/admin/data-entry/finalized-debt-paper">
          <p className="eyebrow">ورود اطلاعات</p>
          <h2>ثبت دستی برگ بدهی قطعی</h2>
          <span>اطلاعات مودی، اداره مالیاتی، مبالغ بدهی و ارسال اختیاری به Camunda.</span>
          <strong className="data-entry-choice-button">ورود به فرم برگ بدهی قطعی</strong>
        </Link>
        <Link className="data-entry-choice" href="/admin/data-entry/execution-paper">
          <p className="eyebrow">ورود اطلاعات</p>
          <h2>ثبت دستی برگ اجرا</h2>
          <span>ایجاد مستقیم برگ اجرا از برگ بدهی قطعی و مودی موجود.</span>
          <strong className="data-entry-choice-button">ورود به فرم برگ اجرا</strong>
        </Link>
      </section>

      <section className="admin-grid">
        <DataList title="برگ‌های بدهی قطعی" items={summary.finalizedDebtPapers || []} render={(item) => `${item.debtPaperUniqueCode} · ${item.taxSource || '-'}`} />
        <DataList title="مودیان" items={summary.taxpayers || []} render={(item) => `${item.debtPaperUniqueCode} · ${item.id}`} />
        <DataList title="برگ‌های اجرا" items={summary.executionPapers || []} render={(item) => `${item.executionPaperUniqueCode} · ${item.currentState}`} />
      </section>
    </AppShell>
  );
}
