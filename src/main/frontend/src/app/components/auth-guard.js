'use client'

import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState, useSyncExternalStore } from 'react';
import { getAccessToken, subscribeAuthTokens } from '../lib/api';
import { fetchIdentityConfiguration } from '../lib/identity-provider';

const PUBLIC_PATHS = ['/login', '/register', '/auth/callback'];

export default function AuthGuard({ children }) {
  const pathname = usePathname();
  const router = useRouter();
  const token = useSyncExternalStore(subscribeAuthTokens, getAccessToken, () => null);
  const isPublic = PUBLIC_PATHS.some(path => pathname === path || pathname.startsWith(`${path}/`));
  const [mode, setMode] = useState(null);

  useEffect(() => {
    let active = true;
    fetchIdentityConfiguration().then(config => { if (active) setMode(config); }).catch(() => {});
    return () => { active = false; };
  }, [pathname]);

  useEffect(() => {
    // During hydration the server snapshot is null even when storage has a session.
    if (!isPublic && !getAccessToken()) {
      const next = `${pathname}${window.location.search || ''}`;
      router.replace(`/login?next=${encodeURIComponent(next)}`);
    }
  }, [isPublic, token, pathname, router]);

  if (!isPublic && !token) {
    return (
      <main className="login-page">
        <section className="login-panel login-panel--compact">
          <p className="eyebrow">احراز هویت</p>
          <h1>در حال بررسی نشست کاربر...</h1>
        </section>
      </main>
    );
  }

  return <>
    {mode && (mode.provider === 'mock' || mode.workflow === 'mock') && <div className="mock-mode-banner" role="status">
      حالت آزمایشی — {mode.provider === 'mock' ? 'ورود محلی' : 'ورود سازمانی'}
      {mode.workflow === 'mock' ? '؛ Camunda غیرفعال و کارتابل خالی است' : ''}
    </div>}
    {children}
  </>;
}
