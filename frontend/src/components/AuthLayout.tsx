import type { ReactNode } from "react";
import { Utensils } from "lucide-react";

interface AuthLayoutProps {
  title: string;
  subtitle: string;
  children: ReactNode;
  footer: ReactNode;
}

export function AuthLayout({ title, subtitle, children, footer }: AuthLayoutProps): ReactNode {
  return (
    <div className="min-h-dvh bg-surface-page dark:bg-surface-page-dark">
      <div className="mx-auto flex min-h-dvh w-full max-w-[1200px] flex-col md:flex-row">
        <aside className="hidden md:flex md:w-1/2 md:flex-col md:bg-surface-card md:p-12 dark:md:bg-surface-card-dark">
          <div className="flex items-center gap-2">
            <span className="grid h-9 w-9 place-items-center rounded-lg bg-primary text-white">
              <Utensils className="h-5 w-5" aria-hidden />
            </span>
            <span className="text-body font-semibold text-text-primary dark:text-text-primary-dark">
              PantryPilot
            </span>
          </div>
          <div className="mt-16">
            <h2 className="text-h2 font-semibold text-text-primary dark:text-text-primary-dark">
              Know what&rsquo;s in your kitchen.
            </h2>
            <p className="mt-3 max-w-md text-body text-text-secondary dark:text-text-secondary-dark">
              Track your pantry, plan meals from what you already have, and stop wasting groceries.
            </p>
            <ul className="mt-8 flex flex-col gap-3 text-body-sm text-text-secondary dark:text-text-secondary-dark">
              <li className="flex items-start gap-2">
                <span className="mt-1.5 h-1.5 w-1.5 rounded-full bg-primary" aria-hidden />
                Track expiry dates so nothing goes to waste
              </li>
              <li className="flex items-start gap-2">
                <span className="mt-1.5 h-1.5 w-1.5 rounded-full bg-primary" aria-hidden />
                Turn what you already have into recipes
              </li>
              <li className="flex items-start gap-2">
                <span className="mt-1.5 h-1.5 w-1.5 rounded-full bg-primary" aria-hidden />
                Build shopping lists from missing ingredients
              </li>
            </ul>
          </div>
          <p className="mt-auto text-caption text-text-secondary dark:text-text-secondary-dark">
            © PantryPilot
          </p>
        </aside>

        <main className="flex flex-1 flex-col justify-center p-6 md:p-12">
          <div className="mx-auto w-full max-w-sm">
            <div className="flex items-center gap-2 md:hidden">
              <span className="grid h-9 w-9 place-items-center rounded-lg bg-primary text-white">
                <Utensils className="h-5 w-5" aria-hidden />
              </span>
              <span className="text-body font-semibold text-text-primary dark:text-text-primary-dark">
                PantryPilot
              </span>
            </div>
            <h1 className="mt-6 text-h1 font-semibold text-text-primary md:mt-0 dark:text-text-primary-dark">
              {title}
            </h1>
            <p className="mt-2 text-body text-text-secondary dark:text-text-secondary-dark">
              {subtitle}
            </p>
            <div className="mt-8">{children}</div>
            <div className="mt-6 text-body-sm text-text-secondary dark:text-text-secondary-dark">
              {footer}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
