import type { ReactNode } from "react";

export function OrDivider(): ReactNode {
  return (
    <div className="flex items-center gap-3" role="separator">
      <span className="h-px flex-1 bg-border-subtle dark:bg-border-subtle-dark" aria-hidden />
      <span className="text-caption uppercase tracking-wide text-text-secondary dark:text-text-secondary-dark">
        or
      </span>
      <span className="h-px flex-1 bg-border-subtle dark:bg-border-subtle-dark" aria-hidden />
    </div>
  );
}
