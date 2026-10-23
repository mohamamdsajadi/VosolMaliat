import Link from 'next/link';
import { identifier, money, paperDate } from '../../lib/execution-papers';
import Icon from './paper-icon';
import { ViewIcon } from './paper-view-tools';
import styles from './execution-papers.module.css';

export const PAPER_COLUMNS = [
  { key: 'issuanceDate', label: 'تاریخ صدور', width: 105, sort: true },
  { key: 'uniqueCode', label: 'شماره برگ اجرا', width: 169 },
  { key: 'taxType', label: 'نوع برگ اجرا', width: 132 },
  { key: 'taxpayerCode', label: 'شناسه مودی', width: 160 },
  { key: 'taxpayerName', label: 'نام مؤدی', width: 156 },
  { key: 'taxpayerType', label: 'نوع مؤدی', width: 80 },
  { key: 'totalDebt', label: 'مانده بدهی(ریال)', width: 156, sort: true },
  { key: 'taxYear', label: 'سال', width: 65, sort: true },
  { key: 'taxPeriod', label: 'دوره', width: 66, sort: true },
];
export const PAPER_LIST_COLUMNS = [...PAPER_COLUMNS, { key: 'details', label: 'جزئیات', width: 40 }];

export default function PaperTable({ papers = [], columns = PAPER_COLUMNS, query = {}, onSort, onOpen, hrefFor,
  dense = true, loading, error, onRetry, canView = true, canViewDetail = true, summary = false, renderReferral, selection }) {
  const totalWidth = columns.reduce((sum, column) => sum + column.width, 0);
  const cell = (paper, column) => {
    if (column.key === 'selection') return <input type="checkbox" checked={selection.ids.has(paper.id)} onChange={() => selection.toggle(paper)} aria-label={`انتخاب برگ اجرا ${identifier(paper.uniqueCode)}`} />;
    if (column.key === 'details') return <div className={styles.paperRowActions}>{canViewDetail ? <Link data-paper-details className={styles.rowDetailsButton} href={hrefFor(paper.id)} aria-label={`مشاهده جزئیات برگ اجرا ${identifier(paper.uniqueCode)}`} title="مشاهده جزئیات"><ViewIcon name="eye" /></Link>
      : <span aria-disabled="true" title="شما مجوز مشاهده جزئیات را ندارید"><ViewIcon name="eye" /></span>}{renderReferral?.(paper)}</div>;
    if (column.key === 'uniqueCode') return hrefFor && canViewDetail ? <Link href={hrefFor(paper.id)} aria-label={`جزئیات برگ اجرا ${identifier(paper.uniqueCode)}`}><bdi>{identifier(paper.uniqueCode)}</bdi></Link> : <bdi>{identifier(paper.uniqueCode)}</bdi>;
    if (column.key === 'issuanceDate') return <bdi>{paperDate(paper.issuanceDate)}</bdi>;
    if (column.key === 'totalDebt') return <bdi>{money(paper.totalDebt)}</bdi>;
    if (['taxpayerCode', 'taxYear', 'taxPeriod'].includes(column.key)) return <bdi>{identifier(paper[column.key])}</bdi>;
    return paper[column.key] || '—';
  };
  return <div className={`${styles.tableScroll} ${summary ? styles.summaryTable : ''}`} role="region" tabIndex={0} aria-label={summary ? 'خلاصه برگ اجرای انتخاب‌شده' : 'جدول برگ‌های اجرا؛ برای مشاهده همه ستون‌ها پیمایش کنید'}>
    <table className={`${styles.paperTable} ${dense ? '' : styles.relaxedRows}`} style={{ '--table-min-width': `${Math.max(680, totalWidth)}px` }}>
      <caption className={styles.srOnly}>{summary ? 'خلاصه مشخصات برگ اجرا' : 'برگ‌های اجرا؛ برای مشاهده جزئیات، نماد چشم یا شماره برگ را انتخاب کنید.'}</caption>
      <colgroup>{columns.map(column => <col key={column.key} style={{ width: `${column.width / totalWidth * 100}%` }} />)}</colgroup>
      <thead><tr>{columns.map(column => <th key={column.key} scope="col" aria-sort={onSort && column.sort ? query.sortBy === column.key ? query.sortDirection === 'asc' ? 'ascending' : 'descending' : 'none' : undefined}>
        {column.key === 'selection' ? <input type="checkbox" aria-label="انتخاب همه برگ‌های این صفحه" checked={papers.length > 0 && selection.ids.size === papers.length} ref={element => { if (element) element.indeterminate = selection.ids.size > 0 && selection.ids.size < papers.length; }} disabled={!papers.length || loading || !canView} onChange={selection.toggleAll} /> : column.sort ? onSort ? <button onClick={() => onSort(column.key)} aria-label={`مرتب‌سازی بر اساس ${column.label}`}>{column.label}<Icon name="sort" /></button> : <span className={styles.summaryColumn}>{column.label}<Icon name="sort" /></span>
          : column.key === 'details' ? <span className={styles.smallColumnHeading}>{column.label}</span> : column.label}
      </th>)}</tr></thead>
      <tbody>{loading ? <tr><td colSpan={columns.length} className={styles.message}><span role="status">در حال دریافت برگ‌های اجرا…</span></td></tr>
        : !canView ? <tr><td colSpan={columns.length} className={styles.message}>شما مجوز مشاهده فهرست برگ‌های اجرا را ندارید.</td></tr>
          : error ? <tr><td colSpan={columns.length}><div role="alert" className={styles.error}><p>{error}</p><button onClick={onRetry}>تلاش مجدد</button></div></td></tr>
            : papers.length === 0 ? <tr><td colSpan={columns.length} className={styles.message}>برگ اجرایی مطابق جستجوی شما یافت نشد.</td></tr>
              : papers.map(paper => <tr key={paper.id} aria-selected={selection ? selection.ids.has(paper.id) : undefined} className={!summary && paper.currentState === 'UNASSIGNED' ? styles.unassignedRow : ''} onClick={onOpen && canViewDetail ? event => { if (!event.target.closest('a,button,input,label') && !window.getSelection()?.toString()) onOpen(paper.id); } : undefined}>
                {columns.map(column => <td key={column.key} title={['taxpayerName', 'taxType'].includes(column.key) ? paper[column.key] : undefined}>{cell(paper, column)}</td>)}
              </tr>)}</tbody>
    </table>
  </div>;
}
