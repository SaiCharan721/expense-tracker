import { useCallback, useEffect, useState } from 'react';
import { errorMessage, expenseApi } from '../api/client';
import { CATEGORIES, type Expense, type ExpenseFilter, type ExpenseInput, type Page } from '../api/types';
import ExpenseForm from '../components/ExpenseForm';
import { categoryLabel, formatMoney, humanize } from '../utils/format';

const EMPTY_FILTER: ExpenseFilter = { from: '', to: '', category: '', q: '' };

export default function ExpensesPage() {
  const [filter, setFilter] = useState<ExpenseFilter>(EMPTY_FILTER);
  const [applied, setApplied] = useState<ExpenseFilter>(EMPTY_FILTER);
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<Expense> | null>(null);
  const [editing, setEditing] = useState<Expense | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    expenseApi
      .list(applied, page)
      .then(setData)
      .catch((err) => setError(errorMessage(err)));
  }, [applied, page]);

  useEffect(load, [load]);

  async function save(input: ExpenseInput) {
    setError(null);
    try {
      if (editing) {
        await expenseApi.update(editing.id, input);
        setEditing(null);
      } else {
        await expenseApi.create(input);
      }
      load();
    } catch (err) {
      setError(errorMessage(err));
      throw err;
    }
  }

  async function remove(expense: Expense) {
    if (!window.confirm(`Delete "${expense.description ?? categoryLabel(expense.category)}"?`)) return;
    try {
      await expenseApi.remove(expense.id);
      // stepping back a page if we just deleted the last row on it
      if (data && data.content.length === 1 && page > 0) setPage(page - 1);
      else load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  function applyFilter() {
    setPage(0);
    setApplied(filter);
  }

  return (
    <div className="page">
      <h1>Expenses</h1>

      <section className="card">
        <h2>{editing ? 'Edit expense' : 'Add expense'}</h2>
        <ExpenseForm
          key={editing?.id ?? 'new'}
          initial={editing ?? undefined}
          onSubmit={save}
          onCancel={editing ? () => setEditing(null) : undefined}
        />
      </section>

      {error && <p className="error">{error}</p>}

      <section className="card">
        <div className="filters">
          <input type="date" value={filter.from} onChange={(e) => setFilter({ ...filter, from: e.target.value })} aria-label="From" />
          <input type="date" value={filter.to} onChange={(e) => setFilter({ ...filter, to: e.target.value })} aria-label="To" />
          <select value={filter.category} onChange={(e) => setFilter({ ...filter, category: e.target.value as ExpenseFilter['category'] })}>
            <option value="">All categories</option>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {categoryLabel(c)}
              </option>
            ))}
          </select>
          <input
            placeholder="Search description"
            value={filter.q}
            onChange={(e) => setFilter({ ...filter, q: e.target.value })}
            onKeyDown={(e) => e.key === 'Enter' && applyFilter()}
          />
          <button onClick={applyFilter}>Filter</button>
          <button
            className="secondary"
            onClick={() => {
              setFilter(EMPTY_FILTER);
              setApplied(EMPTY_FILTER);
              setPage(0);
            }}
          >
            Clear
          </button>
        </div>

        {data && data.content.length === 0 && <p className="muted">No expenses match.</p>}

        {data && data.content.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Description</th>
                <th>Category</th>
                <th>Paid with</th>
                <th className="num">Amount</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.content.map((e) => (
                <tr key={e.id}>
                  <td>{e.date}</td>
                  <td>
                    {e.description ?? <span className="muted">-</span>}
                    {e.tags.map((t) => (
                      <span key={t} className="tag">
                        {t}
                      </span>
                    ))}
                  </td>
                  <td>{categoryLabel(e.category)}</td>
                  <td>{humanize(e.paymentMethod)}</td>
                  <td className="num">{formatMoney(e.amount)}</td>
                  <td className="row-actions">
                    <button className="link-button" onClick={() => setEditing(e)}>
                      Edit
                    </button>
                    <button className="link-button danger" onClick={() => remove(e)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {data && data.totalPages > 1 && (
          <div className="pager">
            <button className="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Previous
            </button>
            <span>
              Page {page + 1} of {data.totalPages} ({data.totalElements} expenses)
            </span>
            <button className="secondary" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>
              Next
            </button>
          </div>
        )}
      </section>
    </div>
  );
}
