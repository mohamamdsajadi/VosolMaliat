'use client';
import { useEffect, useRef, useState } from 'react';
import { identifier, paperDate } from '../../lib/execution-papers';
import Icon from './paper-icon';
import styles from './execution-papers.module.css';

const dateParts = new Intl.DateTimeFormat('en-u-ca-persian-nu-latn', { year: 'numeric', month: 'numeric', day: 'numeric', timeZone: 'UTC' });
function persianDay(date) { return Number(dateParts.formatToParts(date).find(part => part.type === 'day').value); }
function startOfMonth(date) { const result = new Date(date); result.setUTCDate(result.getUTCDate() - persianDay(result) + 1); return result; }
function addDays(date, days) { const result = new Date(date); result.setUTCDate(result.getUTCDate() + days); return result; }

export default function PersianDateFilter({ label, value, onChange, min }) {
  const [open, setOpen] = useState(false);
  const [month, setMonth] = useState(() => startOfMonth(new Date()));
  const ref = useRef(null);
  const button = useRef(null);
  useEffect(() => {
    const outside = event => { if (!ref.current?.contains(event.target)) setOpen(false); };
    const escape = event => { if (event.key === 'Escape') { setOpen(false); button.current?.focus(); } };
    if (open) { document.addEventListener('pointerdown', outside); document.addEventListener('keydown', escape); }
    return () => { document.removeEventListener('pointerdown', outside); document.removeEventListener('keydown', escape); };
  }, [open]);
  const nextMonth = startOfMonth(addDays(month, 35));
  const length = Math.round((nextMonth - month) / 86400000);
  const offset = (month.getUTCDay() + 1) % 7;
  return <div className={styles.dateFilter} ref={ref}>
    <button type="button" ref={button} className={styles.dateButton} aria-label={label} aria-haspopup="dialog" aria-expanded={open}
      onClick={() => { setMonth(startOfMonth(value ? new Date(`${value}T12:00:00Z`) : new Date())); setOpen(!open); }}>
      {value ? <bdi>{paperDate(value)}</bdi> : label}
    </button>
    {open && <div className={styles.calendar} role="dialog" aria-label={`انتخاب ${label}`}>
      <div className={styles.calendarHeader}>
        <button type="button" aria-label="ماه قبل" onClick={() => setMonth(startOfMonth(addDays(month, -1)))}><Icon name="previous" /></button>
        <strong>{new Intl.DateTimeFormat('fa-IR-u-ca-persian', { year: 'numeric', month: 'long', timeZone: 'UTC' }).format(month)}</strong>
        <button type="button" aria-label="ماه بعد" onClick={() => setMonth(nextMonth)}><Icon name="next" /></button>
      </div>
      <div className={styles.calendarGrid}>
        {['ش', 'ی', 'د', 'س', 'چ', 'پ', 'ج'].map(day => <span key={day}>{day}</span>)}
        {Array.from({ length: offset }, (_, index) => <span key={`blank-${index}`} />)}
        {Array.from({ length }, (_, index) => {
          const date = addDays(month, index).toISOString().slice(0, 10);
          return <button type="button" key={date} disabled={Boolean(min && date < min)} aria-label={paperDate(date)} aria-pressed={value === date}
            onClick={() => { onChange(date); setOpen(false); button.current?.focus(); }}>{identifier(index + 1)}</button>;
        })}
      </div>
      <button type="button" className={styles.clearDate} onClick={() => { onChange(''); setOpen(false); button.current?.focus(); }}>پاک کردن تاریخ</button>
    </div>}
  </div>;
}
