'use client'

import { Fragment, useEffect, useMemo, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import AppShell from '../../components/app-shell'
import { apiFetch } from '../../lib/api'

function toPersianDigits(str) {
  const persianDigits = '۰۱۲۳۴۵۶۷۸۹'
  return String(str ?? '').replace(/\d/g, d => persianDigits[d])
}

function formatValue(value) {
  if (value == null || value === '') return '—'
  if (typeof value === 'number') return toPersianDigits(value.toLocaleString('en-US'))
  return toPersianDigits(value)
}

function formatIdentifier(value) {
  if (value == null || value === '') return '—'
  return toPersianDigits(String(value))
}

function formatDetailValue(key, value) {
  if (value == null || value === '') return '—'
  if (isDateTimeField(key)) return formatPersianDateTime(value)
  if (isDateOnlyField(key)) return formatPersianDate(value)
  return formatValue(value)
}

function isDateTimeField(key) {
  return [
    'createdAt',
    'updatedAt',
    'electronicNotificationDate',
    'informRejectedAt',
    'reInformDate',
    'nextDebtInquiryDate',
    'rejectedAt',
    'InformDate',
    'notificationDate'
  ].includes(key)
}

function isDateOnlyField(key) {
  return ['debtPaperIssueDate', 'debtPaperDate', 'birthDate', 'registrationDate'].includes(key)
}

function formatMoney(value) {
  if (value == null || value === '') return '۰'
  const number = Number(value)
  if (Number.isNaN(number)) return formatValue(value)
  return toPersianDigits(Math.round(number).toLocaleString('en-US'))
}

function orgDisplay(name, id) {
  if (name && id) return `${name} - کد ${id}`
  if (name) return name
  if (id) return `کد ${id}`
  return ''
}

const CRITICAL_TRACE_FIELD_LABELS = {
  TASK_CLAIMED: 'مسئول فعلی',
  TASK_UNCLAIMED: 'مسئول فعلی',
  TASK_ASSIGNED: 'مسئول فعلی',
  TASK_DELEGATED_TO_USER: 'مسئول فعلی',
  TASK_DELEGATED_TO_GROUP: 'گروه/نقش مقصد',
  ELECTRONIC_NOTIFICATION_CONFIRMED: 'تاریخ ابلاغ الکترونیکی',
  EXECUTION_PAPER_INFORM_REJECTED: 'وضعیت و علت رد ابلاغ',
  EXECUTION_PAPER_INFORMED: 'تاریخ ابلاغ',
  EXECUTION_PAPER_RE_INFORM_SCHEDULED: 'تاریخ بررسی مجدد ابلاغ',
  NEXT_DEBT_INQUIRY_DATE_SET: 'تاریخ استعلام بعدی بدهی',
  INFORM_DATE_SENT_TO_SANIM: 'تاریخ ابلاغ ارسال‌شده به سنیم',
  SANIM_MESSAGE_CONFIRMED: 'وضعیت دریافت سنیم',
  SANIM_INFORM_DATE_CONFIRMED: 'وضعیت دریافت سنیم',
  RELATED_PERSONS_REFRESHED_FROM_COMPANY_REGISTRY: 'اطلاعات اشخاص مرتبط دریافت‌شده',
  RELATED_PERSONS_STORED_FROM_OFFICIAL_GAZETTE: 'اشخاص مرتبط ذخیره‌شده از روزنامه رسمی',
  OFFICIAL_GAZETTE_REQUEST_SENT: 'شناسه درخواست روزنامه رسمی',
  OFFICIAL_GAZETTE_RESPONSE_SENT: 'خلاصه پاسخ روزنامه رسمی',
  RESTRICTION_REQUEST_SENT: 'خلاصه درخواست اعمال محدودیت',
  RESTRICTION_REQUEST_CONFIRMED: 'نتیجه تایید اعمال محدودیت'
}

const RELATED_PERSON_TRACE_ACTIONS = new Set([
  'RELATED_PERSONS_REFRESHED_FROM_COMPANY_REGISTRY',
  'RELATED_PERSONS_STORED_FROM_OFFICIAL_GAZETTE'
])

const EXTERNAL_REQUEST_FIELD_LABELS = {
  requestId: 'شناسه درخواست',
  officialGazetteRequestId: 'شناسه درخواست روزنامه رسمی',
  executionPaperId: 'شناسه برگ اجرا',
  executionPaperCode: 'کد یکتای برگ اجرا',
  taxpayerName: 'نام مؤدی',
  taxpayerNationalId: 'شناسه ملی/کد ملی مؤدی',
  taxpayerType: 'نوع مؤدی',
  notificationDate: 'تاریخ ابلاغ',
  notificationStatus: 'وضعیت ابلاغ',
  paymentDeadlineStatus: 'وضعیت مهلت پرداخت',
  legalPersonNationalId: 'شناسه ملی مؤدی حقوقی',
  debtPaperDate: 'تاریخ برگ بدهی',
  debtPaperId: 'شناسه برگ بدهی',
  messageType: 'نوع پیام',
  InformDate: 'تاریخ ابلاغ',
  uniqueCode: 'کد یکتای برگ اجرا',
  currentState: 'وضعیت برگ اجرا',
  currentStateLabel: 'عنوان وضعیت برگ اجرا',
  currentStateDisplay: 'وضعیت برگ اجرا',
  taxSource: 'منبع مالیاتی',
  taxSourceLabel: 'عنوان منبع مالیاتی',
  taxSourceDisplay: 'منبع مالیاتی',

  uniqueFinalTaxDebtNoticeId: 'شناسه یکتای برگ بدهی قطعی',
  debtPaperIssueDate: 'تاریخ صدور برگ بدهی',
  notificationId: 'شناسه ابلاغ',
  performanceYear: 'سال عملکرد',
  performancePeriod: 'دوره عملکرد',

  activityTypeTitle: 'عنوان نوع فعالیت',
  activityTypeDisplay: 'نوع فعالیت',
  mainTaxBureauId: 'شناسه اداره کل مالیاتی',
  mainTaxBureauName: 'اداره کل مالیاتی',
  generalDirectorateName: 'اداره کل مالیاتی',
  taxBureauId: 'شناسه اداره مالیاتی',
  taxBureauName: 'اداره مالیاتی',
  taxAffairsOfficeName: 'اداره مالیاتی',


  id: 'شناسه داخلی',
  type: 'نوع مؤدی',
  typeLabel: 'عنوان نوع مؤدی',
  typeDisplay: 'نوع مؤدی',
  nationalCode: 'کد ملی',
  nationalId: 'شناسه ملی',
  firstName: 'نام',
  lastName: 'نام خانوادگی',
  fatherName: 'نام پدر',
  birthDate: 'تاریخ تولد',
  companyName: 'نام شخص حقوقی',
  registrationNumber: 'شماره ثبت',
  registrationDate: 'تاریخ ثبت',
  postalCode: 'کد پستی',
  address: 'نشانی قانونی',










  totalDebt: 'جمع کل بدهی',
  electronicNotificationDate: 'تاریخ ابلاغ الکترونیکی',
  informStatus: 'وضعیت ابلاغ',
  informStatusLabel: 'عنوان وضعیت ابلاغ',
  informRejectionReason: 'علت رد ابلاغ',
  informRejectedAt: 'زمان رد ابلاغ',
  reInformDate: 'زمان ابلاغ مجدد',
  nextDebtInquiryDate: 'زمان استعلام بعدی بدهی',
  rejectionReasonCode: 'کد علت رد',
  rejectionReasonDescription: 'شرح علت رد',
  restrictionRemovalDate: 'تاریخ رفع محدودیت',
  createdAt: 'زمان ایجاد',
  rawPayload: 'متن اصلی درخواست'
}

const EXTERNAL_REQUEST_SECTION_LABELS = {
  summary: 'خلاصه برگ اجرا',
  executionPaper: 'مشخصات برگ اجرا',
  finalTaxDebtPaper: 'مشخصات برگ بدهی قطعی',
  taxpayer: 'مشخصات مؤدی',
  debtInformation: 'مبالغ تجمیعی بدهی',
  notification: 'اطلاعات ابلاغ',
  ruleRejection: 'اطلاعات رد مبتنی بر قانون',
  relatedPersons: 'اشخاص مرتبط مشمول محدودیت'
}

const EXTERNAL_MONEY_FIELDS = new Set([


      'totalDebt'
])

const RELATED_PERSON_POST_LABELS = {
  CEO: 'مدیرعامل', BOARD_CHAIR: 'رئیس هیئت‌مدیره', BOARD_VICE_CHAIR: 'نایب‌رئیس هیئت‌مدیره',
  BOARD_MEMBER: 'عضو هیئت‌مدیره', ALTERNATE_BOARD_MEMBER: 'عضو علی‌البدل هیئت‌مدیره',
  MANAGING_PARTNER: 'شریک مدیر', PARTNER: 'شریک', SHAREHOLDER: 'سهام‌دار',
  LEGAL_REPRESENTATIVE: 'نماینده قانونی', LEGAL_ENTITY_REPRESENTATIVE: 'نماینده شخص حقوقی',
  LIQUIDATOR: 'مدیر تصفیه', PRIMARY_INSPECTOR: 'بازرس اصلی', ALTERNATE_INSPECTOR: 'بازرس علی‌البدل',
  PARENT_COMPANY: 'شرکت مادر', SUBSIDIARY: 'شرکت تابعه', AFFILIATE: 'شخص حقوقی وابسته'
}

function externalRequestFieldLabel(key) {
  return EXTERNAL_REQUEST_FIELD_LABELS[key] || key
}

function externalRequestValue(key, value) {
  if (typeof value === 'boolean') return value ? 'بله' : 'خیر'
  if (key === 'messageType') {
    return { EXECUTION_PAPER_ID: 'ارسال شناسه برگ اجرا', INFORM_DATE: 'ارسال تاریخ ابلاغ', ISSUANCE_REJECTION: 'رد صدور برگ اجرا' }[value] || formatValue(value)
  }
  if (key === 'type') return { NATURAL: 'حقیقی', LEGAL: 'حقوقی' }[value] || formatValue(value)
  if (key === 'taxpayerType') return { NATURAL: 'حقیقی', LEGAL: 'حقوقی' }[value] || formatValue(value)
  if (key === 'currentState') return {
    UNASSIGNED: 'در انتظار ارجاع', ASSIGNED: 'ارجاع‌شده', INFORM_SENT: 'ارسال‌شده برای ابلاغ',
    INFORMED: 'ابلاغ‌شده', INFORM_REJECTED: 'رد ابلاغ', COMPLETED: 'تکمیل‌شده', CANCELLED: 'لغوشده'
  }[value] || formatValue(value)
  if (key === 'notificationStatus' || key === 'informStatus') return {
    NOT_NOTIFIED: 'ابلاغ‌نشده', NOTIFIED: 'ابلاغ‌شده', INFORM_SENT: 'ارسال‌شده برای ابلاغ',
    INFORMED: 'ابلاغ‌شده', INFORM_REJECTED: 'رد ابلاغ'
  }[value] || formatValue(value)
  if (key === 'paymentDeadlineStatus') return {
    NOT_STARTED: 'آغاز نشده', ACTIVE: 'مهلت فعال', EXPIRED: 'مهلت پایان یافته', UNKNOWN: 'نامشخص'
  }[value] || formatValue(value)
  if (EXTERNAL_MONEY_FIELDS.has(key)) return `${formatMoney(value)} ریال`
  return formatDetailValue(key, value)
}

function ExternalRelatedPersonsTable({ persons }) {
  if (!persons.length) return <p style={externalPayloadEmptyStyle}>هیچ شخص مرتبطی در بدنه درخواست ارسال نشده است.</p>
  return (
    <div style={externalPayloadTableScrollStyle}>
      <table style={externalPayloadTableStyle}>
        <thead><tr>
          <th style={externalPayloadHeaderCellStyle}>نوع شخص</th>
          <th style={externalPayloadHeaderCellStyle}>نام</th>
          <th style={externalPayloadHeaderCellStyle}>شناسه</th>
          <th style={externalPayloadHeaderCellStyle}>سمت</th>
          <th style={externalPayloadHeaderCellStyle}>شروع اعتبار</th>
          <th style={externalPayloadHeaderCellStyle}>پایان اعتبار</th>
          <th style={externalPayloadHeaderCellStyle}>حق امضا</th>
          <th style={externalPayloadHeaderCellStyle}>نماینده شخص حقوقی</th>
        </tr></thead>
        <tbody>{persons.map((person, index) => {
          const representative = person.legalRepresentative || {}
          const representativeName = [representative.firstName, representative.lastName].filter(Boolean).join(' ')
          const representativeId = representative.nationalCode || representative.fida
          return <tr key={person.sourceRecordId || `${person.nationalCode || person.nationalId}-${index}`}>
            <td style={externalPayloadCellStyle}>{person.personType === 'LEGAL' ? 'حقوقی' : 'حقیقی'}</td>
            <td style={externalPayloadCellStyle}><strong>{formatValue(person.companyName || [person.firstName, person.lastName].filter(Boolean).join(' '))}</strong></td>
            <td style={externalPayloadCellStyle}>{formatValue(person.nationalCode || person.fida || person.nationalId || person.registrationNumber)}</td>
            <td style={externalPayloadCellStyle}>{formatValue(RELATED_PERSON_POST_LABELS[person.postCode] || person.postCode)}</td>
            <td style={externalPayloadCellStyle}>{formatPersianDate(person.startDate)}</td>
            <td style={externalPayloadCellStyle}>{formatPersianDate(person.endDate)}</td>
            <td style={externalPayloadCellStyle}>{person.hasSignatoryAuthority ? 'دارد' : 'ندارد'}</td>
            <td style={externalPayloadCellStyle}>{formatValue([representativeName, representativeId ? `(${representativeId})` : ''].filter(Boolean).join(' '))}</td>
          </tr>
        })}</tbody>
      </table>
    </div>
  )
}

function StructuredExternalPayload({ value, fieldKey = 'requestBody' }) {
  if (Array.isArray(value)) {
    if (fieldKey === 'relatedPersons') return <ExternalRelatedPersonsTable persons={value} />
    return <div style={externalPayloadListStyle}>{value.map((item, index) => (
      <div key={index} style={externalPayloadListItemStyle}>
        <span style={externalPayloadIndexStyle}>مورد {formatValue(index + 1)}</span>
        <StructuredExternalPayload value={item} />
      </div>
    ))}</div>
  }
  if (!value || typeof value !== 'object') {
    return <strong style={externalPayloadValueStyle}>{externalRequestValue(fieldKey, value)}</strong>
  }

  const entries = Object.entries(value)
  const simpleEntries = entries.filter(([, item]) => item == null || typeof item !== 'object')
  const nestedEntries = entries.filter(([, item]) => item != null && typeof item === 'object')
  return (
    <div style={externalPayloadObjectStyle}>
      {simpleEntries.length > 0 && <div style={externalPayloadGridStyle}>{simpleEntries.map(([key, item]) => (
        <div key={key} style={externalPayloadFieldStyle}>
          <span style={externalPayloadLabelStyle}>{externalRequestFieldLabel(key)}</span>
          <strong style={externalPayloadValueStyle}>{externalRequestValue(key, item)}</strong>
        </div>
      ))}</div>}
      {nestedEntries.map(([key, item]) => (
        <section key={key} style={externalPayloadGroupStyle}>
          <h5 style={externalPayloadGroupTitleStyle}>{EXTERNAL_REQUEST_SECTION_LABELS[key] || externalRequestFieldLabel(key)}</h5>
          <StructuredExternalPayload value={item} fieldKey={key} />
        </section>
      ))}
    </div>
  )
}

const TRACE_EXTERNAL_REQUEST_MATCHERS = {
  EXECUTION_PAPER_ID_SENT_TO_SANIM: request => request.systemCode === 'SANIM' && request.requestType === 'EXECUTION_PAPER_ID',
  INFORM_DATE_SENT_TO_SANIM: request => request.systemCode === 'SANIM' && request.requestType === 'INFORM_DATE',
  INFORM_SENT_TO_ELECTRONIC_NOTIFICATION: request => request.systemCode === 'ELECTRONIC_NOTIFICATION',
  OFFICIAL_GAZETTE_REQUEST_SENT: request => request.systemCode === 'OFFICIAL_GAZETTE',
  RESTRICTION_REQUEST_SENT: request => request.systemCode === 'COMPANY_REGISTRY'
}

function traceTime(value) {
  const time = new Date(value || '').getTime()
  return Number.isNaN(time) ? 0 : time
}

function requestsByTraceIndex(items, requests) {
  const matches = new Map()
  const usedRequestKeys = new Set()
  items.forEach((item, index) => {
    const matcher = TRACE_EXTERNAL_REQUEST_MATCHERS[item.action]
    if (!matcher) return
    const available = requests.filter(request => {
      const key = `${request.systemCode}-${request.id}`
      return !usedRequestKeys.has(key) && matcher(request)
    })
    if (!available.length) return

    const traceValue = extractTraceValue(item.newValue)
    const exactTrackingMatch = available.find(request => request.trackingCode && request.trackingCode === traceValue)
    const request = exactTrackingMatch || available.reduce((nearest, candidate) => {
      if (!nearest) return candidate
      const candidateDistance = Math.abs(traceTime(candidate.sentAt) - traceTime(item.occurredAt))
      const nearestDistance = Math.abs(traceTime(nearest.sentAt) - traceTime(item.occurredAt))
      return candidateDistance < nearestDistance ? candidate : nearest
    }, null)
    usedRequestKeys.add(`${request.systemCode}-${request.id}`)
    matches.set(index, request)
  })
  return matches
}

function TraceExternalRequest({ request }) {
  return (
    <div style={traceExternalRequestStyle}>
      <div style={traceExternalRequestHeaderStyle}>
        <div style={externalRequestIdentityStyle}>
          <span style={externalSystemBadgeStyle}>{request.systemLabel}</span>
          <div>
            <strong style={externalRequestTitleStyle}>{request.requestTypeLabel}</strong>
            <span style={externalRequestSentStyle}>بدنه ثبت‌شده در زمان ارسال: {formatPersianDateTime(request.sentAt)}</span>
          </div>
        </div>
        <span style={{ ...externalStatusBadgeStyle, ...(request.status === 'FAILED' ? externalStatusFailedStyle : request.status === 'SENT' ? externalStatusPendingStyle : externalStatusDoneStyle) }}>
          {request.statusLabel || request.status}
        </span>
      </div>
      <div style={externalRequestMetaStyle}>
        <span>شماره درخواست: {formatValue(request.id)}</span>
        {request.trackingCode && <span>شناسه پیگیری: {formatValue(request.trackingCode)}</span>}
        {request.completedAt && <span>زمان پاسخ/تأیید: {formatPersianDateTime(request.completedAt)}</span>}
      </div>
      <details style={externalRequestBodyStyle}>
        <summary style={externalRequestBodySummaryStyle}>مشاهده اطلاعات دقیق ارسال‌شده به {request.systemLabel}</summary>
        <div style={externalRequestBodyContentStyle}>
          <StructuredExternalPayload value={request.requestBody} />
        </div>
      </details>
    </div>
  )
}

function extractTraceValue(value) {
  if (value == null || value === '') return ''
  const text = String(value)
  const match = text.match(/(?:^|;\s*)value=(.*)$/)
  return match ? match[1] : text
}

function hasTraceChange(item) {
  if (!item || item.type !== 'AUDIT') return false
  if (!CRITICAL_TRACE_FIELD_LABELS[item.action]) return false
  const oldValue = extractTraceValue(item.oldValue)
  const newValue = extractTraceValue(item.newValue)
  return oldValue !== '' || newValue !== ''
}

function traceFieldLabel(item) {
  return CRITICAL_TRACE_FIELD_LABELS[item.action] || 'داده حساس'
}

function formatTraceChangeValue(value) {
  return formatAuditValue(extractTraceValue(value))
}

function parseRelatedPersonsTraceValue(value) {
  const rawValue = extractTraceValue(value)
  if (!rawValue) return null

  try {
    const parsedValue = typeof rawValue === 'string' ? JSON.parse(rawValue) : rawValue
    return parsedValue && Array.isArray(parsedValue.relatedPersons) ? parsedValue : null
  } catch {
    return null
  }
}

function relatedPersonDisplayName(person) {
  if (person.personType === 'LEGAL') return formatValue(person.companyName)
  return formatValue([person.firstName, person.lastName].filter(Boolean).join(' '))
}

function relatedPersonIdentifier(person) {
  return formatValue(
    person.nationalCode ||
    person.fida ||
    person.nationalId ||
    person.registrationNumber
  )
}

function legalRepresentativeDisplay(person) {
  if (person.personType !== 'LEGAL') return '—'
  const name = [person.legalRepresentativeFirstName, person.legalRepresentativeLastName]
    .filter(Boolean)
    .join(' ')
  const identifier = person.legalRepresentativeNationalCode || person.legalRepresentativeFida
  if (!name && !identifier) return '—'
  return formatValue([name, identifier ? `(${identifier})` : ''].filter(Boolean).join(' '))
}

function normalizeRelatedPersonDate(value) {
  if (!value) return ''
  const englishDigits = '0123456789'
  return String(value)
    .trim()
    .replace(/[۰-۹]/g, digit => englishDigits['۰۱۲۳۴۵۶۷۸۹'.indexOf(digit)])
    .replaceAll('/', '-')
}

function isRelatedPersonInDebtYear(person, debtYear) {
  const normalizedYear = normalizeRelatedPersonDate(debtYear).replaceAll(/[^0-9]/g, '')
  if (!normalizedYear) return false
  const yearStart = `${normalizedYear}-01-01`
  const nextYearStart = `${Number(normalizedYear) + 1}-01-01`
  const startDate = normalizeRelatedPersonDate(person.startDate)
  const endDate = normalizeRelatedPersonDate(person.endDate)
  return (!startDate || startDate < nextYearStart) && (!endDate || endDate >= yearStart)
}

function relatedPersonStatus(person, debtYear) {
  if (normalizeRelatedPersonDate(person.endDate)) {
    return {
      key: 'expired',
      label: 'منقضی‌شده',
      badgeStyle: expiredRelatedPersonBadgeStyle,
      rowStyle: expiredRelatedPersonRowStyle
    }
  }
  if (isRelatedPersonInDebtYear(person, debtYear)) {
    return {
      key: 'debt-year',
      label: 'مرتبط در سال بدهی',
      badgeStyle: debtYearRelatedPersonBadgeStyle,
      rowStyle: debtYearRelatedPersonRowStyle
    }
  }
  return {
    key: 'other',
    label: 'خارج از سال بدهی',
    badgeStyle: otherRelatedPersonBadgeStyle,
    rowStyle: otherRelatedPersonRowStyle
  }
}

function RelatedPersonStatusBadge({ person, debtYear }) {
  const status = relatedPersonStatus(person, debtYear)
  return <span style={{ ...relatedPersonStatusBadgeStyle, ...status.badgeStyle }}>{status.label}</span>
}

function RelatedPersonsStatusLegend() {
  return (
    <div style={relatedPersonsLegendStyle} aria-label="راهنمای وضعیت اشخاص مرتبط">
      <span style={{ ...relatedPersonStatusBadgeStyle, ...debtYearRelatedPersonBadgeStyle }}>مرتبط در سال بدهی</span>
      <span style={{ ...relatedPersonStatusBadgeStyle, ...expiredRelatedPersonBadgeStyle }}>منقضی‌شده</span>
      <span style={{ ...relatedPersonStatusBadgeStyle, ...otherRelatedPersonBadgeStyle }}>خارج از سال بدهی</span>
    </div>
  )
}

function RelatedPersonsTraceValue({ value }) {
  const snapshot = parseRelatedPersonsTraceValue(value)
  if (!snapshot) {
    return (
      <div style={traceChangeBoxStyle}>
        <span style={traceChangeFieldStyle}>اطلاعات جدید اشخاص مرتبط</span>
        <strong style={traceFallbackValueStyle}>{formatTraceChangeValue(value)}</strong>
      </div>
    )
  }

  const persons = snapshot.relatedPersons
  return (
    <div style={relatedPersonsTraceStyle}>
      <div style={relatedPersonsTraceHeadingStyle}>
        <strong style={traceChangeFieldStyle}>اطلاعات جدید اشخاص مرتبط</strong>
        <div style={relatedPersonsTraceHeadingMetaStyle}>
          <RelatedPersonsStatusLegend />
          <span style={relatedPersonsCountStyle}>
            {formatValue(persons.length)} شخص
          </span>
        </div>
      </div>
      <div style={relatedPersonsTraceMetaStyle}>
        <div style={relatedPersonsTraceMetaItemStyle}>
          <span style={traceChangeLabelStyle}>شناسه برگ اجرا</span>
          <strong>{formatValue(snapshot.executionPaperId)}</strong>
        </div>
        <div style={relatedPersonsTraceMetaItemStyle}>
          <span style={traceChangeLabelStyle}>شناسه ملی مودی حقوقی</span>
          <strong>{formatValue(snapshot.ownerNationalId)}</strong>
        </div>
        <div style={relatedPersonsTraceMetaItemStyle}>
          <span style={traceChangeLabelStyle}>سال بدهی</span>
          <strong>{formatValue(snapshot.debtYear)}</strong>
        </div>
        <div style={relatedPersonsTraceMetaItemStyle}>
          <span style={traceChangeLabelStyle}>زمان آخرین بروزرسانی</span>
          <strong>{formatPersianDateTime(snapshot.lastSyncedAt)}</strong>
        </div>
      </div>
      {persons.length === 0 ? (
        <p style={relatedPersonsEmptyTraceStyle}>شخص مرتبطی در پاسخ دریافت‌شده وجود ندارد.</p>
      ) : (
        <div style={relatedPersonsTraceTableScrollStyle}>
          <table style={relatedPersonsTraceTableStyle}>
            <thead>
              <tr>
                <th style={relatedPersonHeaderCellStyle}>وضعیت</th>
                <th style={relatedPersonHeaderCellStyle}>نوع شخص</th>
                <th style={relatedPersonHeaderCellStyle}>نام شخص</th>
                <th style={relatedPersonHeaderCellStyle}>شناسه</th>
                <th style={relatedPersonHeaderCellStyle}>سمت</th>
                <th style={relatedPersonHeaderCellStyle}>نماینده شخص حقوقی</th>
                <th style={relatedPersonHeaderCellStyle}>تاریخ شروع</th>
                <th style={relatedPersonHeaderCellStyle}>تاریخ پایان</th>
                <th style={relatedPersonHeaderCellStyle}>حق امضا</th>
              </tr>
            </thead>
            <tbody>
              {persons.map((person, index) => {
                const status = relatedPersonStatus(person, snapshot.debtYear)
                return (
                <tr
                  key={person.relationshipId || `${relatedPersonIdentifier(person)}-${index}`}
                  style={status.rowStyle}
                >
                  <td style={relatedPersonCellStyle}>
                    <RelatedPersonStatusBadge person={person} debtYear={snapshot.debtYear} />
                  </td>
                  <td style={relatedPersonCellStyle}>
                    {person.personType === 'NATURAL' ? 'حقیقی' : person.personType === 'LEGAL' ? 'حقوقی' : 'نامشخص'}
                  </td>
                  <td style={relatedPersonCellStyle}><strong>{relatedPersonDisplayName(person)}</strong></td>
                  <td style={relatedPersonCellStyle}>{relatedPersonIdentifier(person)}</td>
                  <td style={relatedPersonCellStyle}>{formatValue(person.postLabel || person.postCode)}</td>
                  <td style={relatedPersonCellStyle}>{legalRepresentativeDisplay(person)}</td>
                  <td style={relatedPersonCellStyle}>{formatPersianDate(person.startDate)}</td>
                  <td style={relatedPersonCellStyle}>{formatPersianDate(person.endDate)}</td>
                  <td style={relatedPersonCellStyle}>{person.hasSignatoryAuthority ? 'دارد' : 'ندارد'}</td>
                </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function TraceSection({ trace, loading, error, externalRequests, externalRequestsLoading, externalRequestsError }) {
  const externalRequestMatches = useMemo(
    () => requestsByTraceIndex(trace?.items || [], externalRequests || []),
    [trace, externalRequests]
  )
  return (
    <section style={traceCardStyle}>
      <div style={traceHeaderStyle}>
        <div>
          <p style={traceEyebrowStyle}>ردیابی پرونده</p>
          <h3 style={sectionTitleStyle}>سوابق ارجاع و عملیات برگ اجرا</h3>
        </div>
      </div>
      {loading && <p style={mutedTextStyle}>در حال دریافت سوابق...</p>}
      {!loading && error && <p style={errorTextStyle}>خطا در دریافت سوابق: {error}</p>}
      {!loading && externalRequestsLoading && <p style={traceExternalLoadingStyle}>در حال تطبیق بدنه درخواست‌های بیرونی با سوابق پرونده...</p>}
      {!loading && externalRequestsError && <p style={errorTextStyle}>خطا در دریافت بدنه درخواست‌های بیرونی: {externalRequestsError}</p>}
      {!loading && !error && (!trace?.items || trace.items.length === 0) && (
        <p style={mutedTextStyle}>هنوز سابقه عملیاتی برای این برگ اجرا ثبت نشده است.</p>
      )}
      {!loading && !error && trace?.items?.length > 0 && (
        <div style={timelineStyle}>
          {trace.items.map((item, index) => (
            <div key={`${item.type}-${item.action}-${item.occurredAt}-${index}`} style={timelineItemStyle}>
              <div style={timelineDotStyle} />
              <div style={timelineContentStyle}>
                <div style={timelineTopStyle}>
                  <strong style={timelineTitleStyle}>{item.label || item.action}</strong>
                  <span style={timelineDateStyle}>{formatPersianDateTime(item.occurredAt)}</span>
                </div>
                <div style={timelineMetaStyle}>
                  <span>انجام‌دهنده: {formatValue(item.actorUsername || 'سامانه')}</span>
                </div>
                {externalRequestMatches.has(index) && (
                  <TraceExternalRequest request={externalRequestMatches.get(index)} />
                )}
                {RELATED_PERSON_TRACE_ACTIONS.has(item.action) && item.newValue && (
                  <RelatedPersonsTraceValue value={item.newValue} />
                )}
                {!RELATED_PERSON_TRACE_ACTIONS.has(item.action) && hasTraceChange(item) && (
                  <div style={traceChangeBoxStyle}>
                    <span style={traceChangeFieldStyle}>{traceFieldLabel(item)}</span>
                    <div style={traceChangeValuesStyle}>
                      <div style={traceChangeValueStyle}>
                        <span style={traceChangeLabelStyle}>مقدار قبلی</span>
                        <strong>{formatTraceChangeValue(item.oldValue)}</strong>
                      </div>
                      <div style={traceChangeArrowStyle}>←</div>
                      <div style={traceChangeValueStyle}>
                        <span style={traceChangeLabelStyle}>مقدار جدید</span>
                        <strong>{formatTraceChangeValue(item.newValue)}</strong>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

function RelatedPersonsSection({ data, loading, refreshing, error, onRefresh }) {
  const items = data?.relatedPersons || []
  return (
    <section style={relatedPersonsCardStyle}>
      <div style={relatedPersonsHeaderStyle}>
        <div>
          <p style={traceEyebrowStyle}>سامانه ثبت شرکت‌ها</p>
          <h3 style={{ ...sectionTitleStyle, marginBottom: 4 }}>اشخاص مرتبط مودی حقوقی</h3>
          <span style={mutedMetaStyle}>
            سال عملکرد: {formatValue(data?.debtYear)} · آخرین بروزرسانی: {formatPersianDateTime(data?.lastSyncedAt)}
          </span>
          <RelatedPersonsStatusLegend />
        </div>
        <button
          type="button"
          onClick={onRefresh}
          disabled={refreshing}
          style={{ ...refreshRelatedButtonStyle, ...(refreshing ? printButtonDisabledStyle : {}) }}
        >
          {refreshing ? 'در حال دریافت...' : 'دریافت اطلاعات تازه'}
        </button>
      </div>

      {loading && <p style={mutedTextStyle}>در حال دریافت اشخاص مرتبط...</p>}
      {!loading && error && <p style={errorTextStyle}>خطا در دریافت اشخاص مرتبط: {error}</p>}
      {!loading && !error && items.length === 0 && (
        <p style={mutedTextStyle}>برای سال عملکرد این برگ بدهی، شخص مرتبطی ثبت نشده است.</p>
      )}
      {!loading && !error && items.length > 0 && (
        <div style={relatedPersonsTableScrollStyle}>
          <table style={relatedPersonsTableStyle}>
            <thead>
              <tr>
                <th style={relatedPersonHeaderCellStyle}>وضعیت</th>
                <th style={relatedPersonHeaderCellStyle}>نوع شخص</th>
                <th style={relatedPersonHeaderCellStyle}>نام</th>
                <th style={relatedPersonHeaderCellStyle}>شناسه</th>
                <th style={relatedPersonHeaderCellStyle}>سمت</th>
                <th style={relatedPersonHeaderCellStyle}>نماینده حقیقی شخص حقوقی</th>
                <th style={relatedPersonHeaderCellStyle}>تاریخ شروع</th>
                <th style={relatedPersonHeaderCellStyle}>تاریخ پایان</th>
                <th style={relatedPersonHeaderCellStyle}>حق امضا</th>
              </tr>
            </thead>
            <tbody>
              {items.map(item => {
                const status = relatedPersonStatus(item, data?.debtYear)
                return (
                <tr key={item.relationshipId} style={status.rowStyle}>
                  <td style={relatedPersonCellStyle}>
                    <RelatedPersonStatusBadge person={item} debtYear={data?.debtYear} />
                  </td>
                  <td style={relatedPersonCellStyle}>{item.personType === 'NATURAL' ? 'حقیقی' : 'حقوقی'}</td>
                  <td style={relatedPersonCellStyle}>
                    <strong>{item.personType === 'NATURAL'
                      ? formatValue([item.firstName, item.lastName].filter(Boolean).join(' '))
                      : formatValue(item.companyName)}</strong>
                  </td>
                  <td style={relatedPersonCellStyle}>{formatValue(item.nationalCode || item.fida || item.nationalId)}</td>
                  <td style={relatedPersonCellStyle}>{formatValue(item.postLabel)}</td>
                  <td style={relatedPersonCellStyle}>
                    {item.personType === 'LEGAL'
                      ? formatValue(
                          `${[item.legalRepresentativeFirstName, item.legalRepresentativeLastName].filter(Boolean).join(' ')} (${item.legalRepresentativeNationalCode || item.legalRepresentativeFida || '-'})`
                        )
                      : '-'}
                  </td>
                  <td style={relatedPersonCellStyle}>{formatPersianDate(item.startDate)}</td>
                  <td style={relatedPersonCellStyle}>{formatPersianDate(item.endDate)}</td>
                  <td style={relatedPersonCellStyle}>{item.hasSignatoryAuthority ? 'دارد' : 'ندارد'}</td>
                </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function PrintIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M7 8V4h10v4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M7 17H5a2 2 0 0 1-2-2v-4a3 3 0 0 1 3-3h12a3 3 0 0 1 3 3v4a2 2 0 0 1-2 2h-2" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M7 14h10v6H7z" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M17 11.5h.01" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" />
    </svg>
  )
}

function BackIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M9 6l6 6-6 6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

const SANIM_AMOUNT_LABELS = {
  outstandingDebtAmount: 'مانده بدهی',
  totalTaxAmount: 'مجموع مالیات',
  totalDutyAmount: 'مجموع عوارض',
  totalPenaltyAmount: 'مجموع جرایم',
  totalPaidAmount: 'مجموع پرداختی',
  totalCreditAmount: 'مجموع اعتبار',
  totalRemissionAmount: 'مجموع بخشودگی'
}

const FINANCIAL_DETAIL_LABELS = {
  taxLiabilityDetails: 'تفصیل مالیات',
  taxCreditDetails: 'تفصیل اعتبار',
  totalPenaltiesDetails: 'تفصیل جرایم',
  totalPaymentsDetails: 'تفصیل پرداختی‌ها',
  totalRemissionsDetails: 'تفصیل بخشودگی و معافیت',
  outstandingDebtDetails: 'تفصیل مانده بدهی'
}

const FINALIZATION_LABELS = {
  SELF_DECLARATION: 'خوداظهاری',
  FINAL_NOTICE: 'برگ قطعی'
}

const VALIDATION_LABELS = {
  VALIDATED: 'اعتبارسنجی‌شده',
  VALIDATED_WITH_PROVISIONAL_MAPPING: 'اعتبارسنجی با نگاشت موقت',
  LEGACY_REVIEW_REQUIRED: 'نیازمند بررسی سند قدیمی'
}

function exactSanimMoney(value) {
  if (value == null || value === '') return '—'
  const [integer, fraction] = String(value).split('.')
  return toPersianDigits(integer.replace(/\B(?=(\d{3})+(?!\d))/g, ',') + (fraction == null ? '' : '.' + fraction))
}

function readableEnum(value, labels = {}) {
  if (value == null || value === '') return '—'
  return labels[value] || formatValue(value)
}

function personName(taxpayer, source) {
  if (taxpayer?.type === 'LEGAL') return taxpayer.companyName || source?.taxpayerName
  return [taxpayer?.firstName, taxpayer?.lastName].filter(Boolean).join(' ') || source?.taxpayerName
}

function DetailIcon({ type }) {
  const paths = {
    document: <><path d="M7 3h7l4 4v14H7z"/><path d="M14 3v5h5M10 12h5M10 16h5"/></>,
    person: <><circle cx="12" cy="8" r="4"/><path d="M4.5 21a7.5 7.5 0 0 1 15 0"/></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M8 3v4M16 3v4M3 10h18"/></>,
    office: <><path d="M4 21V7l8-4 8 4v14M9 21v-5h6v5"/><path d="M8 9h.01M12 9h.01M16 9h.01M8 13h.01M12 13h.01M16 13h.01"/></>,
    money: <><rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="12" cy="12" r="3"/><path d="M7 9H6v1M17 15h1v-1"/></>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></>,
    technical: <><path d="M8 9 4 12l4 3M16 9l4 3-4 3M14 6l-4 12"/></>
  }
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[type] || paths.document}</svg>
}

function InfoGroup({ title, subtitle, icon, tone = 'blue', children, wide = false }) {
  return <section className={`epd-card epd-card--${tone} ${wide ? 'epd-card--wide' : ''}`}>
    <header className="epd-card__header">
      <span className="epd-card__icon"><DetailIcon type={icon} /></span>
      <div><h3>{title}</h3>{subtitle && <p>{subtitle}</p>}</div>
    </header>
    <div className="epd-info-grid">{children}</div>
  </section>
}

function InfoItem({ label, value, ltr = false, wide = false, emphasize = false, required = false, missing = false }) {
  return <div className={`epd-info-item ${wide ? 'epd-info-item--wide' : ''} ${emphasize ? 'epd-info-item--emphasize' : ''} ${missing ? 'epd-info-item--missing' : ''}`}>
    <span>{label}{required ? ' (اجباری)' : ''}</span>
    <strong dir={ltr ? 'ltr' : undefined}>{value == null || value === '' ? '—' : value}</strong>
  </div>
}

const SOURCE_FIELD_LABELS = {
  referenceId: 'شناسه مرجع ورودی', nationalId: 'شناسه ملی / هویتی ورودی', followCode: 'کد رهگیری ثبت‌نام',
  secondParameter: 'پارامتر دوم', year: 'سال مالیاتی ارسالی', period: 'دوره مالیاتی ارسالی',
  taxOfficeId: 'کد اداره مالیاتی ارسالی', announcementType: 'نوع اعلامیه', taxAmount: 'مبلغ مالیات ارسالی',
  taxSource: 'منبع مالیاتی', hash: 'هش ورودی', produceDate: 'تاریخ تولید منبع', receivedAt: 'زمان دریافت',
  activityTypeId: 'شناسه نوع فعالیت', activityCode: 'کد نوع فعالیت', mobile: 'شماره همراه',
  registrationTraceCode: 'کد رهگیری ثبت‌نام', financialDetails: 'جزئیات مالی', paperType: 'نوع برگه',
  paperName: 'عنوان برگه', mappingVersion: 'نسخه نگاشت', documentNumber: 'شماره ابلاغ برگه بدهی قطعی',
  issueDateRaw: 'تاریخ صدور برگه بدهی قطعی', basisDocumentNumber: 'شماره سند مبنا',
  basisDocumentDateRaw: 'تاریخ سند مبنا', issuanceReason: 'علت قطعیت', taxYear: 'سال مالیاتی',
  taxPeriod: 'دوره مالیاتی', taxPeriodStartRaw: 'شروع دوره مالیاتی', taxPeriodEndRaw: 'پایان دوره مالیاتی',
  taxpayerIdentifier: 'شناسه مودی (TIN)', taxpayerIdentifierType: 'نوع شناسه مودی', identityNumber: 'شناسه هویتی',
  identityType: 'نوع شناسه هویتی', personType: 'نوع مودی', taxpayerName: 'نام مودی', tradeName: 'نام تجاری',
  activityTitle: 'عنوان فعالیت', phone: 'شماره تلفن', postalCode: 'کد پستی', address: 'اقامتگاه قانونی',
  generalDirectorateName: 'نام اداره کل امور مالیاتی', taxOfficeName: 'نام اداره امور مالیاتی',
  sourceTaxOfficeCode: 'کد اداره در منبع', currencyCode: 'واحد پول', taxAmountBasis: 'مبنای مبلغ مالیات',
  remissionBasis: 'مبنای بخشودگی', validationStatus: 'وضعیت اعتبارسنجی', amounts: 'مبالغ تجمیعی',
  totalTaxAmount: 'جمع مالیات', totalDutyAmount: 'جمع عوارض', outstandingDebtAmount: 'مانده بدهی',
  totalPenaltyAmount: 'جمع جرائم', totalPaidAmount: 'جمع پرداختی', totalCreditAmount: 'جمع اعتبار',
  totalRemissionAmount: 'جمع بخشودگی', type: 'نوع مودی', nationalCode: 'کد ملی', fida: 'کد فیدا',
  firstName: 'نام', lastName: 'نام خانوادگی', fatherName: 'نام پدر', birthDate: 'تاریخ تولد',
  companyName: 'نام شخص حقوقی', registrationNumber: 'شماره ثبت', registrationDate: 'تاریخ ثبت',
  companyNationalId: 'شناسه ملی شخص حقوقی', name: 'نام کامل', email: 'پست الکترونیک',
  isForeign: 'آیا مودی تبعه خارجی است؟', identity: 'کد ملی یا فیدا'
}

const REQUIRED_SOURCE_FIELDS = new Set([
  'sourceEnvelope.referenceId', 'sourceEnvelope.nationalId', 'sourceEnvelope.followCode',
  'sourceEnvelope.year', 'sourceEnvelope.period', 'sourceEnvelope.taxOfficeId',
  'sourceEnvelope.announcementType', 'sourceEnvelope.taxSource',
  'sourceDocument.paperType', 'sourceDocument.documentNumber', 'sourceDocument.issueDateRaw',
  'sourceDocument.registrationTraceCode', 'sourceDocument.taxpayerIdentifier', 'sourceDocument.issuanceReason',
  'sourceDocument.basisDocumentNumber', 'sourceDocument.basisDocumentDateRaw',
  'sourceDocument.taxYear', 'sourceDocument.taxPeriodStartRaw', 'sourceDocument.taxPeriodEndRaw',
  'sourceDocument.taxpayerName', 'sourceDocument.tradeName', 'sourceDocument.activityTitle',
  'sourceDocument.phone', 'sourceDocument.mobile',
  'sourceDocument.postalCode', 'sourceDocument.address', 'sourceDocument.amounts.totalTaxAmount',
  'sourceDocument.amounts.totalDutyAmount', 'sourceDocument.amounts.totalPenaltyAmount',
  'sourceDocument.amounts.totalPaidAmount', 'sourceDocument.amounts.totalCreditAmount',
  'sourceDocument.amounts.totalRemissionAmount', 'sourceDocument.amounts.outstandingDebtAmount',
  'sourceTaxpayer.type', 'sourceTaxpayer.postalCode', 'sourceTaxpayer.address',
  'sourceTaxpayer.identity', 'sourceTaxpayer.fatherName', 'sourceTaxpayer.birthDate',
  'sourceTaxpayer.registrationNumber', 'sourceTaxpayer.registrationDate',
  'sourceTaxpayer.companyName', 'sourceTaxpayer.isForeign'
])

function flattenSourceFields(value, path = '') {
  if (value == null || value === '') return [{ path, value: null }]
  if (Array.isArray(value)) return [{ path, value: JSON.stringify(value) }]
  if (typeof value === 'object') {
    const entries = Object.entries(value).filter(([key]) => key !== 'xml')
    if (!entries.length) return [{ path, value: null }]
    return entries.flatMap(([key, child]) => flattenSourceFields(child, path ? `${path}.${key}` : key))
  }
  return [{ path, value }]
}

function isRequiredSourceField(path, taxpayerType) {
  if (!REQUIRED_SOURCE_FIELDS.has(path)) return false
  if (path === 'sourceDocument.tradeName') return taxpayerType === 'LEGAL'
  if (path.startsWith('sourceTaxpayer.')) {
    const field = path.split('.').at(-1)
    if (['fatherName', 'birthDate', 'isForeign'].includes(field)) return taxpayerType !== 'LEGAL'
    if (['registrationNumber', 'registrationDate', 'companyName'].includes(field)) return taxpayerType !== 'NATURAL'
  }
  return true
}

function SourceInputDetails({ envelope, document, taxpayer }) {
  const groups = [
    { key: 'sourceEnvelope', title: 'اطلاعات پاکت ورودی EF03', value: envelope },
    { key: 'sourceDocument', title: 'اطلاعات استخراج‌شده از سند منبع', value: document },
    { key: 'sourceTaxpayer', title: 'اطلاعات هویتی ارسالی مودی', value: taxpayer }
  ]
  return <InfoGroup title="تمام اطلاعات ورودی منبع" subtitle="فیلدهای موجود در درخواست EF03؛ فیلدهای الزامیِ خالی مشخص شده‌اند" icon="document" tone="indigo" wide>
    {groups.map(group => {
      const rows = flattenSourceFields(group.value, group.key)
      if (group.key === 'sourceTaxpayer') {
        const identity = group.value?.nationalCode || group.value?.fida || group.value?.nationalId
        rows.push({ path: 'sourceTaxpayer.identity', value: identity || null })
      }
      for (const requiredPath of REQUIRED_SOURCE_FIELDS) {
        if (requiredPath.startsWith(`${group.key}.`) && !rows.some(row => row.path === requiredPath)) {
          const field = requiredPath.split('.').at(-1)
          if (group.key === 'sourceTaxpayer' && group.value?.type === 'LEGAL'
              && ['fatherName', 'birthDate', 'isForeign'].includes(field)) continue
          if (group.key === 'sourceTaxpayer' && group.value?.type === 'NATURAL'
              && ['registrationNumber', 'registrationDate', 'companyName'].includes(field)) continue
          rows.push({ path: requiredPath, value: null })
        }
      }
      return <Fragment key={group.key}>
        <h4 className="epd-source-group-title epd-info-item--wide">{group.title}</h4>
        {rows.map(row => {
          const required = isRequiredSourceField(row.path, taxpayer?.type)
          const missing = row.value == null || row.value === ''
          const field = row.path.slice(group.key.length + 1).split('.').at(-1)
          const label = SOURCE_FIELD_LABELS[field] || field
          const value = missing && required ? 'این داده الزامی مفقود است' : formatValue(row.value)
          return <InfoItem key={row.path} label={label} value={value} required={required} missing={missing && required}
            ltr={typeof row.value === 'string' && /^[\w-]+$/.test(row.value)} wide={typeof row.value === 'string' && row.value.length > 72} />
        })}
      </Fragment>
    })}
    {(envelope?.xml || document?.xml) && <details className="epd-xml epd-info-item--wide">
      <summary>مشاهده XML اصلی ورودی</summary><pre dir="ltr">{envelope?.xml || document?.xml}</pre>
    </details>}
  </InfoGroup>
}

function AmountComparison({ sourceAmounts = {}, currentAmounts = {} }) {
  return <section className="epd-card epd-card--wide epd-card--money">
    <header className="epd-card__header epd-card__header--with-note">
      <span className="epd-card__icon"><DetailIcon type="money" /></span>
      <div>
        <h3>خلاصه مبالغ برگه</h3>
        <p>مقایسه مبلغ ثبت‌شده هنگام دریافت EF03 با آخرین مقدار جاری</p>
      </div>
      <span className="epd-unit-badge">مبالغ به ریال</span>
    </header>
    <div className="epd-amount-grid">
      {Object.entries(SANIM_AMOUNT_LABELS).map(([key, label]) => {
        const currentValue = currentAmounts?.[key]
        const initialValue = sourceAmounts?.[key]
        const changed = currentValue != null && initialValue != null && String(currentValue) !== String(initialValue)
        return <article className={`epd-amount ${key === 'outstandingDebtAmount' ? 'epd-amount--primary' : ''}`} key={key}>
          <span>{label}</span>
          <strong dir="ltr">{exactSanimMoney(currentValue)}</strong>
          <small>
            هنگام دریافت: <b dir="ltr">{exactSanimMoney(initialValue)}</b>
            {changed && <em>به‌روزرسانی شده</em>}
          </small>
        </article>
      })}
    </div>
  </section>
}

function parseFinancialRows(value) {
  if (!value) return []
  if (Array.isArray(value)) return value
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return null
  }
}

function FinancialBreakdowns({ details, title, defaultOpen = false }) {
  if (!details) return null
  const groups = Object.entries(FINANCIAL_DETAIL_LABELS)
    .map(([key, label]) => ({ key, label, rows: parseFinancialRows(details[key]) }))
    .filter(group => group.rows === null || group.rows.length > 0)
  if (!groups.length) return null

  return <details className="epd-breakdown" open={defaultOpen}>
    <summary>
      <span>{title}</span>
      <small>{toPersianDigits(groups.length)} گروه اطلاعات تفصیلی</small>
    </summary>
    <div className="epd-breakdown__body">
      {groups.map(group => <section className="epd-breakdown__group" key={group.key}>
        <h4>{group.label}</h4>
        {group.rows === null
          ? <p className="epd-empty">جزئیات این بخش قابل خواندن نیست.</p>
          : <div className="epd-table-scroll"><table className="epd-table">
              <thead><tr><th>ردیف</th><th>شرح</th><th>وضعیت گزارش</th><th>مبلغ (ریال)</th></tr></thead>
              <tbody>{group.rows.map((row, index) => <tr key={index}>
                <td>{toPersianDigits(index + 1)}</td>
                <td>{formatValue(row.description || group.label)}</td>
                <td><span className={row.reported === 'false' || row.reported === false ? 'epd-row-status epd-row-status--muted' : 'epd-row-status'}>{row.reported === 'false' || row.reported === false ? 'در XML گزارش نشده' : 'گزارش شده'}</span></td>
                <td dir="ltr">{exactSanimMoney(row.amount)}</td>
              </tr>)}</tbody>
            </table></div>}
      </section>)}
    </div>
  </details>
}

function StatusPill({ value, label }) {
  const warning = String(value || '').includes('REJECT') || String(value || '').includes('REVIEW')
  return <span className={`epd-status ${warning ? 'epd-status--warning' : ''}`}><i />{label || formatValue(value)}</span>
}

function SanimDocumentSection({ data }) {
  const execution = data.executionPaper || {}
  const finalized = data.finalTaxDebtPaper || {}
  const source = finalized.sourceDocument || {}
  const sourceEnvelope = finalized.sourceEnvelope || {}
  const sourceTaxpayer = finalized.sourceTaxpayer || {}
  const taxpayer = data.taxpayer || {}
  const debt = data.debtInformation || {}
  const notification = data.notification || {}
  const rejection = data.ruleRejection || {}
  const current = debt.currentAmounts || {}
  const sourceAmounts = source?.amounts || {}
  const taxpayerDisplayName = personName(taxpayer, source)
  const identity = taxpayer.nationalCode || taxpayer.nationalId || source?.identityNumber
  const hasNotification = Object.keys(notification).length > 0
  const hasRejection = Object.keys(rejection).length > 0

  return <div className="epd-page" dir="rtl">
    <section className="epd-hero">
      <div className="epd-hero__main">
        <div className="epd-hero__eyebrow"><span>برگه اجرای مالیاتی</span><StatusPill value={execution.currentState} label={execution.currentStateDisplay} /></div>
        <h1>{execution.uniqueCode ? toPersianDigits(execution.uniqueCode) : 'شماره برگه ثبت نشده'}</h1>
        <p>{taxpayerDisplayName || 'نام مؤدی ثبت نشده'} <span>•</span> {readableEnum(taxpayer.type, { NATURAL: 'شخص حقیقی', LEGAL: 'شخص حقوقی' })}</p>
      </div>
      <div className="epd-hero__facts">
        <div><span>تاریخ صدور</span><strong>{formatPersianDateTime(execution.issuanceDate)}</strong></div>
        <div><span>مانده بدهی</span><strong dir="ltr">{exactSanimMoney(current.outstandingDebtAmount)}</strong><small>ریال</small></div>
        <div><span>منبع مالیاتی</span><strong>{execution.taxSourceDisplay || execution.taxSource || '—'}</strong></div>
      </div>
    </section>

    {source.validationStatus === 'LEGACY_REVIEW_REQUIRED' && <div className="epd-alert" role="alert">
      این سند قدیمی برای انطباق با مدل جدید نیازمند بررسی و تکمیل است؛ مبالغ نامشخص صفر فرض نشده‌اند.
    </div>}

    <div className="epd-card-grid">
      <InfoGroup title="مشخصات برگه اجرا" subtitle="شناسه‌ها و وضعیت صدور" icon="document" tone="blue">
        <InfoItem label="شناسه داخلی" value={formatIdentifier(execution.id)} ltr />
        <InfoItem label="شماره برگه اجرا" value={formatValue(execution.uniqueCode)} ltr emphasize />
        <InfoItem label="وضعیت فعلی" value={execution.currentStateDisplay || execution.currentState} />
        <InfoItem label="تاریخ صدور" value={formatPersianDateTime(execution.issuanceDate)} />
        <InfoItem label="مبنای صدور" value={readableEnum(execution.finalizationReason, FINALIZATION_LABELS)} />
        <InfoItem label="تاریخ ثبت در سیستم" value={formatPersianDateTime(execution.createdAt)} />
      </InfoGroup>

      <InfoGroup title="مشخصات مؤدی" subtitle="اطلاعات هویتی و ثبت‌نام مالیاتی" icon="person" tone="green">
        <InfoItem label="نام مؤدی" value={taxpayerDisplayName} wide emphasize />
        <InfoItem label="نوع مؤدی" value={readableEnum(taxpayer.type, { NATURAL: 'حقیقی', LEGAL: 'حقوقی' })} />
        <InfoItem label={taxpayer.type === 'LEGAL' ? 'شناسه ملی' : 'کد ملی'} value={formatValue(identity)} ltr />
        <InfoItem label="شناسه مؤدی (TIN)" value={formatValue(taxpayer.tin || source.taxpayerIdentifier)} ltr />
        <InfoItem label="کد رهگیری ثبت‌نام" value={formatValue(taxpayer.registrationTraceCode || source.registrationTraceCode)} ltr />
        <InfoItem label="نوع فعالیت" value={taxpayer.activityType || source.activityTitle} />
        {taxpayer.type === 'NATURAL' && <InfoItem label="نام پدر" value={taxpayer.fatherName} />}
        {taxpayer.type === 'NATURAL' && <InfoItem label="تاریخ تولد" value={formatPersianDate(taxpayer.birthDate)} />}
        {taxpayer.type === 'LEGAL' && <InfoItem label="شماره ثبت" value={formatValue(taxpayer.registrationNumber)} ltr />}
        {taxpayer.type === 'LEGAL' && <InfoItem label="تاریخ ثبت" value={formatPersianDate(taxpayer.registrationDate)} />}
        <InfoItem label="نام تجاری" value={source.tradeName} />
        <InfoItem label="شماره تماس" value={formatValue(source.phone)} ltr />
        <InfoItem label="کد پستی" value={formatValue(taxpayer.postalCode || source.postalCode)} ltr />
        <InfoItem label="نشانی" value={taxpayer.address || source.address} wide />
      </InfoGroup>

      <InfoGroup title="سند بدهی قطعی و مبنای صدور" subtitle="اطلاعات ثبت‌شده در سند سنیم" icon="document" tone="indigo">
        <InfoItem label="شماره نامه سنیم" value={formatValue(source.documentNumber)} ltr emphasize />
        <InfoItem label="تاریخ نامه سنیم" value={formatValue(source.issueDateRaw)} />
        <InfoItem label="شماره سند مبنا" value={formatValue(source.basisDocumentNumber || finalized.uniqueFinalTaxDebtNoticeId)} ltr />
        <InfoItem label="تاریخ صدور برگ بدهی قطعی" value={formatValue(finalized.finalTaxDebtNoticeIssueDate || source.issueDateRaw)} />
        <InfoItem label="علت قطعیت" value={readableEnum(source.issuanceReason || finalized.finalizationReason, FINALIZATION_LABELS)} />
        <InfoItem label="منبع مالیاتی" value={finalized.taxSourceDisplay || finalized.taxSource} />
      </InfoGroup>

      <InfoGroup title="دوره و حوزه مالیاتی" subtitle="سال، بازه عملکرد و اداره صادرکننده" icon="calendar" tone="amber">
        <InfoItem label="سال مالیاتی" value={formatValue(source.taxYear)} emphasize />
        <InfoItem label="دوره مالیاتی" value={formatValue(source.taxPeriod)} />
        <InfoItem label="از تاریخ" value={formatValue(source.taxPeriodStartRaw)} />
        <InfoItem label="تا تاریخ" value={formatValue(source.taxPeriodEndRaw)} />
        <InfoItem label="اداره کل امور مالیاتی" value={source.generalDirectorateName || finalized.generalDirectorateName || execution.generalDirectorateName} wide />
        <InfoItem label="کد داخلی اداره کل" value={formatValue(finalized.mainTaxBureauId || execution.mainTaxBureauId)} ltr />
        <InfoItem label="اداره امور مالیاتی" value={source.taxOfficeName || finalized.taxAffairsOfficeName || execution.taxAffairsOfficeName} wide />
        <InfoItem label="کد اداره در سنیم" value={formatValue(source.sourceTaxOfficeCode)} ltr />
        <InfoItem label="شناسه داخلی اداره" value={formatValue(finalized.taxBureauId || execution.taxBureauId)} ltr />
        <InfoItem label="تلفن اداره" value={formatValue(finalized.taxAffairsOfficePhone || execution.taxAffairsOfficePhone)} ltr />
        <InfoItem label="کد پستی اداره" value={formatValue(finalized.taxAffairsOfficePostalCode || execution.taxAffairsOfficePostalCode)} ltr />
        <InfoItem label="نشانی اداره" value={finalized.taxAffairsOfficeAddress || execution.taxAffairsOfficeAddress} wide />
        <InfoItem label="نشانی اداره کل" value={finalized.generalDirectorateAddress || execution.generalDirectorateAddress} wide />
      </InfoGroup>

      <AmountComparison sourceAmounts={sourceAmounts} currentAmounts={current} />

      <section className="epd-card epd-card--wide epd-card--details">
        <header className="epd-card__header">
          <span className="epd-card__icon"><DetailIcon type="money" /></span>
          <div><h3>اطلاعات تفصیلی مبالغ</h3><p>ریز اقلام مالی به تفکیک زمان دریافت و وضعیت جاری</p></div>
        </header>
        <div className="epd-breakdown-stack">
          <FinancialBreakdowns details={debt.financialDetails} title="جزئیات جاری بدهی" defaultOpen />
          <FinancialBreakdowns details={source.financialDetails} title="جزئیات ثبت‌شده هنگام دریافت EF03" />
          {!debt.financialDetails && !source.financialDetails && <p className="epd-empty">جزئیات مالی برای این برگه ثبت نشده است.</p>}
        </div>
      </section>

      <InfoGroup title="وضعیت ابلاغ" subtitle="آخرین اطلاعات ابلاغ و پیگیری" icon="bell" tone="violet" wide>
        {hasNotification ? <>
          <InfoItem label="وضعیت ابلاغ" value={notification.informStatusLabel || notification.informStatus} emphasize />
          <InfoItem label="تاریخ ابلاغ الکترونیکی" value={formatPersianDateTime(notification.electronicNotificationDate)} />
          <InfoItem label="زمان رد ابلاغ" value={formatPersianDateTime(notification.informRejectedAt)} />
          <InfoItem label="زمان بررسی مجدد" value={formatPersianDateTime(notification.reInformDate)} />
          <InfoItem label="استعلام بعدی بدهی" value={formatPersianDateTime(notification.nextDebtInquiryDate)} />
          <InfoItem label="علت رد ابلاغ" value={notification.informRejectionReason} wide />
        </> : <p className="epd-empty epd-info-item--wide">هنوز اطلاعات ابلاغی برای این برگه ثبت نشده است.</p>}
      </InfoGroup>

      {hasRejection && <InfoGroup title="اطلاعات رد و کنترل‌ها" subtitle="دلایل ثبت‌شده توسط موتور قوانین" icon="bell" tone="red" wide>
        <InfoItem label="نوع رد صدور" value={rejection.rejectionTypeLabel || rejection.rejectionType} />
        <InfoItem label="شناسه دلیل رد" value={formatValue(rejection.rejectionReasonId)} ltr />
        <InfoItem label="زمان رد" value={formatPersianDateTime(rejection.rejectedAt || rejection.informRejectedAt)} />
        <InfoItem label="شرح رد صدور" value={rejection.rejectionReason} wide />
        <InfoItem label="شرح رد ابلاغ" value={rejection.informRejectionReason} wide />
      </InfoGroup>}

      <section className="epd-card epd-card--wide epd-card--technical">
        <details>
          <summary>
            <span className="epd-card__icon"><DetailIcon type="technical" /></span>
            <div><h3>اطلاعات فنی و داده منبع</h3><p>قالب، نسخه نگاشت، مبانی محاسبه و XML اصلی</p></div>
            <b>مشاهده جزئیات</b>
          </summary>
          <div className="epd-technical__body">
            <div className="epd-info-grid">
              <InfoItem label="نوع برگه" value={source.paperType || 'ورود دستی / مشخص‌نشده'} ltr />
              <InfoItem label="عنوان فارسی برگه" value={source.paperName || '-'} />
              <InfoItem label="نسخه نگاشت" value={source.mappingVersion} ltr />
              <InfoItem label="وضعیت اعتبارسنجی" value={readableEnum(source.validationStatus, VALIDATION_LABELS)} />
              <InfoItem label="واحد پول" value={source.currencyCode} ltr />
              <InfoItem label="مبنای مبلغ مالیات" value={source.taxAmountBasis} ltr />
              <InfoItem label="مبنای بخشودگی" value={source.remissionBasis} ltr />
              <InfoItem label="نوع شناسه هویتی" value={source.identityType} ltr />
              <InfoItem label="نوع شناسه مؤدی" value={source.taxpayerIdentifierType} ltr />
            </div>
            <details className="epd-xml">
              <summary>مشاهده XML اصلی دریافتی</summary>
              <pre dir="ltr">{source.xml || 'XML ذخیره نشده است.'}</pre>
            </details>
          </div>
        </details>
      </section>

      <SourceInputDetails envelope={sourceEnvelope} document={source} taxpayer={sourceTaxpayer} />
    </div>
  </div>
}

function PrintExecutionPaper({ data }) {
  if (!data) return null
  return <div className="execution-print-template"><SanimDocumentSection data={data} /></div>
}

export default function ExecutionPaperDetailsPage() {
  const { paperId } = useParams()
  const router = useRouter()
  const [data, setData] = useState(null)
  const [trace, setTrace] = useState(null)
  const [canViewTrace, setCanViewTrace] = useState(true)
  const [loading, setLoading] = useState(true)
  const [traceLoading, setTraceLoading] = useState(true)
  const [error, setError] = useState(null)
  const [traceError, setTraceError] = useState(null)
  const [relatedPersons, setRelatedPersons] = useState(null)
  const [relatedPersonsLoading, setRelatedPersonsLoading] = useState(false)
  const [relatedPersonsRefreshing, setRelatedPersonsRefreshing] = useState(false)
  const [relatedPersonsError, setRelatedPersonsError] = useState(null)
  const [externalRequests, setExternalRequests] = useState([])
  const [externalRequestsLoading, setExternalRequestsLoading] = useState(true)
  const [externalRequestsError, setExternalRequestsError] = useState(null)

  const loadRelatedPersons = () => {
    setRelatedPersonsLoading(true)
    setRelatedPersonsError(null)
    return apiFetch(`/api/v1/execution-papers/${paperId}/related-persons`)
      .then(async response => {
        if (!response.ok) {
          const details = await response.text()
          throw new Error(details || `HTTP ${response.status}`)
        }
        return response.json()
      })
      .then(json => {
        setRelatedPersons(json)
        setRelatedPersonsLoading(false)
        return json
      })
      .catch(error => {
        setRelatedPersonsError(error.message)
        setRelatedPersonsLoading(false)
        throw error
      })
  }

  useEffect(() => {
    setLoading(true)
    setTraceLoading(true)
    setError(null)
    setTraceError(null)
    setCanViewTrace(true)
    setExternalRequestsLoading(true)
    setExternalRequestsError(null)
    apiFetch(`/api/v1/execution-papers/${paperId}`)
      .then(r => {
        if (!r.ok) {
          if (r.status === 401) throw new Error('نشست شما معتبر نیست. دوباره وارد شوید.')
          if (r.status === 403) throw new Error('شما مجوز مشاهده جزئیات برگ اجرا را ندارید.')
          throw new Error(`HTTP ${r.status}`)
        }
        return r.json()
      })
      .then(json => {
        setData(json)
        setLoading(false)
        if (json?.taxpayer?.type === 'LEGAL') {
          loadRelatedPersons().catch(() => {})
        } else {
          setRelatedPersons(null)
          setRelatedPersonsLoading(false)
        }
      })
      .catch(err => { setError(err.message); setLoading(false) })

    apiFetch(`/api/v1/execution-papers/${paperId}/trace`)
      .then(async r => {
        if (r.status === 403 || r.status === 401) {
          setCanViewTrace(false)
          setTraceLoading(false)
          return null
        }
        if (!r.ok) {
          throw new Error(`HTTP ${r.status}`)
        }
        return r.json()
      })
      .then(json => {
        if (json) {
          setTrace(json)
          setTraceLoading(false)
        }
      })
      .catch(err => { setTraceError(err.message); setTraceLoading(false) })

    apiFetch(`/api/v1/execution-papers/${paperId}/external-requests`)
      .then(async response => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`)
        return response.json()
      })
      .then(json => {
        setExternalRequests(Array.isArray(json) ? json : [])
        setExternalRequestsLoading(false)
      })
      .catch(error => {
        setExternalRequestsError(error.message)
        setExternalRequestsLoading(false)
      })
  }, [paperId])

  const refreshRelatedPersons = () => {
    setRelatedPersonsRefreshing(true)
    setRelatedPersonsError(null)
    apiFetch(`/api/v1/execution-papers/${paperId}/related-persons/refresh`, { method: 'POST' })
      .then(async response => {
        if (!response.ok) {
          const details = await response.text()
          throw new Error(details || `HTTP ${response.status}`)
        }
        return response.json()
      })
      .then(json => {
        setRelatedPersons(json)
        setRelatedPersonsRefreshing(false)
        return apiFetch(`/api/v1/execution-papers/${paperId}/trace`)
      })
      .then(response => response.ok ? response.json() : null)
      .then(json => { if (json) setTrace(json) })
      .catch(error => {
        setRelatedPersonsError(error.message)
        setRelatedPersonsRefreshing(false)
      })
  }

  const printExecutionPaper = () => {
    document.body.classList.add('execution-printing')
    const cleanup = () => {
      document.body.classList.remove('execution-printing')
      window.removeEventListener('afterprint', cleanup)
    }
    window.addEventListener('afterprint', cleanup)
    window.setTimeout(() => {
      window.print()
      window.setTimeout(cleanup, 500)
    }, 50)
  }

  return (
    <>
      <AppShell
        active="execution-papers"
        title="دستیار هوشمند حسابرسی"
        eyebrow="برگ‌های اجرا"
      >
        <main className="epd-shell">
          <div style={pageTitleRowStyle}>
            <h2 style={pageTitleStyle}>مشاهده جزئیات برگ اجرا</h2>
            <div style={pageActionGroupStyle}>
              <button type="button" onClick={() => router.push('/execution-papers')} style={backButtonStyle}>
                <BackIcon />
                <span>بازگشت به لیست</span>
              </button>
              <button
                type="button"
                onClick={printExecutionPaper}
                disabled={loading || error || !data}
                style={{
                  ...printButtonStyle,
                  ...((loading || error || !data) ? printButtonDisabledStyle : {})
                }}
              >
                <PrintIcon />
                <span>چاپ برگ اجرا</span>
              </button>
            </div>
          </div>
          {loading && <div style={cardStyle}>در حال دریافت اطلاعات...</div>}
          {!loading && error && <div style={{ ...cardStyle, color: '#dc2626' }}>خطا در دریافت اطلاعات: {error}</div>}
          {!loading && !error && data && <SanimDocumentSection data={data} />}
          {!loading && !error && data?.taxpayer?.type === 'LEGAL' && (
            <RelatedPersonsSection
              data={relatedPersons}
              loading={relatedPersonsLoading}
              refreshing={relatedPersonsRefreshing}
              error={relatedPersonsError}
              onRefresh={refreshRelatedPersons}
            />
          )}
          {!loading && !error && canViewTrace && (
            <TraceSection
              trace={trace}
              loading={traceLoading}
              error={traceError}
              externalRequests={externalRequests}
              externalRequestsLoading={externalRequestsLoading}
              externalRequestsError={externalRequestsError}
            />
          )}
        </main>
      </AppShell>
      {!loading && !error && data && <PrintExecutionPaper data={data} />}
    </>
  )
}

function shortDate(value) {
  if (!value) return ''
  const raw = String(value)
  if (/^\d{4}-\d{2}-\d{2}/.test(raw)) {
    const [year, month, day] = raw.slice(0, 10).split('-')
    return `${year}/${month}/${day}`
  }
  return raw
}

function jalaliDate(value) {
  if (!value) return ''
  const raw = String(value)
  if (/^1[34]\d{2}\/\d{1,2}\/\d{1,2}$/.test(raw)) {
    return normalizeDateSlash(raw)
  }
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) {
    return normalizeDateSlash(raw)
  }
  return new Intl.DateTimeFormat('fa-IR-u-ca-persian', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(date).replace(/\u200e/g, '')
}

function formatPersianDate(value) {
  if (!value) return '—'
  const raw = String(value).trim()
  if (/^1[34]\d{2}[/-]\d{1,2}[/-]\d{1,2}$/.test(raw)) {
    return toPersianDigits(normalizeDateSlash(raw))
  }
  const date = parseDateValue(raw)
  if (!date) return formatValue(raw)
  return cleanPersianDateTime(new Intl.DateTimeFormat('fa-IR-u-ca-persian', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(date))
}

function formatPersianDateTime(value) {
  if (!value) return '—'
  const raw = String(value).trim()
  if (/^1[34]\d{2}[/-]\d{1,2}[/-]\d{1,2}$/.test(raw)) {
    return toPersianDigits(normalizeDateSlash(raw))
  }
  const date = parseDateValue(raw)
  if (!date) return formatValue(raw)
  return cleanPersianDateTime(new Intl.DateTimeFormat('fa-IR-u-ca-persian', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date))
}

function parseDateValue(value) {
  const raw = String(value).trim()
  const dateOnly = raw.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})$/)
  if (dateOnly) {
    if (Number(dateOnly[1]) < 1700) return null
    return new Date(Number(dateOnly[1]), Number(dateOnly[2]) - 1, Number(dateOnly[3]))
  }
  const normalized = raw.includes(' ') && !raw.includes('T')
    ? raw.replace(' ', 'T')
    : raw
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

function cleanPersianDateTime(value) {
  return toPersianDigits(String(value).replace(/\u200e/g, '').replace(',', ' -'))
}

function addDaysJalali(value, days) {
  const date = value ? new Date(value) : new Date()
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  date.setDate(date.getDate() + days)
  return jalaliDate(date.toISOString())
}

function normalizeDateSlash(value) {
  return String(value).replaceAll('-', '/')
}

function sumValues(values) {
  return values.reduce((sum, value) => {
    const number = Number(value)
    return Number.isNaN(number) ? sum : sum + number
  }, 0)
}

function numberToPersianWords(value) {
  const number = Math.round(Number(value || 0))
  if (number === 0) return 'صفر'
  const groups = ['', 'هزار', 'میلیون', 'میلیارد', 'هزار میلیارد']
  const parts = []
  let remaining = number
  let index = 0
  while (remaining > 0 && index < groups.length) {
    const chunk = remaining % 1000
    if (chunk) {
      parts.unshift(`${threeDigitToWords(chunk)}${groups[index] ? ` ${groups[index]}` : ''}`)
    }
    remaining = Math.floor(remaining / 1000)
    index += 1
  }
  return parts.join(' و ')
}

function threeDigitToWords(number) {
  const ones = ['', 'یک', 'دو', 'سه', 'چهار', 'پنج', 'شش', 'هفت', 'هشت', 'نه']
  const teens = ['ده', 'یازده', 'دوازده', 'سیزده', 'چهارده', 'پانزده', 'شانزده', 'هفده', 'هجده', 'نوزده']
  const tens = ['', '', 'بیست', 'سی', 'چهل', 'پنجاه', 'شصت', 'هفتاد', 'هشتاد', 'نود']
  const hundreds = ['', 'یکصد', 'دویست', 'سیصد', 'چهارصد', 'پانصد', 'ششصد', 'هفتصد', 'هشتصد', 'نهصد']
  const parts = []
  const h = Math.floor(number / 100)
  const rest = number % 100
  if (h) parts.push(hundreds[h])
  if (rest >= 10 && rest < 20) {
    parts.push(teens[rest - 10])
  } else {
    const t = Math.floor(rest / 10)
    const o = rest % 10
    if (t) parts.push(tens[t])
    if (o) parts.push(ones[o])
  }
  return parts.join(' و ')
}

function formatDateTime(value) {
  if (!value) return '—'
  try {
    return new Intl.DateTimeFormat('fa-IR-u-ca-persian', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(value))
  } catch {
    return formatValue(value)
  }
}

function formatAuditValue(value) {
  if (!value) return '—'
  const cleaned = String(value)
    .replace(/taskKey=\d+;\s*/g, '')
    .replace('value=', '')
    .trim()

  if (/^\d{4}-\d{2}-\d{2}(?:$|[T\s])/.test(cleaned)) {
    return formatPersianDateTime(cleaned)
  }

  return formatValue(cleaned)
}

const cardStyle = { background: 'white', border: '1px solid #e5e7eb', borderRadius: 12, boxShadow: '0 1px 4px rgba(0,0,0,0.06)', padding: 20, marginBottom: 18 }
const pageTitleRowStyle = { display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap', margin: '16px 0 24px' }
const pageTitleStyle = { color: '#1e3a5f', margin: 0, fontSize: 24 }
const pageActionGroupStyle = { display: 'inline-flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }
const sectionTitleStyle = { margin: '0 0 16px', color: '#1f2937', fontSize: 18 }
const gridStyle = { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }
const itemStyle = { border: '1px solid #eef2ff', borderRadius: 10, background: '#f8fafc', padding: 12 }
const labelStyle = { display: 'block', color: '#6b7280', fontSize: 12, marginBottom: 6 }
const valueStyle = { color: '#111827', fontSize: 14, wordBreak: 'break-word' }
const backButtonStyle = {
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: 8,
  minHeight: 40,
  padding: '9px 14px',
  borderRadius: 8,
  border: '1px solid #d6dde8',
  background: '#ffffff',
  color: '#334155',
  cursor: 'pointer',
  fontSize: 14,
  fontWeight: 800,
  boxShadow: '0 1px 2px rgba(15, 23, 42, 0.04)'
}
const printButtonStyle = {
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: 8,
  minHeight: 40,
  padding: '9px 16px',
  borderRadius: 8,
  border: '1px solid #b6c9e4',
  background: '#f8fbff',
  color: '#14365d',
  cursor: 'pointer',
  fontSize: 14,
  fontWeight: 800,
  boxShadow: '0 1px 2px rgba(15, 23, 42, 0.05)'
}
const printButtonDisabledStyle = { opacity: 0.55, cursor: 'not-allowed' }
const traceCardStyle = { ...cardStyle, borderColor: '#dbeafe', background: 'linear-gradient(180deg, #ffffff, #f8fbff)' }
const traceHeaderStyle = { display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, marginBottom: 16 }
const traceEyebrowStyle = { margin: 0, color: '#15803d', fontSize: 12, fontWeight: 800 }
const mutedTextStyle = { color: '#64748b', margin: 0, lineHeight: 1.9 }
const errorTextStyle = { color: '#dc2626', margin: 0, lineHeight: 1.9 }
const timelineStyle = { display: 'grid', gap: 14, borderRight: '2px solid #dbeafe', paddingRight: 18 }
const timelineItemStyle = { position: 'relative', display: 'grid', gap: 8 }
const timelineDotStyle = { position: 'absolute', right: -24, top: 10, width: 10, height: 10, borderRadius: 999, background: '#15803d', boxShadow: '0 0 0 4px #dcfce7' }
const timelineContentStyle = { border: '1px solid #e5e7eb', borderRadius: 10, background: '#ffffff', padding: 14 }
const timelineTopStyle = { display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center', marginBottom: 10 }
const timelineTitleStyle = { color: '#14365d', fontSize: 14 }
const timelineDateStyle = { color: '#64748b', fontSize: 12, whiteSpace: 'nowrap' }
const timelineMetaStyle = { display: 'flex', flexWrap: 'wrap', gap: 8, color: '#475569', fontSize: 12, lineHeight: 1.8 }
const traceChangeBoxStyle = {
  display: 'grid',
  gap: 8,
  marginTop: 12,
  padding: 10,
  border: '1px solid #dbeafe',
  borderRadius: 8,
  background: '#f8fbff'
}
const traceChangeFieldStyle = { color: '#1e3a8a', fontSize: 12, fontWeight: 900 }
const traceChangeValuesStyle = {
  display: 'grid',
  gridTemplateColumns: 'minmax(0, 1fr) auto minmax(0, 1fr)',
  alignItems: 'stretch',
  gap: 8
}
const traceChangeValueStyle = {
  display: 'grid',
  gap: 4,
  minWidth: 0,
  padding: '8px 10px',
  border: '1px solid #e2e8f0',
  borderRadius: 7,
  background: '#ffffff',
  color: '#0f172a',
  fontSize: 12,
  lineHeight: 1.8,
  overflowWrap: 'anywhere'
}
const traceChangeLabelStyle = { color: '#64748b', fontSize: 11, fontWeight: 800 }
const traceChangeArrowStyle = {
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  color: '#2563eb',
  fontWeight: 900
}
const traceFallbackValueStyle = {
  color: '#0f172a',
  fontSize: 12,
  lineHeight: 1.9,
  overflowWrap: 'anywhere'
}
const relatedPersonsTraceStyle = {
  display: 'grid',
  gap: 12,
  marginTop: 12,
  padding: 12,
  border: '1px solid #bbf7d0',
  borderRadius: 10,
  background: '#f7fff9'
}
const relatedPersonsTraceHeadingStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: 10,
  flexWrap: 'wrap'
}
const relatedPersonsTraceHeadingMetaStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'flex-end',
  gap: 8,
  flexWrap: 'wrap'
}
const relatedPersonsCountStyle = {
  padding: '3px 9px',
  borderRadius: 999,
  color: '#166534',
  background: '#dcfce7',
  fontSize: 11,
  fontWeight: 900
}
const relatedPersonsTraceMetaStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(155px, 1fr))',
  gap: 8
}
const relatedPersonsTraceMetaItemStyle = {
  display: 'grid',
  gap: 4,
  minWidth: 0,
  padding: '8px 10px',
  border: '1px solid #dcfce7',
  borderRadius: 8,
  background: '#ffffff',
  color: '#0f172a',
  fontSize: 12,
  overflowWrap: 'anywhere'
}
const relatedPersonsEmptyTraceStyle = {
  margin: 0,
  padding: 10,
  borderRadius: 8,
  color: '#64748b',
  background: '#ffffff',
  fontSize: 12
}
const relatedPersonsTraceTableScrollStyle = {
  overflowX: 'auto',
  border: '1px solid #dcfce7',
  borderRadius: 9
}
const relatedPersonsTraceTableStyle = {
  width: '100%',
  minWidth: 1000,
  borderCollapse: 'collapse',
  background: '#ffffff'
}
const relatedPersonsCardStyle = { ...cardStyle, borderColor: '#bbf7d0', background: 'linear-gradient(180deg, #ffffff, #f7fff9)' }
const traceExternalRequestStyle = { marginTop: 12, border: '1px solid #c7d2fe', borderRadius: 10, background: '#f8faff', overflow: 'hidden' }
const traceExternalRequestHeaderStyle = { display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap', padding: '12px 13px 9px' }
const traceExternalLoadingStyle = { margin: '0 0 12px', padding: '8px 10px', borderRadius: 8, color: '#475569', background: '#f1f5f9', fontSize: 11 }
const externalRequestIdentityStyle = { display: 'flex', alignItems: 'center', gap: 11, flexWrap: 'wrap' }
const externalSystemBadgeStyle = { display: 'inline-flex', alignItems: 'center', justifyContent: 'center', minHeight: 32, padding: '5px 10px', border: '1px solid #a5b4fc', borderRadius: 8, color: '#3730a3', background: '#eef2ff', fontSize: 12, fontWeight: 900 }
const externalRequestTitleStyle = { display: 'block', color: '#172554', fontSize: 14, marginBottom: 4 }
const externalRequestSentStyle = { display: 'block', color: '#64748b', fontSize: 11 }
const externalStatusBadgeStyle = { display: 'inline-flex', alignItems: 'center', padding: '5px 10px', border: '1px solid transparent', borderRadius: 999, fontSize: 11, fontWeight: 900 }
const externalStatusPendingStyle = { color: '#92400e', background: '#fef3c7', borderColor: '#fcd34d' }
const externalStatusDoneStyle = { color: '#166534', background: '#dcfce7', borderColor: '#86efac' }
const externalStatusFailedStyle = { color: '#991b1b', background: '#fee2e2', borderColor: '#fca5a5' }
const externalRequestMetaStyle = { display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap', padding: '0 16px 12px', color: '#475569', fontSize: 11 }
const externalRequestBodyStyle = { borderTop: '1px solid #e2e8f0', background: '#f8fafc' }
const externalRequestBodySummaryStyle = { padding: '11px 16px', color: '#3730a3', cursor: 'pointer', fontSize: 12, fontWeight: 900, userSelect: 'none' }
const externalRequestBodyContentStyle = { padding: '2px 16px 16px' }
const externalPayloadObjectStyle = { display: 'grid', gap: 12 }
const externalPayloadGridStyle = { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(185px, 1fr))', gap: 8 }
const externalPayloadFieldStyle = { display: 'grid', alignContent: 'start', gap: 5, minWidth: 0, padding: '9px 10px', border: '1px solid #e2e8f0', borderRadius: 8, background: '#ffffff' }
const externalPayloadLabelStyle = { color: '#64748b', fontSize: 11, fontWeight: 800 }
const externalPayloadValueStyle = { color: '#0f172a', fontSize: 12, lineHeight: 1.8, overflowWrap: 'anywhere', whiteSpace: 'pre-wrap' }
const externalPayloadGroupStyle = { display: 'grid', gap: 9, padding: 10, border: '1px solid #dbeafe', borderRadius: 9, background: '#f8fbff' }
const externalPayloadGroupTitleStyle = { margin: 0, color: '#1e3a8a', fontSize: 12, fontWeight: 900 }
const externalPayloadListStyle = { display: 'grid', gap: 8 }
const externalPayloadListItemStyle = { display: 'grid', gap: 7, padding: 9, border: '1px solid #e2e8f0', borderRadius: 8, background: '#ffffff' }
const externalPayloadIndexStyle = { color: '#6366f1', fontSize: 10, fontWeight: 900 }
const externalPayloadEmptyStyle = { margin: 0, padding: 10, color: '#64748b', background: '#ffffff', borderRadius: 8, fontSize: 12 }
const externalPayloadTableScrollStyle = { overflowX: 'auto', border: '1px solid #dbeafe', borderRadius: 8 }
const externalPayloadTableStyle = { width: '100%', minWidth: 980, borderCollapse: 'collapse', background: '#ffffff' }
const externalPayloadHeaderCellStyle = { padding: '9px 10px', color: '#1e3a8a', background: '#eff6ff', borderBottom: '1px solid #bfdbfe', fontSize: 11, whiteSpace: 'nowrap', textAlign: 'center' }
const externalPayloadCellStyle = { padding: '9px 10px', color: '#334155', borderBottom: '1px solid #e2e8f0', fontSize: 11, lineHeight: 1.7, textAlign: 'center', whiteSpace: 'nowrap' }
const relatedPersonsHeaderStyle = { display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 14, flexWrap: 'wrap', marginBottom: 16 }
const mutedMetaStyle = { color: '#64748b', fontSize: 12, lineHeight: 1.8 }
const refreshRelatedButtonStyle = {
  minHeight: 38,
  padding: '7px 13px',
  border: '1px solid #86efac',
  borderRadius: 8,
  background: '#15803d',
  color: '#ffffff',
  cursor: 'pointer',
  fontSize: 12,
  fontWeight: 900
}
const relatedPersonsTableScrollStyle = { overflowX: 'auto', border: '1px solid #dcfce7', borderRadius: 9 }
const relatedPersonsTableStyle = { width: '100%', minWidth: 1050, borderCollapse: 'collapse', background: '#ffffff' }
const relatedPersonHeaderCellStyle = { padding: '10px 12px', color: '#166534', background: '#f0fdf4', borderBottom: '1px solid #bbf7d0', fontSize: 12, whiteSpace: 'nowrap', textAlign: 'center' }
const relatedPersonCellStyle = { padding: '11px 12px', color: '#334155', borderBottom: '1px solid rgba(203, 213, 225, 0.55)', fontSize: 12, lineHeight: 1.7, textAlign: 'center', whiteSpace: 'nowrap' }
const relatedPersonsLegendStyle = { display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap', marginTop: 7 }
const relatedPersonStatusBadgeStyle = {
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  minWidth: 88,
  padding: '4px 9px',
  border: '1px solid transparent',
  borderRadius: 999,
  fontSize: 11,
  fontWeight: 900,
  lineHeight: 1.5,
  whiteSpace: 'nowrap'
}
const debtYearRelatedPersonBadgeStyle = { color: '#166534', background: '#dcfce7', borderColor: '#86efac' }
const expiredRelatedPersonBadgeStyle = { color: '#991b1b', background: '#fee2e2', borderColor: '#fca5a5' }
const otherRelatedPersonBadgeStyle = { color: '#475569', background: '#f1f5f9', borderColor: '#cbd5e1' }
const debtYearRelatedPersonRowStyle = { background: '#f7fff9', boxShadow: 'inset -4px 0 #22c55e' }
const expiredRelatedPersonRowStyle = { background: '#fff7f7', boxShadow: 'inset -4px 0 #ef4444' }
const otherRelatedPersonRowStyle = { background: '#f8fafc', boxShadow: 'inset -4px 0 #94a3b8' }
