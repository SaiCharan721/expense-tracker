import type { BudgetStatus, Category } from '../api/types';

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

export function formatMoney(amount: number): string {
  return usd.format(amount);
}

/** "BANK_TRANSFER" -> "Bank transfer" */
export function humanize(value: string): string {
  const text = value.replace(/_/g, ' ').toLowerCase();
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function categoryLabel(category: Category): string {
  return humanize(category);
}

/** yyyy-MM for a Date, using local time (what the user sees on their calendar) */
export function toMonthValue(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

export function todayValue(): string {
  const d = new Date();
  return `${toMonthValue(d)}-${String(d.getDate()).padStart(2, '0')}`;
}

/** "2026-10" -> "Oct 2026" */
export function monthLabel(month: string): string {
  const [year, m] = month.split('-').map(Number);
  return new Date(year, m - 1, 1).toLocaleString('en-US', { month: 'short', year: 'numeric' });
}

/** Width of the progress bar. Capped at 100 so an over-budget bar does not overflow the card. */
export function barWidth(percentUsed: number | null): number {
  if (percentUsed == null || percentUsed < 0) return 0;
  return Math.min(percentUsed, 100);
}

export function statusText(status: BudgetStatus, percentUsed: number | null): string {
  switch (status) {
    case 'OVER_BUDGET':
      return `Over budget (${percentUsed}%)`;
    case 'WARNING':
    case 'ON_TRACK':
      return `${percentUsed}% used`;
    default:
      return 'No budget';
  }
}
