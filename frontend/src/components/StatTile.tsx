import type { ReactNode } from "react";
import type { LucideIcon } from "lucide-react";

interface StatTileProps {
  icon: LucideIcon;
  label: string;
  value: number | undefined;
  loading?: boolean;
  "data-testid"?: string;
}

export function StatTile({
  icon: Icon,
  label,
  value,
  loading,
  "data-testid": dataTestId,
}: StatTileProps): ReactNode {
  return (
    <div
      data-testid={dataTestId}
      className={
        "flex items-center gap-3 rounded-lg bg-surface-card/60 p-3 md:p-4 " +
        "dark:bg-surface-card-dark/60"
      }
    >
      <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg text-text-secondary dark:text-text-secondary-dark">
        <Icon className="h-4 w-4" aria-hidden />
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-caption uppercase tracking-wide text-text-secondary dark:text-text-secondary-dark">
          {label}
        </p>
        {loading || value === undefined ? (
          <p
            aria-busy="true"
            className="mt-0.5 h-6 w-10 animate-pulse rounded bg-border-subtle dark:bg-border-subtle-dark"
          />
        ) : (
          <p
            className="text-h3 font-semibold text-text-primary dark:text-text-primary-dark"
            data-testid={dataTestId ? `${dataTestId}-value` : undefined}
          >
            {value}
          </p>
        )}
      </div>
    </div>
  );
}
