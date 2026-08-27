import type { ReactNode } from "react";

interface LoadingStateProps {
  rows?: number;
}

export function LoadingState({ rows = 3 }: LoadingStateProps): ReactNode {
  return (
    <ul aria-busy="true" aria-label="Loading" className="flex flex-col gap-3">
      {Array.from({ length: rows }).map((_, i) => (
        <li
          key={i}
          className="h-24 animate-pulse rounded-lg border border-border-subtle bg-surface-card dark:border-border-subtle-dark dark:bg-surface-card-dark"
        />
      ))}
    </ul>
  );
}
