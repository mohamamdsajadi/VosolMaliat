'use client';
import { useCallback, useEffect, useState } from 'react';
import { apiJson } from '../../lib/api';
import { isAdminUser } from '../../lib/navigation';
import PaperDetailPanel from './paper-detail-page';

function useSummaryResource(path, enabled, revision) {
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState({ key: '', data: null, error: '' });
  const key = `${path}:${revision}:${attempt}`;
  useEffect(() => {
    if (!enabled) return;
    const controller = new AbortController();
    apiJson(path, { signal: controller.signal })
      .then(data => { if (!controller.signal.aborted) setResult({ key, data, error: '' }); })
      .catch(error => { if (!controller.signal.aborted) setResult({ key, data: null, error: error.message }); });
    return () => controller.abort();
  }, [path, enabled, key]);
  const retry = useCallback(() => setAttempt(value => value + 1), []);
  return { ...(!enabled ? { data: null, error: '', loading: false } : result.key === key ? { ...result, loading: false } : { data: null, error: '', loading: true }), retry };
}

export default function PaperSummaryPanel({ paperId, user, onClose, revision = 0, paymentDeadline }) {
  const hasPermission = permission => isAdminUser(user) || [...(user?.allowedActions || []), ...(user?.localBusinessPermissions || [])].includes(permission);
  const canView = ['REPORT_VIEW', 'EXECUTION_PAPER_DETAIL_VIEW'].some(hasPermission);
  const canTrace = ['REPORT_VIEW', 'AUDIT_VIEW', 'EXECUTION_PAPER_TRACE_VIEW'].some(hasPermission);
  const path = `/api/v1/execution-papers/${encodeURIComponent(paperId)}`;
  const detail = useSummaryResource(path, canView, revision);
  const trace = useSummaryResource(`${path}/trace`, canView && canTrace, revision);
  const related = useSummaryResource(`${path}/related-persons`, canView && detail.data?.taxpayer?.type === 'LEGAL', revision);
  const external = useSummaryResource(`${path}/external-requests`, canView && canTrace && !!detail.data, revision);
  return <PaperDetailPanel key={paperId} embedded resource={canView ? detail : { data: null, loading: false, error: 'شما مجوز مشاهده جزئیات برگ اجرا را ندارید.' }} trace={trace} related={related} external={external} canTrace={canTrace}
    onClose={onClose} paymentDeadline={paymentDeadline} hrefFor={id => `/execution-papers/${encodeURIComponent(id)}`} />;
}
