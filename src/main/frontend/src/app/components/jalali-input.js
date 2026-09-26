'use client'

import { useEffect, useMemo, useState } from 'react'

const persianDigits = '۰۱۲۳۴۵۶۷۸۹'
const arabicDigits = '٠١٢٣٤٥٦٧٨٩'
const monthNames = ['فروردین', 'اردیبهشت', 'خرداد', 'تیر', 'مرداد', 'شهریور', 'مهر', 'آبان', 'آذر', 'دی', 'بهمن', 'اسفند']
const weekDays = ['ش', 'ی', 'د', 'س', 'چ', 'پ', 'ج']

export default function JalaliInput({
  value,
  onChangeValue,
  required = false,
  mode = 'date',
  style,
  className = '',
  placeholder,
  name
}) {
  const formattedValue = useMemo(() => mode === 'datetime'
    ? formatIsoDateTimeToJalali(value)
    : formatIsoDateToJalali(value), [mode, value])
  const [draft, setDraft] = useState(formattedValue)
  const [focused, setFocused] = useState(false)
  const [open, setOpen] = useState(false)
  const selectedParts = useMemo(() => parseSelectedJalali(value), [value])
  const todayParts = useMemo(() => {
    const now = new Date()
    const [year, month, day] = toJalali(now.getFullYear(), now.getMonth() + 1, now.getDate())
    return { year, month, day, hour: now.getHours(), minute: now.getMinutes() }
  }, [])
  const [view, setView] = useState(() => selectedParts || todayParts)
  const [time, setTime] = useState(() => ({
    hour: selectedParts?.hour ?? todayParts.hour,
    minute: selectedParts?.minute ?? todayParts.minute
  }))

  useEffect(() => {
    if (!focused) {
      setDraft(formattedValue)
    }
  }, [focused, formattedValue])

  useEffect(() => {
    if (selectedParts) {
      setView(selectedParts)
      setTime({
        hour: selectedParts.hour ?? todayParts.hour,
        minute: selectedParts.minute ?? todayParts.minute
      })
    }
  }, [selectedParts, todayParts.hour, todayParts.minute])

  const handleChange = event => {
    const raw = event.target.value
    setDraft(raw)
    if (raw.trim() === '') {
      onChangeValue('')
      return
    }
    const nextValue = mode === 'datetime' ? parseStrictJalaliDateTime(raw) : parseStrictJalaliDate(raw)
    if (nextValue) {
      onChangeValue(nextValue)
    }
  }

  const handleBlur = () => {
    setFocused(false)
    const raw = draft.trim()
    if (raw === '') {
      onChangeValue('')
      setDraft('')
      return
    }
    const nextValue = mode === 'datetime' ? parseJalaliDateTime(raw) : parseJalaliDate(raw)
    if (nextValue) {
      onChangeValue(nextValue)
      setDraft(mode === 'datetime' ? formatIsoDateTimeToJalali(nextValue) : formatIsoDateToJalali(nextValue))
    }
  }

  return (
    <span className="jalali-picker">
      <input
        name={name}
        className={`jalali-input ${className}`.trim()}
        value={focused ? draft : formattedValue}
        onChange={handleChange}
        onFocus={() => {
          setFocused(true)
          setDraft(formattedValue)
        }}
        onBlur={handleBlur}
        required={required}
        style={style}
        dir="ltr"
        inputMode="numeric"
        placeholder={placeholder || (mode === 'datetime' ? '۱۴۰۵/۰۴/۲۴ ۱۱:۳۷' : '۱۴۰۵/۰۴/۲۴')}
      />
      <button
        type="button"
        className="jalali-picker__button"
        onClick={() => {
          setOpen(current => !current)
          setView(selectedParts || todayParts)
        }}
        aria-label="انتخاب تاریخ شمسی"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M7 3v3M17 3v3M4 9h16M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z" />
        </svg>
      </button>
      {open && (
        <JalaliCalendar
          mode={mode}
          view={view}
          selected={selectedParts}
          time={time}
          onViewChange={setView}
          onTimeChange={setTime}
          onSelect={(day) => {
            const hour = mode === 'datetime' ? time.hour : null
            const minute = mode === 'datetime' ? time.minute : null
            const next = commitJalaliValue({ year: view.year, month: view.month, day, hour, minute }, mode)
            onChangeValue(next)
            setDraft(mode === 'datetime' ? formatIsoDateTimeToJalali(next) : formatIsoDateToJalali(next))
            if (mode === 'date') setOpen(false)
          }}
          onApply={() => {
            const selected = parseSelectedJalali(value) || { ...view, day: 1 }
            const next = commitJalaliValue({ ...selected, hour: time.hour, minute: time.minute }, mode)
            onChangeValue(next)
            setDraft(formatIsoDateTimeToJalali(next))
            setOpen(false)
          }}
          onClose={() => setOpen(false)}
        />
      )}
    </span>
  )
}

function JalaliCalendar({ mode, view, selected, time, onViewChange, onTimeChange, onSelect, onApply, onClose }) {
  const days = buildMonthDays(view.year, view.month)
  return (
    <div className="jalali-picker__popover" dir="rtl">
      <div className="jalali-picker__header">
        <button type="button" onClick={() => onViewChange(previousMonth(view))}>‹</button>
        <strong>{monthNames[view.month - 1]} {toPersianDigits(view.year)}</strong>
        <button type="button" onClick={() => onViewChange(nextMonth(view))}>›</button>
      </div>
      <div className="jalali-picker__weekdays">
        {weekDays.map(day => <span key={day}>{day}</span>)}
      </div>
      <div className="jalali-picker__days">
        {days.map((item, index) => item ? (
          <button
            type="button"
            key={item}
            className={selected?.year === view.year && selected?.month === view.month && selected?.day === item ? 'is-selected' : ''}
            onClick={() => onSelect(item)}
          >
            {toPersianDigits(item)}
          </button>
        ) : <span key={`empty-${index}`} />)}
      </div>
      {mode === 'datetime' && (
        <div className="jalali-picker__time">
          <label>
            <span>ساعت</span>
            <input value={pad(time.hour)} onChange={event => onTimeChange(current => ({ ...current, hour: clampTime(event.target.value, 23) }))} inputMode="numeric" />
          </label>
          <label>
            <span>دقیقه</span>
            <input value={pad(time.minute)} onChange={event => onTimeChange(current => ({ ...current, minute: clampTime(event.target.value, 59) }))} inputMode="numeric" />
          </label>
        </div>
      )}
      <div className="jalali-picker__footer">
        <button type="button" onClick={onClose}>بستن</button>
        {mode === 'datetime' && <button type="button" onClick={onApply}>ثبت زمان</button>}
      </div>
    </div>
  )
}

export function formatIsoDateToJalali(value) {
  const parts = parseIsoDateParts(value)
  if (!parts) return normalizeDigits(value || '')
  const [jy, jm, jd] = toJalali(parts.year, parts.month, parts.day)
  return toPersianDigits(`${jy}/${pad(jm)}/${pad(jd)}`)
}

export function formatIsoDateTimeToJalali(value) {
  const parts = parseIsoDateParts(value)
  if (!parts) return normalizeDigits(value || '')
  const [jy, jm, jd] = toJalali(parts.year, parts.month, parts.day)
  const time = parts.hour != null ? ` ${pad(parts.hour)}:${pad(parts.minute || 0)}` : ''
  return toPersianDigits(`${jy}/${pad(jm)}/${pad(jd)}${time}`)
}

export function parseJalaliDate(value) {
  const parts = parseJalaliInput(value)
  if (!parts) return null
  const [gy, gm, gd] = toGregorian(parts.year, parts.month, parts.day)
  return `${gy}-${pad(gm)}-${pad(gd)}`
}

export function parseJalaliDateTime(value) {
  const parts = parseJalaliInput(value)
  if (!parts) return null
  const [gy, gm, gd] = toGregorian(parts.year, parts.month, parts.day)
  return `${gy}-${pad(gm)}-${pad(gd)}T${pad(parts.hour ?? 0)}:${pad(parts.minute ?? 0)}`
}

function parseStrictJalaliDate(value) {
  const normalized = normalizeDigits(value).replaceAll('-', '/').trim()
  if (!/^\d{4}\/\d{2}\/\d{2}$/.test(normalized)) return null
  return parseJalaliDate(normalized)
}

function parseStrictJalaliDateTime(value) {
  const normalized = normalizeDigits(value).replaceAll('-', '/').trim()
  if (!/^\d{4}\/\d{2}\/\d{2}\s+\d{2}:\d{2}$/.test(normalized)) return null
  return parseJalaliDateTime(normalized)
}

function parseIsoDateParts(value) {
  if (!value) return null
  const match = String(value).trim().match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})(?:[T\s](\d{1,2}):(\d{1,2}))?/)
  if (!match) return null
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  if (!year || year < 1700 || !month || !day) return null
  return {
    year,
    month,
    day,
    hour: match[4] == null ? null : Number(match[4]),
    minute: match[5] == null ? null : Number(match[5])
  }
}

function parseSelectedJalali(value) {
  const parts = parseIsoDateParts(value)
  if (!parts) return null
  const [year, month, day] = toJalali(parts.year, parts.month, parts.day)
  return {
    year,
    month,
    day,
    hour: parts.hour,
    minute: parts.minute
  }
}

function commitJalaliValue(parts, mode) {
  const [gy, gm, gd] = toGregorian(parts.year, parts.month, parts.day)
  if (mode === 'datetime') {
    return `${gy}-${pad(gm)}-${pad(gd)}T${pad(parts.hour ?? 0)}:${pad(parts.minute ?? 0)}`
  }
  return `${gy}-${pad(gm)}-${pad(gd)}`
}

function buildMonthDays(year, month) {
  const daysInMonth = jalaliMonthLength(year, month)
  const [gy, gm, gd] = toGregorian(year, month, 1)
  const firstDay = new Date(gy, gm - 1, gd).getDay()
  const offset = (firstDay + 1) % 7
  return [
    ...Array.from({ length: offset }, () => null),
    ...Array.from({ length: daysInMonth }, (_, index) => index + 1)
  ]
}

function jalaliMonthLength(year, month) {
  if (month <= 6) return 31
  if (month <= 11) return 30
  return isJalaliLeapYear(year) ? 30 : 29
}

function isJalaliLeapYear(year) {
  const breaks = [-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178]
  let jp = breaks[0]
  let jump = 0
  for (let i = 1; i < breaks.length; i++) {
    const jm = breaks[i]
    jump = jm - jp
    if (year < jm) break
    jp = jm
  }
  let n = year - jp
  if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
  let leap = (((n + 1) % 33) - 1) % 4
  if (leap === -1) leap = 4
  return leap === 0
}

function previousMonth(view) {
  if (view.month === 1) return { ...view, year: view.year - 1, month: 12 }
  return { ...view, month: view.month - 1 }
}

function nextMonth(view) {
  if (view.month === 12) return { ...view, year: view.year + 1, month: 1 }
  return { ...view, month: view.month + 1 }
}

function clampTime(value, max) {
  const normalized = normalizeDigits(value).replace(/\D/g, '')
  if (normalized === '') return 0
  return Math.max(0, Math.min(max, Number(normalized)))
}

function parseJalaliInput(value) {
  const normalized = normalizeDigits(value).replaceAll('-', '/').trim()
  const match = normalized.match(/^(\d{4})\/(\d{1,2})\/(\d{1,2})(?:\s+(\d{1,2}):(\d{1,2}))?$/)
  if (!match) return null
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  if (!year || month < 1 || month > 12 || day < 1 || day > 31) return null
  return {
    year,
    month,
    day,
    hour: match[4] == null ? null : Number(match[4]),
    minute: match[5] == null ? null : Number(match[5])
  }
}

function normalizeDigits(value) {
  return String(value ?? '')
    .replace(/[۰-۹]/g, digit => String(persianDigits.indexOf(digit)))
    .replace(/[٠-٩]/g, digit => String(arabicDigits.indexOf(digit)))
}

function toPersianDigits(value) {
  return String(value ?? '').replace(/\d/g, digit => persianDigits[Number(digit)])
}

function pad(value) {
  return String(value).padStart(2, '0')
}

function div(a, b) {
  return ~~(a / b)
}

function toJalali(gy, gm, gd) {
  const gDaysInMonth = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  const jDaysInMonth = [31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29]
  gy -= 1600
  gm -= 1
  gd -= 1
  let gDayNo = 365 * gy + div(gy + 3, 4) - div(gy + 99, 100) + div(gy + 399, 400)
  for (let i = 0; i < gm; ++i) gDayNo += gDaysInMonth[i]
  if (gm > 1 && ((gy + 1600) % 4 === 0 && ((gy + 1600) % 100 !== 0 || (gy + 1600) % 400 === 0))) gDayNo += 1
  gDayNo += gd
  let jDayNo = gDayNo - 79
  const jNp = div(jDayNo, 12053)
  jDayNo %= 12053
  let jy = 979 + 33 * jNp + 4 * div(jDayNo, 1461)
  jDayNo %= 1461
  if (jDayNo >= 366) {
    jy += div(jDayNo - 1, 365)
    jDayNo = (jDayNo - 1) % 365
  }
  let i = 0
  for (; i < 11 && jDayNo >= jDaysInMonth[i]; ++i) jDayNo -= jDaysInMonth[i]
  return [jy, i + 1, jDayNo + 1]
}

function toGregorian(jy, jm, jd) {
  const gDaysInMonth = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  const jDaysInMonth = [31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29]
  jy -= 979
  jm -= 1
  jd -= 1
  let jDayNo = 365 * jy + div(jy, 33) * 8 + div((jy % 33) + 3, 4)
  for (let i = 0; i < jm; ++i) jDayNo += jDaysInMonth[i]
  jDayNo += jd
  let gDayNo = jDayNo + 79
  let gy = 1600 + 400 * div(gDayNo, 146097)
  gDayNo %= 146097
  let leap = true
  if (gDayNo >= 36525) {
    gDayNo--
    gy += 100 * div(gDayNo, 36524)
    gDayNo %= 36524
    if (gDayNo >= 365) gDayNo++
    else leap = false
  }
  gy += 4 * div(gDayNo, 1461)
  gDayNo %= 1461
  if (gDayNo >= 366) {
    leap = false
    gDayNo--
    gy += div(gDayNo, 365)
    gDayNo %= 365
  }
  let i = 0
  for (; gDayNo >= gDaysInMonth[i] + (i === 1 && leap ? 1 : 0); i++) {
    gDayNo -= gDaysInMonth[i] + (i === 1 && leap ? 1 : 0)
  }
  return [gy, i + 1, gDayNo + 1]
}
