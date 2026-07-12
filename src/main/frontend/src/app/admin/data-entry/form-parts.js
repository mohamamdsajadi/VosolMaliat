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
  sendingUnitId: '',
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
    mainTaxBureauId: numberOrNull(form.mainTaxBureauId),
    taxBureauId: numberOrNull(form.taxBureauId),
    taxUnitId: numberOrNull(form.taxUnitId),
    sendingUnitId: numberOrNull(form.sendingUnitId),
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
