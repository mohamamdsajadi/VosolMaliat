'use client'

import Link from 'next/link';
import { useEffect, useState } from 'react';
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
  contextItems = []
}) {
  const [currentUser, setCurrentUser] = useState(null);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [now, setNow] = useState(new Date());
  const navItems = visibleNavItems(currentUser);

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
    <main className={`app-frame${sidebarOpen ? "" : " app-frame--sidebar-collapsed"}`}>
      <aside className="app-sidebar" aria-label="منوی اصلی">
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
              onClick={() => setSidebarOpen((current) => !current)}
              aria-label={sidebarOpen ? "بستن منوی اصلی" : "باز کردن منوی اصلی"}
              aria-expanded={sidebarOpen}
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
