import { createContext, useState, useEffect, ReactNode } from "react";
import type { User } from "../types";
import { setAuthToken, getCurrentUser, login as apiLogin, logout as apiLogout, register as apiRegister } from "../api";

export interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string, inviteCode?: string) => Promise<void>;
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | null>(null);

const TOKEN_KEY = "auth_token";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(() => !!localStorage.getItem(TOKEN_KEY));

  useEffect(() => {
    const storedToken = localStorage.getItem(TOKEN_KEY);
    if (storedToken) {
      setAuthToken(storedToken);
      getCurrentUser()
        .then((u) => setUser(u))
        .finally(() => setLoading(false));
    }
  }, []);

  const login = async (username: string, password: string) => {
    const response = await apiLogin(username, password);
    localStorage.setItem(TOKEN_KEY, response.token);
    setAuthToken(response.token);
    setUser(response.user);
  };

  const register = async (username: string, password: string, inviteCode?: string) => {
    const response = await apiRegister(username, password, inviteCode);
    localStorage.setItem(TOKEN_KEY, response.token);
    setAuthToken(response.token);
    setUser(response.user);
  };

  const logout = async () => {
    await apiLogout();
    localStorage.removeItem(TOKEN_KEY);
    setAuthToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
