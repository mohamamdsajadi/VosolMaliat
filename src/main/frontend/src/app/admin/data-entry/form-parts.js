'use client'

export const initialDebtPaper = {
  debtPaperUniqueCode: '',
  debtPaperIssueDate: '',
  notificationId: '',
  finalizationReason: '',
  taxSource: '',
  performanceYear: '',
  performancePeriod: '',
  activityTypeCode: '',
  activityTypeTitle: '',
  mainTaxBureauId: '',
  taxBureauId: '',
  taxUnitId: '',
  sendingUnitId: '',
  taxpayerType: 'NATURAL',
  nationalCode: '',
  firstName: '',
  lastName: '',
  fatherName: '',
  birthDate: '',
  postalCode: '',
  address: '',
  nationalId: '',
  registrationNumber: '',
  registrationDate: '',
  companyName: '',
  taxAmount: '',
  dutyAmount: '',
  penaltyArticle22: '',
  penaltyArticle23: '',
  deductiblePenalty: '',
  nonDeductiblePenalty: '',
  taxPaidAmount: '',
  penaltyPaidAmount: '',
  taxCredit: '',
  remainingTax: '',
  municipalityShare: '',
  guildShare: '',
  complianceBonus: ''
};

export const initialExecutionPaper = {
  executionPaperUniqueCode: '',
  finalizedDebtPaperId: '',
  taxpayerId: '',
  mainTaxBureauId: '',
  taxBureauId: '',
  taxUnitId: '',
  finalizationReason: '',
  taxSource: '',
  currentState: 'UNASSIGNED',
  taxAmount: '',
  dutyAmount: '',
  penaltyArticle22: '',
  penaltyArticle23: '',
  deductiblePenalty: '',
  nonDeductiblePenalty: '',
  taxPaidAmount: '',
  penaltyPaidAmount: '',
  taxCredit: '',
  remainingTax: ''
};

const firstNames = ['علی', 'مریم', 'رضا', 'زهرا', 'حسین', 'فاطمه'];
const lastNames = ['محمدی', 'کریمی', 'احمدی', 'رضایی', 'حسینی', 'جعفری'];
const companyNames = ['شرکت پارس تجارت آریا', 'توسعه مالیات مهر', 'بازرگانی سپهر شرق', 'صنایع داده پردازان'];
const taxSources = ['VAT', 'INCOME_TAX', 'CORPORATE_TAX', 'SALARY_TAX'];
const activityTitles = ['خدمات عمومی', 'خرده‌فروشی', 'تولید صنعتی', 'بازرگانی', 'پیمانکاری'];
const finalizationReasons = ['قطعیت پس از رسیدگی', 'عدم اعتراض در مهلت قانونی', 'رای قطعی هیات حل اختلاف', 'ابلاغ قطعی'];
const sampleAddresses = [
  'تهران، خیابان ولیعصر، بالاتر از میدان ونک، پلاک ۲۴',
  'اصفهان، خیابان چهارباغ بالا، کوچه بهار، پلاک ۱۲',
  'شیراز، بلوار زند، خیابان رودکی، پلاک ۸',
  'مشهد، احمدآباد، خیابان عارف، پلاک ۳۱'
];

export function randomDebtPaperSample(masterdata = {}) {
  const natural = randomInt(1, 10) <= 7;
  const taxAmount = randomAmount(80_000_000, 850_000_000);
  const dutyAmount = randomAmount(5_000_000, 80_000_000);
  const penaltyArticle22 = randomAmount(2_000_000, 45_000_000);
  const penaltyArticle23 = randomAmount(1_000_000, 35_000_000);
  const taxPaidAmount = randomAmount(0, Math.floor(taxAmount * 0.25));
  const penaltyPaidAmount = randomAmount(0, Math.floor((penaltyArticle22 + penaltyArticle23) * 0.2));
  const selected = sampleMasterdata(masterdata);

  return {
    ...initialDebtPaper,
    debtPaperUniqueCode: `FDP-${Date.now()}-${randomInt(100, 999)}`,
    debtPaperIssueDate: randomJalaliDate(),
    notificationId: `NOT-${randomInt(100000, 999999)}`,
    finalizationReason: pick(finalizationReasons),
    taxSource: pick(taxSources),
    performanceYear: String(randomInt(1399, 1403)),
    performancePeriod: String(randomInt(1, 4)),
    activityTypeCode: String(randomInt(1000, 9999)),
    activityTypeTitle: pick(activityTitles),
    mainTaxBureauId: selected.generalId,
    taxBureauId: selected.officeId,
    taxUnitId: selected.unitId,
    sendingUnitId: selected.unitId,
    taxpayerType: natural ? 'NATURAL' : 'LEGAL',
    nationalCode: natural ? randomNationalCode() : '',
    firstName: natural ? pick(firstNames) : '',
    lastName: natural ? pick(lastNames) : '',
    fatherName: natural ? pick(firstNames) : '',
    birthDate: natural ? randomJalaliDate(1345, 1379) : '',
    postalCode: randomPostalCode(),
    address: pick(sampleAddresses),
    nationalId: natural ? '' : randomNationalId(),
    registrationNumber: natural ? '' : String(randomInt(100000, 999999)),
    registrationDate: natural ? '' : randomJalaliDate(1380, 1402),
    companyName: natural ? '' : pick(companyNames),
    taxAmount: String(taxAmount),
    dutyAmount: String(dutyAmount),
    penaltyArticle22: String(penaltyArticle22),
    penaltyArticle23: String(penaltyArticle23),
    deductiblePenalty: String(randomAmount(0, 15_000_000)),
    nonDeductiblePenalty: String(randomAmount(0, 30_000_000)),
    taxPaidAmount: String(taxPaidAmount),
    penaltyPaidAmount: String(penaltyPaidAmount),
    taxCredit: String(randomAmount(0, 20_000_000)),
    remainingTax: String(Math.max(0, taxAmount + dutyAmount + penaltyArticle22 + penaltyArticle23 - taxPaidAmount - penaltyPaidAmount)),
    municipalityShare: String(randomAmount(0, 10_000_000)),
    guildShare: String(randomAmount(0, 8_000_000)),
    complianceBonus: String(randomAmount(0, 5_000_000))
  };
}

export function randomExecutionPaperSample(summary = {}, masterdata = {}) {
  const selected = sampleHierarchicalMasterdata(masterdata);
  const debtPaper = pick(summary.finalizedDebtPapers || []);
  const taxpayer = pick(summary.taxpayers || []);
  const taxAmount = randomAmount(60_000_000, 700_000_000);
  const dutyAmount = randomAmount(5_000_000, 70_000_000);
  const penaltyArticle22 = randomAmount(2_000_000, 35_000_000);
  const penaltyArticle23 = randomAmount(1_000_000, 25_000_000);
  const taxPaidAmount = randomAmount(0, Math.floor(taxAmount * 0.2));
  const penaltyPaidAmount = randomAmount(0, Math.floor((penaltyArticle22 + penaltyArticle23) * 0.2));

  return {
    ...initialExecutionPaper,
    executionPaperUniqueCode: `EP-${Date.now()}-${randomInt(100, 999)}`,
    finalizedDebtPaperId: debtPaper?.id ? String(debtPaper.id) : '',
    taxpayerId: taxpayer?.id ? String(taxpayer.id) : '',
    mainTaxBureauId: selected.generalId,
    taxBureauId: selected.officeId,
    taxUnitId: selected.unitId,
    finalizationReason: pick(finalizationReasons),
    taxSource: pick(taxSources),
    currentState: 'UNASSIGNED',
    taxAmount: String(taxAmount),
    dutyAmount: String(dutyAmount),
    penaltyArticle22: String(penaltyArticle22),
    penaltyArticle23: String(penaltyArticle23),
    deductiblePenalty: String(randomAmount(0, 12_000_000)),
    nonDeductiblePenalty: String(randomAmount(0, 25_000_000)),
    taxPaidAmount: String(taxPaidAmount),
    penaltyPaidAmount: String(penaltyPaidAmount),
    taxCredit: String(randomAmount(0, 16_000_000)),
    remainingTax: String(Math.max(0, taxAmount + dutyAmount + penaltyArticle22 + penaltyArticle23 - taxPaidAmount - penaltyPaidAmount))
  };
}

export function toDebtPaperPayload(form) {
  return {
    debtPaperUniqueCode: form.debtPaperUniqueCode,
    debtPaperIssueDate: form.debtPaperIssueDate,
    notificationId: form.notificationId,
    finalizationReason: form.finalizationReason,
    taxSource: form.taxSource,
    performanceYear: form.performanceYear,
    performancePeriod: form.performancePeriod,
    activityTypeCode: form.activityTypeCode,
    activityTypeTitle: form.activityTypeTitle,
    mainTaxBureauId: numberOrNull(form.mainTaxBureauId),
    taxBureauId: numberOrNull(form.taxBureauId),
    taxUnitId: numberOrNull(form.taxUnitId),
    sendingUnitId: numberOrNull(form.sendingUnitId),
    taxpayer: {
      type: form.taxpayerType,
      nationalCode: form.nationalCode,
      firstName: form.firstName,
      lastName: form.lastName,
      fatherName: form.fatherName,
      birthDate: form.birthDate,
      postalCode: form.postalCode,
      address: form.address,
      nationalId: form.nationalId,
      registrationNumber: form.registrationNumber,
      registrationDate: form.registrationDate,
      companyName: form.companyName
    },
    debtInfo: debtInfoPayload(form),
    relatedPersons: []
  };
}

export function toExecutionPayload(form) {
  return {
    executionPaperUniqueCode: form.executionPaperUniqueCode || null,
    finalizedDebtPaperId: numberOrNull(form.finalizedDebtPaperId),
    taxpayerId: numberOrNull(form.taxpayerId),
    taxUnitId: numberOrNull(form.taxUnitId),
    finalizationReason: form.finalizationReason,
    taxSource: form.taxSource,
    currentState: form.currentState,
    debtInfo: debtInfoPayload(form)
  };
}

export function debtInfoPayload(form) {
  return {
    taxAmount: numberOrZero(form.taxAmount),
    dutyAmount: numberOrZero(form.dutyAmount),
    penaltyArticle22: numberOrZero(form.penaltyArticle22),
    penaltyArticle23: numberOrZero(form.penaltyArticle23),
    deductiblePenalty: numberOrZero(form.deductiblePenalty),
    nonDeductiblePenalty: numberOrZero(form.nonDeductiblePenalty),
    taxPaidAmount: numberOrZero(form.taxPaidAmount),
    penaltyPaidAmount: numberOrZero(form.penaltyPaidAmount),
    taxCredit: numberOrZero(form.taxCredit),
    remainingTax: numberOrZero(form.remainingTax),
    municipalityShare: numberOrZero(form.municipalityShare),
    guildShare: numberOrZero(form.guildShare),
    complianceBonus: numberOrZero(form.complianceBonus)
  };
}

function numberOrNull(value) {
  return value === '' || value == null ? null : Number(value);
}

function numberOrZero(value) {
  return value === '' || value == null ? 0 : Number(value);
}

function sampleMasterdata(masterdata) {
  const general = pick(masterdata.generals || []);
  const office = pick(masterdata.offices || []);
  const unit = pick(masterdata.units || []);
  return {
    generalId: general?.id ? String(general.id) : '',
    officeId: office?.id ? String(office.id) : '',
    unitId: unit?.id ? String(unit.id) : ''
  };
}

function sampleHierarchicalMasterdata(masterdata) {
  const unit = pick(masterdata.units || []);
  const office = unit
    ? (masterdata.offices || []).find((item) => String(item.id) === String(unit.taxAffairsOfficeId))
    : pick(masterdata.offices || []);
  const general = office
    ? (masterdata.generals || []).find((item) => String(item.id) === String(office.generalDirectorateOfTaxAffairsId))
    : pick(masterdata.generals || []);
  return {
    generalId: general?.id ? String(general.id) : '',
    officeId: office?.id ? String(office.id) : '',
    unitId: unit?.id ? String(unit.id) : ''
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
  return (
    <FieldGrid>
      <Input label="اصل مالیات" name="taxAmount" type="number" value={form.taxAmount} onChange={onChange} />
      <Input label="عوارض" name="dutyAmount" type="number" value={form.dutyAmount} onChange={onChange} />
      <Input label="جریمه ماده ۲۲" name="penaltyArticle22" type="number" value={form.penaltyArticle22} onChange={onChange} />
      <Input label="جریمه ماده ۲۳" name="penaltyArticle23" type="number" value={form.penaltyArticle23} onChange={onChange} />
      <Input label="جریمه قابل بخشش" name="deductiblePenalty" type="number" value={form.deductiblePenalty} onChange={onChange} />
      <Input label="جریمه غیرقابل بخشش" name="nonDeductiblePenalty" type="number" value={form.nonDeductiblePenalty} onChange={onChange} />
      <Input label="پرداخت اصل" name="taxPaidAmount" type="number" value={form.taxPaidAmount} onChange={onChange} />
      <Input label="پرداخت جریمه" name="penaltyPaidAmount" type="number" value={form.penaltyPaidAmount} onChange={onChange} />
      <Input label="اعتبار مالیاتی" name="taxCredit" type="number" value={form.taxCredit} onChange={onChange} />
      <Input label="مانده بدهی" name="remainingTax" type="number" value={form.remainingTax} onChange={onChange} />
    </FieldGrid>
  );
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
