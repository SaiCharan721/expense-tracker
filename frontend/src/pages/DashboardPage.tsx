import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage, reportApi, summaryApi } from '../api/client';
import type { MonthlySummary, TrendMonth } from '../api/types';
import BudgetBar from '../components/BudgetBar';
import TrendChart from '../components/TrendChart';
import { formatMoney, monthLabel, toMonthValue } from '../utils/format';

export default function DashboardPage() {
  const [month, setMonth] = useState(() => toMonthValue(new Date()));
  const [summary, setSummary] = useState<MonthlySummary | null>(null);
  const [trend, setTrend] = useState<TrendMonth[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [trendError, setTrendError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setError(null);
    summaryApi
      .monthly(month)
      .then((s) => !cancelled && setSummary(s))
      .catch((err) => !cancelled && setError(errorMessage(err)));
    return () => {
      cancelled = true;
    };
  }, [month]);

  useEffect(() => {
    // the trend comes from report-service; the dashboard still works if that service is down
    reportApi
      .trend(6)
      .then(setTrend)
      .catch(() => setTrendError(true));
  }, []);

  async function downloadCsv() {
    try {
      await reportApi.downloadMonthlyCsv(month);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  const remaining = summary && summary.totalBudget > 0 ? summary.totalBudget - summary.totalSpent : null;
  const overBudget = summary?.categories.filter((c) => c.status === 'OVER_BUDGET') ?? [];

  return (
    <div className="page">
      <div className="page-head">
        <h1>{monthLabel(month)}</h1>
        <div className="actions">
          <input type="month" value={month} max={toMonthValue(new Date())} onChange={(e) => e.target.value && setMonth(e.target.value)} />
          <button className="secondary" onClick={downloadCsv}>
            Export CSV
          </button>
        </div>
      </div>

      {error && <p className="error">{error}</p>}

      {summary && (
        <>
          <div className="stats">
            <div className="stat">
              <span className="muted">Spent</span>
              <strong>{formatMoney(summary.totalSpent)}</strong>
              <small>{summary.expenseCount} expenses</small>
            </div>
            <div className="stat">
              <span className="muted">Budgeted</span>
              <strong>{summary.totalBudget > 0 ? formatMoney(summary.totalBudget) : '-'}</strong>
              <small>
                <Link to="/budgets">Edit budgets</Link>
              </small>
            </div>
            <div className="stat">
              <span className="muted">Left in budget</span>
              <strong className={remaining != null && remaining < 0 ? 'negative' : ''}>
                {remaining != null ? formatMoney(remaining) : '-'}
              </strong>
              <small>{overBudget.length > 0 ? `${overBudget.length} over budget` : 'all categories in budget'}</small>
            </div>
          </div>

          <section className="card">
            <h2>By category</h2>
            {summary.categories.length === 0 ? (
              <p className="muted">
                Nothing recorded for this month yet. <Link to="/expenses">Add an expense</Link>
              </p>
            ) : (
              summary.categories.map((row) => <BudgetBar key={row.category} row={row} />)
            )}
          </section>
        </>
      )}

      <section className="card">
        <h2>Last 6 months</h2>
        {trendError ? <p className="muted">Trend is unavailable (report-service not reachable).</p> : <TrendChart months={trend} />}
      </section>
    </div>
  );
}
