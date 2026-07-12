'use client'

import { useCallback, useEffect, useState } from 'react';
import AppShell from '../../components/app-shell';
import {
  createUserProfile,
  deleteUserProfile,
  fetchTaxUnits,
  fetchUsers,
  updateUserProfile,
  assignUserTaxUnit,
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
  taxUnitId: ''
};

export default function AdminUsersPage() {
  const [users, setUsers] = useState([]);
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
      const [userResult, organizationResult] = await Promise.all([
        fetchUsers(),
        fetchTaxUnits()
      ]);
      setUsers(userResult);
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
        phoneNumber: form.phoneNumber
      });
      if (form.taxUnitId) {
        await assignUserTaxUnit(editingId, Number(form.taxUnitId));
      }
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
      active="users"
      title="مدیریت کاربران"
      eyebrow="پروفایل‌های محلی"
      subtitle="کاربر ابتدا در Keycloak ساخته می‌شود، سپس UUID او در این صفحه به پروفایل کسب‌وکاری متصل می‌شود."
      actions={<button className="ghost-action" onClick={loadUsers} disabled={loading}>بروزرسانی</button>}
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
                  <span>{user.username} · {user.keycloakUserId}</span>
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
