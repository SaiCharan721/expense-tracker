import { useEffect, useState } from 'react';
import { budgetApi, errorMessage } from '../api/client';
import { CATEGORIES, type Category } from '../api/types';
import { categoryLabel } from '../utils/format';

type Drafts = Partial<Record<Category, string>>;

export default function BudgetsPage() {
  const [saved, setSaved] = useState<Drafts>({});
  const [drafts, setDrafts] = useState<Drafts>({});
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    budgetApi
      .list()
      .then((list) => {
        const values: Drafts = {};
        list.forEach((b) => (values[b.category] = String(b.monthlyLimit)));
        setSaved(values);
        setDrafts(values);
      })
      .catch((err) => setError(errorMessage(err)));
  }, []);

  async function save(category: Category) {
    setError(null);
    setMessage(null);
    const value = drafts[category]?.trim() ?? '';
    try {
      if (value === '') {
        if (saved[category] !== undefined) await budgetApi.remove(category);
        setSaved((s) => {
          const next = { ...s };
          delete next[category];
          return next;
        });
        setMessage(`Removed budget for ${categoryLabel(category)}`);
      } else {
        const budget = await budgetApi.set(category, Number(value));
        setSaved((s) => ({ ...s, [category]: String(budget.monthlyLimit) }));
        setMessage(`Saved ${categoryLabel(category)} budget`);
      }
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div className="page">
      <h1>Monthly budgets</h1>
      <p className="muted">Set a limit for the categories you want to watch. Leave a field empty to remove its budget.</p>
      {message && <p className="success">{message}</p>}
      {error && <p className="error">{error}</p>}

      <section className="card budgets">
        {CATEGORIES.map((c) => {
          const changed = (drafts[c] ?? '') !== (saved[c] ?? '');
          return (
            <div key={c} className="budget-edit">
              <span>{categoryLabel(c)}</span>
              <input
                type="number"
                min="1"
                step="1"
                placeholder="No limit"
                value={drafts[c] ?? ''}
                onChange={(e) => setDrafts({ ...drafts, [c]: e.target.value })}
              />
              <button disabled={!changed} onClick={() => save(c)}>
                Save
              </button>
            </div>
          );
        })}
      </section>
    </div>
  );
}
