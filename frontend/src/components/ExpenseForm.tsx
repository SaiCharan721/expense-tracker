import { useState, type FormEvent } from 'react';
import { CATEGORIES, PAYMENT_METHODS, type Category, type Expense, type ExpenseInput, type PaymentMethod } from '../api/types';
import { categoryLabel, humanize, todayValue } from '../utils/format';

interface Props {
  initial?: Expense;
  onSubmit: (input: ExpenseInput) => Promise<void>;
  onCancel?: () => void;
}

export default function ExpenseForm({ initial, onSubmit, onCancel }: Props) {
  const [amount, setAmount] = useState(initial ? String(initial.amount) : '');
  const [category, setCategory] = useState<Category>(initial?.category ?? 'FOOD');
  const [date, setDate] = useState(initial?.date ?? todayValue());
  const [description, setDescription] = useState(initial?.description ?? '');
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>(initial?.paymentMethod ?? 'CARD');
  const [tags, setTags] = useState(initial?.tags.join(', ') ?? '');
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      await onSubmit({
        amount: Number(amount),
        category,
        date,
        description: description.trim() || null,
        paymentMethod,
        tags: tags
          .split(',')
          .map((t) => t.trim())
          .filter(Boolean),
      });
      if (!initial) {
        setAmount('');
        setDescription('');
        setTags('');
      }
    } catch {
      // the page shows the error; keep what was typed so it can be fixed
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="expense-form" onSubmit={handleSubmit}>
      <label>
        Amount ($)
        <input type="number" min="0.01" step="0.01" required value={amount} onChange={(e) => setAmount(e.target.value)} />
      </label>
      <label>
        Category
        <select value={category} onChange={(e) => setCategory(e.target.value as Category)}>
          {CATEGORIES.map((c) => (
            <option key={c} value={c}>
              {categoryLabel(c)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Date
        <input type="date" required max={todayValue()} value={date} onChange={(e) => setDate(e.target.value)} />
      </label>
      <label className="wide">
        Description
        <input maxLength={200} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="e.g. Costco run" />
      </label>
      <label>
        Paid with
        <select value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value as PaymentMethod)}>
          {PAYMENT_METHODS.map((p) => (
            <option key={p} value={p}>
              {humanize(p)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Tags
        <input value={tags} onChange={(e) => setTags(e.target.value)} placeholder="comma separated" />
      </label>
      <div className="form-actions">
        <button type="submit" disabled={saving}>
          {saving ? 'Saving...' : initial ? 'Save changes' : 'Add expense'}
        </button>
        {onCancel && (
          <button type="button" className="secondary" onClick={onCancel}>
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
