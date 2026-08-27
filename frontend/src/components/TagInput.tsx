import { useId, useState } from "react";
import type { KeyboardEvent, ReactNode } from "react";
import { X } from "lucide-react";

interface TagInputProps {
  label: string;
  value: string[];
  onChange: (next: string[]) => void;
  hint?: string;
  error?: string | null;
  placeholder?: string;
  maxTagLength?: number;
}

export function TagInput({
  label,
  value,
  onChange,
  hint,
  error,
  placeholder = "Type and press Enter",
  maxTagLength = 40,
}: TagInputProps): ReactNode {
  const inputId = useId();
  const describedById = error
    ? `${inputId}-error`
    : hint
      ? `${inputId}-hint`
      : undefined;
  const [draft, setDraft] = useState("");

  function commit(raw: string): void {
    const trimmed = raw.trim();
    if (!trimmed) return;
    if (trimmed.length > maxTagLength) return;
    if (value.some((t) => t.toLowerCase() === trimmed.toLowerCase())) {
      setDraft("");
      return;
    }
    onChange([...value, trimmed]);
    setDraft("");
  }

  function removeAt(index: number): void {
    onChange(value.filter((_, i) => i !== index));
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>): void {
    if (event.key === "Enter" || event.key === ",") {
      event.preventDefault();
      commit(draft);
      return;
    }
    if (event.key === "Backspace" && !draft && value.length > 0) {
      onChange(value.slice(0, -1));
    }
  }

  const borderClass = error
    ? "border-warning focus-within:border-warning focus-within:ring-warning"
    : "border-border-subtle focus-within:border-primary focus-within:ring-primary " +
      "dark:border-border-subtle-dark";

  return (
    <div className="flex flex-col gap-1.5">
      <label
        htmlFor={inputId}
        className="text-body-sm font-medium text-text-primary dark:text-text-primary-dark"
      >
        {label}
      </label>
      <div
        className={
          "flex min-h-11 w-full flex-wrap items-center gap-1.5 rounded-lg border bg-white p-1.5 " +
          "focus-within:outline-none focus-within:ring-2 " +
          "transition-colors duration-150 " +
          "dark:bg-surface-elevated-dark " +
          borderClass
        }
      >
        {value.map((tag, index) => (
          <span
            key={`${tag}-${index}`}
            className="inline-flex items-center gap-1 rounded-md bg-primary/10 px-2 py-1 text-body-sm text-primary dark:bg-primary/20"
          >
            {tag}
            <button
              type="button"
              onClick={() => removeAt(index)}
              aria-label={`Remove ${tag}`}
              className="grid h-4 w-4 place-items-center rounded-sm hover:bg-primary/20"
            >
              <X className="h-3 w-3" aria-hidden />
            </button>
          </span>
        ))}
        <input
          id={inputId}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={handleKeyDown}
          onBlur={() => commit(draft)}
          placeholder={value.length === 0 ? placeholder : ""}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedById}
          data-testid="tag-input"
          className={
            "min-w-24 flex-1 bg-transparent px-2 py-1 text-body text-text-primary " +
            "placeholder:text-text-secondary/70 focus:outline-none " +
            "dark:text-text-primary-dark dark:placeholder:text-text-secondary-dark/70"
          }
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
