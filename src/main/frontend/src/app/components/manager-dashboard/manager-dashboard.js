'use client';

import Link from 'next/link';
import dynamic from 'next/dynamic';
import { formatDashboardNumber as number } from '../../lib/manager-dashboard';
import FigmaShell from './figma-shell';
import TaskReferralAction from '../task-referral/task-referral-action';
import { formatJalaliDate } from '../../lib/display';
import Icon from './dashboard-icon';
import styles from './manager-dashboard.module.css';

const CollectionChart = dynamic(() => import('./collection-chart'), {
  ssr: false,
  loading: () => <div className={styles.chartLoading}>در حال بارگذاری نمودار...</div>,
});

export default function ManagerDashboard({ user, data, loading, onRefresh }) {
  const stats = data?.stats;
  const tasks = data?.tasks || [];
  const performance = data?.performance;
  const metrics = [
    { key: 'active', label: 'پرونده‌های فعال', icon: 'briefcase', tone: 'blue', value: stats?.activeExecutionPapers,
      detail: 'برگ‌های اجرای قابل مشاهده' },
    { key: 'completed', label: 'اقدام شده', icon: 'check', tone: 'green', value: stats?.myActionsCompleted,
      detail: 'کارهای دارای اقدام شما' },
    { key: 'pending', label: 'در انتظار اقدام', icon: 'alert', tone: 'red', value: stats?.myActionsAwaitingCompletion,
      detail: 'کارهای فعلی تحت مسئولیت شما' },
    { key: 'closed', label: 'پرونده‌های مختومه', icon: 'archive', tone: 'purple',
      value: stats?.closedExecutionPapers,
      detail: 'برگ‌های اجرای قابل مشاهده' },
  ];
  const taskRows = [...tasks].sort((a, b) => {
    const aDate = a.dueDate ? Date.parse(a.dueDate) : Infinity;
    const bDate = b.dueDate ? Date.parse(b.dueDate) : Infinity;
    return aDate - bDate;
  }).slice(0, 5);

  return (
    <FigmaShell user={user} data={data} loading={loading}>
        <p id="dashboard-scope" className={styles.srOnly}>اقدامات مطابق مسئولیت کاربر؛ عملکرد بر اساس دستورهای بازداشت مربوط به کاربر.</p>
        <div className={styles.contentGrid} aria-busy={loading} aria-describedby="dashboard-scope">
          <div className={styles.primaryColumn}>
            {data?.statsError && <ErrorNotice message="دریافت آمار داشبورد ناموفق بود." onRetry={onRefresh} />}
            <section className={styles.metrics} aria-label="آمار پرونده‌ها">
              {metrics.map(metric => <article key={metric.key} className={`${styles.metric} ${styles[metric.tone]}`}>
                <div className={styles.metricLabel}><span className={styles.metricIcon}><Icon name={`${metric.tone}Background`} /><span className={styles.metricIconForeground}><Icon name={metric.icon} /></span></span><h2>{metric.label}</h2></div>
                <strong className={styles.metricValue}>{loading ? '…' : number(metric.value)}</strong>
                <p>{metric.detail}</p>
              </article>)}
            </section>

            <section className={`${styles.card} ${styles.pendingCard}`} aria-labelledby="pending-heading">
              <div className={styles.cardHeading}><h2 id="pending-heading" className={styles.dangerHeading}><Icon name="info" />نیازمند اقدام شما</h2><Link className={styles.viewAll} href="/arrest-orders">دستورهای بازداشت</Link><Link className={styles.viewAll} href="/tasks">کارتابل</Link></div>
              {data?.tasksError && <ErrorNotice message="دریافت بخشی از کارتابل ناموفق بود." onRetry={onRefresh} />}<div className={styles.tableScroll} tabIndex={0} role="region" aria-label="کارهای نیازمند اقدام؛ برای مشاهده همه ستون‌ها پیمایش افقی کنید">
                <table className={styles.tasksTable}>
                  <thead><tr>{['ردیف', 'شماره برگ اجرا', 'نوع اقدام', 'اولویت', 'مهلت اقدام', 'عملیات سریع'].map(label => <th key={label} scope="col">{label}</th>)}</tr></thead>
                  <tbody>
                    {loading ? <tr><td colSpan={6} className={styles.tableEmpty}>در حال دریافت کارتابل...</td></tr> : taskRows.length === 0 ? <tr><td colSpan={6} className={styles.tableEmpty}>در حال حاضر اقدامی در کارتابل شما وجود ندارد.</td></tr> : taskRows.map((task, index) => {
                      const priority = task.priority;
                      return <tr key={task.key}>
                        <td>{number(index + 1)}</td><td><bdi>{persianIdentifier(task.paperCode)}</bdi></td><td>{task.name}</td>
                        <td><span className={`${styles.priority} ${priority === 'urgent' ? styles.urgent : priority === 'normal' ? styles.normal : styles.unknown}`}>{priority === 'urgent' ? 'فوری' : priority === 'normal' ? 'عادی' : 'نامشخص'}</span></td>
                        <td><bdi>{formatJalaliDate(task.dueDate)}</bdi></td>
                        <td><div className={styles.quickActions}><Link className={styles.quickAction} href={task.href} aria-label={`مشاهده کار ${task.name}، برگ ${task.paperCode}`}>مشاهده کار</Link><TaskReferralAction task={task.sourceTask} user={user} /></div></td>
                      </tr>;
                    })}
                  </tbody>
                </table>
              </div>
            </section>

            <section className={`${styles.card} ${styles.unitCard}`} aria-labelledby="unit-heading">
              <div className={styles.cardHeading}><h2 id="unit-heading"><Icon name="chart" />عملکرد کلی واحد</h2></div>
              <div className={styles.unitContent}>
                <div className={styles.financialSummary}>
                  <h3>مبلغ وصول شده</h3>
                  <p className={styles.money}><strong>{loading ? '…' : number(stats?.collectedAmounts)}</strong><span>ریال</span></p>
                  <p className={styles.unavailable}>مقایسه ماهانه در دسترس نیست.</p>
                  <div className={styles.financialDivider} />
                  <h3>تعداد پرونده‌های وصولی</h3>
                  <p className={styles.money}><strong>—</strong><span>پرونده</span></p>
                  <p className={styles.unavailable}>گزارش پرونده‌های وصولی در دسترس نیست.</p>
                </div>
                {data?.collectionSeries?.length > 0 ? <CollectionChart data={data.collectionSeries} /> : <div className={styles.chartLoading} role="status">گزارش دوره‌ای وصول در دسترس نیست.</div>}
              </div>
            </section>
          </div>

          <div className={styles.secondaryColumn}>
            <section className={`${styles.card} ${styles.performanceCard}`} aria-labelledby="performance-heading">
              <h2 id="performance-heading">عملکرد من</h2>
              <div className={styles.donut} role="img" aria-label={performance ? `عملکرد ${number(performance.percent)} درصد` : 'گزارش عملکرد فردی در دسترس نیست'} style={{ '--performance': `${performance?.percent || 0}%` }}><strong>{performance ? `${number(performance.percent)}٪` : '—'}</strong></div>
              {performance ?
              <div className={styles.performanceDetails}>
                <p><Icon name="tick" />اقدام روی {number(performance.completed)} دستور از {number(performance.total)} مورد</p>
                <p>صدور دستور بازداشت</p>
              </div>
              : <p className={styles.unavailable}>گزارش عملکرد فردی در دسترس نیست.</p>}
            </section>
            <section className={`${styles.card} ${styles.activityCard}`} aria-labelledby="activity-heading">
              <h2 id="activity-heading">فعالیت‌های اخیر</h2>
              {data?.recentActivities?.length > 0 ? <div className={styles.activityList}>{data.recentActivities.map(activity => <article key={activity.key} className={styles.activity}>
                <span className={styles.activityIcon}><Icon name={activity.icon} /></span>
                <div><h3>{activity.name}</h3><p>پرونده <bdi>{activity.paperCode}</bdi></p></div>
                <time dateTime={activity.createdAt}>{formatJalaliDate(activity.createdAt)}</time>
              </article>)}</div> : <p className={styles.unavailable}>گزارش فعالیت‌های اخیر در دسترس نیست.</p>}
            </section>
          </div>
        </div>
    </FigmaShell>
  );
}

function ErrorNotice({ message, onRetry }) {
  return <div className={styles.error} role="alert"><span>{message}</span><button onClick={onRetry}>تلاش مجدد</button></div>;
}

function persianIdentifier(value) {
  return String(value).replace(/\d/g, digit => '۰۱۲۳۴۵۶۷۸۹'[digit]);
}
