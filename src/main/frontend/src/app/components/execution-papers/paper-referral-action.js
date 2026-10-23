'use client';
import { identifier } from '../../lib/execution-papers';
import { ViewIcon } from './paper-view-tools';
import styles from './execution-papers.module.css';

export default function PaperReferralAction({ paper, onOpen }) {
  return <button data-paper-referral type="button" className={styles.rowDetailsButton} aria-haspopup="dialog"
    title="ارجاع" aria-label={`ارجاع برگ اجرا ${identifier(paper.uniqueCode)}`} onClick={() => onOpen([paper])}><ViewIcon name="referral" /></button>;
}
