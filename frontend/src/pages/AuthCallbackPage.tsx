import { useEffect, useRef } from "react";
import type { ReactNode } from "react";
import { Navigate, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/useAuth";

export function AuthCallbackPage(): ReactNode {
  const { adoptSession, status } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const adoptedRef = useRef(false);

  const accessToken = params.get("accessToken");
  const expiresInRaw = params.get("expiresIn");
  const expiresIn = expiresInRaw ? Number(expiresInRaw) : NaN;
  const paramsValid = accessToken !== null && accessToken.length > 0 && Number.isFinite(expiresIn) && expiresIn > 0;

  useEffect(() => {
    if (!paramsValid || adoptedRef.current) return;
    adoptedRef.current = true;
    adoptSession(accessToken as string, expiresIn);
    navigate("/dashboard", { replace: true });
  }, [paramsValid, accessToken, expiresIn, adoptSession, navigate]);

  if (!paramsValid) {
    return <Navigate to="/login?error=oauth_callback_missing_token" replace />;
  }

  return (
    <div className="grid min-h-dvh place-items-center bg-surface-page dark:bg-surface-page-dark">
      <div className="flex flex-col items-center gap-3 text-text-secondary dark:text-text-secondary-dark">
        <div className="h-8 w-8 animate-pulse rounded-full bg-border-subtle dark:bg-border-subtle-dark" />
        <p className="text-body-sm">
          {status === "loading" ? "Restoring your session…" : "Signing you in…"}
        </p>
      </div>
    </div>
  );
}
