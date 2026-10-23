'use client'

import Image from 'next/image';
import { useCallback, useEffect, useState } from 'react';
import { startKeycloakLogin } from '../lib/keycloak';
import { fetchIdentityConfiguration, fetchMockUsers } from '../lib/identity-provider';
import { businessRoleLabel } from '../lib/business-roles';

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [configuration, setConfiguration] = useState(null);
  const [users, setUsers] = useState([]);
  const [username, setUsername] = useState('');
  const [configLoading, setConfigLoading] = useState(true);

  const loadConfiguration = useCallback(async () => {
    setConfigLoading(true);
    setConfiguration(null);
    setMessage('');
    try {
      const config = await fetchIdentityConfiguration();
      if (config.provider === 'mock') {
        const available = await fetchMockUsers();
        setUsers(available);
        setUsername(available.find(user => user.username === 'admin')?.username || available[0]?.username || '');
      }
      setConfiguration(config);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setConfigLoading(false);
    }
  }, []);

  useEffect(() => { void loadConfiguration(); }, [loadConfiguration]);
  const mock = configuration?.provider === 'mock';

  const login = async () => {
    setLoading(true);
    setMessage('');
    try {
      const next = new URLSearchParams(window.location.search).get('next') || '/';
      await startKeycloakLogin(next, username);
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
            <span>{mock ? 'حالت آزمایشی محلی — ورود بدون رمز عبور و بدون اتصال به Keycloak' : 'احراز هویت از طریق Keycloak و حساب سازمانی انجام می‌شود.'}</span>
            <span className="login-hint">برای ادامه، ابتدا وارد سامانه شوید.</span>
          </div>
        </div>

        {mock && <div className="mock-login-options">
          <label htmlFor="mock-user">کاربر آزمایشی</label>
          <select id="mock-user" value={username} onChange={event => setUsername(event.target.value)}>
            {users.map(user => <option key={user.username} value={user.username}>{businessRoleLabel(user.businessRole)} — {user.username}</option>)}
          </select>
          {!users.length && <p role="status">کاربر آزمایشی فعالی موجود نیست.</p>}
          {configuration.workflow === 'mock' && <p>Camunda غیرفعال است؛ کارتابل آزمایشی خالی است و فرایندها اجرا نمی‌شوند.</p>}
        </div>}
        <button className="primary-action login-action" onClick={login} disabled={loading || configLoading || !configuration || (mock && !username)}>
          {configLoading ? 'در حال دریافت تنظیمات ورود' : loading ? 'در حال ورود…' : mock ? 'ورود آزمایشی' : 'ورود با حساب سازمانی'}
        </button>

        {message && <div className="notice error" role="alert">{message}</div>}
        {!configLoading && !configuration && <button className="secondary-action" onClick={loadConfiguration}>تلاش دوباره</button>}
      </section>
    </main>
  );
}
