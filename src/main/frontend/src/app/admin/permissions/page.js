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
                <option value={permission.code} key={permission.id}>{permission.code}</option>
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
                  <strong>{item.permission?.code}</strong>
                  <span>{item.groupName} · {item.permission?.description || 'بدون توضیح'}</span>
                </div>
                <button className="ghost-action danger-action" onClick={() => revoke(item)}>لغو</button>
              </div>
            ))}
            {!loading && filteredMappings.length === 0 && <div className="empty-state compact">مجوزی برای این نقش/گروه ثبت نشده است.</div>}
          </div>
        </section>
      </section>
    </AppShell>
  );
}
