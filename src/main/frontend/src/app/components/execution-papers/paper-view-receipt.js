'use client';
import { useEffect, useState } from 'react';
import { apiJson } from '../../lib/api';
import styles from './execution-papers.module.css';

// Mounted only after the full details page has loaded; inline previews never mount this.
export default function PaperViewReceipt({ paperId }) {
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState({ attempt: -1, error: '' });
  useEffect(() => {
    const controller = new AbortController();
    apiJson(`/api/v1/execution-papers/${encodeURIComponent(paperId)}/viewed`, { method: 'POST', signal: controller.signal })
      .then(() => {
        if (!controller.signal.aborted) {
          setResult({ attempt, error: '' });
          window.dispatchEvent(new Event('execution-paper-viewed'));
        }
      })
      .catch(error => { if (!controller.signal.aborted) setResult({ attempt, error: error.message }); });
    return () => controller.abort();
  }, [paperId, attempt]);
  return result.attempt === attempt && result.error ? <div className={styles.viewReceiptError} role="alert"><span>ثبت وضعیت مشاهده ناموفق بود: {result.error}</span><button type="button" onClick={() => setAttempt(value => value + 1)}>تلاش مجدد</button></div> : null;
}
