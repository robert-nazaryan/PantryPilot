import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description: string;
  action?: ReactNode;
}

export function EmptyState({ icon: Icon, title, description, action }: EmptyStateProps): ReactNode {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center text-center">
      <span className="grid h-12 w-12 place-items-center rounded-lg bg-white text-text-secondary dark:bg-surface-elevated-dark dark:text-text-secondary-dark">
        <Icon className="h-6 w-6" aria-hidden />
      </span>
      <h2 className="mt-4 text-h3 font-semibold text-text-primary dark:text-text-primary-dark">
        {title}
      </h2>
      <p className="mt-2 text-body text-text-secondary dark:text-text-secondary-dark">
        {description}
      </p>
      {action && <div className="mt-6">{action}</div>}
    </div>
  );
}
