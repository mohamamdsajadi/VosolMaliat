'use client';
import Link from 'next/link';
import { canShowReferral, canOpenReferral, referralHref, taskVariables } from '../../lib/task-referral';
import styles from './task-referral.module.css';

export default function TaskReferralAction({ task, user, processDefinitionKey }) {
  if (!canShowReferral(task, user)) return null;
  const values = taskVariables(task);
  const paper = values.executionPaperCode || values.executionPaperUniqueCode || values.executionPaperId || values.execution_paper_id;
  const content = <><svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M14 4h6v6M20 4l-9 9M20 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1h5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" /></svg><span>ارجاع</span></>;
  return canOpenReferral(task)
    ? <Link className={styles.action} href={referralHref(task, processDefinitionKey)} aria-label={`ارجاع برگ اجرا ${paper}`}>{content}</Link>
    : <button className={styles.action} type="button" disabled title="ارجاع این کار در وضعیت فعلی مجاز نیست" aria-label={`ارجاع برگ اجرا ${paper}؛ در وضعیت فعلی مجاز نیست`}>{content}</button>;
}
