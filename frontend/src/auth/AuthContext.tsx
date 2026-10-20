import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { authApi, loadSession, saveSession } from '../api/client';
import type { AuthResponse } from '../api/types';

interface AuthState {
  user: AuthResponse | null;
  login: (email: string, password: string) => Promise<void>;
  register: (fullName: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(() => loadSession());

  const start = useCallback((session: AuthResponse) => {
    saveSession(session);
    setUser(session);
  }, []);

  const value = useMemo<AuthState>(
    () => ({
      user,
      login: async (email, password) => start(await authApi.login(email, password)),
      register: async (fullName, email, password) => start(await authApi.register(fullName, email, password)),
      logout: () => {
        saveSession(null);
        setUser(null);
      },
    }),
    [user, start],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
