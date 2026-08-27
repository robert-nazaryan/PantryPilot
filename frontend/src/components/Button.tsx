import type { ButtonHTMLAttributes, ReactNode } from "react";
import { Loader2 } from "lucide-react";

type Variant = "primary" | "secondary" | "ghost";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  loading?: boolean;
  children: ReactNode;
}

const base =
  "inline-flex items-center justify-center gap-2 rounded-lg text-body-sm font-medium " +
  "min-h-11 px-4 transition-colors duration-150 " +
  "disabled:cursor-not-allowed disabled:opacity-60 " +
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const variants: Record<Variant, string> = {
  primary:
    "bg-primary text-white hover:bg-primary-hover active:bg-primary-hover",
  secondary:
    "bg-surface-card text-text-primary border border-border-subtle hover:bg-border-subtle " +
    "dark:bg-surface-elevated-dark dark:text-text-primary-dark dark:border-border-subtle-dark " +
    "dark:hover:bg-border-subtle-dark",
  ghost:
    "bg-transparent text-text-secondary hover:text-text-primary hover:bg-surface-card " +
    "dark:text-text-secondary-dark dark:hover:text-text-primary-dark " +
    "dark:hover:bg-surface-elevated-dark",
};

export function Button({
  variant = "primary",
  loading = false,
  disabled,
  children,
  className = "",
  type = "button",
  ...rest
}: ButtonProps): ReactNode {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      className={`${base} ${variants[variant]} ${className}`}
      {...rest}
    >
      {loading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden />}
      {children}
    </button>
  );
}
