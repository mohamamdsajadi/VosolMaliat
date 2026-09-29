'use client'

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import AppShell from '../components/app-shell';
import RefreshButton from '../components/refresh-button';
import { fetchCurrentUser, fetchGroupPermissions, fetchPermissions, fetchTaxAffairsOffices, fetchUsers } from '../lib/security';

function enabledUsers(users) {
  return users.filter((user) => user.enabledInBusinessSystem).length;
}

function moduleCards(users, permissions, offices) {
  return [
    {href:'/admin/ef03-receipts',eyebrow:'دریافت',title:'دریافت EF03',description:'ورود فایل برگه سنیم و ثبت درخواست صدور.',stat:'',statLabel:''},
    {
      href: '/admin/users',
      eyebrow: 'کاربران',
      title: 'مدیریت کاربران',
      description: 'ایجاد و ویرایش پروفایل‌های متصل به Keycloak، نقش کسب‌وکاری و وضعیت دسترسی.',
      stat: users.length,
      statLabel: 'پروفایل'
    },
    {
      href: '/admin/permissions',
      eyebrow: 'دسترسی',
      title: 'مجوزها',
      description: 'اعطا و لغو مجوزهای کسب‌وکاری برای نقش‌ها و گروه‌های سامانه.',
      stat: permissions.length,
      statLabel: 'مجوز'
    },
    {
      href: '/admin/organizations',
      eyebrow: 'سازمان',
      title: 'واحدهای سازمانی',
      description: 'مدیریت اداره‌های کل و اداره‌های مالیاتی مرتبط با کاربران و پرونده‌ها.',
      stat: offices.length,
      statLabel: 'اداره'
    }
  ];
}

export default function AdminDashboardPage() {
  const [currentUser, setCurrentUser] = useState(null);
  const [users, setUsers] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [groupPermissions, setGroupPermissions] = useState([]);
  const [offices, setOffices] = useState([]);
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
      fetchTaxAffairsOffices()
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
      setOffices(organizationResult.value);
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
        `اداره‌ها: ${offices.length.toLocaleString('fa-IR')}`
      ]}
      actions={<RefreshButton onClick={loadAdmin} loading={loading} />}
    >
      {message && <div className="notice error">{message}</div>}

      <section className="admin-control-hero">
        <div className="admin-control-hero__main">
          <p className="eyebrow">کنترل دسترسی و ساختار</p>
          <h2>مدیریت سامانه</h2>
          <span>نمای متمرکز برای کاربران، مجوزها و واحدهای سازمانی سامانه وصول و اجرا.</span>
        </div>
        <div className="admin-control-hero__profile">
          <span>کاربر جاری</span>
          <strong>{currentUser?.fullName || currentUser?.username || 'نامشخص'}</strong>
          <small>{currentUser?.businessRole || currentUser?.keycloakRoles?.join('، ') || 'بدون نقش'}</small>
        </div>
      </section>

      <section className="admin-control-stats">
        <article>
          <span>کاربران فعال</span>
          <strong>{enabledUsers(users).toLocaleString('fa-IR')}</strong>
          <small>از {users.length.toLocaleString('fa-IR')} پروفایل</small>
        </article>
        <article>
          <span>مجوزهای تعریف‌شده</span>
          <strong>{permissions.length.toLocaleString('fa-IR')}</strong>
          <small>{groupPermissions.length.toLocaleString('fa-IR')} نگاشت نقش/گروه</small>
        </article>
        <article>
          <span>اداره‌های مالیاتی</span>
          <strong>{offices.length.toLocaleString('fa-IR')}</strong>
          <small>متصل به اداره‌های کل</small>
        </article>
      </section>

      <section className="admin-module-grid">
        {moduleCards(users, permissions, offices).map((card) => (
          <Link className="admin-module-card" href={card.href} key={card.href}>
            <div>
              <p className="eyebrow">{card.eyebrow}</p>
              <h2>{card.title}</h2>
              <span>{card.description}</span>
            </div>
            <footer>
              <strong>{card.stat.toLocaleString('fa-IR')}</strong>
              <span>{card.statLabel}</span>
            </footer>
          </Link>
        ))}
      </section>

      <section className="admin-preview-grid">
        <div className="admin-preview-panel">
          <div className="admin-preview-panel__header">
            <div>
              <p className="eyebrow">کاربران</p>
              <h2>آخرین پروفایل‌ها</h2>
            </div>
            <Link href="/admin/users">مشاهده همه</Link>
          </div>
          <div className="admin-preview-list">
            {users.slice(0, 6).map((user) => (
              <div className="admin-preview-row" key={user.id}>
                <div>
                  <strong>{user.fullName || user.username}</strong>
                  <span>{user.username} · {user.email || 'بدون ایمیل'}</span>
                </div>
                <em className={user.enabledInBusinessSystem ? 'admin-preview-status admin-preview-status--ok' : 'admin-preview-status'}>
                  {user.enabledInBusinessSystem ? 'فعال' : 'غیرفعال'}
                </em>
              </div>
            ))}
            {!loading && users.length === 0 && <div className="empty-state compact">هنوز پروفایل کاربری ثبت نشده است.</div>}
          </div>
        </div>

        <div className="admin-preview-panel">
          <div className="admin-preview-panel__header">
            <div>
              <p className="eyebrow">سازمان</p>
              <h2>اداره‌های مالیاتی</h2>
            </div>
            <Link href="/admin/organizations">مدیریت ساختار</Link>
          </div>
          <div className="admin-preview-list">
            {offices.slice(0, 6).map((office) => (
              <div className="admin-preview-row" key={office.id}>
                <div>
                  <strong>{office.officeName}</strong>
                  <span>{office.generalDirectorateName || 'بدون اداره کل'}</span>
                </div>
                <em className="admin-preview-status admin-preview-status--ok">ثبت‌شده</em>
              </div>
            ))}
            {!loading && offices.length === 0 && <div className="empty-state compact">هنوز اداره مالیاتی ثبت نشده است.</div>}
          </div>
        </div>
      </section>

      <section className="admin-access-panel">
        <div className="admin-preview-panel__header">
          <div>
            <p className="eyebrow">مجوزها</p>
            <h2>نگاشت‌های فعال نقش/گروه</h2>
          </div>
          <Link href="/admin/permissions">مدیریت مجوزها</Link>
        </div>
        <div className="admin-access-cloud">
          {groupPermissions.slice(0, 18).map((item) => (
            <span key={item.id}>{item.groupName}: {item.permission?.code}</span>
          ))}
          {!loading && groupPermissions.length === 0 && <div className="empty-state compact">هنوز نگاشت مجوزی ثبت نشده است.</div>}
        </div>
      </section>
    </AppShell>
  );
}
