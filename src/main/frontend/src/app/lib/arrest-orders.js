import { apiJson } from './api';

export const STAGES = {
  SPECIAL_DEPARTMENT_REVIEW: 'بررسی اداره وصول و اجرای ویژه',
  AWAITING_APPROVAL: 'در انتظار تأیید صدور',
  AWAITING_OFFICER_ASSIGNMENT: 'در انتظار ارجاع به مأمور اجرا',
  ASSIGNED_TO_OFFICER: 'ارجاع‌شده به مأمور اجرا',
};
export const EVENT_LABELS = {
  OFFICER_SELECTED: 'انتخاب مأمور اجرا برای ارجاع مستقیم',
  STARTED: 'آغاز فرایند و ثبت ارجاع', SUPERVISOR_SELECTED: 'انتخاب سرپرست هیئت اجرا',
  DRAFT_REFRESHED: 'بروزرسانی پیش‌نویس و مبلغ بدهی',
  DEBT_INQUIRY_MOCKED: 'استعلام بدهی', DRAFT_PREPARED: 'تهیه پیش‌نویس دستور بازداشت اموال',
  APPROVED_AND_ISSUED: 'تأیید و صدور دستور بازداشت اموال', LETTER_ATTACHED: 'پیوست دستور به برگ اجرا',
  REFERRED_TO_SUPERVISOR: 'ارجاع به سرپرست هیئت اجرا', ASSIGNED_TO_OFFICER: 'ارجاع به مأمور اجرا',
};
export const hasArrestPermission = (user, code) => user?.businessRole === 'ADMIN' ||
  [...(user?.allowedActions || []), ...(user?.localBusinessPermissions || [])].includes(code);
export const canStartArrest = user => hasArrestPermission(user, 'ARREST_ORDER_START') &&
  ['ADMIN', 'DIRECTORATE_EXECUTION_UNIT_HEAD'].includes(user?.businessRole);
export const arrestTargets = params => apiJson(`/api/v1/arrest-orders/targets?${new URLSearchParams(params)}`);
export const arrestDetail = id => apiJson(`/api/v1/arrest-orders/${encodeURIComponent(id)}`);
export const startArrestOrders = payload => apiJson('/api/v1/arrest-orders', { method: 'POST', body: JSON.stringify(payload) });
export const actOnArrestOrder = (id, action, payload) => apiJson(`/api/v1/arrest-orders/${id}/${action}`, { method: 'POST', body: JSON.stringify(payload) });
export function arrestDate(value, withTime = true) {
  if (!value) return '—';
  const normalized = /^\d{4}-\d\d-\d\dT/.test(value) && !/(Z|[+-]\d\d:\d\d)$/.test(value) ? `${value}+03:30` : value;
  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('fa-IR-u-ca-persian', { timeZone: 'Asia/Tehran', year: 'numeric', month: '2-digit', day: '2-digit',
    ...(withTime ? { hour: '2-digit', minute: '2-digit', hour12: false } : {}) }).format(date);
}
export const digits = value => value == null || value === '' ? '—' : String(value).replace(/\d/g, n => '۰۱۲۳۴۵۶۷۸۹'[n]);
