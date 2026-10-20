import { describe, expect, it } from 'vitest';
import { barWidth, formatMoney, humanize, monthLabel, statusText, toMonthValue } from './format';

describe('formatMoney', () => {
  it('formats as US dollars with cents', () => {
    expect(formatMoney(1350)).toBe('$1,350.00');
    expect(formatMoney(4.5)).toBe('$4.50');
  });
});

describe('humanize', () => {
  it('turns enum names into labels', () => {
    expect(humanize('BANK_TRANSFER')).toBe('Bank transfer');
    expect(humanize('FOOD')).toBe('Food');
  });
});

describe('month helpers', () => {
  it('builds the yyyy-MM value for input[type=month]', () => {
    expect(toMonthValue(new Date(2026, 0, 31))).toBe('2026-01');
    expect(toMonthValue(new Date(2026, 9, 1))).toBe('2026-10');
  });

  it('shows a short month label', () => {
    expect(monthLabel('2026-10')).toBe('Oct 2026');
  });
});

describe('budget bar', () => {
  it('caps the bar at 100% and handles missing budgets', () => {
    expect(barWidth(45)).toBe(45);
    expect(barWidth(130)).toBe(100);
    expect(barWidth(null)).toBe(0);
  });

  it('describes the status', () => {
    expect(statusText('OVER_BUDGET', 112)).toBe('Over budget (112%)');
    expect(statusText('WARNING', 85)).toBe('85% used');
    expect(statusText('NO_BUDGET', null)).toBe('No budget');
  });
});
