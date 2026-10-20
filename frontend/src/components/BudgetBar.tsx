import type { CategorySummary } from '../api/types';
import { barWidth, categoryLabel, formatMoney, statusText } from '../utils/format';

export default function BudgetBar({ row }: { row: CategorySummary }) {
  return (
    <div className="budget-row">
      <div className="budget-row-head">
        <strong>{categoryLabel(row.category)}</strong>
        <span>
          {formatMoney(row.spent)}
          {row.monthlyLimit != null && <span className="muted"> / {formatMoney(row.monthlyLimit)}</span>}
        </span>
      </div>
      {row.monthlyLimit != null && (
        <div className="bar">
          <div className={`bar-fill ${row.status.toLowerCase()}`} style={{ width: `${barWidth(row.percentUsed)}%` }} />
        </div>
      )}
      <small className={`status ${row.status.toLowerCase()}`}>
        {statusText(row.status, row.percentUsed)} · {row.count} {row.count === 1 ? 'expense' : 'expenses'}
      </small>
    </div>
  );
}
