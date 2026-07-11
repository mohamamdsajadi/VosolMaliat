import { apiJson } from './api';

export function fetchMyCartableTasks() {
  return apiJson('/api/v1/cartable/my-tasks');
}

export function fetchCandidateCartableTasks() {
  return apiJson('/api/v1/cartable/candidate-tasks');
}

export function fetchCartableTask(taskKey) {
  return apiJson(`/api/v1/cartable/tasks/${taskKey}`);
}

export function claimCartableTask(taskKey, assignee) {
  return apiJson(`/api/v1/cartable/tasks/${taskKey}/claim`, {
    method: 'POST',
    body: JSON.stringify({ assignee })
  });
}

export function unclaimCartableTask(taskKey) {
  return apiJson(`/api/v1/cartable/tasks/${taskKey}/unclaim`, {
    method: 'POST'
  });
}

export function completeCartableTask(taskKey, variables = {}) {
  return apiJson(`/api/v1/cartable/tasks/${taskKey}/complete`, {
    method: 'POST',
    body: JSON.stringify({ variables })
  });
}

export function assignCartableTask(taskKey, assignee) {
  return apiJson(`/api/v1/cartable/tasks/${taskKey}/assign`, {
    method: 'POST',
    body: JSON.stringify({ assignee })
  });
}
