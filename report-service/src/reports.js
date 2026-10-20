// Pure helpers. No Express or Mongo in here so they are easy to test.

const MONTH_RE = /^(\d{4})-(0[1-9]|1[0-2])$/;

export function parseMonth(value) {
  const match = MONTH_RE.exec(value ?? '');
  if (!match) return null;
  return { year: Number(match[1]), month: Number(match[2]) };
}

export function formatMonth({ year, month }) {
  return `${year}-${String(month).padStart(2, '0')}`;
}

/** [from, to) as UTC dates. expense-api stores LocalDate as UTC midnight. */
export function monthRange({ year, month }) {
  return {
    from: new Date(Date.UTC(year, month - 1, 1)),
    to: new Date(Date.UTC(year, month, 1)),
  };
}

/** The last `count` months ending with the month of `now`, oldest first. */
export function lastMonths(count, now = new Date()) {
  const months = [];
  for (let i = count - 1; i >= 0; i--) {
    const d = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() - i, 1));
    months.push({ year: d.getUTCFullYear(), month: d.getUTCMonth() + 1 });
  }
  return months;
}

// Work in cents so 0.1 + 0.2 style float errors don't show up in totals
const toCents = (amount) => Math.round(Number(amount) * 100);
const fromCents = (cents) => cents / 100;

export function buildTrend(expenses, months) {
  const buckets = new Map(months.map((m) => [formatMonth(m), { total: 0, byCategory: {} }]));

  for (const e of expenses) {
    const key = e.date.toISOString().slice(0, 7);
    const bucket = buckets.get(key);
    if (!bucket) continue;
    const cents = toCents(e.amount);
    bucket.total += cents;
    bucket.byCategory[e.category] = (bucket.byCategory[e.category] ?? 0) + cents;
  }

  return [...buckets.entries()].map(([month, b]) => ({
    month,
    total: fromCents(b.total),
    byCategory: Object.fromEntries(Object.entries(b.byCategory).map(([k, v]) => [k, fromCents(v)])),
  }));
}

function csvCell(value) {
  let text = value == null ? '' : String(value);
  // Excel runs cells starting with these as formulas
  if (/^[=+\-@\t\r]/.test(text)) text = `'${text}`;
  if (/[",\n\r]/.test(text)) text = `"${text.replace(/"/g, '""')}"`;
  return text;
}

export function toCsv(expenses) {
  const header = ['Date', 'Category', 'Description', 'Payment method', 'Tags', 'Amount'];
  const lines = [header.join(',')];
  let totalCents = 0;

  for (const e of expenses) {
    totalCents += toCents(e.amount);
    lines.push([
      e.date.toISOString().slice(0, 10),
      e.category,
      e.description,
      e.paymentMethod,
      (e.tags ?? []).join(' '),
      fromCents(toCents(e.amount)).toFixed(2),
    ].map(csvCell).join(','));
  }
  lines.push(['', '', '', '', 'Total', fromCents(totalCents).toFixed(2)].join(','));
  return lines.join('\r\n') + '\r\n';
}
