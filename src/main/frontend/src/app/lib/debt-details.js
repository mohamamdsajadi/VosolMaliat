import labels from './debt-field-labels.json';

// Labels are taken from docs/analysis/sanim-field-inventory.csv. Repeated tags
// retain their source order; aggregate/footer amounts always come from the DB.
export const DEBT_GROUPS = [
  { key: 'principalAmount', detail: 'taxLiabilityDetails', label: 'اصل مالیات و عوارض', heading: 'منبع', total: 'جمع اصل مالیات و عوارض' },
  { key: 'creditAmount', detail: 'taxCreditDetails', label: 'اعتبار مالیاتی', heading: 'شرح اعتبار', total: 'جمع اعتبار مالیاتی' },
  { key: 'penaltyAmount', detail: 'totalPenaltiesDetails', label: 'جرائم', heading: 'شرح جریمه', total: 'جمع جرائم' },
  { key: 'remissionAmount', detail: 'totalRemissionsDetails', label: 'بخشودگی', heading: 'شرح بخشودگی', total: 'جمع بخشودگی' },
  { key: 'paidAmount', detail: 'totalPaymentsDetails', label: 'پرداختی', heading: 'شرح پرداخت', total: 'جمع پرداختی' },
  { key: 'outstandingAmount', detail: 'outstandingDebtDetails', label: 'مانده بدهی', heading: 'شرح مانده بدهی', total: 'مانده بدهی ثبت‌شده' },
];

export function parseDebtDetails(value, paperType) {
  if (!value || value === '') return { rows: [] };
  try {
    if (Array.isArray(value) || String(value).trim().startsWith('[')) {
      const rows = Array.isArray(value) ? value : JSON.parse(value);
      return { rows: rows.map(row => ({ label: row.description || row.tag || 'شرح ثبت نشده', amount: row.amount,
        reported: row.reported !== false && row.reported !== 'false' })) };
    }
    // DOMParser parses data only; source markup is never inserted into HTML.
    const xml = new DOMParser().parseFromString(String(value), 'application/xml');
    if (xml.querySelector('parsererror') || xml.doctype) return { rows: [], error: true };
    const root = xml.documentElement;
    const dictionary = labels[root.getAttribute('paperType') || paperType] || {};
    const occurrences = {};
    const rows = [];
    const add = (tag, amount, description, reported = true) => {
      const index = occurrences[tag] || 0;
      occurrences[tag] = index + 1;
      const names = dictionary[tag] || [];
      rows.push({ label: description || names[Math.min(index, names.length - 1)] || tag,
        amount, reported, tag });
    };
    for (const node of root.children) {
      if (node.localName === 'item') {
        const fields = Object.fromEntries(Array.from(node.children).map(child => [child.getAttribute('name') || child.localName, child.textContent.trim()]));
        add(fields.tag || fields.code || 'شرح ثبت نشده', fields.amount, fields.description, fields.reported !== 'false');
      } else if (node.children.length) {
        // Some formats retain nested penalty rows with description and amount.
        const fields = Object.fromEntries(Array.from(node.children).map(child => [child.localName, child.textContent.trim()]));
        add(node.localName, fields.amount || fields.MB, fields.description || fields.KB || node.textContent.trim());
      } else add(node.localName, node.textContent.trim());
    }
    return { rows };
  } catch { return { rows: [], error: true }; }
}

export function isDebtAmount(value) { return value != null && /^[+-]?\d+(?:\.\d+)?$/.test(String(value).trim()); }
