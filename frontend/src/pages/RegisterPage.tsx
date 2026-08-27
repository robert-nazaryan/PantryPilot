import { useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AuthLayout } from "../components/AuthLayout";
import { Button } from "../components/Button";
import { GoogleButton } from "../components/GoogleButton";
import { OrDivider } from "../components/OrDivider";
import { TextField } from "../components/TextField";
import { useAuth } from "../context/useAuth";
import { ApiError } from "../api/client";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_PASSWORD_LENGTH = 8;
const MAX_PASSWORD_LENGTH = 100;
const MAX_DISPLAY_NAME_LENGTH = 100;

interface FieldErrors {
  email?: string;
  password?: string;
  displayName?: string;
}

export function RegisterPage(): ReactNode {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function validate(): FieldErrors {
    const errors: FieldErrors = {};
    if (!email.trim()) errors.email = "Email is required.";
    else if (!EMAIL_PATTERN.test(email.trim())) errors.email = "Enter a valid email address.";
    if (!password) errors.password = "Password is required.";
    else if (password.length < MIN_PASSWORD_LENGTH) {
      errors.password = `Password must be at least ${MIN_PASSWORD_LENGTH} characters.`;
    } else if (password.length > MAX_PASSWORD_LENGTH) {
      errors.password = `Password must be at most ${MAX_PASSWORD_LENGTH} characters.`;
    }
    if (displayName.length > MAX_DISPLAY_NAME_LENGTH) {
      errors.displayName = `Display name must be at most ${MAX_DISPLAY_NAME_LENGTH} characters.`;
    }
    return errors;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    setFormError(null);
    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    try {
      await register({
        email: email.trim(),
        password,
        displayName: displayName.trim() || undefined,
      });
      navigate("/", { replace: true });
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setFieldErrors({ email: "That email is already registered." });
      } else if (err instanceof ApiError && err.status === 400) {
        setFormError(err.message);
      } else {
        setFormError("Something went wrong. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Create your account"
      subtitle="Start tracking your pantry in under a minute."
      footer={
        <>
          Already have an account?{" "}
          <Link to="/login" className="font-medium text-primary hover:text-primary-hover">
            Sign in
          </Link>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <GoogleButton label="Sign up with Google" />
        <OrDivider />
      </div>
      <form onSubmit={handleSubmit} noValidate className="mt-4 flex flex-col gap-4">
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={fieldErrors.email}
          placeholder="you@example.com"
          required
        />
        <TextField
          label="Password"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={fieldErrors.password}
          hint={fieldErrors.password ? undefined : `At least ${MIN_PASSWORD_LENGTH} characters.`}
          required
        />
        <TextField
          label="Display name"
          type="text"
          autoComplete="nickname"
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
          error={fieldErrors.displayName}
          hint={fieldErrors.displayName ? undefined : "Optional — we'll use this to greet you."}
        />
        {formError && (
          <div
            role="alert"
            className="rounded-lg border border-warning/40 bg-warning/5 px-3 py-2 text-body-sm text-warning dark:bg-warning/10"
          >
            {formError}
          </div>
        )}
        <Button type="submit" loading={submitting} className="w-full">
          {submitting ? "Creating account…" : "Create account"}
        </Button>
      </form>
    </AuthLayout>
  );
}
