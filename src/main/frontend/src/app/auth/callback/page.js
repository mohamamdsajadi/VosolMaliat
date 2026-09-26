'use client'

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { completeKeycloakLogin, consumeLoginNext } from '../../lib/keycloak';

export default function AuthCallbackPage() {
  const [message, setMessage] = useState('در حال تکمیل ورود...');
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const complete = async () => {
      const params = new URLSearchParams(window.location.search);
      try {
        await completeKeycloakLogin(params.get('code'), params.get('state'));
        window.location.replace(consumeLoginNext());
      } catch (error) {
        setFailed(true);
        setMessage(error.message || 'تکمیل ورود ناموفق بود.');
      }
    };

    complete();
  }, []);

  return (
    <main className="login-page">
      <section className="login-panel login-panel--compact">
        <p className="eyebrow">Keycloak</p>
        <h1>{message}</h1>
        {failed && <Link className="primary-action login-action" href="/login">بازگشت به ورود</Link>}
      </section>
    </main>
  );
}
