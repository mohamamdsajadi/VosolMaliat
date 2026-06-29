'use client'

import Link from 'next/link';
import { useParams, useRouter, useSearchParams } from 'next/navigation';
import AppShell from '../../components/app-shell';
import TaskDetail from '../../components/task';
import { formatCompactId } from '../../lib/display';

export default function TaskDetailPage() {
  const params = useParams();
  const router = useRouter();
  const searchParams = useSearchParams();
  const taskId = params?.taskId;
  const processDefinitionKey = searchParams.get('processDefinitionKey');
  const backHref = processDefinitionKey
    ? `/processes/${encodeURIComponent(processDefinitionKey)}/tasks`
    : '/tasks';

  return (
    <AppShell
      active="tasks"
      title="تکمیل کار فعال"
      eyebrow="فرم مرحله"
      subtitle="اطلاعات این مرحله را بررسی کنید، فرم را کامل کنید و نتیجه را برای ادامه فرایند ثبت کنید."
      contextItems={[
        `شناسه کار: ${formatCompactId(taskId)}`,
        processDefinitionKey ? `کلید فرایند: ${formatCompactId(processDefinitionKey)}` : "صف مشترک"
      ]}
      actions={(
        <Link className="ghost-action" href={backHref}>بازگشت به صف</Link>
      )}
    >
      <section className="detail-grid">
        <aside className="case-aside">
          <p className="eyebrow">پرونده در جریان</p>
          <h2>مسیر تکمیل</h2>
          <div className="case-timeline">
            <span>کار از صف عملیاتی دریافت شد</span>
            <span>فرم مرحله بارگذاری می‌شود</span>
            <span>پس از ثبت، کار بسته می‌شود</span>
          </div>
        </aside>

        <TaskDetail
          taskId={taskId}
          order={1}
          onDone={() => router.push(backHref)}
        />
      </section>
    </AppShell>
  );
}
