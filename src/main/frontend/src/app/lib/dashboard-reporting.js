// Chart geometry uses relative weights; labels and details retain exact monetary strings.
export const FINANCIAL_LABELS = {
  liability: 'اصل مالیات و عوارض', penalties: 'جرایم', credits: 'اعتبارات',
  paid: 'پرداخت‌های ثبت‌شده', remissions: 'بخشودگی‌ها', remaining: 'مانده بدهی',
};

export const ENFORCEMENT_LABELS = {
  ISSUED: 'دستور بازداشت صادر شده',
  NOT_ISSUED: 'دستور بازداشت صادر نشده',
  SEIZURE: 'توقیف شده',
  AUCTION: 'فروش رفته',
};

export function chartWeights(values) {
  if (values.some(value => value == null || !/^\d+(\.\d+)?$/.test(String(value)))) return null;
  const parts = values.map(value => String(value).split('.'));
  const scale = Math.max(0, ...parts.map(part => (part[1] || '').length));
  const exact = parts.map(([integer, fraction = '']) => BigInt(integer + fraction.padEnd(scale, '0')));
  const total = exact.reduce((sum, value) => sum + value, 0n);
  return exact.map(value => total ? Number(value * 1000000n / total) / 10000 : 0);
}
