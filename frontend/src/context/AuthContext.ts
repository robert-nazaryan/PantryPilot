import { createContext } from "react";
import type { AuthUser, LoginRequest, RegisterRequest } from "../types/auth";

export type AuthStatus = "loading" | "authenticated" | "unauthenticated";

export interface AuthContextValue {
  status: AuthStatus;
  user: AuthUser | null;
  accessToken: string | null;
  login: (req: LoginRequest) => Promise<void>;
  register: (req: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  adoptSession: (accessToken: string, expiresIn: number, displayName?: string) => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
