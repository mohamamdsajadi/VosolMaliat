'use client';
import Link from 'next/link';
import { useEffect, useRef, useState } from 'react';
import { logoutFromKeycloak } from '../../lib/keycloak';
import { DASHBOARD_TASK_CATEGORIES, formatDashboardNumber as number } from '../../lib/manager-dashboard';
import { canSeeNavItem, NAV_ITEMS } from '../../lib/navigation';
import Icon from './dashboard-icon';
import PaperIcon from '../execution-papers/paper-icon';
import { ViewIcon } from '../execution-papers/paper-view-tools';
import styles from './manager-dashboard.module.css';
import { businessRoleLabel } from '../../lib/business-roles';
import PageHelp, { ArrestGuide, DashboardGuide, PaperGuide, PermissionsGuide, RestrictionsGuide } from '../page-help';
import { LEGAL_RESTRICTION_GROUPS } from '../../lib/legal-restrictions';
import RestrictionIcon from '../legal-restrictions/restriction-icon';

function NavigationIcon({name}) { return name === 'referral' ? <ViewIcon name="referral" /> : ['homeOutline','documentActive'].includes(name) ? <PaperIcon name={name} /> : <Icon name={name} />; }

export default function FigmaShell({user,data,loading,active='dashboard',toolbar,toolbarClassName='',mainClassName='',navigationItems=DASHBOARD_TASK_CATEGORIES,papersLabel='برگ اجرا',children}) {
  const [sidebarOpen, setSidebarOpen] = useState(() => typeof window === 'undefined' || window.innerWidth > 800);
  const [profileOpen, setProfileOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [restrictionsOpen, setRestrictionsOpen] = useState(active === 'legal-restrictions');
  const [now, setNow] = useState(() => new Date());
  const profileRef = useRef(null);
  const notificationRef = useRef(null);
  const stats = data?.stats;
  const tasks = data?.tasks || [];
  const canViewReports = canSeeNavItem(user, NAV_ITEMS.find(item => item.key === 'execution-papers'));
  const canViewOperations = canSeeNavItem(user, NAV_ITEMS.find(item => item.key === 'operations'));
  const roleName = businessRoleLabel(user?.businessRole);
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
              <span className={styles.profileText}><strong>{user?.fullName || user?.username || 'کاربر سامانه'}</strong><small title={user?.organization?.generalDirectorateName}>{roleName}{user?.organization?.generalDirectorateName ? `، ${user.organization.generalDirectorateName.replace(/^اداره کل امور مالیاتی(?: استان)?\s*/, '')}` : ''}</small></span>
              <Icon name="down" className={styles.profileChevron} />
            </button>
            {profileOpen && <div id="dashboard-profile" className={styles.popover}><strong>{user?.fullName || user?.username}</strong><button onClick={logoutFromKeycloak}>خروج از حساب کاربری</button></div>}
          </div>
        </div>
      </header>

      <aside id="manager-sidebar" className={styles.sidebar} aria-label="منوی اصلی" inert={!sidebarOpen}>
        <button ref={sidebarCloseRef} className={styles.closeSidebar} onClick={closeSidebar} aria-label="بستن منوی اصلی"><Icon name="close" /></button>
        <nav className={styles.navigation}>
          <Link href="/" className={`${styles.navItem} ${active === 'dashboard' ? styles.navActive : ''}`} aria-current={active === 'dashboard' ? 'page' : undefined}><NavigationIcon name={active === 'dashboard' ? 'home' : 'homeOutline'} /><span>داشبورد</span></Link>
          <PermissionLink allowed={canViewReports} href="/execution-papers" current={active === 'execution-papers'} className={`${styles.navItem} ${active === 'execution-papers' ? styles.navActive : ''}`}><NavigationIcon name={active === 'execution-papers' ? 'documentActive' : 'document'} /><span>{papersLabel}</span><Badge value={loading ? null : stats?.totalIssuedExecutionPapers} /></PermissionLink>
          {canSeeNavItem(user, NAV_ITEMS.find(item => item.key === 'arrest')) && <Link href="/arrest-orders" className={`${styles.navItem} ${active === 'arrest' ? styles.navActive : ''}`} aria-current={active === 'arrest' ? 'page' : undefined}><Icon name="lock" /><span>دستور بازداشت اموال</span></Link>}
          {canSeeNavItem(user, NAV_ITEMS.find(item => item.key === 'legal-restrictions')) && <div>
            <button className={`${styles.navItem} ${styles.navParent} ${active === 'legal-restrictions' ? styles.navActive : ''}`} aria-expanded={restrictionsOpen} aria-controls="legal-restriction-navigation" onClick={() => setRestrictionsOpen(value => !value)}><Icon name="shield" /><span>محدودیت‌های قانونی</span><RestrictionIcon name="chevron" size={18} className={restrictionsOpen ? styles.chevronOpen : styles.chevron} /></button>
            {restrictionsOpen && <div id="legal-restriction-navigation" className={styles.subNavigation}>{LEGAL_RESTRICTION_GROUPS.map(group => group.href ? <Link href={group.href} key={group.key} className={`${styles.subNavItem} ${active === 'legal-restrictions' ? styles.subNavActive : ''}`} aria-current={active === 'legal-restrictions' ? 'page' : undefined}><RestrictionIcon name={group.icon} size={18} /><span>{group.label}</span></Link> : <span className={`${styles.subNavItem} ${styles.subNavDisabled}`} aria-disabled="true" key={group.key}><RestrictionIcon name={group.icon} size={18} /><span>{group.label}{group.key === 'debtors' && <small>تبصره ۲ ماده ۱۶۹ مکرر</small>}</span><small className={styles.soon}>غیرفعال</small></span>)}</div>}
          </div>}
          {navigationItems.filter(item => !['arrest', 'restriction'].includes(item.key)).map(item => <Link key={item.key} href={`/tasks?taskName=${encodeURIComponent(item.label)}`} className={styles.navItem}>
            <NavigationIcon name={item.icon} /><span>{item.label}</span><Badge value={data?.tasksError || loading ? null : tasks.filter(task => task.name.includes(item.label)).length} />
          </Link>)}
          <div className={styles.navDivider} />
          <PermissionLink allowed={canViewOperations} href="/operations" className={styles.navItem}><Icon name="reports" /><span>گزارش ها</span></PermissionLink>
          {canSeeNavItem(user, NAV_ITEMS.find(item => item.key === 'role-permissions')) && <Link href="/admin/permissions" className={`${styles.navItem} ${active === 'role-permissions' ? styles.navActive : ''}`} aria-current={active === 'role-permissions' ? 'page' : undefined}><Icon name="settings" /><span>نقش‌ها و دسترسی‌ها</span></Link>}
        </nav>
        <button className={`${styles.navItem} ${styles.settings}`} onClick={() => {
          setProfileOpen(true);
          profileRef.current?.querySelector('button')?.focus();
        }}><Icon name="settings" /><span>تنظیمات</span></button>
      </aside>

      <main className={`${styles.main} ${mainClassName}`}>
        <div className={`${styles.welcome} ${toolbarClassName}`}>
          <div className={styles.welcomeTitle}>
            {!sidebarOpen && <button ref={sidebarOpenRef} className={styles.openSidebar} onClick={openSidebar} aria-label="باز کردن منوی اصلی" aria-controls="manager-sidebar" aria-expanded={sidebarOpen}><Icon name="menu" /></button>}
            {toolbar || <h1>به سامانه جامع وصول و اجرا خوش آمدید</h1>}
          </div>
          <div className={styles.date}><PageHelp title={active === 'legal-restrictions' ? 'راهنمای محدودیت‌های قانونی' : active === 'arrest' ? 'راهنمای دستور بازداشت' : active === 'dashboard' ? 'راهنمای داشبورد' : active === 'execution-papers' ? 'راهنمای برگ‌های اجرا' : 'راهنمای نقش‌ها و دسترسی‌ها'}>{active === 'legal-restrictions' ? <RestrictionsGuide /> : active === 'arrest' ? <ArrestGuide /> : active === 'dashboard' ? <DashboardGuide /> : active === 'execution-papers' ? <PaperGuide /> : <PermissionsGuide />}</PageHelp><Icon name="calendar" /><time dateTime={now?.toISOString()} suppressHydrationWarning>{now ? formatHeaderDate(now) : '—'}</time></div>
        </div>

        {children}
      </main>
    </div>
  );
}

function Badge({ value }) {
  return <span className={styles.badge}>{value == null ? '—' : value > 999 ? '+۹۹۹' : number(value)}</span>;
}

function PermissionLink({ allowed, href, className, current, children }) {
  return allowed ? <Link href={href} className={className} aria-current={current ? 'page' : undefined}>{children}</Link>
    : <span className={`${className} ${styles.disabledNav}`} aria-disabled="true" title="شما مجوز مشاهده این بخش را ندارید.">{children}</span>;
}

function formatHeaderDate(date) {
  const options = { timeZone: 'Asia/Tehran' };
  return `${new Intl.DateTimeFormat('fa-IR-u-ca-persian', { ...options, day: 'numeric', month: 'long', year: 'numeric' }).format(date)}  ${new Intl.DateTimeFormat('fa-IR', { ...options, hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).format(date)}`;
}
