import { isAdminUser } from './navigation';

// Only roles explicitly responsible for selecting the next responsible person.
// Review/approval of special collection and automatic allocation are separate tasks.
export const MANUAL_REFERRAL_ROLES = new Set([
  'DIRECTORATE_EXECUTION_UNIT_HEAD',
  'DIRECTORATE_EXECUTION_TEAM_SUPERVISOR',
  'SPECIAL_COLLECTION_DIRECTOR_GENERAL',
  'SPECIAL_EXECUTION_DEPUTY',
  'SPECIAL_EXECUTION_DEPARTMENT_HEAD',
]);

export function taskVariables(task) {
  return Array.isArray(task?.variables) ? Object.fromEntries(task.variables.filter(item => item?.name).map(item => [item.name, item.value])) : task?.data || {};
}

export function canShowReferral(task, user) {
  const variables = taskVariables(task);
  return canReferExecutionPaper(user)
    && task?.allowedActions?.view !== false
    && Boolean(variables.executionPaperId || variables.execution_paper_id);
}

export function canReferExecutionPaper(user) {
  return !isAdminUser(user) && MANUAL_REFERRAL_ROLES.has(user?.businessRole);
}

export function referralTasksForPaper(tasks, paper, user) {
  return (Array.isArray(tasks) ? tasks : []).filter(task => {
    const values = taskVariables(task);
    const id = values.executionPaperId ?? values.execution_paper_id;
    return id != null && String(id) === String(paper.id)
      && canShowReferral(task, user) && isReferralTask(task);
  });
}

export function canOpenReferral(task) {
  return task?.allowedActions?.complete === true || task?.allowedActions?.claim === true;
}

export function referralHref(task, processDefinitionKey) {
  const params = new URLSearchParams({ intent: 'referral' });
  if (processDefinitionKey) params.set('processDefinitionKey', processDefinitionKey);
  return `/tasks/${encodeURIComponent(task.taskKey || task.id)}?${params}`;
}

const normalize = value => String(value || '').replaceAll('ي', 'ی').replaceAll('ك', 'ک').replace(/[\s‌]+/g, ' ').trim();
const REFERRAL_TASK_NAMES = new Set([
  'انتخاب مأمور اجرا جهت ارجاع برگ اجرا',
  'انتخاب سرپرست هیئت اجرائی جهت ارجاع برگ اجرا',
  'انتخاب سرپرست هیئت اجرایی جهت ارجاع برگ اجرا',
  'ارجاع برگ اجرا به معاون اجرائیات',
  'انتخاب رئیس اداره اجرائیات جهت ارجاع برگ اجرا',
].map(normalize));

export function isReferralTask(task) {
  return taskVariables(task).uiAction === 'referral' || REFERRAL_TASK_NAMES.has(normalize(task?.name));
}
