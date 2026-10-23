'use client';
import Link from 'next/link';
import { useRef } from 'react';
import styles from './execution-papers.module.css';

// Small controls unavailable as local design exports use the same 24px stroke grid.
export function ViewIcon({ name }) {
  return <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {name === 'filter' && <><path d="M3 6h5m6 0h7M3 18h11m6 0h1M3 12h12m6 0h0" /><circle cx="11" cy="6" r="3" /><circle cx="18" cy="12" r="3" /><circle cx="17" cy="18" r="3" /></>}
    {name === 'help' && <><path d="M9 19H6a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3h12a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3h-4l-4 3v-3Z" /><path d="M10 8a2 2 0 1 1 3 1.7c-.7.4-1 1-1 1.8M12 15h.01" /></>}
    {name === 'settings' && <><path d="m9.5 3-.7 2.2-1.6.9L5 5.7 3 9.2l1.5 1.7v1.8L3 14.5 5 18l2.2-.4 1.6.9.7 2.2h4l.7-2.2 1.6-.9 2.2.4 2-3.5-1.5-1.8v-1.8L20 9.2l-2-3.5-2.2.4-1.6-.9-.7-2.2h-4Z" /><circle cx="11.5" cy="11.8" r="3" /></>}
    {name === 'referral' && <><rect x="3" y="3" width="18" height="18" rx="4" /><path d="M8 16 16 8m-6 0h6v6M8 11v5h5" /></>}
    {name === 'eye' && <><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" /><circle cx="12" cy="12" r="3" /></>}
    {name === 'print' && <><path d="M7 8V3h10v5M7 17H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-3" /><path d="M7 14h10v7H7zM17 11h1" /></>}
    {name === 'alert' && <><path d="m12 2 3 2 4 .5.5 4L22 12l-2.5 3.5-.5 4-4 .5-3 2-3-2-4-.5-.5-4L2 12l2.5-3.5.5-4L9 4l3-2Z" /><path d="M12 7v6m0 4h.01" /></>}
  </svg>;
}

export default function PaperViewTools({ columns, hiddenColumns, onToggleColumn, dense, onDenseChange, createAllowed }) {
  const settings = useRef(null);
  return <div className={styles.pageTools}>
    <button type="button" className={styles.viewToolButton} aria-label="تنظیم نمایش فهرست" aria-haspopup="dialog" onClick={() => settings.current.showModal()}><ViewIcon name="settings" /></button>
    <dialog ref={settings} className={styles.viewDialog} aria-labelledby="paper-settings-title" onClick={event => { if (event.target === event.currentTarget) settings.current.close(); }}>
      <div><form method="dialog"><button aria-label="بستن تنظیم نمایش" className={styles.dialogClose}>×</button></form>
        <h2 id="paper-settings-title">تنظیم نمایش فهرست</h2>
        <label className={styles.settingOption}><input type="checkbox" checked={dense} onChange={event => onDenseChange(event.target.checked)} />نمایش فشرده ردیف‌ها</label>
        <fieldset><legend>ستون‌های قابل نمایش</legend>{columns.map(column => <label key={column.key} className={styles.settingOption}>
          <input type="checkbox" checked={!hiddenColumns.includes(column.key)} disabled={column.key === 'uniqueCode'} onChange={() => onToggleColumn(column.key)} />{column.label}
        </label>)}</fieldset>
        {createAllowed && <Link className={styles.entryLink} href="/admin/data-entry/execution-paper">صدور برگ اجرا</Link>}
      </div>
    </dialog>
  </div>;
}
