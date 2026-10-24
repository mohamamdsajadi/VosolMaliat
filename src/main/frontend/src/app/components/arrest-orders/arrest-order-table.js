'use client';
import Link from 'next/link';
import { STAGES, arrestDate, digits } from '../../lib/arrest-orders';
import { money } from '../../lib/execution-papers';
import styles from './arrest-orders.module.css';

export const CANDIDATE_COLUMNS = [
  { key: 'paper', label: 'برگ اجرا / مؤدی', width: 235 },
  { key: 'debt', label: 'بدهی باقیمانده (ریال)', width: 170 },
  { key: 'notified', label: 'تاریخ ابلاغ', width: 115 },
  { key: 'deadline', label: 'پایان مهلت پرداخت', width: 125 },
  { key: 'status', label: 'وضعیت', width: 110 },
  { key: 'actions', label: 'عملیات', width: 130 },
];
export const ORDER_COLUMNS = [
  ...CANDIDATE_COLUMNS.slice(0, 2),
  { key: 'referred', label: 'تاریخ ارجاع', width: 145 },
  { key: 'destination', label: 'مقصد ارجاع', width: 190 },
  { key: 'issued', label: 'وضعیت صدور', width: 150 },
  { key: 'stage', label: 'مرحله جاری', width: 160 },
  CANDIDATE_COLUMNS.at(-1),
];

export default function ArrestOrderTable({ rows, candidates, canStart, columns, dense, selection, onStart, onOpen }) {
  const cell = (row, column) => {
    const paper = candidates ? row.paper : row;
    switch (column.key) {
      case 'paper': return <><Link className={styles.paperLink} href={`/execution-papers/${candidates ? paper.id : paper.executionPaperId}`}>{digits(paper.uniqueCode || paper.executionPaperCode)}</Link><strong>{paper.taxpayerName || '—'}</strong><small>شناسه ملی: {digits(paper.taxpayerNationalId)}</small></>;
      case 'debt': return <bdi className={styles.amount}>{money(paper.totalDebt ?? paper.debtAmount)}</bdi>;
      case 'notified': return arrestDate(paper.notificationDate, false);
      case 'deadline': return arrestDate(row.paymentDeadline, false);
      case 'status': return <span className={styles.ready}>آماده صدور</span>;
      case 'referred': return arrestDate(row.referredAt);
      case 'destination': return <><strong>{row.referralDestination}</strong><small>{row.assignee?.roleTitle || 'صف رئیس اداره اجرائیات ویژه'}</small></>;
      case 'issued': return <><span className={row.issuanceStatus === 'ISSUED' ? styles.ready : styles.pending}>{row.issuanceStatus === 'ISSUED' ? 'صادر شده' : 'در انتظار صدور'}</span>{row.letterNumber && <small>{digits(row.letterNumber)}</small>}</>;
      case 'stage': return STAGES[row.stage] || row.stage;
      case 'actions': return candidates ? canStart ? <button className={styles.rowAction} onClick={() => onStart(paper)}>آغاز فرایند</button> : <small>شروع توسط رئیس واحد اجرائیات</small> : <button className={styles.rowAction} onClick={() => onOpen(row.id)}>{row.allowedActions.length ? 'بررسی و اقدام' : 'جزئیات و نامه'}</button>;
      default: return '—';
    }
  };
  return <div className={styles.tableScroll} role="region" aria-label="جدول دستورهای بازداشت" tabIndex={0}><table className={`${styles.table} ${dense ? styles.denseTable : ''}`} style={{ minWidth: Math.max(330, 45 + columns.reduce((sum, column) => sum + column.width, 0)) }}>
    <thead><tr><th scope="col"><input type="checkbox" aria-label="انتخاب همه برگ‌های این صفحه" checked={rows.length > 0 && selection.ids.size === rows.length} ref={element => { if (element) element.indeterminate = selection.ids.size > 0 && selection.ids.size < rows.length; }} onChange={selection.toggleAll} /></th>{columns.map(column => <th scope="col" key={column.key}>{column.label}</th>)}</tr></thead>
    <tbody>{rows.map(row => {
      const id = candidates ? row.paper.id : row.id;
      const code = candidates ? row.paper.uniqueCode : row.executionPaperCode;
      return <tr key={id} aria-selected={selection.ids.has(id)} onClick={event => { if (!event.target.closest('a,button,input,label') && !window.getSelection()?.toString()) selection.toggle(id); }}><td><input type="checkbox" aria-label={`انتخاب برگ ${code}`} checked={selection.ids.has(id)} onChange={() => selection.toggle(id)} /></td>{columns.map(column => <td key={column.key}>{cell(row, column)}</td>)}</tr>;
    })}</tbody>
  </table></div>;
}
