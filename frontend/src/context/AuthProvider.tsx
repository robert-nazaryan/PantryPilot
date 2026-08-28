import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import * as authApi from "../api/auth";
import { configureApiClient } from "../api/client";
import type { AuthUser, LoginRequest, RegisterRequest } from "../types/auth";
import { AuthContext } from "./AuthContext";
import type { AuthContextValue, AuthStatus } from "./AuthContext";
import { decodeJwt } from "./jwt";

const PROACTIVE_REFRESH_FRACTION = 0.8;
const MIN_REFRESH_DELAY_SECONDS = 30;
const DISPLAY_NAME_STORAGE_KEY = "pantrypilot-display-name";

function loadStoredDisplayName(email: string): string | undefined {
  if (typeof window === "undefined") return undefined;
  try {
    const raw = window.localStorage.getItem(DISPLAY_NAME_STORAGE_KEY);
    if (!raw) return undefined;
    const parsed = JSON.parse(raw) as { email?: string; displayName?: string };
    return parsed.email === email ? parsed.displayName : undefined;
  } catch {
    return undefined;
  }
}

function persistDisplayName(email: string, displayName: string | undefined): void {
  if (typeof window === "undefined") return;
  try {
    if (displayName) {
      window.localStorage.setItem(
        DISPLAY_NAME_STORAGE_KEY,
        JSON.stringify({ email, displayName }),
      );
    }
  } catch {
    // ignore quota / privacy-mode errors
  }
}

function clearStoredDisplayName(): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.removeItem(DISPLAY_NAME_STORAGE_KEY);
  } catch {
    // ignore
  }
}

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps): ReactNode {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");

  const accessTokenRef = useRef<string | null>(null);
  const refreshTokensRef = useRef<() => Promise<string | null>>(async () => null);
  const refreshInFlightRef = useRef<Promise<string | null> | null>(null);
  const refreshTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clearRefreshTimer = useCallback(() => {
    if (refreshTimerRef.current) {
      clearTimeout(refreshTimerRef.current);
      refreshTimerRef.current = null;
    }
  }, []);

  const applyTokens = useCallback((token: string, expiresInSeconds: number, displayName?: string) => {
    setAccessToken(token);
    accessTokenRef.current = token;
    const claims = decodeJwt(token);
    const email = claims?.email ?? "";
    setUser((prev) => {
      const resolved =
        displayName ??
        (prev && prev.email === email ? prev.displayName : undefined) ??
        loadStoredDisplayName(email);
      if (resolved) persistDisplayName(email, resolved);
      return { email, displayName: resolved };
    });
    setStatus("authenticated");

    clearRefreshTimer();
    const delaySeconds = Math.max(
      Math.floor(expiresInSeconds * PROACTIVE_REFRESH_FRACTION),
      MIN_REFRESH_DELAY_SECONDS,
    );
    refreshTimerRef.current = setTimeout(() => {
      void refreshTokensRef.current();
    }, delaySeconds * 1000);
  }, [clearRefreshTimer]);

  const clearSession = useCallback(() => {
    setAccessToken(null);
    accessTokenRef.current = null;
    setUser(null);
    setStatus("unauthenticated");
    clearRefreshTimer();
    clearStoredDisplayName();
  }, [clearRefreshTimer]);

  const refreshTokens = useCallback(async (): Promise<string | null> => {
    if (refreshInFlightRef.current) return refreshInFlightRef.current;
    const inflight = (async () => {
      try {
        const resp = await authApi.refresh();
        applyTokens(resp.accessToken, resp.expiresIn);
        return resp.accessToken;
      } catch {
        clearSession();
        return null;
      } finally {
        refreshInFlightRef.current = null;
      }
    })();
    refreshInFlightRef.current = inflight;
    return inflight;
  }, [applyTokens, clearSession]);

  useEffect(() => {
    refreshTokensRef.current = refreshTokens;
  }, [refreshTokens]);

  useEffect(() => {
    configureApiClient({
      getAccessToken: () => accessTokenRef.current,
      refreshTokens,
      onAuthFailure: clearSession,
    });
  }, [refreshTokens, clearSession]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const token = await refreshTokens();
      if (cancelled) return;
      if (!token) setStatus("unauthenticated");
    })();
    return () => {
      cancelled = true;
      clearRefreshTimer();
    };
  }, [refreshTokens, clearRefreshTimer]);

  const login = useCallback(async (req: LoginRequest) => {
    const resp = await authApi.login(req);
    applyTokens(resp.accessToken, resp.expiresIn);
  }, [applyTokens]);

  const register = useCallback(async (req: RegisterRequest) => {
    const resp = await authApi.register(req);
    applyTokens(resp.accessToken, resp.expiresIn, req.displayName);
  }, [applyTokens]);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // logout is idempotent server-side; ignore transport errors
    }
    clearSession();
  }, [clearSession]);

  const adoptSession = useCallback((token: string, expiresIn: number) => {
    applyTokens(token, expiresIn);
  }, [applyTokens]);

  const value = useMemo<AuthContextValue>(() => ({
    status,
    user,
    accessToken,
    login,
    register,
    logout,
    adoptSession,
  }), [status, user, accessToken, login, register, logout, adoptSession]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
