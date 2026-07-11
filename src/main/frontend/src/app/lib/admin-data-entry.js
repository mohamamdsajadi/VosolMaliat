import { apiJson } from './api';

export function fetchAdminDataSummary() {
  return apiJson('/api/v1/admin/data-entry/summary');
}

export function createFinalizedDebtPaper(payload) {
  return apiJson('/api/v1/admin/data-entry/finalized-debt-papers', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function sendFinalizedDebtPaper(payload) {
  return apiJson('/api/v1/admin/data-entry/finalized-debt-papers/send', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function createManualExecutionPaper(payload) {
  return apiJson('/api/v1/admin/data-entry/execution-papers', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}
