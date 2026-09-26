'use client'

import { useCallback, useEffect, useState } from 'react';
import AppShell from '../../components/app-shell';
import RefreshButton from '../../components/refresh-button';
import {
  createUserProfile,
  deleteUserProfile,
  fetchGeneralDirectorates,
  fetchTaxAffairsOffices,
  fetchTaxUnits,
  fetchUsers,
  updateUserProfile,
  setUserLock,
  setUserStatus
} from '../../lib/security';

const initialForm = {
  keycloakUserId: '',
  username: '',
  email: '',
  firstName: '',
  lastName: '',
  phoneNumber: '',
  businessRole: 'TAX_EXPERT',
  generalDirectorateId: '',
  taxAffairsOfficeId: '',
  taxUnitId: ''
};

const businessRoles = [
  { value: 'GENERAL_OFFICE_HEAD', label: 'رئیس اداره کل' },
  { value: 'TAX_OFFICE_HEAD', label: 'رئیس اداره مالیات' },
  { value: 'TAX_UNIT_HEAD', label: 'رئیس واحد مالیاتی' },
  { value: 'TAX_EXPERT', label: 'کارشناس مالیاتی' },
  { value: 'ADMIN', label: 'مدیر سامانه' }
];

export default function AdminUsersPage() {
  const [users, setUsers] = useState([]);
  const [generals, setGenerals] = useState([]);
  const [offices, setOffices] = useState([]);
  const [taxUnits, setTaxUnits] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setMessage('');
    try {
      const [userResult, generalResult, officeResult, organizationResult] = await Promise.all([
        fetchUsers(),
        fetchGeneralDirectorates(),
        fetchTaxAffairsOffices(),
        fetchTaxUnits()
      ]);
      setUsers(userResult);
      setGenerals(generalResult);
      setOffices(officeResult);
      setTaxUnits(organizationResult);
    } catch (error) {
      setMessage(error.message || 'دریافت کاربران ناموفق بود.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const updateForm = (event) => {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  };

  const createProfile = async (event) => {
    event.preventDefault();
    setSaving(true);
    setMessage('');
    try {
      await createUserProfile({
        ...form,
        generalDirectorateId: form.generalDirectorateId ? Number(form.generalDirectorateId) : null,
        taxAffairsOfficeId: form.taxAffairsOfficeId ? Number(form.taxAffairsOfficeId) : null,
        taxUnitId: form.taxUnitId ? Number(form.taxUnitId) : null
      });
      setForm(initialForm);
      await loadUsers();
    } catch (error) {
      setMessage(error.message || 'ثبت پروفایل ناموفق بود.');
    } finally {
      setSaving(false);
    }
  };

  const startEdit = (user) => {
    setEditingId(user.id);
    setForm({
      keycloakUserId: user.keycloakUserId,
      username: user.username,
      email: user.email || '',
      firstName: user.firstName || '',
      lastName: user.lastName || '',
      phoneNumber: user.phoneNumber || '',
      businessRole: user.businessRole || 'TAX_EXPERT',
      generalDirectorateId: user.organization?.generalDirectorateId ? String(user.organization.generalDirectorateId) : '',
      taxAffairsOfficeId: user.organization?.taxAffairsOfficeId ? String(user.organization.taxAffairsOfficeId) : '',
      taxUnitId: user.organization?.taxUnitId ? String(user.organization.taxUnitId) : ''
    });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setForm(initialForm);
  };

  const saveEdit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setMessage('');
    try {
      await updateUserProfile(editingId, {
        email: form.email,
        firstName: form.firstName,
        lastName: form.lastName,
        phoneNumber: form.phoneNumber,
        businessRole: form.businessRole,
        generalDirectorateId: form.generalDirectorateId ? Number(form.generalDirectorateId) : null,
        taxAffairsOfficeId: form.taxAffairsOfficeId ? Number(form.taxAffairsOfficeId) : null,
        taxUnitId: form.taxUnitId ? Number(form.taxUnitId) : null
      });
      cancelEdit();
      await loadUsers();
    } catch (error) {
      setMessage(error.message || 'ویرایش پروفایل ناموفق بود.');
    } finally {
      setSaving(false);
    }
  };

  const removeUser = async (user) => {
    if (!window.confirm(`پروفایل ${user.username} حذف شود؟`)) {
      return;
    }
    try {
      await deleteUserProfile(user.id);
      await loadUsers();
    } catch (error) {
      setMessage(error.message || 'حذف پروفایل ناموفق بود.');
    }
  };

  const toggleStatus = async (user) => {
    await setUserStatus(user.id, !user.enabledInBusinessSystem);
    await loadUsers();
  };

  const toggleLock = async (user) => {
    await setUserLock(user.id, !user.lockedInBusinessSystem);
    await loadUsers();
  };

  return (
    <AppShell
      active="admin"
      title="مدیریت کاربران"
      eyebrow="پروفایل‌های محلی"
      actions={<RefreshButton onClick={loadUsers} loading={loading} />}
    >
      {message && <div className="notice error">{message}</div>}

      <section className="admin-two-column">
        <form className="admin-form" onSubmit={editingId ? saveEdit : createProfile}>
          <div className="section-heading compact-heading">
            <div>
              <p className="eyebrow">اتصال کاربر</p>
              <h2>{editingId ? 'ویرایش پروفایل محلی' : 'ایجاد پروفایل محلی'}</h2>
            </div>
          </div>
          <label>
            <span>Keycloak User ID</span>
            <input name="keycloakUserId" value={form.keycloakUserId} onChange={updateForm} required disabled={Boolean(editingId)} />
          </label>
          <label>
            <span>نام کاربری</span>
            <input name="username" value={form.username} onChange={updateForm} required disabled={Boolean(editingId)} />
          </label>
          <label>
            <span>ایمیل</span>
            <input name="email" type="email" value={form.email} onChange={updateForm} />
          </label>
          <div className="form-grid">
            <label>
              <span>نام</span>
              <input name="firstName" value={form.firstName} onChange={updateForm} />
            </label>
            <label>
              <span>نام خانوادگی</span>
              <input name="lastName" value={form.lastName} onChange={updateForm} />
            </label>
          </div>
          <div className="form-grid">
            <label>
              <span>تلفن</span>
              <input name="phoneNumber" value={form.phoneNumber} onChange={updateForm} />
            </label>
            <label>
              <span>نقش کسب‌وکاری</span>
              <select name="businessRole" value={form.businessRole} onChange={updateForm} required>
                {businessRoles.map((role) => <option value={role.value} key={role.value}>{role.label}</option>)}
              </select>
            </label>
          </div>
          <div className="form-grid">
            <label>
              <span>اداره کل</span>
              <select name="generalDirectorateId" value={form.generalDirectorateId} onChange={updateForm}>
                <option value="">بدون اداره کل</option>
                {generals.map((general) => <option value={general.id} key={general.id}>{general.name}</option>)}
              </select>
            </label>
            <label>
              <span>اداره مالیات</span>
              <select name="taxAffairsOfficeId" value={form.taxAffairsOfficeId} onChange={updateForm}>
                <option value="">بدون اداره مالیات</option>
                {offices.map((office) => <option value={office.id} key={office.id}>{office.officeName}</option>)}
              </select>
            </label>
          </div>
          <div className="form-grid">
            <label>
              <span>واحد مالیاتی</span>
              <select name="taxUnitId" value={form.taxUnitId} onChange={updateForm}>
                <option value="">بدون واحد</option>
                {taxUnits.map((unit) => (
                  <option value={unit.id} key={unit.id}>{unit.taxUnitName} - {unit.taxAffairsOfficeName || 'اداره نامشخص'}</option>
                ))}
              </select>
            </label>
          </div>
          <div className="row-actions">
            <button className="primary-action" disabled={saving}>{saving ? 'در حال ثبت' : editingId ? 'ذخیره تغییرات' : 'ثبت پروفایل'}</button>
            {editingId && <button className="ghost-action" type="button" onClick={cancelEdit}>انصراف</button>}
          </div>
        </form>

        <section className="panel-section admin-list-panel">
          <div className="section-heading compact-heading">
            <div>
              <p className="eyebrow">فهرست کاربران</p>
              <h2>وضعیت دسترسی داخلی</h2>
            </div>
          </div>

          <div className="admin-table">
            {users.map((user) => (
              <div className="admin-row admin-row--with-actions" key={user.id}>
                <div>
                  <strong>{user.fullName || user.username}</strong>
                  <span>{roleLabel(user.businessRole)} · {user.username} · {user.keycloakUserId}</span>
                </div>
                <div className="row-actions">
                  <button className="ghost-action" onClick={() => toggleStatus(user)}>
                    {user.enabledInBusinessSystem ? 'غیرفعال' : 'فعال'}
                  </button>
                  <button className="ghost-action" onClick={() => toggleLock(user)}>
                    {user.lockedInBusinessSystem ? 'بازکردن قفل' : 'قفل'}
                  </button>
                  <button className="ghost-action" onClick={() => startEdit(user)}>ویرایش</button>
                  <button className="ghost-action danger-action" onClick={() => removeUser(user)}>حذف</button>
                </div>
              </div>
            ))}
            {!loading && users.length === 0 && <div className="empty-state compact">کاربری برای نمایش وجود ندارد.</div>}
          </div>
        </section>
      </section>
    </AppShell>
  );
}

function roleLabel(value) {
  return businessRoles.find((role) => role.value === value)?.label || value || 'بدون نقش';
}
