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
  const referral = searchParams.get('intent') === 'referral';
  const backHref = processDefinitionKey
    ? `/processes/${encodeURIComponent(processDefinitionKey)}/tasks`
    : '/tasks';

  return (
    <AppShell
      mobileDrawer
      active="tasks"
      title={referral ? 'ارجاع برگ اجرا' : 'تکمیل کار فعال'}
      eyebrow="فرم مرحله"
      contextItems={[
        `شناسه کار: ${formatCompactId(taskId)}`,
        processDefinitionKey ? `کلید فرایند: ${formatCompactId(processDefinitionKey)}` : "صف مشترک"
      ]}
      actions={(
        <Link className="task-page-back-action" href={backHref}>
          <span aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path d="M19 12H5" />
              <path d="m12 19-7-7 7-7" />
            </svg>
          </span>
          بازگشت به صف
        </Link>
      )}
    >
      <section className="task-detail-layout">
        <aside className="task-detail-aside">
          <div className="task-detail-aside__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path d="M7 3.75h6.2L18.25 8.8V20A1.25 1.25 0 0 1 17 21.25H7A1.25 1.25 0 0 1 5.75 20V5A1.25 1.25 0 0 1 7 3.75Z" />
              <path d="M13 4v5h5" />
              <path d="M9 13h6M9 16h4" />
            </svg>
          </div>
          <div>
            <p className="eyebrow">پرونده در جریان</p>
            <h2>{referral ? 'ارجاع به مسئول مرحله بعد' : 'مسیر تکمیل کار'}</h2>
            <p>{referral ? 'فرم ارجاع مرحله جاری را بررسی کنید. پس از ثبت موفق، پرونده طبق فرایند به مسئول بعدی می‌رسد.' : 'فرم این مرحله را بررسی و نتیجه را ثبت کنید. پس از ثبت موفق، کار از صف فعال خارج می‌شود.'}</p>
          </div>
          <div className="task-detail-steps">
            <span className="task-detail-step task-detail-step--done">دریافت از صف</span>
            <span className="task-detail-step task-detail-step--active">تکمیل فرم مرحله</span>
            <span className="task-detail-step">ثبت و بازگشت</span>
          </div>
        </aside>

        <TaskDetail
          taskId={taskId}
          intent={referral ? 'referral' : undefined}
          order={1}
          onDone={() => router.push(backHref)}
        />
      </section>
    </AppShell>
  );
}
