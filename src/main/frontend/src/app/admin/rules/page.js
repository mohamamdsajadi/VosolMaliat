'use client'

import { useEffect, useMemo, useState } from 'react'
import AppShell from '../../components/app-shell'
import JalaliInput from '../../components/jalali-input'
import { apiJson } from '../../lib/api'
import { formatJalaliDate } from '../../lib/display'

const FIELD_OPTIONS = [
  { value: 'performanceYear', label: 'سال (و دوره) عملکرد', type: 'text', placeholder: 'مثلاً 1403' },
  { value: 'mainTaxBureauId', label: 'اداره کل مالیاتی', type: 'number', placeholder: 'مثلاً 12' },
  { value: 'taxBureauId', label: 'اداره امور مالیاتی', type: 'number', placeholder: 'مثلاً 1203' },
  { value: 'taxSource', label: 'منبع مالیاتی', type: 'text', placeholder: 'مثلاً VAT' },
  { value: 'activityTitle', label: 'نوع فعالیت', type: 'text', placeholder: 'مثلاً 2' },
  { value: 'outstandingDebtAmount', label: 'مانده بدهی', type: 'decimal', placeholder: 'ریال؛ مقدار دقیق' },
  { value: 'totalTaxAmount', label: 'مالیات', type: 'decimal', placeholder: 'ریال؛ مقدار دقیق' },
  { value: 'totalDutyAmount', label: 'عوارض', type: 'decimal', placeholder: 'ریال؛ مقدار دقیق' },
  { value: 'totalPenaltyAmount', label: 'جرایم', type: 'decimal', placeholder: 'ریال؛ مقدار دقیق' },
  { value: 'totalPaidAmount', label: 'پرداختی', type: 'decimal', placeholder: 'ریال؛ مقدار دقیق' },
  { value: 'totalCreditAmount', label: 'اعتبار', type: 'decimal', placeholder: 'ریال؛ مقدار دقیق' },
  { value: 'totalRemissionAmount', label: 'بخشودگی', type: 'decimal', placeholder: 'ریال؛ مقدار دقیق' },
]

const OPERATORS = [
  { value: 'EQUALS', label: 'برابر باشد با' },
  { value: 'NOT_EQUALS', label: 'برابر نباشد با' },
  { value: 'GREATER_THAN', label: 'بزرگ‌تر از' },
  { value: 'GREATER_THAN_OR_EQUAL', label: 'بزرگ‌تر یا مساوی' },
  { value: 'LESS_THAN', label: 'کوچک‌تر از' },
  { value: 'LESS_THAN_OR_EQUAL', label: 'کوچک‌تر یا مساوی' },
  { value: 'IN', label: 'یکی از مقادیر' },
  { value: 'NOT_IN', label: 'هیچ‌کدام از مقادیر' },
]

const emptyCondition = () => ({ field: 'performanceYear', operator: 'EQUALS', value: '' })

function toRuleValue(rawValue, operator, field) {
  const fieldMeta = FIELD_OPTIONS.find(item => item.value === field)
  if (operator === 'IN' || operator === 'NOT_IN') {
    return rawValue.split(',').map(item => item.trim()).filter(Boolean).map(item => fieldMeta?.type === 'number' ? Number(item) : item)
  }
  return fieldMeta?.type === 'number' ? Number(rawValue) : rawValue
}

function describeRule(rule) {
  try {
    const parsed = JSON.parse(rule.ruleJson || '{}')
    const joiner = parsed.logicalOperator === 'OR' ? 'یا' : 'و'
    return (parsed.conditions || []).map(condition => {
      const field = FIELD_OPTIONS.find(item => item.value === condition.field)?.label || condition.field
      const operator = OPERATORS.find(item => item.value === condition.operator)?.label || condition.operator
      return `${field} ${operator} ${Array.isArray(condition.value) ? condition.value.join(', ') : condition.value}`
    }).join(` ${joiner} `)
  } catch {
    return rule.ruleJson
  }
}

export default function RuleDashboardPage() {
  const [rules, setRules] = useState([])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')
  const [conditions, setConditions] = useState([emptyCondition()])
  const [logicalOperator, setLogicalOperator] = useState('AND')
  const [reasonCode, setReasonCode] = useState('ADMIN_RULE_BLOCKED')
  const [description, setDescription] = useState('عدم احراز شرایط صدور برگ اجرا براساس قانون مدیریتی')
  const [startDate, setStartDate] = useState(new Date().toISOString().slice(0, 10))
  const [endDate, setEndDate] = useState('2026-12-29')
  const [documentNo, setDocumentNo] = useState('')

  const preview = useMemo(() => ({
    logicalOperator,
    rejectionReasonCode: reasonCode,
    rejectionReasonDescription: description,
    conditions: conditions.map(condition => ({
      ...condition,
      value: toRuleValue(condition.value, condition.operator, condition.field),
    })),
  }), [conditions, description, logicalOperator, reasonCode])

  const loadRules = async () => {
    setLoading(true)
    setRules(await apiJson('/api/v1/admin/rejection-rules'))
    setLoading(false)
  }

  useEffect(() => {
    loadRules().catch(error => { setMessage(error.message); setLoading(false) })
  }, [])

  const updateCondition = (index, key, value) => {
    setConditions(current => current.map((condition, idx) => idx === index ? { ...condition, [key]: value } : condition))
  }

  const saveRule = async event => {
    event.preventDefault()
    setMessage('')
    await apiJson('/api/v1/admin/rejection-rules', {
      method: 'POST',
      body: JSON.stringify({
        ruleJson: JSON.stringify(preview),
        startDate,
        endDate,
        documentNo,
        filePath: null,
        description,
      }),
    })
    setMessage('قانون با موفقیت ثبت شد و برای نمونه‌های جدید اعمال می‌شود.')
    setConditions([emptyCondition()])
    await loadRules()
  }

  const deleteRule = async id => {
    await apiJson(`/api/v1/admin/rejection-rules/${id}`, { method: 'DELETE' })
    await loadRules()
  }

  return (
    <AppShell
      active="rule-management"
      title="پنل مدیریت قوانین صدور"
      eyebrow="قانون‌ساز مدیریتی"
    >
      <main style={{ maxWidth: 1280, margin: '0 auto' }}>
        <section style={heroStyle}>
          <div>
            <p style={eyebrowStyle}>قانون‌ساز مدیریتی</p>
            <h1 style={{ margin: '6px 0 10px', color: '#123b63' }}>ترکیب شرط‌ها روی برگ بدهی قطعی</h1>
            <p style={{ margin: 0, color: '#58708a', lineHeight: 1.9 }}>
              هر قانون فعال اگر با اطلاعات برگ بدهی قطعی منطبق شود، گام بررسی شرایط در BPMN صدور برگ اجرا را متوقف می‌کند. قوانین جدید فقط روی نمونه‌های جدید فرایند اعمال می‌شوند.
            </p>
          </div>
          <div style={summaryBoxStyle}>{rules.length} قانون ثبت‌شده</div>
        </section>

        {message && <div style={noticeStyle}>{message}</div>}

        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1.2fr) minmax(320px, 0.8fr)', gap: 20 }}>
          <form onSubmit={saveRule} style={cardStyle}>
            <h2 style={titleStyle}>ساخت قانون جدید</h2>
            <div style={toolbarStyle}>
              <label style={fieldStyle}>ترکیب شرط‌ها
                <select value={logicalOperator} onChange={e => setLogicalOperator(e.target.value)} style={inputStyle}>
                  <option value="AND">همه شرط‌ها برقرار باشد (AND)</option>
                  <option value="OR">حداقل یکی برقرار باشد (OR)</option>
                </select>
              </label>
              <label style={fieldStyle}>کد علت رد
                <input value={reasonCode} onChange={e => setReasonCode(e.target.value)} style={inputStyle} />
              </label>
            </div>

            {conditions.map((condition, index) => {
              const meta = FIELD_OPTIONS.find(item => item.value === condition.field)
              return (
                <div key={index} style={conditionRowStyle}>
                  <select value={condition.field} onChange={e => updateCondition(index, 'field', e.target.value)} style={inputStyle}>
                    {FIELD_OPTIONS.map(field => <option key={field.value} value={field.value}>{field.label}</option>)}
                  </select>
                  <select value={condition.operator} onChange={e => updateCondition(index, 'operator', e.target.value)} style={inputStyle}>
                    {OPERATORS.map(operator => <option key={operator.value} value={operator.value}>{operator.label}</option>)}
                  </select>
                  <input
                    type={meta?.type === 'number' && !['IN', 'NOT_IN'].includes(condition.operator) ? 'number' : 'text'}
                    value={condition.value}
                    onChange={e => updateCondition(index, 'value', e.target.value)}
                    placeholder={['IN', 'NOT_IN'].includes(condition.operator) ? 'مقادیر را با کاما جدا کنید' : meta?.placeholder}
                    style={inputStyle}
                    required
                  />
                  <button type="button" onClick={() => setConditions(current => current.filter((_, idx) => idx !== index))} disabled={conditions.length === 1} style={dangerButtonStyle}>حذف</button>
                </div>
              )
            })}

            <button type="button" onClick={() => setConditions(current => [...current, emptyCondition()])} style={secondaryButtonStyle}>+ افزودن شرط</button>

            <div style={toolbarStyle}>
              <label style={fieldStyle}>تاریخ شروع
                <JalaliInput value={startDate} onChangeValue={setStartDate} style={inputStyle} required />
              </label>
              <label style={fieldStyle}>تاریخ پایان
                <JalaliInput value={endDate} onChangeValue={setEndDate} style={inputStyle} required />
              </label>
              <label style={fieldStyle}>شماره مستند
                <input value={documentNo} onChange={e => setDocumentNo(e.target.value)} style={inputStyle} />
              </label>
            </div>

            <label style={fieldStyle}>شرح علت عدم امکان صدور
              <textarea value={description} onChange={e => setDescription(e.target.value)} style={{ ...inputStyle, minHeight: 76 }} required />
            </label>

            <button type="submit" style={primaryButtonStyle}>ثبت و اعمال برای نمونه‌های جدید</button>
          </form>

          <aside style={cardStyle}>
            <h2 style={titleStyle}>پیش‌نمایش JSON قانون</h2>
            <pre style={previewStyle}>{JSON.stringify(preview, null, 2)}</pre>
          </aside>
        </div>

        <section style={{ ...cardStyle, marginTop: 20 }}>
          <h2 style={titleStyle}>قوانین فعال و ثبت‌شده</h2>
          {loading ? <p>در حال دریافت قوانین...</p> : rules.length === 0 ? <p>هنوز قانونی ثبت نشده است.</p> : (
            <div style={{ display: 'grid', gap: 12 }}>
              {rules.map(rule => (
                <article key={rule.id} style={ruleItemStyle}>
                  <div>
                    <strong>{rule.description || 'قانون بدون شرح'}</strong>
                    <p style={{ margin: '8px 0', color: '#4b5563' }}>{describeRule(rule)}</p>
                    <small>{formatJalaliDate(rule.startDate)} تا {formatJalaliDate(rule.endDate)} {rule.documentNo ? ` | مستند: ${rule.documentNo}` : ''}</small>
                  </div>
                  <button onClick={() => deleteRule(rule.id)} style={dangerButtonStyle}>حذف</button>
                </article>
              ))}
            </div>
          )}
        </section>
      </main>
    </AppShell>
  )
}

const heroStyle = { background: 'linear-gradient(135deg, #e0f2fe, #ffffff)', border: '1px solid #bfdbfe', borderRadius: 18, padding: 24, marginBottom: 20, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 20 }
const eyebrowStyle = { margin: 0, color: '#2563eb', fontWeight: 800, fontSize: 13 }
const summaryBoxStyle = { background: '#1d4ed8', color: 'white', borderRadius: 16, padding: '18px 24px', fontWeight: 900, whiteSpace: 'nowrap' }
const cardStyle = { background: 'white', border: '1px solid #e5e7eb', borderRadius: 16, padding: 20, boxShadow: '0 10px 24px rgba(15, 23, 42, 0.06)' }
const titleStyle = { margin: '0 0 16px', color: '#1f2937', fontSize: 18 }
const toolbarStyle = { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12, marginBottom: 14 }
const fieldStyle = { display: 'grid', gap: 6, color: '#374151', fontSize: 13, fontWeight: 700 }
const inputStyle = { width: '100%', boxSizing: 'border-box', border: '1px solid #d1d5db', borderRadius: 10, padding: '10px 12px', fontSize: 14, background: 'white' }
const conditionRowStyle = { display: 'grid', gridTemplateColumns: '1.4fr 1fr 1.2fr auto', gap: 10, alignItems: 'center', background: '#f8fafc', border: '1px solid #e5e7eb', borderRadius: 12, padding: 12, marginBottom: 10 }
const primaryButtonStyle = { background: '#2563eb', color: 'white', border: 'none', borderRadius: 10, padding: '12px 18px', cursor: 'pointer', fontWeight: 800, marginTop: 12 }
const secondaryButtonStyle = { background: '#eff6ff', color: '#1d4ed8', border: '1px solid #bfdbfe', borderRadius: 10, padding: '10px 14px', cursor: 'pointer', fontWeight: 800, marginBottom: 16 }
const dangerButtonStyle = { background: '#fee2e2', color: '#b91c1c', border: '1px solid #fecaca', borderRadius: 10, padding: '9px 12px', cursor: 'pointer', fontWeight: 800 }
const previewStyle = { direction: 'ltr', textAlign: 'left', background: '#0f172a', color: '#dbeafe', borderRadius: 12, padding: 16, overflow: 'auto', fontSize: 12 }
const noticeStyle = { background: '#ecfdf5', color: '#047857', border: '1px solid #a7f3d0', borderRadius: 12, padding: 12, marginBottom: 16 }
const ruleItemStyle = { display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'center', background: '#f9fafb', border: '1px solid #e5e7eb', borderRadius: 12, padding: 14 }
