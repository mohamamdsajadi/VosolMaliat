'use client';
import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import FigmaShell from '../manager-dashboard/figma-shell';
import Icon from '../manager-dashboard/dashboard-icon';
import { BUSINESS_ROLES } from '../../lib/business-roles';
import { isAdminUser } from '../../lib/navigation';
import { fetchCurrentUser, fetchPermissions, fetchGroupPermissions, assignGroupPermission, revokeGroupPermission } from '../../lib/security';
import styles from './permissions.module.css';

const LABELS = {
  LEGAL_RESTRICTION_VIEW: 'مشاهده درخواست‌ها، اشخاص مشمول و سوابق محدودیت‌های قانونی',
  ARREST_ORDER_VIEW: 'مشاهده دستورهای بازداشت و برگ‌های آماده صدور', ARREST_ORDER_START: 'آغاز دستور بازداشت و تهیه پیش‌نویس',
  ARREST_ORDER_APPROVE: 'تأیید و صدور دستور بازداشت', ARREST_ORDER_ASSIGN: 'ارجاع دستور بازداشت به مأمور اجرا',
  USER_PROFILE_CREATE: 'ایجاد کاربر و ورود دستی داده', USER_PROFILE_UPDATE: 'ویرایش کاربران', USER_PROFILE_VIEW: 'مشاهده کاربران',
  USER_PROFILE_ENABLE_DISABLE: 'فعال و غیرفعال کردن کاربران', USER_PROFILE_ASSIGN_ORGANIZATION: 'تخصیص سازمان به کاربر',
  PERMISSION_VIEW: 'مشاهده فهرست مجوزها', PERMISSION_ASSIGN: 'ایجاد مجوز و مدیریت قوانین', GROUP_PERMISSION_VIEW: 'مشاهده پنل نقش‌ها و دسترسی‌ها',
  GROUP_PERMISSION_UPDATE: 'اعطا و لغو دسترسی نقش‌ها', TASK_VIEW_OWN: 'مشاهده کارهای اختصاص‌یافته', TASK_VIEW_CANDIDATE: 'مشاهده کارهای قابل اخذ',
  TASK_CLAIM: 'اخذ کار', TASK_UNCLAIM: 'رها کردن کار', TASK_COMPLETE: 'تکمیل کار', TASK_ASSIGN: 'تخصیص کار به کاربر', TASK_DELEGATE: 'تفویض کار',
  TASK_VIEW_ORG_SCOPE: 'مشاهده کارها در محدوده سازمانی', EXECUTION_PAPER_LIST_VIEW: 'مشاهده فهرست برگ‌های اجرا',
  EXECUTION_PAPER_DETAIL_VIEW: 'مشاهده جزئیات برگ اجرا', EXECUTION_PAPER_TRACE_VIEW: 'مشاهده سوابق برگ اجرا', REPORT_VIEW: 'مشاهده گزارش‌ها', AUDIT_VIEW: 'مشاهده سوابق ممیزی',
};
const GROUPS = [
  { title: 'محدودیت‌های قانونی', accepts: code => code.startsWith('LEGAL_RESTRICTION_') },
  { title: 'دستور بازداشت اموال', accepts: code => code.startsWith('ARREST_ORDER_') },
  { title: 'برگ‌های اجرا و گزارش‌ها', accepts: code => code.startsWith('EXECUTION_') || ['REPORT_VIEW', 'AUDIT_VIEW'].includes(code) },
  { title: 'کارتابل و اقدامات اجرایی', accepts: code => code.startsWith('TASK_') },
  { title: 'مدیریت کاربران و کنترل دسترسی', accepts: code => code.startsWith('USER_') || code.includes('PERMISSION') },
  { title: 'سایر مجوزها', accepts: code => !code.startsWith('LEGAL_RESTRICTION_') && !code.startsWith('ARREST_ORDER_') && !code.startsWith('EXECUTION_') && !code.startsWith('TASK_') && !code.startsWith('USER_') && !code.includes('PERMISSION') && !['REPORT_VIEW', 'AUDIT_VIEW'].includes(code) },
];
const has = (profile, code) => isAdminUser(profile) || [...(profile?.allowedActions || []), ...(profile?.localBusinessPermissions || [])].includes(code);

export default function RolePermissionsPage() {
  const [user, setUser] = useState(null);
  const [permissions, setPermissions] = useState([]);
  const [mappings, setMappings] = useState([]);
  const [role, setRole] = useState(BUSINESS_ROLES[0].value);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const canRead = has(user, 'GROUP_PERMISSION_VIEW') && has(user, 'PERMISSION_VIEW');
  const canEdit = has(user, 'GROUP_PERMISSION_UPDATE');
  const selected = BUSINESS_ROLES.find(item => item.value === role);
  const admin = role === 'ADMIN';
  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const current = await fetchCurrentUser(); setUser(current);
      if (has(current, 'GROUP_PERMISSION_VIEW') && has(current, 'PERMISSION_VIEW')) {
        const [catalog, grants] = await Promise.all([fetchPermissions(), fetchGroupPermissions()]);
        setPermissions(catalog); setMappings(grants);
      }
    } catch (failure) { setError(failure.message || 'دریافت دسترسی‌ها ناموفق بود.'); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { load(); }, [load]);
  const toggle = async (permission, grant) => {
    setSaving(permission.code); setError(''); setNotice('');
    try {
      if (grant) {
        await revokeGroupPermission(grant.id);
        setMappings(current => current.filter(item => item.id !== grant.id));
      } else {
        const created = await assignGroupPermission(role, permission.code);
        setMappings(current => [...current, created]);
      }
      setUser(await fetchCurrentUser());
      setNotice('تغییر دسترسی ذخیره شد.');
    } catch (failure) { setError(failure.message || 'ذخیره دسترسی ناموفق بود.'); }
    finally { setSaving(''); }
  };
  const count = admin ? permissions.filter(item => item.enabled).length : mappings.filter(item => item.groupName === role && item.permission?.enabled).length;
  return <FigmaShell user={user} active="role-permissions" toolbar={<nav aria-label="مسیر صفحه"><Link href="/">سامانه جامع وصول و اجرا</Link> / نقش‌ها و دسترسی‌ها</nav>}>
    <section className={styles.page} aria-busy={loading}>
      <header className={styles.heading}><div><h1>مدیریت نقش‌ها و دسترسی‌ها</h1><p>انتخاب نقش و تنظیم دسترسی به بخش‌ها و عملیات سامانه</p></div><button onClick={load} disabled={loading || !!saving}>بروزرسانی</button></header>
      {error && <div className={styles.error} role="alert">{error}<button onClick={load} disabled={!!saving}>تلاش مجدد</button></div>}
      <p role="status" className={styles.notice}>{notice}</p>
      {loading ? <p className={styles.empty} role="status">در حال دریافت نقش‌ها و دسترسی‌ها…</p> : !canRead ? <p className={styles.empty}>شما مجوز مشاهده این پنل را ندارید.</p> : <div className={styles.layout}>
        <aside className={styles.roles} aria-label="نقش‌های سامانه"><h2>نقش‌ها <span>{BUSINESS_ROLES.length.toLocaleString('fa-IR')}</span></h2>{BUSINESS_ROLES.map(item => <button key={item.value} aria-pressed={role === item.value} disabled={!!saving} onClick={() => { setRole(item.value); setError(''); setNotice(''); }}><span>{item.label}</span>{item.value === 'ADMIN' && <small>دسترسی کامل</small>}</button>)}</aside>
        <div className={styles.access}><header className={styles.roleHeading}><div><h2>{selected.label}</h2><p>{selected.national ? 'محدوده سازمانی: سراسری' : 'محدوده سازمانی: اداره کل مرتبط با کاربر'}</p></div><span>{count.toLocaleString('fa-IR')} مجوز فعال</span></header>
          {GROUPS.map(group => {
            const items = permissions.filter(item => group.accepts(item.code));
            return items.length > 0 && <fieldset key={group.title} className={styles.group}><legend>{group.title}</legend><div className={styles.grid}>{items.map(permission => {
              const grant = mappings.find(item => item.groupName === role && item.permission?.code === permission.code);
              return <label key={permission.code} className={styles.permission}><input type="checkbox" checked={admin || !!grant} disabled={admin || !canEdit || !!saving || !permission.enabled} onChange={() => toggle(permission, grant)} /><span>{LABELS[permission.code] || permission.description || permission.code}{!permission.enabled && <small>مجوز غیرفعال</small>}{saving === permission.code && <small>در حال ذخیره…</small>}</span></label>;
            })}</div></fieldset>;
          })}
          {permissions.length === 0 && <p className={styles.empty}>مجوزی در سامانه تعریف نشده است.</p>}
          <p className={styles.note}><Icon name="info" />تغییرات برای همه کاربران نقش انتخاب‌شده ذخیره می‌شود. دسترسی هر صفحه ممکن است به چند مجوز وابسته باشد؛ محدودیت سازمانی و قواعد فرایند نیز برقرار است.</p>
        </div>
      </div>}
    </section>
  </FigmaShell>;
}
