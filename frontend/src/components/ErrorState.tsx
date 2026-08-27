import type { ReactNode } from "react";
import { AlertTriangle } from "lucide-react";
import { Button } from "./Button";

interface ErrorStateProps {
  title?: string;
  description?: string;
  onRetry?: () => void;
}

export function ErrorState({
  title = "Something went wrong",
  description = "We couldn't load this right now. Please try again.",
  onRetry,
}: ErrorStateProps): ReactNode {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center text-center">
      <span className="grid h-12 w-12 place-items-center rounded-lg bg-warning/10 text-warning dark:bg-warning/20">
        <AlertTriangle className="h-6 w-6" aria-hidden />
      </span>
      <h2 className="mt-4 text-h3 font-semibold text-text-primary dark:text-text-primary-dark">
        {title}
      </h2>
      <p className="mt-2 text-body text-text-secondary dark:text-text-secondary-dark">
        {description}
      </p>
      {onRetry && (
        <div className="mt-6">
          <Button onClick={onRetry}>Try again</Button>
        </div>
      )}
    </div>
  );
}
