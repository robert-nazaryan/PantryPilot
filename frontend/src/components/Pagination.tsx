import type { ReactNode } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

interface PaginationProps {
  page: number;
  totalPages: number;
  onChange: (nextPage: number) => void;
}

const buttonClass =
  "grid h-11 w-11 place-items-center rounded-lg border border-border-subtle text-text-secondary " +
  "transition-colors duration-150 hover:enabled:bg-surface-card hover:enabled:text-text-primary " +
  "disabled:opacity-40 " +
  "dark:border-border-subtle-dark dark:text-text-secondary-dark " +
  "dark:hover:enabled:bg-surface-elevated-dark dark:hover:enabled:text-text-primary-dark";

export function Pagination({ page, totalPages, onChange }: PaginationProps): ReactNode {
  if (totalPages <= 1) return null;
  const canPrev = page > 0;
  const canNext = page < totalPages - 1;

  return (
    <nav
      aria-label="Pagination"
      className="flex items-center justify-between gap-3 pt-2"
    >
      <button
        type="button"
        onClick={() => canPrev && onChange(page - 1)}
        disabled={!canPrev}
        className={buttonClass}
        aria-label="Previous page"
      >
        <ChevronLeft className="h-4 w-4" aria-hidden />
      </button>
      <span className="text-body-sm text-text-secondary dark:text-text-secondary-dark">
        Page {page + 1} of {totalPages}
      </span>
      <button
        type="button"
        onClick={() => canNext && onChange(page + 1)}
        disabled={!canNext}
        className={buttonClass}
        aria-label="Next page"
      >
        <ChevronRight className="h-4 w-4" aria-hidden />
      </button>
    </nav>
  );
}
