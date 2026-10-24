import { Suspense } from 'react';
import DebtStatementWorkspace from '../components/debt-statement/debt-statement-workspace';

export default function DebtStatementPage() {
  return <Suspense fallback={<p role="status">در حال دریافت صورت وضعیت بدهی…</p>}><DebtStatementWorkspace /></Suspense>;
}
