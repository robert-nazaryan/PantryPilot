import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/useAuth";

interface ProtectedRouteProps {
  children: ReactNode;
}

export function ProtectedRoute({ children }: ProtectedRouteProps): ReactNode {
  const { status } = useAuth();
  const location = useLocation();

  if (status === "loading") return <FullPageLoading />;
  if (status === "unauthenticated") {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return children;
}

function FullPageLoading(): ReactNode {
  return (
    <div className="grid min-h-dvh place-items-center bg-surface-page dark:bg-surface-page-dark">
      <div className="flex flex-col items-center gap-3 text-text-secondary dark:text-text-secondary-dark">
        <div className="h-8 w-8 animate-pulse rounded-full bg-border-subtle dark:bg-border-subtle-dark" />
        <p className="text-body-sm">Loading…</p>
      </div>
    </div>
  );
}
