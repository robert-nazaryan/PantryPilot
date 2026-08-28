import type { ReactNode } from "react";
import { CalendarClock, CircleAlert } from "lucide-react";
import type { PantryItemResponse } from "../types/pantry";

interface DashboardExpiryCardProps {
  item: PantryItemResponse;
  onClick: (item: PantryItemResponse) => void;
}

const EXPIRING_SOON_DAYS = 7;

export function DashboardExpiryCard({
  item,
  onClick,
}: DashboardExpiryCardProps): ReactNode {
  const expiry = getExpiryStatus(item.expiryDate);
  const isWarning = expiry?.tone === "warning";
  const isExpired = expiry?.tone === "expired";

  return (
    <button
      type="button"
      onClick={() => onClick(item)}
      data-testid={`dashboard-expiry-card-${item.id}`}
      className={
        "group flex w-64 shrink-0 flex-col gap-2 rounded-lg border p-4 text-left " +
        "transition-colors duration-150 md:w-auto " +
        "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary " +
        (isExpired
          ? "border-warning/50 bg-warning/5 hover:bg-warning/10 dark:border-warning/40 dark:bg-warning/10 dark:hover:bg-warning/20"
          : isWarning
            ? "border-warning/30 bg-white hover:border-warning/50 hover:bg-warning/5 dark:border-warning/30 dark:bg-surface-card-dark dark:hover:bg-warning/10"
            : "border-border-subtle bg-white hover:border-primary/40 hover:bg-surface-card dark:border-border-subtle-dark dark:bg-surface-card-dark dark:hover:bg-surface-elevated-dark")
      }
    >
      <div className="flex items-start justify-between gap-2">
        <h3 className="min-w-0 flex-1 truncate text-body font-semibold text-text-primary dark:text-text-primary-dark">
          {item.name}
        </h3>
      </div>
      <p className="text-body-sm text-text-secondary dark:text-text-secondary-dark">
        {formatQuantity(item.quantity)} {item.unit}
      </p>
      {expiry && (
        <p
          className={
            "flex items-center gap-1.5 text-body-sm " +
            (isWarning || isExpired
              ? "text-warning"
              : "text-text-secondary dark:text-text-secondary-dark")
          }
        >
          {isWarning || isExpired ? (
            <CircleAlert className="h-3.5 w-3.5 shrink-0" aria-hidden />
          ) : (
            <CalendarClock className="h-3.5 w-3.5 shrink-0" aria-hidden />
          )}
          <span className="truncate">{expiry.label}</span>
        </p>
      )}
    </button>
  );
}

function formatQuantity(value: number): string {
  const rounded = Math.round(value * 1000) / 1000;
  return rounded.toString();
}

interface ExpiryStatus {
  tone: "expired" | "warning" | "normal";
  label: string;
}

function getExpiryStatus(expiryDate: string | null): ExpiryStatus | null {
  if (!expiryDate) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const expiry = new Date(`${expiryDate}T00:00:00`);
  const days = Math.round((expiry.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  if (days < 0) {
    const abs = Math.abs(days);
    return { tone: "expired", label: `Expired ${abs} day${abs === 1 ? "" : "s"} ago` };
  }
  if (days === 0) return { tone: "warning", label: "Expires today" };
  if (days <= EXPIRING_SOON_DAYS) {
    return { tone: "warning", label: `Expires in ${days} day${days === 1 ? "" : "s"}` };
  }
  return {
    tone: "normal",
    label: `Expires ${expiry.toLocaleDateString(undefined, { month: "short", day: "numeric" })}`,
  };
}
