import { useEffect, useId, useMemo, useRef, useState } from "react";
import type { KeyboardEvent, ReactNode } from "react";
import type { LucideIcon } from "lucide-react";
import { ChevronDown } from "lucide-react";

interface ComboboxProps {
  label: string;
  value: string;
  onChange: (next: string) => void;
  options: string[];
  icon?: LucideIcon;
  placeholder?: string;
  hint?: string;
  error?: string | null;
  required?: boolean;
  maxLength?: number;
  "data-testid"?: string;
}

export function Combobox({
  label,
  value,
  onChange,
  options,
  icon: Icon,
  placeholder,
  hint,
  error,
  required,
  maxLength,
  "data-testid": dataTestId,
}: ComboboxProps): ReactNode {
  const inputId = useId();
  const listboxId = `${inputId}-listbox`;
  const describedById = error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined;
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const rootRef = useRef<HTMLDivElement | null>(null);

  const filtered = useMemo(() => {
    const q = value.trim().toLowerCase();
    if (!q) return options;
    return options.filter((o) => o.toLowerCase().includes(q));
  }, [value, options]);

  useEffect(() => {
    if (!open) return;
    function onDocDown(event: MouseEvent) {
      if (!rootRef.current) return;
      if (!rootRef.current.contains(event.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", onDocDown);
    return () => document.removeEventListener("mousedown", onDocDown);
  }, [open]);

  const clampedActive =
    active >= 0 && filtered.length > 0
      ? Math.min(active, filtered.length - 1)
      : -1;

  function commit(next: string): void {
    onChange(next);
    setOpen(false);
    setActive(-1);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>): void {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      if (!open) setOpen(true);
      setActive((i) => Math.min(i + 1, filtered.length - 1));
      return;
    }
    if (event.key === "ArrowUp") {
      event.preventDefault();
      setActive((i) => Math.max(i - 1, 0));
      return;
    }
    if (event.key === "Enter") {
      if (open && clampedActive >= 0) {
        event.preventDefault();
        commit(filtered[clampedActive]);
      }
      return;
    }
    if (event.key === "Escape") {
      if (open) {
        event.preventDefault();
        event.nativeEvent.stopImmediatePropagation();
        setOpen(false);
        setActive(-1);
      }
    }
  }

  const borderClass = error
    ? "border-warning focus-within:border-warning focus-within:ring-warning"
    : "border-border-subtle focus-within:border-primary focus-within:ring-primary " +
      "dark:border-border-subtle-dark";

  const iconTone = error
    ? "text-warning"
    : "text-text-secondary dark:text-text-secondary-dark";

  return (
    <div className="flex flex-col gap-1.5" ref={rootRef}>
      <label
        htmlFor={inputId}
        className="text-body-sm font-medium text-text-primary dark:text-text-primary-dark"
      >
        {label}
      </label>
      <div
        className={
          "relative flex min-h-11 w-full items-center rounded-lg border bg-white " +
          "focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-0 " +
          "transition-colors duration-150 " +
          "dark:bg-surface-elevated-dark " +
          borderClass
        }
      >
        {Icon && (
          <Icon
            className={"pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 " + iconTone}
            aria-hidden
          />
        )}
        <input
          id={inputId}
          type="text"
          role="combobox"
          aria-expanded={open}
          aria-autocomplete="list"
          aria-controls={listboxId}
          aria-activedescendant={
            open && clampedActive >= 0 ? `${listboxId}-opt-${clampedActive}` : undefined
          }
          value={value}
          onChange={(e) => {
            const v = e.target.value;
            onChange(v);
            if (!open) setOpen(true);
            setActive(-1);
          }}
          onFocus={() => setOpen(true)}
          onClick={() => setOpen(true)}
          onBlur={() => {
            setOpen(false);
            setActive(-1);
          }}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          required={required}
          maxLength={maxLength}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedById}
          data-testid={dataTestId}
          autoComplete="off"
          className={
            "w-full bg-transparent text-body text-text-primary " +
            (Icon ? "pl-9 pr-9 " : "pl-3 pr-9 ") +
            "min-h-11 rounded-lg " +
            "placeholder:text-text-secondary/70 focus:outline-none " +
            "dark:text-text-primary-dark dark:placeholder:text-text-secondary-dark/70"
          }
        />
        <button
          type="button"
          tabIndex={-1}
          onMouseDown={(e) => {
            e.preventDefault();
            setOpen((v) => !v);
          }}
          aria-label={open ? "Close suggestions" : "Show suggestions"}
          className="absolute right-2 top-1/2 grid h-8 w-8 -translate-y-1/2 place-items-center rounded-md text-text-secondary transition-colors duration-150 hover:bg-surface-card dark:text-text-secondary-dark dark:hover:bg-surface-card-dark"
        >
          <ChevronDown
            className={"h-4 w-4 transition-transform duration-150 " + (open ? "rotate-180" : "")}
            aria-hidden
          />
        </button>
        {open && filtered.length > 0 && (
          <ul
            id={listboxId}
            role="listbox"
            className="absolute left-0 right-0 top-full z-30 mt-1 max-h-56 overflow-auto rounded-lg border border-border-subtle bg-white py-1 shadow-lg dark:border-border-subtle-dark dark:bg-surface-card-dark"
          >
            {filtered.map((opt, i) => {
              const isActive = i === clampedActive;
              return (
                <li
                  key={opt}
                  id={`${listboxId}-opt-${i}`}
                  role="option"
                  aria-selected={opt === value}
                  onMouseEnter={() => setActive(i)}
                  onMouseDown={(e) => {
                    e.preventDefault();
                    commit(opt);
                  }}
                  className={
                    "cursor-pointer px-3 py-2 text-body-sm " +
                    (isActive
                      ? "bg-primary/10 text-primary dark:bg-primary/20"
                      : "text-text-primary hover:bg-surface-card dark:text-text-primary-dark dark:hover:bg-surface-elevated-dark")
                  }
                >
                  {opt}
                </li>
              );
            })}
          </ul>
        )}
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
