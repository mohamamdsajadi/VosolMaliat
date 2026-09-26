'use client'

import Image from 'next/image';
import { useState } from 'react';
import { startKeycloakLogin } from '../lib/keycloak';

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const login = async () => {
    setLoading(true);
    setMessage('');
    try {
      const next = new URLSearchParams(window.location.search).get('next') || '/';
      await startKeycloakLogin(next);
    } catch (error) {
      setMessage(error.message || 'شروع ورود با خطا روبه‌رو شد.');
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-panel">
        <div className="login-brand">
          <Image
            src="/images/iran-tax-logo.png"
            alt="سازمان امور مالیاتی کشور"
            width={96}
            height={96}
            priority
          />
          <div>
            <p className="eyebrow">سامانه وصول و اجرای مالیاتی</p>
            <h1>ورود کاربران سازمان امور مالیاتی</h1>
            <span>احراز هویت از طریق Keycloak و حساب سازمانی انجام می‌شود.</span>
            <span className="login-hint">برای ادامه، ابتدا وارد سامانه شوید.</span>
          </div>
        </div>

        <button className="primary-action login-action" onClick={login} disabled={loading}>
          {loading ? 'در حال انتقال به Keycloak' : 'ورود با حساب سازمانی'}
        </button>

        {message && <div className="notice error">{message}</div>}
      </section>
    </main>
  );
}
