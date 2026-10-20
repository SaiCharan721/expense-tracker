import type { TrendMonth } from '../api/types';
import { formatMoney, monthLabel } from '../utils/format';

// Plain CSS bars. A chart library felt like overkill for six bars.
export default function TrendChart({ months }: { months: TrendMonth[] }) {
  const max = Math.max(...months.map((m) => m.total), 1);

  return (
    <div className="trend">
      {months.map((m) => (
        <div key={m.month} className="trend-col" title={formatMoney(m.total)}>
          <span className="trend-value">{m.total > 0 ? formatMoney(Math.round(m.total)) : '-'}</span>
          <div className="trend-bar" style={{ height: `${(m.total / max) * 100}%` }} />
          <span className="trend-label">{monthLabel(m.month)}</span>
        </div>
      ))}
    </div>
  );
}
