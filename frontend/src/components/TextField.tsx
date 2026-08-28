import { useId } from "react";
import type { InputHTMLAttributes, ReactNode } from "react";
import type { LucideIcon } from "lucide-react";

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  hint?: string;
  error?: string | null;
  icon?: LucideIcon;
}

export function TextField({
  label,
  hint,
  error,
  icon: Icon,
  id,
  className = "",
  ...rest
}: TextFieldProps): ReactNode {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const describedById = error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined;

  const borderClass = error
    ? "border-warning focus:border-warning focus:ring-warning"
    : "border-border-subtle focus:border-primary focus:ring-primary " +
      "dark:border-border-subtle-dark dark:focus:border-primary";

  const iconTone = error
    ? "text-warning"
    : "text-text-secondary dark:text-text-secondary-dark";

  return (
    <div className="flex flex-col gap-1.5">
      <label
        htmlFor={inputId}
        className="text-body-sm font-medium text-text-primary dark:text-text-primary-dark"
      >
        {label}
      </label>
      <div className="relative">
        {Icon && (
          <Icon
            className={"pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 " + iconTone}
            aria-hidden
          />
        )}
        <input
          id={inputId}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedById}
          className={
            "min-h-11 w-full rounded-lg border bg-white text-body text-text-primary " +
            (Icon ? "pl-9 pr-3 " : "px-3 ") +
            "placeholder:text-text-secondary/70 " +
            "focus:outline-none focus:ring-2 focus:ring-offset-0 " +
            "transition-colors duration-150 " +
            "dark:bg-surface-elevated-dark dark:text-text-primary-dark " +
            "dark:placeholder:text-text-secondary-dark/70 " +
            "[color-scheme:light] dark:[color-scheme:dark] " +
            borderClass + " " + className
          }
          {...rest}
        />
      </div>
      {error ? (
        <p id={`${inputId}-error`} className="text-body-sm text-warning">
          {error}
        </p>
      ) : hint ? (
        <p
          id={`${inputId}-hint`}
          className="text-body-sm text-text-secondary dark:text-text-secondary-dark"
        >
          {hint}
        </p>
      ) : null}
    </div>
  );
}
