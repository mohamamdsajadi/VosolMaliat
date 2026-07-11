'use client'

import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { getAccessToken } from '../lib/api';

const PUBLIC_PATHS = ['/login', '/auth/callback'];

export default function AuthGuard({ children }) {
  const pathname = usePathname();
  const router = useRouter();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const isPublic = PUBLIC_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`));
    if (isPublic) {
      setReady(true);
      return;
    }
    if (!getAccessToken()) {
      const next = `${pathname}${window.location.search || ''}`;
      router.replace(`/login?next=${encodeURIComponent(next)}`);
      return;
    }
    setReady(true);
  }, [pathname, router]);

  if (!ready) {
    return (
      <main className="login-page">
        <section className="login-panel login-panel--compact">
          <p className="eyebrow">احراز هویت</p>
          <h1>در حال بررسی نشست کاربر...</h1>
        </section>
      </main>
    );
  }

  return children;
}
