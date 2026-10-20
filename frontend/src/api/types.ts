export const CATEGORIES = [
  'FOOD',
  'GROCERIES',
  'RENT',
  'UTILITIES',
  'TRANSPORT',
  'SHOPPING',
  'HEALTH',
  'ENTERTAINMENT',
  'TRAVEL',
  'EDUCATION',
  'OTHER',
] as const;
export type Category = (typeof CATEGORIES)[number];

export const PAYMENT_METHODS = ['CARD', 'CASH', 'BANK_TRANSFER', 'OTHER'] as const;
export type PaymentMethod = (typeof PAYMENT_METHODS)[number];

export interface AuthResponse {
  token: string;
  userId: string;
  fullName: string;
  email: string;
}

export interface Expense {
  id: string;
  amount: number;
  category: Category;
  date: string; // yyyy-MM-dd
  description: string | null;
  paymentMethod: PaymentMethod;
  tags: string[];
}

export type ExpenseInput = Omit<Expense, 'id'>;

export interface ExpenseFilter {
  from?: string;
  to?: string;
  category?: Category | '';
  q?: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Budget {
  category: Category;
  monthlyLimit: number;
}

export type BudgetStatus = 'NO_BUDGET' | 'ON_TRACK' | 'WARNING' | 'OVER_BUDGET';

export interface CategorySummary {
  category: Category;
  spent: number;
  count: number;
  monthlyLimit: number | null;
  percentUsed: number | null;
  status: BudgetStatus;
}

export interface MonthlySummary {
  month: string;
  totalSpent: number;
  totalBudget: number;
  expenseCount: number;
  categories: CategorySummary[];
}

export interface TrendMonth {
  month: string;
  total: number;
  byCategory: Partial<Record<Category, number>>;
}
