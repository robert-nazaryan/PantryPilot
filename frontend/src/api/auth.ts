import { apiFetch } from "./client";
import type { AuthResponse, LoginRequest, RegisterRequest } from "../types/auth";

export function login(body: LoginRequest): Promise<AuthResponse> {
  return apiFetch<AuthResponse>("/api/auth/login", {
    method: "POST",
    body,
    authenticated: false,
  });
}

export function register(body: RegisterRequest): Promise<AuthResponse> {
  return apiFetch<AuthResponse>("/api/auth/register", {
    method: "POST",
    body,
    authenticated: false,
  });
}

export function refresh(): Promise<AuthResponse> {
  return apiFetch<AuthResponse>("/api/auth/refresh", {
    method: "POST",
    authenticated: false,
  });
}

export function logout(): Promise<void> {
  return apiFetch<void>("/api/auth/logout", {
    method: "POST",
    authenticated: false,
  });
}
