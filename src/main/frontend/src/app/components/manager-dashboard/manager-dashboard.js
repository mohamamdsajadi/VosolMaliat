'use client';

import Link from 'next/link';
import dynamic from 'next/dynamic';
import { useEffect, useRef, useState } from 'react';
import { logoutFromKeycloak } from '../../lib/keycloak';
import { DASHBOARD_TASK_CATEGORIES, formatDashboardNumber as number } from '../../lib/manager-dashboard';
import { canSeeNavItem, NAV_ITEMS } from '../../lib/navigation';
import { formatJalaliDate } from '../../lib/display';
import Icon from './dashboard-icon';
import styles from './manager-dashboard.module.css';

const CollectionChart = dynamic(() => import('./collection-chart'), {
  ssr: false,
  loading: () => <div className={styles.chartLoading}>در حال بارگذاری نمودار...</div>,
});

export default function ManagerDashboard({ user, data, loading, onRefresh }) {
  const [sidebarOpen, setSidebarOpen] = useState(() => typeof window === 'undefined' || window.innerWidth > 800);
  const [profileOpen, setProfileOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [now, setNow] = useState(() => new Date());
  const profileRef = useRef(null);
  const notificationRef = useRef(null);
  const stats = data?.stats;
  const tasks = data?.tasks || [];
  const performance = data?.performance;
  const canViewReports = canSeeNavItem(user, NAV_ITEMS.find(item => item.key === 'execution-papers'));
  const canViewOperations = canSeeNavItem(user, NAV_ITEMS.find(item => item.key === 'operations'));
  const sidebarCloseRef = useRef(null);
  const sidebarOpenRef = useRef(null);

  const closeSidebar = () => {
    setSidebarOpen(false);
    window.requestAnimationFrame(() => sidebarOpenRef.current?.focus());
  };
  const openSidebar = () => {
    setSidebarOpen(true);
    window.requestAnimationFrame(() => sidebarCloseRef.current?.focus());
  };

  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 1000);
    const closeMenus = event => {
      if (!profileRef.current?.contains(event.target)) setProfileOpen(false);
      if (!notificationRef.current?.contains(event.target)) setNotificationsOpen(false);
    };
    const escape = event => {
      if (event.key === 'Escape') {
        setProfileOpen(false);
        setNotificationsOpen(false);
        if (window.innerWidth <= 800) {
          setSidebarOpen(false);
          window.requestAnimationFrame(() => sidebarOpenRef.current?.focus());
        }
      }
    };
    document.addEventListener('pointerdown', closeMenus);
    document.addEventListener('keydown', escape);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener('pointerdown', closeMenus);
      document.removeEventListener('keydown', escape);
    };
  }, []);

  const metrics = [
    { key: 'active', label: 'پرونده‌های فعال', icon: 'briefcase', tone: 'blue', value: stats?.activeExecutionPapers,
      detail: 'مقایسه هفتگی موجود نیست' },
    { key: 'completed', label: 'اقدام شده', icon: 'check', tone: 'green', value: stats?.myActionsCompleted,
      detail: 'آمار اقدامات تکمیل‌شده' },
    { key: 'pending', label: 'در انتظار اقدام', icon: 'alert', tone: 'red', value: stats?.myActionsAwaitingCompletion,
      detail: 'آمار تجمیعی در انتظار تکمیل' },
    { key: 'closed', label: 'پرونده‌های مختومه', icon: 'archive', tone: 'purple',
      value: stats ? stats.myActionsCompleted + stats.myActionsCancelled : null,
      detail: 'از تمام پرونده‌های سازمان' },
  ];
  const taskRows = [...tasks].sort((a, b) => {
    const aDate = a.dueDate ? Date.parse(a.dueDate) : Infinity;
    const bDate = b.dueDate ? Date.parse(b.dueDate) : Infinity;
    return aDate - bDate;
  }).slice(0, 5);

  return (
    <div className={`${styles.dashboard} ${sidebarOpen ? '' : styles.sidebarCollapsed}`}>
      <header className={styles.header}>
        <Link href="/" className={styles.brand} aria-label="سامانه جامع وصول و اجرا، داشبورد">
          <Icon name="logo" />
          <span><strong>سازمان امور مالیاتی کشور</strong><small>سامانه جامع وصول و اجرا</small></span>
        </Link>
        <div className={styles.headerTools}>
          <div className={styles.notificationWrap} ref={notificationRef}>
            <button className={styles.notificationButton} onClick={() => setNotificationsOpen(value => !value)} aria-label="اعلان‌ها" aria-expanded={notificationsOpen} aria-controls="dashboard-notifications">
              <Icon name="bell" />
            </button>
            {notificationsOpen && <div id="dashboard-notifications" className={styles.popover}>
              <strong>اعلان‌ها</strong>
              <p>سرویس اعلان‌ها در دسترس نیست.</p>
              <Link href="/tasks">مشاهده کارتابل</Link>
            </div>}
          </div>
          <div className={styles.profileWrap} ref={profileRef}>
            <button className={styles.profileButton} onClick={() => setProfileOpen(value => !value)} aria-label={`حساب کاربری ${user?.fullName || user?.username || ''}`} aria-expanded={profileOpen} aria-controls="dashboard-profile">
              <span className={styles.avatar}><Icon name="user" /></span>
              <span className={styles.profileText}><strong>{user?.fullName || user?.username || 'کاربر سامانه'}</strong><small title={user?.organization?.generalDirectorateName}>{user?.businessRole === 'GENERAL_OFFICE_HEAD' ? 'رئیس اداره کل' : 'مدیر سامانه'}{user?.organization?.generalDirectorateName ? `، ${user.organization.generalDirectorateName.replace(/^اداره کل امور مالیاتی(?: استان)?\s*/, '')}` : ''}</small></span>
              <Icon name="down" className={styles.profileChevron} />
            </button>
            {profileOpen && <div id="dashboard-profile" className={styles.popover}><strong>{user?.fullName || user?.username}</strong><button onClick={logoutFromKeycloak}>خروج از حساب کاربری</button></div>}
          </div>
        </div>
      </header>

      <aside id="manager-sidebar" className={styles.sidebar} aria-label="منوی اصلی" inert={!sidebarOpen}>
        <button ref={sidebarCloseRef} className={styles.closeSidebar} onClick={closeSidebar} aria-label="بستن منوی اصلی"><Icon name="close" /></button>
        <nav className={styles.navigation}>
          <Link href="/" className={`${styles.navItem} ${styles.navActive}`} aria-current="page"><Icon name="home" /><span>داشبورد</span></Link>
          <PermissionLink allowed={canViewReports} href="/execution-papers" className={styles.navItem}><Icon name="document" /><span>برگ اجرا</span><Badge value={loading ? null : stats?.totalIssuedExecutionPapers} /></PermissionLink>
          {DASHBOARD_TASK_CATEGORIES.map(item => <Link key={item.key} href={`/tasks?taskName=${encodeURIComponent(item.label)}`} className={styles.navItem}>
            <Icon name={item.icon} /><span>{item.label}</span><Badge value={data?.tasksError || loading ? null : tasks.filter(task => task.name.includes(item.label)).length} />
          </Link>)}
          <div className={styles.navDivider} />
          <PermissionLink allowed={canViewOperations} href="/operations" className={styles.navItem}><Icon name="reports" /><span>گزارش ها</span></PermissionLink>
        </nav>
        <button className={`${styles.navItem} ${styles.settings}`} onClick={() => {
          setProfileOpen(true);
          profileRef.current?.querySelector('button')?.focus();
        }}><Icon name="settings" /><span>تنظیمات</span></button>
      </aside>

      <main className={styles.main}>
        <div className={styles.welcome}>
          <div className={styles.welcomeTitle}>
            {!sidebarOpen && <button ref={sidebarOpenRef} className={styles.openSidebar} onClick={openSidebar} aria-label="باز کردن منوی اصلی" aria-controls="manager-sidebar" aria-expanded={sidebarOpen}><Icon name="menu" /></button>}
            <h1>به سامانه جامع وصول و اجرا خوش آمدید</h1>
          </div>
          <div className={styles.date}><Icon name="calendar" /><time dateTime={now?.toISOString()} suppressHydrationWarning>{now ? formatHeaderDate(now) : '—'}</time></div>
        </div>

        <p id="dashboard-scope" className={styles.srOnly}>آمار تجمیعی کل سامانه؛ کارتابل مطابق دسترسی کاربر.</p>
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
              <div className={styles.cardHeading}><h2 id="pending-heading" className={styles.dangerHeading}><Icon name="info" />نیازمند اقدام شما</h2><Link className={styles.viewAll} href="/tasks">مشاهده همه</Link></div>
              {data?.tasksError ? <ErrorNotice message="دریافت کارتابل ناموفق بود." onRetry={onRefresh} /> : <div className={styles.tableScroll} tabIndex={0} role="region" aria-label="کارهای نیازمند اقدام؛ برای مشاهده همه ستون‌ها پیمایش افقی کنید">
                <table className={styles.tasksTable}>
                  <thead><tr>{['ردیف', 'شماره برگ اجرا', 'نوع اقدام', 'اولویت', 'مهلت اقدام', 'عملیات سریع'].map(label => <th key={label} scope="col">{label}</th>)}</tr></thead>
                  <tbody>
                    {loading ? <tr><td colSpan={6} className={styles.tableEmpty}>در حال دریافت کارتابل...</td></tr> : taskRows.length === 0 ? <tr><td colSpan={6} className={styles.tableEmpty}>در حال حاضر اقدامی در کارتابل شما وجود ندارد.</td></tr> : taskRows.map((task, index) => {
                      const priority = task.priority;
                      return <tr key={task.key}>
                        <td>{number(index + 1)}</td><td><bdi>{persianIdentifier(task.paperCode)}</bdi></td><td>{task.name}</td>
                        <td><span className={`${styles.priority} ${priority === 'urgent' ? styles.urgent : priority === 'normal' ? styles.normal : styles.unknown}`}>{priority === 'urgent' ? 'فوری' : priority === 'normal' ? 'عادی' : 'نامشخص'}</span></td>
                        <td><bdi>{formatJalaliDate(task.dueDate)}</bdi></td>
                        <td><Link className={styles.quickAction} href={task.href} aria-label={`مشاهده کار ${task.name}، برگ ${task.paperCode}`}>مشاهده کار</Link></td>
                      </tr>;
                    })}
                  </tbody>
                </table>
              </div>}
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
                <p><Icon name="tick" />انجام {number(performance.completed)} مورد از {number(performance.total)} مورد</p>
                <p><Icon name="up" className={styles.positiveIcon} />{number(performance.growth)}٪ افزایش عملکرد نسبت به ماه گذشته</p>
                <p><Icon name="calendarFilled" />میانگین زمان رسیدگی {performance.averageDays} روز</p>
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
      </main>
    </div>
  );
}

function Badge({ value }) {
  return <span className={styles.badge}>{value == null ? '—' : value > 999 ? '+۹۹۹' : number(value)}</span>;
}

function ErrorNotice({ message, onRetry }) {
  return <div className={styles.error} role="alert"><span>{message}</span><button onClick={onRetry}>تلاش مجدد</button></div>;
}

function PermissionLink({ allowed, href, className, children }) {
  return allowed ? <Link href={href} className={className}>{children}</Link>
    : <span className={`${className} ${styles.disabledNav}`} aria-disabled="true" title="شما مجوز مشاهده این بخش را ندارید.">{children}</span>;
}

function persianIdentifier(value) {
  return String(value).replace(/\d/g, digit => '۰۱۲۳۴۵۶۷۸۹'[digit]);
}

function formatHeaderDate(date) {
  const options = { timeZone: 'Asia/Tehran' };
  return `${new Intl.DateTimeFormat('fa-IR-u-ca-persian', { ...options, day: 'numeric', month: 'long', year: 'numeric' }).format(date)}  ${new Intl.DateTimeFormat('fa-IR', { ...options, hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).format(date)}`;
}
