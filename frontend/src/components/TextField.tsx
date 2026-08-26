import { useId } from "react";
import type { InputHTMLAttributes, ReactNode } from "react";

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  hint?: string;
  error?: string | null;
}

export function TextField({
  label,
  hint,
  error,
  id,
  className = "",
  ...rest
}: TextFieldProps): ReactNode {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const describedById = error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined;

  const borderClass = error
    ? "border-warning focus:border-warning focus:ring-warning"
    : "border-border-subtle focus:border-primary focus:ring-primary";

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={inputId} className="text-body-sm font-medium text-text-primary">
        {label}
      </label>
      <input
        id={inputId}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedById}
        className={
          "min-h-11 w-full rounded-lg border bg-white px-3 text-body text-text-primary " +
          "placeholder:text-text-secondary/70 " +
          "focus:outline-none focus:ring-2 focus:ring-offset-0 " +
          "transition-colors duration-150 " +
          borderClass + " " + className
        }
        {...rest}
      />
      {error ? (
        <p id={`${inputId}-error`} className="text-body-sm text-warning">
          {error}
        </p>
      ) : hint ? (
        <p id={`${inputId}-hint`} className="text-body-sm text-text-secondary">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
