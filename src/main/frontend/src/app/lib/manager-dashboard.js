import { apiJson } from './api';
import { fetchCartableTasks } from './tasks';
import { sanitizeDisplayText } from './display';

export async function fetchManagerDashboard() {
  const [stats, tasks] = await Promise.allSettled([
    apiJson('/api/v1/dashboard/stats'),
    fetchCartableTasks(),
  ]);
  return {
    stats: stats.status === 'fulfilled' ? stats.value : null,
    tasks: tasks.status === 'fulfilled' && Array.isArray(tasks.value) ? tasks.value.map(toDashboardTask) : [],
    statsError: stats.status === 'rejected' ? stats.reason.message : '',
    tasksError: tasks.status === 'rejected' ? tasks.reason.message
      : !Array.isArray(tasks.value) ? 'پاسخ کارتابل معتبر نیست.' : '',
    // No existing endpoint supplies these reports; never substitute design sample values.
    performance: null,
    collectionSeries: [],
    recentActivities: null,
  };
}

export function toDashboardTask(task) {
  const variables = Object.fromEntries((task.variables || []).map(item => [item.name, item.value]));
  const priority = String(variables.priority ?? variables.taskPriority ?? '').toUpperCase();
  return {
    key: task.taskKey,
    name: sanitizeDisplayText(task.name || 'بررسی برگ اجرا'),
    paperCode: variables.executionPaperCode || variables.executionPaperUniqueCode || variables.execution_paper_code || variables.executionPaperId || '—',
    dueDate: task.dueDate,
    createdAt: task.createdAt,
    // No urgency rule is inferred from a date or a task's name.
    priority: ['URGENT', 'HIGH', 'فوری'].includes(priority) ? 'urgent'
      : ['NORMAL', 'LOW', 'عادی'].includes(priority) ? 'normal' : null,
    href: `/tasks/${encodeURIComponent(task.taskKey)}`,
  };
}

export function formatDashboardNumber(value) {
  if (value == null || value === '') return '—';
  // Preserve monetary strings returned by ExactDecimalSerializer without Number coercion.
  const raw = String(value);
  if (!/^-?\d+(\.\d+)?$/.test(raw)) return '—';
  const [integer, decimal] = raw.split('.');
  return (integer.replace(/\B(?=(\d{3})+(?!\d))/g, '٬') + (decimal ? `٫${decimal}` : ''))
    .replace(/\d/g, digit => '۰۱۲۳۴۵۶۷۸۹'[digit]);
}

export const DASHBOARD_TASK_CATEGORIES = [
  { key: 'arrest', label: 'دستور بازداشت', icon: 'lock' },
  { key: 'debt', label: 'تعیین تکلیف بدهی', icon: 'receipt' },
  { key: 'restriction', label: 'محدودیت قانونی', icon: 'shield' },
  { key: 'execution', label: 'اقدامات اجرایی', icon: 'list' },
  { key: 'auction', label: 'مزایده و فروش', icon: 'gavel' },
];

