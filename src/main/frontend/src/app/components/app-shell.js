'use client'

import Link from 'next/link';
import { useEffect, useRef, useState } from 'react';
import { logoutFromKeycloak } from '../lib/keycloak';
import { visibleNavItems } from '../lib/navigation';
import { fetchCurrentUser } from '../lib/security';

export default function AppShell({
  active = "processes",
  title,
  eyebrow = "میز عملیات",
  subtitle,
  actions,
  children,
  processLinks = [],
  contextItems = [],
  mobileDrawer = false
}) {
  const [currentUser, setCurrentUser] = useState(null);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [now, setNow] = useState(new Date());
  const toggleRef = useRef(null);
  const drawerCloseRef = useRef(null);
  const navItems = visibleNavItems(currentUser);
  const closeSidebar = () => { setSidebarOpen(false); toggleRef.current?.focus(); };
  useEffect(() => {
    if (!mobileDrawer) return;
    const frame = window.requestAnimationFrame(() => { if (window.innerWidth <= 920) setSidebarOpen(false); });
    const escape = event => { if (event.key === 'Escape' && window.innerWidth <= 920) { setSidebarOpen(false); toggleRef.current?.focus(); } };
    document.addEventListener('keydown', escape);
    return () => { window.cancelAnimationFrame(frame); document.removeEventListener('keydown', escape); };
  }, [mobileDrawer]);

  useEffect(() => {
    fetchCurrentUser()
      .then(setCurrentUser)
      .catch(() => setCurrentUser(null));
  }, []);

  useEffect(() => {
    const interval = window.setInterval(() => setNow(new Date()), 1000);
    return () => window.clearInterval(interval);
  }, []);

  return (
    <main className={`app-frame${sidebarOpen ? "" : " app-frame--sidebar-collapsed"}${mobileDrawer ? ' app-frame--mobile-drawer' : ''}`}>
      {mobileDrawer && sidebarOpen && <button className="app-sidebar-backdrop" type="button" onClick={closeSidebar} aria-label="بستن منوی اصلی" />}
      <aside id="app-sidebar" className="app-sidebar" aria-label="منوی اصلی" inert={!sidebarOpen} onKeyDown={event => {
        if (!mobileDrawer || window.innerWidth > 920 || event.key !== 'Tab') return;
        const controls = Array.from(event.currentTarget.querySelectorAll('a[href],button:not(:disabled)'));
        const first = controls[0], last = controls.at(-1);
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
      }}>
        {mobileDrawer && <button ref={drawerCloseRef} className="app-sidebar-close" type="button" onClick={closeSidebar} aria-label="بستن منوی کارتابل">×</button>}
        <Link className="sidebar-brand" href="/">
          <img src="/images/iran-tax-logo.png" alt="سازمان امور مالیاتی کشور" />
          <span>
            <strong>سازمان امور مالیاتی کشور</strong>
            <small>سامانه وصول مالیات</small>
          </span>
        </Link>

        <div className="sidebar-section">
          <p>منوی کارتابل</p>
          <nav className="side-nav">
            {navItems.map((item) => (
              <Link
                className={`side-nav-item${active === item.key ? " side-nav-item--active" : ""}`}
                href={item.href}
                key={item.key}
              >
                <span className="side-nav-icon" aria-hidden="true">
                  <MenuIcon name={item.key} />
                </span>
                <span>
                  <strong>{item.label}</strong>
                  <small>{item.helper}</small>
                </span>
              </Link>
            ))}
          </nav>
        </div>

        {processLinks.length > 0 && (
          <div className="sidebar-section sidebar-section--processes">
            <p>فرایندهای آماده</p>
            <div className="side-process-list">
              {processLinks.slice(0, 5).map((item) => (
                <Link
                  className="side-process-item"
                  href={`/processes/${encodeURIComponent(item.processDefinitionKey)}/tasks`}
                  key={item.processDefinitionKey}
                >
                  <span>{item.title}</span>
                  <small>صف کار</small>
                </Link>
              ))}
            </div>
          </div>
        )}

        <div className="sidebar-footer">
          <span>سناریوی دمو</span>
          <strong>مرکز عملیات درخواست‌های سازمانی</strong>
        </div>
      </aside>

      <section className="app-main">
        <header className="topbar formal-header">
          <div className="formal-header__identity">
            <button
              className={`sidebar-toggle${sidebarOpen ? " sidebar-toggle--open" : ""}`}
              type="button"
              ref={toggleRef}
              onClick={() => {
                setSidebarOpen(!sidebarOpen);
                if (!sidebarOpen && mobileDrawer && window.innerWidth <= 920) window.requestAnimationFrame(() => drawerCloseRef.current?.focus());
              }}
              aria-label={sidebarOpen ? "بستن منوی اصلی" : "باز کردن منوی اصلی"}
              aria-expanded={sidebarOpen}
              aria-controls="app-sidebar"
            >
              <span />
              <span />
              <span />
            </button>
            <img src="/images/iran-tax-logo.png" alt="سازمان امور مالیاتی کشور" />
            <div>
              <strong>سازمان امور مالیاتی کشور</strong>
              <small>سامانه جامع وصول مالیات</small>
            </div>
          </div>
          <div className="formal-header__page">
            <span>{eyebrow}</span>
            <strong>{title}</strong>
          </div>
          <div className="formal-header__meta">
            <div className="formal-header__datetime">
              <strong>{formatPersianDate(now)}</strong>
              <span>{formatPersianTime(now)}</span>
            </div>
            <div className="formal-header__user">
              <span className="user-avatar">{userInitial(currentUser)}</span>
              <strong>{currentUser?.fullName || currentUser?.username || "نامشخص"}</strong>
            </div>
            <button className="topbar-logout" onClick={logoutFromKeycloak}>خروج</button>
          </div>
        </header>

        {(subtitle || contextItems.length > 0 || actions) && (
          <div className="shell-toolbar">
            <div className="shell-toolbar__content">
              {subtitle && <p className="page-subtitle">{subtitle}</p>}
              {contextItems.length > 0 && (
                <div className="context-strip">
                  {contextItems.map((item) => (
                    <span key={item}>{item}</span>
                  ))}
                </div>
              )}
            </div>
            {actions && <div className="heading-actions">{actions}</div>}
          </div>
        )}

        {children}
      </section>
    </main>
  );
}

function formatPersianDate(date) {
  return new Intl.DateTimeFormat("fa-IR-u-ca-persian", {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric"
  }).format(date);
}

function formatPersianTime(date) {
  return new Intl.DateTimeFormat("fa-IR", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false
  }).format(date);
}

function userInitial(currentUser) {
  const name = currentUser?.fullName || currentUser?.username || "؟";
  return name.trim().slice(0, 1);
}

function MenuIcon({ name }) {
  const common = {
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.8,
    strokeLinecap: "round",
    strokeLinejoin: "round"
  };

  switch (name) {
    case "processes":
      return (
        <svg viewBox="0 0 24 24" {...common}>
          <rect x="4" y="4" width="6" height="6" rx="1.5" />
          <rect x="14" y="4" width="6" height="6" rx="1.5" />
          <rect x="9" y="14" width="6" height="6" rx="1.5" />
          <path d="M10 7h4M17 10v2.5L15 15M7 10v2.5L9 15" />
        </svg>
      );
    case "execution-papers":
      return (
        <svg viewBox="0 0 24 24" {...common}>
          <path d="M7 3h7l4 4v14H7z" />
          <path d="M14 3v5h5M9 12h6M9 16h6" />
        </svg>
      );
    case "tasks":
      return (
        <svg viewBox="0 0 24 24" {...common}>
          <path d="M6 5h12v14H6z" />
          <path d="M9 9l1.2 1.2L13 7.8M15 10h1M9 15l1.2 1.2L13 13.8M15 16h1" />
        </svg>
      );
    case "operations":
      return (
        <svg viewBox="0 0 24 24" {...common}>
          <path d="M4 18V8M10 18V5M16 18v-7M21 20H3" />
          <path d="M7 10l3-3 3 4 3-2 4 4" />
        </svg>
      );
    case "admin":
    case "users":
      return (
        <svg viewBox="0 0 24 24" {...common}>
          <path d="M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM3.5 19c.7-3.2 2.6-5 5.5-5s4.8 1.8 5.5 5" />
          <path d="M16 10a2.5 2.5 0 1 0 0-5M16 14c2.3.2 3.8 1.8 4.5 4" />
        </svg>
      );
    case "permissions":
      return (
        <svg viewBox="0 0 24 24" {...common}>
          <path d="M8 11V8a4 4 0 0 1 8 0v3" />
          <rect x="5" y="11" width="14" height="10" rx="2" />
          <path d="M12 15v2" />
        </svg>
      );
    case "organizations":
      return (
        <svg viewBox="0 0 24 24" {...common}>
          <path d="M4 20h16M6 20V8l6-4 6 4v12" />
          <path d="M9 20v-6h6v6M9 10h.01M12 10h.01M15 10h.01" />
        </svg>
      );
    case "rule-management":
    case "rules":
      return (
        <svg viewBox="0 0 24 24" {...common}>
          <path d="M7 4h10v16H7z" />
          <path d="M9.5 9h5M9.5 13h5M9.5 17h3" />
          <path d="M15.5 6.5l1 1 2-2" />
        </svg>
      );
    case "inform-rules":
      return (
        <svg viewBox="0 0 24 24" {...common}>
          <path d="M5 6h14v10H8l-3 3z" />
          <path d="M9 10h6M9 13h4M17 4l2 2 2-2" />
        </svg>
      );
    case "test-front":
      return (
        <svg viewBox="0 0 24 24" {...common}>
          <path d="M9 3h6M10 3v5l-5 9a2 2 0 0 0 1.8 3h10.4a2 2 0 0 0 1.8-3l-5-9V3" />
          <path d="M8 14h8M10 17h.01M14 17h.01" />
        </svg>
      );
    case "data-entry":
      return (
        <svg viewBox="0 0 24 24" {...common}>
          <path d="M5 20h14" />
          <path d="M7 17l1-4 7.5-7.5a2.1 2.1 0 0 1 3 3L11 16z" />
          <path d="M14.5 6.5l3 3" />
        </svg>
      );
    default:
      return (
        <svg viewBox="0 0 24 24" {...common}>
          <circle cx="12" cy="12" r="7" />
          <path d="M12 8v4l3 2" />
        </svg>
      );
  }
}
