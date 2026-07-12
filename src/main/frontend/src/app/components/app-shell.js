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
  const navItems = visibleNavItems(currentUser);

  useEffect(() => {
    fetchCurrentUser()
      .then(setCurrentUser)
      .catch(() => setCurrentUser(null));
  }, []);

  return (
    <main className="app-frame">
      <aside className="app-sidebar" aria-label="منوی اصلی">
        <Link className="sidebar-brand" href="/">
          <img src="/brand/mehrpars-purple.svg" alt="لوگوی مهرپارس" />
          <span>
            <strong>مهرپارس</strong>
            <small>کارتابل فرایندهای سازمانی</small>
          </span>
        </Link>

        <div className="sidebar-section">
          <p>منوی کارتابل</p>
          <nav className="side-nav">
            {navItems.map((item, index) => (
              <Link
                className={`side-nav-item${active === item.key ? " side-nav-item--active" : ""}`}
                href={item.href}
                key={item.key}
              >
                <span className="nav-code">{String(index + 1).padStart(2, "0")}</span>
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
        <header className="topbar">
          <div className="topbar-status">
            <span className="live-dot" aria-hidden="true" />
            <span>اتصال به موتور فرایند</span>
          </div>
          <div className="topbar-links">
            {currentUser && <span className="topbar-user">{currentUser.fullName || currentUser.username}</span>}
            <button className="topbar-logout" onClick={logoutFromKeycloak}>خروج</button>
            <a href="https://www.mehrparsict.com/" target="_blank" rel="noreferrer">
              mehrparsict.com
            </a>
            <span>دموی عملیاتی</span>
          </div>
        </header>

        <section className="page-heading">
          <div>
            <p className="eyebrow">{eyebrow}</p>
            <h1>{title}</h1>
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
        </section>

        {children}
      </section>
    </main>
  );
}
