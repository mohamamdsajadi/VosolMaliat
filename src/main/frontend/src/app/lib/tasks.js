import { apiJson } from './api';

const EXECUTION_PAPER_REVIEW_TASK_IDS = new Set([
  'Activity_0generalHeadReview',
  'Activity_0taxOfficeHeadReview',
  'Activity_0taxExpertProcess'
]);

export function fetchCartableTasks(filters = {}) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      params.set(key, String(value).trim());
    }
  });
  const query = params.toString();
  return apiJson(`/api/v1/cartable/my-tasks${query ? `?${query}` : ''}`);
}

export function fetchDelegationTargets(taskKey) {
  return apiJson(`/api/v1/cartable/tasks/${taskKey}/delegation-targets`);
}

export function claimCartableTask(taskKey) {
  return apiJson(`/api/v1/cartable/tasks/${taskKey}/claim`, {
    method: 'POST',
    body: JSON.stringify({})
  });
}

export function delegateTask(taskKey, payload) {
  return apiJson(`/api/v1/cartable/tasks/${taskKey}/delegate`, {
    method: 'POST',
    body: JSON.stringify(payload || {})
  });
}

export async function fetchProcessDefinitions() {
  const tasks = await fetchAllTasks();
  const definitions = new Map();
  tasks.forEach((task) => {
    const key = task.processDefinitionKey || task.processDefinitionId || "execution-paper";
    if (!definitions.has(key)) {
      definitions.set(key, {
        processDefinitionKey: key,
        processDefinitionId: task.processDefinitionId || key,
        name: task.processDefinitionId || "فرایند برگ اجرا",
        version: null
      });
    }
  });
  return Array.from(definitions.values());
}

export async function startProcessInstance(processDefinitionKey, variables = {}) {
  throw new Error("شروع فرایند از این صفحه در نسخه فعلی پشتیبانی نمی‌شود. برای صدور برگ اجرا از صفحه ورود داده استفاده کنید.");
}

export async function fetchTasks(processDefinitionKey) {
  const tasks = await fetchAllTasks();
  return tasks.filter((task) => {
    const key = task.processDefinitionKey || task.processDefinitionId || "execution-paper";
    return String(key) === String(processDefinitionKey);
  });
}

export async function fetchAllTasks() {
  const data = await apiJson("/api/v1/cartable/my-tasks");
  return Array.isArray(data) ? data.map(normalizeCartableTask).filter(isVisibleTaskSummary) : [];
}

export async function fetchTask(taskId) {
  const data = await apiJson(`/api/v1/cartable/tasks/${taskId}`);
  return normalizeCartableTask(data);
}

export async function completeTask(taskId, data) {
  return apiJson(`/api/v1/cartable/tasks/${taskId}/complete`, {
    method: "POST",
    body: JSON.stringify({ variables: data || {} })
  });
}

export function isEvaluationReviewTask(task) {
  const name = String(task?.name || "").trim();
  return name === "بازبینی توسط کارشناس";
}

export function isIncompleteInformationTask(task) {
  const name = String(task?.name || "").trim();
  return name === "تکمیل مجدد اطلاعات ناقص توسط کاربر";
}

export function isExecutionPaperReviewTask(task) {
  return EXECUTION_PAPER_REVIEW_TASK_IDS.has(String(task?.taskDefinitionId || '').trim());
}

export function hasRenderableSchema(task) {
  return Boolean(
    task?.schema &&
    Array.isArray(task.schema.components) &&
    task.schema.components.length > 0
  );
}

export function isVisibleTaskSummary(task) {
  const name = String(task?.name || "").toLowerCase();
  const hiddenName = ["rea", "ct"].join("");

  return Boolean(task?.id) && !name.includes(hiddenName);
}

function normalizeCartableTask(task) {
  if (!task) {
    return task;
  }
  const processDefinitionKey = task.processDefinitionId || task.processDefinitionKey || "execution-paper";
  return {
    ...task,
    id: task.id || task.taskKey,
    taskKey: task.taskKey || task.id,
    processDefinitionKey,
    processDefinitionId: task.processDefinitionId || processDefinitionKey,
    creationDate: task.createdAt,
    processName: task.processDefinitionId || "فرایند برگ اجرا",
    data: task.variables?.reduce((acc, item) => ({ ...acc, [item.name]: item.value }), {}) || {},
    schema: task.schema
  };
}
