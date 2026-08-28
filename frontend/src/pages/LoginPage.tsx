import { useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Lock, Mail } from "lucide-react";
import { AuthLayout } from "../components/AuthLayout";
import { Button } from "../components/Button";
import { GoogleButton } from "../components/GoogleButton";
import { OrDivider } from "../components/OrDivider";
import { TextField } from "../components/TextField";
import { useAuth } from "../context/useAuth";
import { ApiError } from "../api/client";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

interface FieldErrors {
  email?: string;
  password?: string;
}

const OAUTH_ERROR_MESSAGES: Record<string, string> = {
  google_login_failed: "Google sign-in didn't complete. Please try again.",
  oauth_callback_missing_token: "We couldn't complete the sign-in. Please try again.",
};

export function LoginPage(): ReactNode {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const oauthError = searchParams.get("error");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(
    oauthError ? (OAUTH_ERROR_MESSAGES[oauthError] ?? null) : null,
  );
  const [submitting, setSubmitting] = useState(false);

  function validate(): FieldErrors {
    const errors: FieldErrors = {};
    if (!email.trim()) errors.email = "Email is required.";
    else if (!EMAIL_PATTERN.test(email.trim())) errors.email = "Enter a valid email address.";
    if (!password) errors.password = "Password is required.";
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
      await login({ email: email.trim(), password });
      navigate("/", { replace: true });
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setFormError("Email or password is incorrect.");
      } else {
        setFormError("Something went wrong. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Sign in"
      subtitle="Welcome back to your kitchen."
      footer={
        <>
          New here?{" "}
          <Link to="/register" className="font-medium text-primary hover:text-primary-hover">
            Create an account
          </Link>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <GoogleButton label="Continue with Google" />
        <OrDivider />
      </div>
      <form onSubmit={handleSubmit} noValidate className="mt-4 flex flex-col gap-4">
        <TextField
          label="Email"
          icon={Mail}
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
          icon={Lock}
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={fieldErrors.password}
          required
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
          {submitting ? "Signing in…" : "Sign in"}
        </Button>
      </form>
    </AuthLayout>
  );
}
