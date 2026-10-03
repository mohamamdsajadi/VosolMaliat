'use client'

export const aggregateLabels = {
  outstandingDebtAmount: 'مانده بدهی',
  totalTaxAmount: 'مالیات',
  totalDutyAmount: 'عوارض',
  totalPenaltyAmount: 'جرایم',
  totalPaidAmount: 'پرداختی',
  totalCreditAmount: 'اعتبار',
  totalRemissionAmount: 'بخشودگی',
}
const emptyAmounts = Object.fromEntries(Object.keys(aggregateLabels).map(key => [key, '']))
export const initialDebtPaper = {
  taxpayerIdentifier: '', registrationTraceCode: '', basisDocumentDateRaw: '', taxPeriodStartRaw: '', taxPeriodEndRaw: '',
  debtPaperUniqueCode: '', documentNumber: '', issueDateRaw: '', issuanceReason: '', taxSource: '',
  taxYear: '', taxPeriod: '', activityTitle: '', activityTypeId: '', phone: '', mobile: '', tradeName: '', paperType: 'EF03', sourceTaxOfficeCode: '',
  mainTaxBureauId: '', taxBureauId: '',
  taxpayerType: 'NATURAL', nationalCode: '', firstName: '', lastName: '', fatherName: '', birthDate: '',
  postalCode: '', address: '', nationalId: '', registrationNumber: '', registrationDate: '', companyName: '',
  ...emptyAmounts
}
export const initialExecutionPaper = {
  executionPaperUniqueCode: '', finalizedDebtPaperId: '', taxpayerId: '', mainTaxBureauId: '',
  taxBureauId: '', taxSource: '', currentState: 'UNASSIGNED', ...emptyAmounts
}
export function randomDebtPaperSample(masterdata = {}, forcedTaxpayerType = null) {
  const natural = forcedTaxpayerType !== 'LEGAL'
  const selected = sampleHierarchicalMasterdata(masterdata)
  return {
    ...initialDebtPaper, taxpayerIdentifier: String(Date.now()), registrationTraceCode: `REG-${Date.now()}`, basisDocumentDateRaw: randomJalaliDate(), debtPaperUniqueCode: `DOC-${Date.now()}`, documentNumber: `LETTER-${Date.now()}`,
    issueDateRaw: randomJalaliDate(), taxYear: '1403', taxPeriod: '1', taxSource: 'VAT', issuanceReason: 'SELF_DECLARATION', activityTypeId: String(masterdata.activities?.[0]?.id || ''), activityTitle: masterdata.activities?.[0]?.title || '', phone: '02112345678', mobile: '09121234567', tradeName: 'نمونه', fatherName: 'حسن', birthDate: '1360/01/01', registrationNumber: '12345', registrationDate: '1390/01/01',
    mainTaxBureauId: selected.generalId, taxBureauId: selected.officeId,
    taxpayerType: natural ? 'NATURAL' : 'LEGAL', nationalCode: natural ? randomNationalCode() : '',
    firstName: natural ? 'علی' : '', lastName: natural ? 'نمونه' : '',
    nationalId: natural ? '' : randomNationalId(), companyName: natural ? '' : 'شرکت نمونه',
    address: 'تهران، نشانی نمونه', postalCode: randomPostalCode(),
    outstandingDebtAmount: '105000000.25', totalTaxAmount: '100000000.25', totalDutyAmount: '5000000',
    totalPenaltyAmount: '10000000', totalPaidAmount: '10000000', totalCreditAmount: '0', totalRemissionAmount: '0'
  }
}
export function randomLegalPersonDebtPaperSample(masterdata = {}) { return randomDebtPaperSample(masterdata, 'LEGAL') }
export function randomExecutionPaperSample(summary = {}, masterdata = {}) {
  return { ...initialExecutionPaper, ...randomDebtPaperSample(masterdata), executionPaperUniqueCode: '',
    finalizedDebtPaperId: String(pick(summary.finalizedDebtPapers || [])?.id || ''),
    taxpayerId: String(pick(summary.taxpayers || [])?.id || '') }
}
export function aggregatePayload(form) {
  // Exact decimal strings: no Number coercion, no default-zero for unspecified amounts.
  return Object.fromEntries(Object.keys(aggregateLabels).map(key => [key, form[key]?.trim() || null]))
}
export function toDebtPaperPayload(form) {
  const natural = form.taxpayerType === 'NATURAL'
  return {
    debtPaperUniqueCode: form.debtPaperUniqueCode, taxSource: form.taxSource,
    mainTaxBureauId: numberOrNull(form.mainTaxBureauId), taxBureauId: numberOrNull(form.taxBureauId),
    taxpayer: { type: form.taxpayerType, nationalCode: form.nationalCode || null,
      firstName: form.firstName, lastName: form.lastName, fatherName: form.fatherName, birthDate: form.birthDate,
      postalCode: form.postalCode, address: form.address, nationalId: form.nationalId || null,
      registrationNumber: form.registrationNumber, registrationDate: form.registrationDate, companyName: form.companyName },
    relatedPersons: [],
    sourceDocument: {
      documentNumber: form.documentNumber, issueDateRaw: form.issueDateRaw,
      taxpayerIdentifier: form.taxpayerIdentifier, registrationTraceCode: form.registrationTraceCode,
      basisDocumentNumber: form.debtPaperUniqueCode, basisDocumentDateRaw: form.basisDocumentDateRaw,
      taxPeriodStartRaw: form.taxPeriodStartRaw, taxPeriodEndRaw: form.taxPeriodEndRaw,
      issuanceReason: form.issuanceReason, taxYear: form.taxYear, taxPeriod: form.taxPeriod,
      paperType: form.paperType, activityTypeId: numberOrNull(form.activityTypeId), phone: form.phone, mobile: form.mobile, tradeName: form.tradeName,
      activityTitle: form.activityTitle, sourceTaxOfficeCode: form.sourceTaxOfficeCode || null,
      personType: form.taxpayerType, identityNumber: natural ? form.nationalCode : form.nationalId,
      taxpayerName: natural ? [form.firstName, form.lastName].filter(Boolean).join(' ') : form.companyName,
      address: form.address, postalCode: form.postalCode, currencyCode: 'IRR',
      validationStatus: 'MANUAL_ENTRY', amounts: aggregatePayload(form)
    }
  }
}
export function toExecutionPayload(form) {
  return { executionPaperUniqueCode: form.executionPaperUniqueCode || null,
    finalizedDebtPaperId: numberOrNull(form.finalizedDebtPaperId), taxpayerId: numberOrNull(form.taxpayerId),
    taxSource: form.taxSource, currentState: form.currentState,
    currentAmounts: aggregatePayload(form) }
}
function numberOrNull(value) { return value === '' || value == null ? null : Number(value) }

function sampleHierarchicalMasterdata(masterdata) {
  const office = pick(masterdata.offices || []);
  const general = office
    ? (masterdata.generals || []).find((item) => String(item.id) === String(office.generalDirectorateOfTaxAffairsId))
    : pick(masterdata.generals || []);
  return {
    generalId: general?.id ? String(general.id) : '',
    officeId: office?.id ? String(office.id) : ''
  };
}

function pick(items) {
  if (!items || items.length === 0) {
    return undefined;
  }
  return items[randomInt(0, items.length - 1)];
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomAmount(min, max) {
  return Math.round(randomInt(min, max) / 10000) * 10000;
}

function randomJalaliDate(minYear = 1399, maxYear = 1403) {
  return `${randomInt(minYear, maxYear)}/${String(randomInt(1, 12)).padStart(2, '0')}/${String(randomInt(1, 28)).padStart(2, '0')}`;
}

function randomNationalCode() {
  return String(randomInt(1000000000, 9999999999));
}

function randomNationalId() {
  return String(randomInt(10000000000, 99999999999));
}

function randomPostalCode() {
  return String(randomInt(1000000000, 9999999999));
}

export function FieldGrid({ children }) {
  return <div className="form-grid wide-grid">{children}</div>;
}

export function Input({ label, ...props }) {
  return (
    <label>
      <span>{label}</span>
      <input {...props} />
    </label>
  );
}

export function Select({ label, options = [], ...props }) {
  return (
    <label>
      <span>{label}</span>
      <select {...props}>
        <option value="">انتخاب کنید</option>
        {options.map((item) => <option value={item.value} key={item.value}>{item.label}</option>)}
      </select>
    </label>
  );
}

export function MoneyFields({ form, onChange }) {
  return <FieldGrid>{Object.entries(aggregateLabels).map(([key, label]) =>
    <Input key={key} label={label + ' (ریال)'} name={key} type="text" inputMode="decimal"
      value={form[key]} onChange={onChange} required />
  )}</FieldGrid>
}

export function DataList({ title, items, render }) {
  return (
    <section className="panel-section data-list-panel">
      <div className="section-heading compact-heading">
        <div>
          <p className="eyebrow">لیست</p>
          <h2>{title}</h2>
        </div>
      </div>
      <div className="admin-table">
        {items.slice(0, 12).map((item) => (
          <div className="admin-row" key={item.id}>
            <div>
              <strong>{render(item)}</strong>
              <span>شناسه داخلی: {item.id}</span>
            </div>
          </div>
        ))}
        {items.length === 0 && <div className="empty-state compact">داده‌ای ثبت نشده است.</div>}
      </div>
    </section>
  );
}
