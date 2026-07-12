'use client'

import { useCallback, useEffect, useMemo, useState } from 'react';
import AppShell from '../../components/app-shell';
import {
  assignGroupPermission,
  fetchGroupPermissions,
  fetchPermissions,
  revokeGroupPermission
} from '../../lib/security';

const initialForm = {
  groupName: 'TAX_UNIT_USER',
  permissionCode: ''
};

const permissionLabels = {
  USER_PROFILE_CREATE: 'ایجاد پروفایل کاربر',
  USER_PROFILE_UPDATE: 'ویرایش پروفایل کاربر',
  USER_PROFILE_VIEW: 'مشاهده پروفایل کاربران',
  USER_PROFILE_ENABLE_DISABLE: 'فعال/غیرفعال کردن کاربر',
  USER_PROFILE_ASSIGN_ORGANIZATION: 'اختصاص واحد سازمانی به کاربر',
  PERMISSION_VIEW: 'مشاهده مجوزها',
  PERMISSION_ASSIGN: 'اعطای مجوز',
  GROUP_PERMISSION_VIEW: 'مشاهده مجوزهای نقش/گروه',
  GROUP_PERMISSION_UPDATE: 'ویرایش مجوزهای نقش/گروه',
  TASK_VIEW_OWN: 'مشاهده کارهای اختصاص‌یافته به خود',
  TASK_VIEW_CANDIDATE: 'مشاهده کارهای قابل اخذ',
  TASK_CLAIM: 'اخذ کار',
  TASK_UNCLAIM: 'رها کردن کار',
  TASK_COMPLETE: 'تکمیل کار',
  TASK_ASSIGN: 'ارجاع کار به کاربر دیگر',
  TASK_DELEGATE: 'تفویض کار به نقش پایین‌دست',
  TASK_VIEW_ORG_SCOPE: 'مشاهده کارها در محدوده سازمانی',
  EXECUTION_PAPER_DETAIL_VIEW: 'مشاهده فقط جزئیات برگ اجرا',
  EXECUTION_PAPER_TRACE_VIEW: 'مشاهده فقط سوابق برگ اجرا',
  REPORT_VIEW: 'مشاهده گزارش‌ها',
  AUDIT_VIEW: 'مشاهده سوابق و ممیزی'
};

function permissionLabel(code) {
  return permissionLabels[code] || code;
}

export default function AdminPermissionsPage() {
  const [permissions, setPermissions] = useState([]);
  const [groupPermissions, setGroupPermissions] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setMessage('');
    try {
      const [permissionResult, groupPermissionResult] = await Promise.all([
        fetchPermissions(),
        fetchGroupPermissions()
      ]);
      setPermissions(permissionResult);
      setGroupPermissions(groupPermissionResult);
      setForm((current) => ({ ...current, permissionCode: current.permissionCode || permissionResult[0]?.code || '' }));
    } catch (error) {
      setMessage(error.message || 'دریافت مجوزها ناموفق بود.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const filteredMappings = useMemo(() => {
    if (!form.groupName) {
      return groupPermissions;
    }
    return groupPermissions.filter((item) => item.groupName === form.groupName);
  }, [form.groupName, groupPermissions]);

  const updateForm = (event) => {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  };

  const grant = async (event) => {
    event.preventDefault();
    setMessage('');
    try {
      await assignGroupPermission(form.groupName, form.permissionCode);
      await load();
    } catch (error) {
      setMessage(error.message || 'اعطای مجوز ناموفق بود.');
    }
  };

  const revoke = async (item) => {
    try {
      await revokeGroupPermission(item.id);
      await load();
    } catch (error) {
      setMessage(error.message || 'لغو مجوز ناموفق بود.');
    }
  };

  return (
    <AppShell
      active="permissions"
      title="مدیریت مجوزها"
      eyebrow="کنترل دسترسی"
      subtitle="مجوزهای کسب‌وکاری را به نام نقش یا گروه Keycloak اعطا یا از آن لغو کنید."
      actions={<button className="ghost-action" onClick={load} disabled={loading}>بروزرسانی</button>}
    >
      {message && <div className="notice error">{message}</div>}

      <section className="admin-two-column">
        <form className="admin-form" onSubmit={grant}>
          <div className="section-heading compact-heading">
            <div>
              <p className="eyebrow">اعطای مجوز</p>
              <h2>نقش/گروه Keycloak</h2>
            </div>
          </div>
          <label>
            <span>نام نقش یا گروه</span>
            <input name="groupName" value={form.groupName} onChange={updateForm} required />
          </label>
          <label>
            <span>مجوز</span>
            <select name="permissionCode" value={form.permissionCode} onChange={updateForm} required>
              {permissions.map((permission) => (
                <option value={permission.code} key={permission.id}>
                  {permissionLabel(permission.code)} - {permission.code}
                </option>
              ))}
            </select>
          </label>
          <button className="primary-action">اعطای مجوز</button>
        </form>

        <section className="panel-section admin-list-panel">
          <div className="section-heading compact-heading">
            <div>
              <p className="eyebrow">مجوزهای فعلی</p>
              <h2>{form.groupName || 'همه گروه‌ها'}</h2>
            </div>
          </div>
          <div className="admin-table">
            {filteredMappings.map((item) => (
              <div className="admin-row admin-row--with-actions" key={item.id}>
                <div>
                  <strong>{permissionLabel(item.permission?.code)}</strong>
                  <span>{item.groupName} · {item.permission?.code}</span>
                </div>
                <button className="ghost-action danger-action" onClick={() => revoke(item)}>لغو</button>
              </div>
            ))}
            {!loading && filteredMappings.length === 0 && <div className="empty-state compact">مجوزی برای این نقش/گروه ثبت نشده است.</div>}
          </div>
        </section>
      </section>

      <section className="panel-section permission-reference">
        <div className="section-heading compact-heading">
          <div>
            <p className="eyebrow">فهرست مرجع</p>
            <h2>همه مجوزها</h2>
          </div>
        </div>
        <div className="permission-reference-grid">
          {permissions.map((permission) => (
            <div className="permission-reference-item" key={permission.id}>
              <strong>{permissionLabel(permission.code)}</strong>
              <span>{permission.code}</span>
            </div>
          ))}
        </div>
      </section>
    </AppShell>
  );
}
