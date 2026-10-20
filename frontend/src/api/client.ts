import axios, { AxiosError } from 'axios';
import type {
  AuthResponse,
  Budget,
  Category,
  Expense,
  ExpenseFilter,
  ExpenseInput,
  MonthlySummary,
  Page,
  TrendMonth,
} from './types';

const SESSION_KEY = 'expense-tracker.session';

export function saveSession(session: AuthResponse | null) {
  if (session) localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  else localStorage.removeItem(SESSION_KEY);
}

export function loadSession(): AuthResponse | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    return raw ? (JSON.parse(raw) as AuthResponse) : null;
  } catch {
    return null;
  }
}

// expense-api is served under /api and report-service under /reports (Vite proxy in dev, nginx in Docker)
const http = axios.create();

http.interceptors.request.use((config) => {
  const session = loadSession();
  if (session) config.headers.Authorization = `Bearer ${session.token}`;
  return config;
});

http.interceptors.response.use(undefined, (error: AxiosError) => {
  // token expired or was revoked: drop it and send the user to the login page
  if (error.response?.status === 401 && loadSession() && !error.config?.url?.includes('/auth/')) {
    saveSession(null);
    window.location.assign('/login');
  }
  return Promise.reject(error);
});

export function errorMessage(err: unknown): string {
  if (err instanceof AxiosError) {
    const data = err.response?.data as { detail?: string; errors?: Record<string, string> } | undefined;
    if (data?.errors) {
      return Object.entries(data.errors)
        .map(([field, msg]) => `${field} ${msg}`)
        .join(', ');
    }
    if (data?.detail) return data.detail;
    if (!err.response) return 'Cannot reach the server. Is the API running?';
  }
  return 'Something went wrong. Please try again.';
}

export const authApi = {
  login: (email: string, password: string) =>
    http.post<AuthResponse>('/api/auth/login', { email, password }).then((r) => r.data),
  register: (fullName: string, email: string, password: string) =>
    http.post<AuthResponse>('/api/auth/register', { fullName, email, password }).then((r) => r.data),
};

export const expenseApi = {
  list: (filter: ExpenseFilter, page = 0, size = 15) =>
    http
      .get<Page<Expense>>('/api/expenses', {
        params: {
          from: filter.from || undefined,
          to: filter.to || undefined,
          category: filter.category || undefined,
          q: filter.q?.trim() || undefined,
          page,
          size,
        },
      })
      .then((r) => r.data),
  create: (input: ExpenseInput) => http.post<Expense>('/api/expenses', input).then((r) => r.data),
  update: (id: string, input: ExpenseInput) => http.put<Expense>(`/api/expenses/${id}`, input).then((r) => r.data),
  remove: (id: string) => http.delete(`/api/expenses/${id}`),
};

export const budgetApi = {
  list: () => http.get<Budget[]>('/api/budgets').then((r) => r.data),
  set: (category: Category, monthlyLimit: number) =>
    http.put<Budget>(`/api/budgets/${category}`, { monthlyLimit }).then((r) => r.data),
  remove: (category: Category) => http.delete(`/api/budgets/${category}`),
};

export const summaryApi = {
  monthly: (month: string) => http.get<MonthlySummary>('/api/summary', { params: { month } }).then((r) => r.data),
};

export const reportApi = {
  trend: (months = 6) =>
    http.get<{ months: TrendMonth[] }>('/reports/trend', { params: { months } }).then((r) => r.data.months),

  /** Fetches the CSV with the auth header and hands it to the browser as a download. */
  async downloadMonthlyCsv(month: string) {
    const res = await http.get<Blob>('/reports/monthly.csv', { params: { month }, responseType: 'blob' });
    const url = URL.createObjectURL(res.data);
    const link = document.createElement('a');
    link.href = url;
    link.download = `expenses-${month}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  },
};
