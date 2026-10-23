import { apiJson } from './api';
import { formatJalaliDate, toPersianDigits } from './display';
import { formatDashboardNumber } from './manager-dashboard';

export const PAPER_STATES = {
  UNASSIGNED: 'تخصیص نیافته', ASSIGNED: 'تخصیص یافته', INFORM_SENT: 'ارسال برای ابلاغ',
  INFORMED: 'ابلاغ شده', NOTIFIED: 'ابلاغ شده', INFORM_REJECTED: 'رد ابلاغ', NOTIFICATION_FAILED: 'عدم ابلاغ',
  AWAITING_ARREST_ORDER: 'در انتظار دستور بازداشت', ARREST_ORDER_ISSUED: 'دستور بازداشت',
  ARREST_ORDER_NOT_ISSUED: 'دستور بازداشت صادر نشده', COMPLETED: 'تکمیل شده', CANCELLED: 'لغو شده',
};
export const PAPER_FILTER_DEFAULTS = {
  search: '', taxpayerType: '', currentState: '', issuedFrom: '', issuedTo: '',
  uniqueCode: '', taxpayerName: '', taxpayerNationalId: '', notificationStatus: '', minDebt: '', maxDebt: '',
};

export function normalizeDigits(value) {
  return String(value).replace(/[۰-۹٠-٩]/g, digit => String('۰۱۲۳۴۵۶۷۸۹'.indexOf(digit) >= 0
    ? '۰۱۲۳۴۵۶۷۸۹'.indexOf(digit) : '٠١٢٣٤٥٦٧٨٩'.indexOf(digit)));
}

export function paperQuery({ page = 0, size = 50, scope = '', sortBy = 'id', sortDirection = 'desc', ...filters }) {
  const params = new URLSearchParams({ page: String(page), size: String(size), sortBy, sortDirection });
  Object.entries(filters).forEach(([key, value]) => {
    if (Object.hasOwn(PAPER_FILTER_DEFAULTS, key) && value !== '') params.set(key, normalizeDigits(value));
  });
  if (scope) params.set('scope', scope);
  return params.toString();
}

export function fetchPapers(query, options) {
  const params = new URLSearchParams(query);
  const scope = params.get('scope');
  params.delete('scope');
  return apiJson(`/api/v1/execution-papers${['assigned', 'unassigned'].includes(scope) ? `/${scope}` : ''}?${params}`, options);
}

export function fetchPaper(id, suffix = '', options) {
  return apiJson(`/api/v1/execution-papers/${encodeURIComponent(id)}${suffix}`, options);
}

export function stateLabel(value) { return PAPER_STATES[value] || value || '—'; }
export function identifier(value) { return value == null || value === '' ? '—' : toPersianDigits(value); }
export function money(value) { return formatDashboardNumber(value); }
export function paperDate(value) { return value ? formatJalaliDate(value) : '—'; }

// Financial source fields stay exact; missing values are never treated as zero.
export function sumExact(values) {
  if (values.some(value => value == null || value === '')) return null;
  const parts = values.map(value => String(value).match(/^(-?)(\d+)(?:\.(\d+))?$/));
  if (parts.some(value => !value)) return null;
  const scale = Math.max(...parts.map(value => (value[3] || '').length));
  const sum = parts.reduce((total, value) => total + BigInt(`${value[1]}${value[2]}${(value[3] || '').padEnd(scale, '0')}`), 0n);
  const digits = (sum < 0n ? -sum : sum).toString().padStart(scale + 1, '0');
  return `${sum < 0n ? '-' : ''}${scale ? `${digits.slice(0, -scale)}.${digits.slice(-scale)}` : digits}`;
}

export function compareExact(first, second) {
  const parts = [first, second].map(value => value == null ? null : String(value).match(/^(-?)(\d+)(?:\.(\d+))?$/));
  if (parts.some(value => !value)) return null;
  const scale = Math.max(...parts.map(value => (value[3] || '').length));
  const [a, b] = parts.map(value => BigInt(`${value[1]}${value[2]}${(value[3] || '').padEnd(scale, '0')}`));
  return a > b ? 1 : a < b ? -1 : 0;
}

export const WORKFLOW_STEPS = [
  { key: 'issue', label: 'صدور برگ اجرا', icon: 'document', actions: ['EXECUTION_PAPER_CREATED', 'EXECUTION_PAPER_ISSUED'] },
  { key: 'notify', label: 'ابلاغ برگ اجرا', icon: 'receipt', actions: ['ELECTRONIC_NOTIFICATION_CONFIRMED', 'EXECUTION_PAPER_INFORMED', 'INFORMED', 'NOTIFIED'], started: ['INFORM_SENT_TO_ELECTRONIC_NOTIFICATION', 'INFORM_SENT'] },
  { key: 'restriction', label: 'اعمال محدودیت قانونی', icon: 'shield', actions: ['RESTRICTION_REQUEST_CONFIRMED'], started: ['RESTRICTION_REQUEST_SENT'] },
  { key: 'arrest', label: 'صدور دستور بازداشت', icon: 'receipt', actions: ['ARREST_ORDER_ISSUED', 'ARREST_ORDER_APPROVED_AND_ISSUED'], started: ['ARREST_ORDER_STARTED', 'ARREST_ORDER_DRAFT_PREPARED'] },
  { key: 'execution', label: 'اقدامات اجرایی', icon: 'task', actions: [] },
  { key: 'auction', label: 'مزایده و فروش', icon: 'auction', actions: [] },
  { key: 'closed', label: 'پرداخت و مختومه', icon: 'closed', actions: ['EXECUTION_PAPER_COMPLETED', 'COMPLETED'] },
];

export function workflowSteps(data, trace) {
  const execution = data?.executionPaper || {};
  const state = execution.currentState || data?.summary?.currentState;
  const taxpayerType = data?.taxpayer?.type || data?.summary?.taxpayerType;
  return WORKFLOW_STEPS.filter(step => step.key !== 'restriction' || ['LEGAL', 'حقوقی'].includes(taxpayerType)).map(step => {
    const items = trace?.items || [];
    const doneIndex = items.findLastIndex(item => step.actions.includes(item.action));
    const startIndex = items.findLastIndex(item => step.started?.includes(item.action));
    const latest = items[Math.max(doneIndex, startIndex)];
    const issued = step.key === 'issue' && Boolean(execution.issuanceDate);
    const arrest = step.key === 'arrest' && state === 'ARREST_ORDER_ISSUED';
    const closed = step.key === 'closed' && state === 'COMPLETED';
    const notified = step.key === 'notify' && ['INFORMED', 'NOTIFIED'].includes(state);
    const active = startIndex > doneIndex || step.key === 'notify' && state === 'INFORM_SENT' || step.key === 'arrest' && state === 'AWAITING_ARREST_ORDER';
    const done = Boolean(doneIndex >= 0 || issued || arrest || closed || notified);
    // Do not infer that earlier stages completed from a later workflow state.
    return { ...step, status: active && !closed ? 'active' : done ? 'done' : 'unknown', event: latest,
      date: latest?.occurredAt || (issued ? execution.issuanceDate : null) };
  });
}
