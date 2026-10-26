import { apiFetch, clearAuthTokens, setAuthTokens } from './api';

export async function fetchIdentityConfiguration() {
  const response = await fetch('/api/v1/auth/config', { cache: 'no-store' });
  if (!response.ok) throw new Error('دریافت تنظیمات ورود از سرور ناموفق بود.');
  const config = await response.json();
  if (!['keycloak', 'mock'].includes(config.provider)) throw new Error('نوع سرویس ورود معتبر نیست.');
  return config;
}

export function safeLoginNext(value) {
  // Only navigate within this application, including after a mock login.
  return typeof value === 'string' && value.startsWith('/') && !value.startsWith('//')
    && !value.includes('\\') && !/[\u0000-\u001f]/.test(value) ? value : '/';
}

export async function fetchMockUsers() {
  const response = await fetch('/api/v1/auth/mock/users', { cache: 'no-store' });
  if (!response.ok) throw new Error('دریافت کاربران ناموفق بود.');
  return response.json();
}

export async function loginWithMockIdentity(username, nextPath) {
  const response = await fetch('/api/v1/auth/mock/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username }),
  });
  if (!response.ok) throw new Error('ورود ناموفق بود؛ وضعیت کاربر را بررسی کنید.');
  const tokens = await response.json();
  clearAuthTokens();
  setAuthTokens(tokens);
  window.location.assign(safeLoginNext(nextPath));
}

export async function logoutMockIdentity() {
  const response = await apiFetch('/api/v1/auth/mock/logout', { method: 'POST' });
  if (!response.ok && response.status !== 401) throw new Error('خروج ناموفق بود؛ دوباره تلاش کنید.');
  clearAuthTokens();
  window.location.assign('/login');
}
