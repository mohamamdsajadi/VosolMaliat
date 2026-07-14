'use client'

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import AppShell from '../components/app-shell';
import { fetchCurrentUser, fetchGroupPermissions, fetchPermissions, fetchTaxUnits, fetchUsers } from '../lib/security';

export default function AdminDashboardPage() {
  const [currentUser, setCurrentUser] = useState(null);
  const [users, setUsers] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [groupPermissions, setGroupPermissions] = useState([]);
  const [taxUnits, setTaxUnits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');

  const loadAdmin = useCallback(async () => {
    setLoading(true);
    setMessage('');
    const [meResult, usersResult, permissionsResult, groupPermissionsResult, organizationResult] = await Promise.allSettled([
      fetchCurrentUser(),
      fetchUsers(),
      fetchPermissions(),
      fetchGroupPermissions(),
      fetchTaxUnits()
    ]);

    if (meResult.status === 'fulfilled') {
      setCurrentUser(meResult.value);
    }
    if (usersResult.status === 'fulfilled') {
      setUsers(usersResult.value);
    }
    if (permissionsResult.status === 'fulfilled') {
      setPermissions(permissionsResult.value);
    }
    if (groupPermissionsResult.status === 'fulfilled') {
      setGroupPermissions(groupPermissionsResult.value);
    }
    if (organizationResult.status === 'fulfilled') {
      setTaxUnits(organizationResult.value);
    }

    const failed = [meResult, usersResult, permissionsResult, groupPermissionsResult, organizationResult].find((item) => item.status === 'rejected');
    if (failed) {
      setMessage(failed.reason?.message || 'دریافت اطلاعات مدیریتی ناموفق بود.');
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    loadAdmin();
  }, [loadAdmin]);

  return (
    <AppShell
      active="admin"
      title="داشبورد مدیریت سامانه"
      eyebrow="مدیریت دسترسی"
      contextItems={[
        `کاربران: ${users.length.toLocaleString('fa-IR')}`,
        `مجوزها: ${permissions.length.toLocaleString('fa-IR')}`,
        `واحدها: ${taxUnits.length.toLocaleString('fa-IR')}`
      ]}
      actions={<button className="ghost-action" onClick={loadAdmin} disabled={loading}>{loading ? 'در حال بروزرسانی' : 'بروزرسانی'}</button>}
    >
      {message && <div className="notice error">{message}</div>}

      <section className="admin-grid">
        <article className="metric-card">
          <span>کاربر جاری</span>
          <strong>{currentUser?.username || 'نامشخص'}</strong>
          <small>{currentUser?.fullName || 'اطلاعات پروفایل در دسترس نیست'}</small>
        </article>
        <article className="metric-card metric-card--success">
          <span>دسترسی‌های کاربر جاری</span>
          <strong>{(currentUser?.localBusinessPermissions?.length || 0).toLocaleString('fa-IR')}</strong>
          <small>{currentUser?.keycloakRoles?.join('، ') || 'بدون نقش'}</small>
        </article>
        <article className="metric-card metric-card--warning">
          <span>واحدهای مالیاتی</span>
          <strong>{taxUnits.length.toLocaleString('fa-IR')}</strong>
          <small>اداره کل، اداره مالیات و واحد مالیاتی</small>
        </article>
      </section>

      <section className="panel-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">ورود دستی داده</p>
            <h2>ثبت برگ بدهی قطعی و برگ اجرا</h2>
          </div>
          <Link className="text-action" href="/admin/data-entry">رفتن به داشبورد داده</Link>
        </div>
        <div className="empty-state compact">
          <strong>ثبت دستی موجودیت‌های عملیاتی</strong>
          <span>از این بخش می‌توانید برگ بدهی قطعی را با فرم کامل وارد کنید، آن را به Camunda ارسال کنید، یا برگ اجرا را مستقیما از داده‌های موجود بسازید.</span>
        </div>
      </section>

      <section className="panel-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">کاربران</p>
            <h2>پروفایل‌های متصل به Keycloak</h2>
          </div>
          <Link className="text-action" href="/admin/users">مدیریت کاربران</Link>
        </div>
        <div className="admin-table">
          {users.slice(0, 8).map((user) => (
            <div className="admin-row" key={user.id}>
              <div>
                <strong>{user.fullName || user.username}</strong>
                <span>{user.username} · {user.email || 'بدون ایمیل'}</span>
              </div>
              <span className={`status-pill ${user.enabledInBusinessSystem ? 'status-pill--ready' : 'status-pill--loading'}`}>
                {user.enabledInBusinessSystem ? 'فعال' : 'غیرفعال'}
              </span>
            </div>
          ))}
          {!loading && users.length === 0 && <div className="empty-state compact">هنوز پروفایل کاربری ثبت نشده است.</div>}
        </div>
      </section>

      <section className="panel-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">مجوزها</p>
            <h2>نگاشت نقش/گروه به مجوزهای کسب‌وکاری</h2>
          </div>
          <Link className="text-action" href="/admin/permissions">مدیریت مجوزها</Link>
        </div>
        <div className="permission-cloud">
          {groupPermissions.slice(0, 24).map((item) => (
            <span key={item.id}>{item.groupName}: {item.permission?.code}</span>
          ))}
        </div>
      </section>

      <section className="panel-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">سازمان</p>
            <h2>ساختار واحدهای مالیاتی</h2>
          </div>
          <Link className="text-action" href="/admin/organizations">مدیریت واحدها</Link>
        </div>
        <div className="admin-table">
          {taxUnits.slice(0, 8).map((unit) => (
            <div className="admin-row" key={unit.id}>
              <div>
                <strong>{unit.taxUnitName}</strong>
                <span>{unit.taxAffairsOfficeName || 'بدون اداره مالیات'} · {unit.generalDirectorateName || 'بدون اداره کل'}</span>
              </div>
              <span className="status-pill status-pill--ready">ثبت‌شده</span>
            </div>
          ))}
        </div>
      </section>
    </AppShell>
  );
}
