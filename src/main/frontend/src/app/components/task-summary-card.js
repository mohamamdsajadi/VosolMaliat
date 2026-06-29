import Link from 'next/link';
import {
  formatCompactId,
  getDisplayTaskTitle,
  getTaskReference,
  getTaskSubtitle
} from '../lib/display';

export default function TaskSummaryCard({ task, order }) {
  const taskId = task?.id;
  const processDefinitionKey = task?.processDefinitionKey;
  const href = processDefinitionKey
    ? `/tasks/${taskId}?processDefinitionKey=${encodeURIComponent(processDefinitionKey)}`
    : `/tasks/${taskId}`;
  const title = getDisplayTaskTitle(
    task,
    `درخواست شماره ${Number(order || 1).toLocaleString('fa-IR')}`
  );

  return (
    <Link className="task-summary-card" href={href}>
      <div className="task-summary-main">
        <span className="task-index">{Number(order || 1).toLocaleString('fa-IR')}</span>
        <div>
          <p className="eyebrow">{getTaskReference(task)}</p>
          <h3>{title}</h3>
          <p className="summary-copy">
            {getTaskSubtitle(task)}
          </p>
        </div>
      </div>
      <div className="task-summary-side">
        <span className="status-pill status-pill--ready">آماده اقدام</span>
        <span className="task-meta-chip">کار {formatCompactId(taskId)}</span>
        <span className="open-indicator">باز کردن فرم</span>
      </div>
    </Link>
  );
}
