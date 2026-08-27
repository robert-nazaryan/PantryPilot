import type { ReactNode } from "react";
import { Sparkles } from "lucide-react";
import { useAuth } from "../context/useAuth";

export function DashboardPage(): ReactNode {
  const { user } = useAuth();
  const name = user?.displayName?.trim() || user?.email?.split("@")[0] || "friend";

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-h1 font-semibold text-text-primary dark:text-text-primary-dark">
          Welcome back, {name}.
        </h1>
        <p className="mt-1 text-body text-text-secondary dark:text-text-secondary-dark">
          Here&rsquo;s your kitchen at a glance.
        </p>
      </div>

      <div className="flex min-h-[240px] flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-border-subtle bg-surface-card/60 p-8 text-center dark:border-border-subtle-dark dark:bg-surface-card-dark/60">
        <Sparkles className="h-6 w-6 text-text-secondary dark:text-text-secondary-dark" aria-hidden />
        <p className="text-body text-text-secondary dark:text-text-secondary-dark">
          Your kitchen overview will show up here.
        </p>
        <p className="text-body-sm text-text-secondary/80 dark:text-text-secondary-dark/80">
          Expiring soon, recent recipes, and active shopping lists — coming next.
        </p>
      </div>
    </div>
  );
}
