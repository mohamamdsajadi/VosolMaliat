"use client";

import { useCallback, useEffect, useState } from 'react';
import { fetchCurrentUser } from './lib/security';
import ManagerDashboard from './components/manager-dashboard/manager-dashboard';
import { fetchManagerDashboard } from './lib/manager-dashboard';

export default function DashboardPage() {
  const [user, setUser] = useState(null);
  const [accessError, setAccessError] = useState('');
  const [accessAttempt, setAccessAttempt] = useState(0);

  useEffect(() => {
    let active = true;
    fetchCurrentUser().then(current => {
      if (active) setUser(current);
    }).catch(() => { if (active) setAccessError('دریافت اطلاعات کاربر ناموفق بود.'); });
    return () => { active = false; };
  }, [accessAttempt]);

  if (accessError) return <div className="dashboard-error" role="alert"><strong>{accessError}</strong><button className="primary-action" onClick={() => { setAccessError(''); setAccessAttempt(value => value + 1); }}>تلاش مجدد</button></div>;
  if (!user) return <div className="dashboard-loading" role="status">در حال دریافت میزکار...</div>;
  return <UserDashboard user={user} />;
}

function UserDashboard({ user }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const refresh = useCallback(async () => {
    setLoading(true);
    try { setData(await fetchManagerDashboard()); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { refresh(); }, [refresh]);
  return <ManagerDashboard user={user} data={data} loading={loading} onRefresh={refresh} />;
}
