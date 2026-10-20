import { test } from 'node:test';
import assert from 'node:assert/strict';
import { buildTrend, lastMonths, monthRange, parseMonth, toCsv } from '../src/reports.js';

const day = (iso) => new Date(`${iso}T00:00:00Z`);

test('parseMonth accepts yyyy-MM only', () => {
  assert.deepEqual(parseMonth('2026-10'), { year: 2026, month: 10 });
  assert.equal(parseMonth('2026-13'), null);
  assert.equal(parseMonth('2026-1'), null);
  assert.equal(parseMonth('October'), null);
  assert.equal(parseMonth(undefined), null);
});

test('monthRange covers the whole month in UTC', () => {
  const { from, to } = monthRange({ year: 2026, month: 12 });
  assert.equal(from.toISOString(), '2026-12-01T00:00:00.000Z');
  assert.equal(to.toISOString(), '2027-01-01T00:00:00.000Z');
});

test('lastMonths crosses the year boundary', () => {
  const months = lastMonths(3, new Date('2026-02-10T12:00:00Z'));
  assert.deepEqual(months, [
    { year: 2025, month: 12 },
    { year: 2026, month: 1 },
    { year: 2026, month: 2 },
  ]);
});

test('buildTrend groups by month and category, including empty months', () => {
  const months = lastMonths(3, new Date('2026-10-05T00:00:00Z'));
  const trend = buildTrend(
    [
      { date: day('2026-08-03'), category: 'FOOD', amount: 0.1 },
      { date: day('2026-08-20'), category: 'FOOD', amount: 0.2 },
      { date: day('2026-08-21'), category: 'RENT', amount: 1350 },
      { date: day('2026-10-01'), category: 'TRANSPORT', amount: 42.5 },
      { date: day('2026-05-01'), category: 'FOOD', amount: 99 }, // outside the window
    ],
    months,
  );

  assert.deepEqual(trend, [
    { month: '2026-08', total: 1350.3, byCategory: { FOOD: 0.3, RENT: 1350 } },
    { month: '2026-09', total: 0, byCategory: {} },
    { month: '2026-10', total: 42.5, byCategory: { TRANSPORT: 42.5 } },
  ]);
});

test('toCsv escapes commas and quotes and adds a total row', () => {
  const csv = toCsv([
    { date: day('2026-10-02'), category: 'FOOD', description: 'Pizza, wings', paymentMethod: 'CARD', tags: ['party'], amount: 32.1 },
    { date: day('2026-10-03'), category: 'SHOPPING', description: 'The "good" headphones', paymentMethod: 'CARD', tags: [], amount: 79.99 },
  ]);
  const lines = csv.trim().split('\r\n');

  assert.equal(lines[0], 'Date,Category,Description,Payment method,Tags,Amount');
  assert.equal(lines[1], '2026-10-02,FOOD,"Pizza, wings",CARD,party,32.10');
  assert.equal(lines[2], '2026-10-03,SHOPPING,"The ""good"" headphones",CARD,,79.99');
  assert.equal(lines[3], ',,,,Total,112.09');
});

test('toCsv neutralizes values that Excel would run as formulas', () => {
  const csv = toCsv([
    { date: day('2026-10-02'), category: 'OTHER', description: '=HYPERLINK("http://x")', paymentMethod: 'CASH', amount: 1 },
  ]);
  assert.match(csv, /,"'=HYPERLINK\(""http:\/\/x""\)",/);
});
