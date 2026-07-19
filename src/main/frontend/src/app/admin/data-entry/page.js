'use client'

import Link from 'next/link';
import AppShell from '../../components/app-shell';

export default function AdminDataEntryHubPage() {
  return (
    <AppShell
      active="data-entry"
      title="داشبورد ورود دستی داده"
      eyebrow="ثبت دستی"
    >
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
    </AppShell>
  );
}
