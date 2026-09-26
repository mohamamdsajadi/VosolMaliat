'use client'

import Link from 'next/link'
import AppShell from '../../components/app-shell'

export default function RuleManagementPage() {
  return (
    <AppShell
      active="rule-management"
      title="مدیریت قوانین سامانه"
      eyebrow="Rule Management"
      contextItems={[
        'قوانین صدور برگ اجرا',
        'موتور قوانین ابلاغ'
      ]}
    >
      <section className="data-entry-choice-grid">
        <Link className="data-entry-choice" href="/admin/rules">
          <h2>قوانین صدور</h2>
          <span>تعریف و مدیریت شرایطی که مانع صدور برگ اجرا می‌شوند.</span>
          <strong className="data-entry-choice-button">ورود به قوانین صدور</strong>
        </Link>

        <Link className="data-entry-choice" href="/admin/inform-rules">
          <h2>قوانین ابلاغ</h2>
          <span>تعریف و مدیریت شرایطی که مانع ابلاغ برگ اجرا می‌شوند.</span>
          <strong className="data-entry-choice-button">ورود به قوانین ابلاغ</strong>
        </Link>
      </section>
    </AppShell>
  )
}
