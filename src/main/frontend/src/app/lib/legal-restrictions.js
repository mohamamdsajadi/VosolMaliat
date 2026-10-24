export const LEGAL_RESTRICTION_GROUPS = [
  { key: 'article186', label: 'تبصره ۳ ماده ۱۸۶', subtitle: 'محدودیت اشخاص مرتبط با مؤدی حقوقی', icon: 'company', href: '/legal-restrictions' },
  { key: 'debtors', label: 'سرویس بدهکاران', subtitle: 'تبصره ۲ ماده ۱۶۹ مکرر', icon: 'debt', href: null },
  { key: 'travelBan', label: 'ممنوع‌الخروجی', subtitle: 'محدودیت خروج از کشور', icon: 'plane', href: null },
];
export const RESTRICTION_STATUSES = {
  SENT: { label: 'در انتظار پاسخ', tone: 'pending', icon: 'clock' },
  CONFIRMED: { label: 'تأیید شده', tone: 'confirmed', icon: 'check' },
  FAILED: { label: 'ناموفق', tone: 'failed', icon: 'alert' },
};
export const canViewRestrictions = user => user?.businessRole === 'ADMIN' ||
  [...(user?.allowedActions || []), ...(user?.localBusinessPermissions || [])].includes('LEGAL_RESTRICTION_VIEW');
