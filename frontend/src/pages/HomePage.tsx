import { useState } from "react";
import type { ReactNode } from "react";
import { LogOut, PackageOpen, Utensils } from "lucide-react";
import { Button } from "../components/Button";
import { useAuth } from "../context/useAuth";

export function HomePage(): ReactNode {
  const { user, logout } = useAuth();
  const [signingOut, setSigningOut] = useState(false);

  const greetingName = user?.displayName?.trim() || user?.email || "there";

  async function handleLogout() {
    setSigningOut(true);
    try {
      await logout();
    } finally {
      setSigningOut(false);
    }
  }

  return (
    <div className="min-h-dvh bg-surface-page">
      <header className="border-b border-border-subtle bg-white">
        <div className="mx-auto flex max-w-[1200px] items-center justify-between px-4 py-3 md:px-6">
          <div className="flex items-center gap-2">
            <span className="grid h-9 w-9 place-items-center rounded-lg bg-primary text-white">
              <Utensils className="h-5 w-5" aria-hidden />
            </span>
            <span className="text-body font-semibold text-text-primary">PantryPilot</span>
          </div>
          <Button variant="ghost" onClick={handleLogout} loading={signingOut}>
            <LogOut className="h-4 w-4" aria-hidden />
            Sign out
          </Button>
        </div>
      </header>

      <main className="mx-auto max-w-[1200px] px-4 py-8 md:px-6 md:py-12">
        <h1 className="text-h1 font-semibold text-text-primary">
          Welcome, {greetingName}.
        </h1>
        <p className="mt-2 text-body text-text-secondary">
          You&rsquo;re signed in. This is where your pantry will live.
        </p>

        <section className="mt-8 rounded-lg border border-border-subtle bg-surface-card p-6 md:p-8">
          <div className="mx-auto flex max-w-md flex-col items-center text-center">
            <span className="grid h-12 w-12 place-items-center rounded-lg bg-white text-text-secondary">
              <PackageOpen className="h-6 w-6" aria-hidden />
            </span>
            <h2 className="mt-4 text-h3 font-semibold text-text-primary">
              Your pantry is empty
            </h2>
            <p className="mt-2 text-body text-text-secondary">
              Pantry, recipes, and shopping lists arrive in the next release. For now, this page
              just proves the auth loop works.
            </p>
          </div>
        </section>
      </main>
    </div>
  );
}
