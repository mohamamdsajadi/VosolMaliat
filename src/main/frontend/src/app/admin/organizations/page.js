'use client'

import { useCallback, useEffect, useState } from 'react';
import AppShell from '../../components/app-shell';
import RefreshButton from '../../components/refresh-button';
import {
  createGeneralDirectorate,
  createTaxAffairsOffice,
  createTaxUnit,
  deleteGeneralDirectorate,
  deleteTaxAffairsOffice,
  deleteTaxUnit,
  fetchGeneralDirectorates,
  fetchTaxAffairsOffices,
  fetchTaxUnits,
  updateGeneralDirectorate,
  updateTaxAffairsOffice,
  updateTaxUnit
} from '../../lib/security';

const initialGeneral = { name: '', postalCode: '', address: '', phone: '' };
const initialOffice = { generalDirectorateOfTaxAffairsId: '', officeName: '', postalCode: '', address: '', phone: '' };
const initialUnit = { taxAffairsOfficeId: '', taxUnitName: '', postalCode: '', address: '', phone: '' };

export default function AdminOrganizationsPage() {
  const [generals, setGenerals] = useState([]);
  const [offices, setOffices] = useState([]);
  const [units, setUnits] = useState([]);
  const [generalForm, setGeneralForm] = useState(initialGeneral);
  const [officeForm, setOfficeForm] = useState(initialOffice);
  const [unitForm, setUnitForm] = useState(initialUnit);
  const [editing, setEditing] = useState({ type: null, id: null });
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setMessage('');
    try {
      const [generalResult, officeResult, unitResult] = await Promise.all([
        fetchGeneralDirectorates(),
        fetchTaxAffairsOffices(),
        fetchTaxUnits()
      ]);
      setGenerals(generalResult);
      setOffices(officeResult);
      setUnits(unitResult);
    } catch (error) {
      setMessage(error.message || 'دریافت اطلاعات سازمانی ناموفق بود.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const saveGeneral = async (event) => {
    event.preventDefault();
    const payload = {
      name: generalForm.name,
      postalCode: generalForm.postalCode,
      address: generalForm.address,
      phone: generalForm.phone
    };
    try {
      if (editing.type === 'general') {
        await updateGeneralDirectorate(editing.id, payload);
      } else {
        await createGeneralDirectorate(payload);
      }
      resetForms();
      await load();
    } catch (error) {
      setMessage(error.message || 'ذخیره اداره کل ناموفق بود.');
    }
  };

  const saveOffice = async (event) => {
    event.preventDefault();
    const payload = {
      generalDirectorateOfTaxAffairsId: Number(officeForm.generalDirectorateOfTaxAffairsId),
      officeName: officeForm.officeName,
      postalCode: officeForm.postalCode,
      address: officeForm.address,
      phone: officeForm.phone
    };
    try {
      if (editing.type === 'office') {
        await updateTaxAffairsOffice(editing.id, payload);
      } else {
        await createTaxAffairsOffice(payload);
      }
      resetForms();
      await load();
    } catch (error) {
      setMessage(error.message || 'ذخیره اداره مالیات ناموفق بود.');
    }
  };

  const saveUnit = async (event) => {
    event.preventDefault();
    const payload = {
      taxAffairsOfficeId: Number(unitForm.taxAffairsOfficeId),
      taxUnitName: unitForm.taxUnitName,
      postalCode: unitForm.postalCode,
      address: unitForm.address,
      phone: unitForm.phone
    };
    try {
      if (editing.type === 'unit') {
        await updateTaxUnit(editing.id, payload);
      } else {
        await createTaxUnit(payload);
      }
      resetForms();
      await load();
    } catch (error) {
      setMessage(error.message || 'ذخیره واحد مالیاتی ناموفق بود.');
    }
  };

  const resetForms = () => {
    setEditing({ type: null, id: null });
    setGeneralForm(initialGeneral);
    setOfficeForm(initialOffice);
    setUnitForm(initialUnit);
  };

  const remove = async (type, item) => {
    if (!window.confirm('این مورد حذف شود؟')) {
      return;
    }
    try {
      if (type === 'general') {
        await deleteGeneralDirectorate(item.id);
      } else if (type === 'office') {
        await deleteTaxAffairsOffice(item.id);
      } else {
        await deleteTaxUnit(item.id);
      }
      await load();
    } catch (error) {
      setMessage(error.message || 'حذف ناموفق بود.');
    }
  };

  return (
    <AppShell
      active="organizations"
      title="مدیریت ساختار مالیاتی"
      eyebrow="Masterdata"
      actions={<RefreshButton onClick={load} loading={loading} />}
      contextItems={[
        `اداره کل: ${generals.length.toLocaleString('fa-IR')}`,
        `اداره مالیات: ${offices.length.toLocaleString('fa-IR')}`,
        `واحد مالیاتی: ${units.length.toLocaleString('fa-IR')}`
      ]}
    >
      {message && <div className="notice error">{message}</div>}

      <section className="masterdata-grid">
        <MasterdataPanel
          title="اداره کل امور مالیاتی"
          eyebrow="GeneralDirectorateOfTaxAffairs"
          onSubmit={saveGeneral}
          form={(
            <>
              <label>
                <span>نام اداره کل</span>
                <input value={generalForm.name} onChange={(event) => setGeneralForm((current) => ({ ...current, name: event.target.value }))} required />
              </label>
              <label>
                <span>کد پستی</span>
                <input value={generalForm.postalCode} onChange={(event) => setGeneralForm((current) => ({ ...current, postalCode: event.target.value }))} />
              </label>
              <label>
                <span>نشانی</span>
                <input value={generalForm.address} onChange={(event) => setGeneralForm((current) => ({ ...current, address: event.target.value }))} />
              </label>
              <label>
                <span>تلفن</span>
                <input value={generalForm.phone} onChange={(event) => setGeneralForm((current) => ({ ...current, phone: event.target.value }))} />
              </label>
            </>
          )}
          editing={editing.type === 'general'}
          onCancel={resetForms}
        >
          {generals.map((item) => (
            <MasterdataRow
              key={item.id}
              title={item.name}
              subtitle={contactSubtitle(item, `شناسه: ${item.id}`)}
              onEdit={() => {
                setEditing({ type: 'general', id: item.id });
                setGeneralForm({
                  name: item.name,
                  postalCode: item.postalCode || '',
                  address: item.address || '',
                  phone: item.phone || ''
                });
              }}
              onDelete={() => remove('general', item)}
            />
          ))}
        </MasterdataPanel>

        <MasterdataPanel
          title="اداره مالیات"
          eyebrow="TaxAffairsOffice"
          onSubmit={saveOffice}
          form={(
            <>
              <label>
                <span>اداره کل</span>
                <select value={officeForm.generalDirectorateOfTaxAffairsId} onChange={(event) => setOfficeForm((current) => ({ ...current, generalDirectorateOfTaxAffairsId: event.target.value }))} required>
                  <option value="">انتخاب کنید</option>
                  {generals.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
                </select>
              </label>
              <label>
                <span>نام اداره مالیات</span>
                <input value={officeForm.officeName} onChange={(event) => setOfficeForm((current) => ({ ...current, officeName: event.target.value }))} required />
              </label>
              <label>
                <span>کد پستی</span>
                <input value={officeForm.postalCode} onChange={(event) => setOfficeForm((current) => ({ ...current, postalCode: event.target.value }))} />
              </label>
              <label>
                <span>نشانی</span>
                <input value={officeForm.address} onChange={(event) => setOfficeForm((current) => ({ ...current, address: event.target.value }))} />
              </label>
              <label>
                <span>تلفن</span>
                <input value={officeForm.phone} onChange={(event) => setOfficeForm((current) => ({ ...current, phone: event.target.value }))} />
              </label>
            </>
          )}
          editing={editing.type === 'office'}
          onCancel={resetForms}
        >
          {offices.map((item) => (
            <MasterdataRow
              key={item.id}
              title={item.officeName}
              subtitle={contactSubtitle(item, item.generalDirectorateName || 'بدون اداره کل')}
              onEdit={() => {
                setEditing({ type: 'office', id: item.id });
                setOfficeForm({
                  generalDirectorateOfTaxAffairsId: String(item.generalDirectorateOfTaxAffairsId),
                  officeName: item.officeName,
                  postalCode: item.postalCode || '',
                  address: item.address || '',
                  phone: item.phone || ''
                });
              }}
              onDelete={() => remove('office', item)}
            />
          ))}
        </MasterdataPanel>

        <MasterdataPanel
          title="واحد مالیاتی"
          eyebrow="TaxUnit"
          onSubmit={saveUnit}
          form={(
            <>
              <label>
                <span>اداره مالیات</span>
                <select value={unitForm.taxAffairsOfficeId} onChange={(event) => setUnitForm((current) => ({ ...current, taxAffairsOfficeId: event.target.value }))} required>
                  <option value="">انتخاب کنید</option>
                  {offices.map((item) => <option key={item.id} value={item.id}>{item.officeName}</option>)}
                </select>
              </label>
              <label>
                <span>نام واحد مالیاتی</span>
                <input value={unitForm.taxUnitName} onChange={(event) => setUnitForm((current) => ({ ...current, taxUnitName: event.target.value }))} required />
              </label>
              <label>
                <span>کد پستی</span>
                <input value={unitForm.postalCode} onChange={(event) => setUnitForm((current) => ({ ...current, postalCode: event.target.value }))} />
              </label>
              <label>
                <span>نشانی</span>
                <input value={unitForm.address} onChange={(event) => setUnitForm((current) => ({ ...current, address: event.target.value }))} />
              </label>
              <label>
                <span>تلفن</span>
                <input value={unitForm.phone} onChange={(event) => setUnitForm((current) => ({ ...current, phone: event.target.value }))} />
              </label>
            </>
          )}
          editing={editing.type === 'unit'}
          onCancel={resetForms}
        >
          {units.map((item) => (
            <MasterdataRow
              key={item.id}
              title={item.taxUnitName}
              subtitle={contactSubtitle(item, `${item.taxAffairsOfficeName || 'بدون اداره مالیات'} · ${item.generalDirectorateName || 'بدون اداره کل'}`)}
              onEdit={() => {
                setEditing({ type: 'unit', id: item.id });
                setUnitForm({
                  taxAffairsOfficeId: String(item.taxAffairsOfficeId),
                  taxUnitName: item.taxUnitName,
                  postalCode: item.postalCode || '',
                  address: item.address || '',
                  phone: item.phone || ''
                });
              }}
              onDelete={() => remove('unit', item)}
            />
          ))}
        </MasterdataPanel>
      </section>
    </AppShell>
  );
}

function contactSubtitle(item, fallback) {
  return [
    fallback,
    item.postalCode ? `کد پستی: ${item.postalCode}` : null,
    item.phone ? `تلفن: ${item.phone}` : null,
    item.address ? `نشانی: ${item.address}` : null
  ].filter(Boolean).join(' · ');
}

function MasterdataPanel({ eyebrow, title, form, onSubmit, editing, onCancel, children }) {
  return (
    <section className="panel-section masterdata-panel">
      <form className="admin-form masterdata-form" onSubmit={onSubmit}>
        <div className="section-heading compact-heading">
          <div>
            <p className="eyebrow">{eyebrow}</p>
            <h2>{title}</h2>
          </div>
        </div>
        {form}
        <div className="row-actions">
          <button className="primary-action">{editing ? 'ذخیره تغییرات' : 'ثبت'}</button>
          {editing && <button type="button" className="ghost-action" onClick={onCancel}>انصراف</button>}
        </div>
      </form>
      <div className="admin-table masterdata-list">{children}</div>
    </section>
  );
}

function MasterdataRow({ title, subtitle, onEdit, onDelete }) {
  return (
    <div className="admin-row admin-row--with-actions">
      <div>
        <strong>{title}</strong>
        <span>{subtitle}</span>
      </div>
      <div className="row-actions">
        <button className="ghost-action" onClick={onEdit}>ویرایش</button>
        <button className="ghost-action danger-action" onClick={onDelete}>حذف</button>
      </div>
    </div>
  );
}
