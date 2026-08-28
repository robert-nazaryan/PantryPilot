import { useState } from "react";
import type { ReactNode } from "react";
import { Link, NavLink } from "react-router-dom";
import {
  BookOpen,
  ClipboardList,
  LogOut,
  Menu,
  Moon,
  PackageOpen,
  LayoutDashboard,
  Sun,
  Utensils,
  X,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { Button } from "./Button";
import { useAuth } from "../context/useAuth";
import { useTheme } from "../context/useTheme";

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
}

const NAV_ITEMS: NavItem[] = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/pantry", label: "Pantry", icon: PackageOpen },
  { to: "/recipes", label: "Recipes", icon: BookOpen },
  { to: "/shopping-lists", label: "Shopping lists", icon: ClipboardList },
];

export function Navbar(): ReactNode {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [signingOut, setSigningOut] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const closeMobile = () => setMobileOpen(false);

  async function handleLogout() {
    setSigningOut(true);
    try {
      await logout();
    } finally {
      setSigningOut(false);
    }
  }

  const displayName = user?.displayName?.trim() || user?.email || "";
  const ThemeIcon = theme === "dark" ? Sun : Moon;
  const themeLabel = theme === "dark" ? "Switch to light mode" : "Switch to dark mode";

  return (
    <header className="sticky top-0 z-40 border-b border-border-subtle bg-white dark:border-border-subtle-dark dark:bg-surface-card-dark">
      <div className="mx-auto flex max-w-[1200px] items-center justify-between gap-4 px-4 py-3 md:px-6">
        <Link to="/dashboard" className="flex items-center gap-2">
          <span className="grid h-9 w-9 place-items-center rounded-lg bg-primary text-white">
            <Utensils className="h-5 w-5" aria-hidden />
          </span>
          <span className="text-body font-semibold text-text-primary dark:text-text-primary-dark">
            PantryPilot
          </span>
        </Link>

        <nav aria-label="Primary" className="hidden md:flex md:items-center md:gap-1">
          {NAV_ITEMS.map((item) => (
            <DesktopNavLink key={item.to} item={item} />
          ))}
        </nav>

        <div className="hidden items-center gap-2 md:flex">
          <ThemeButton theme={theme} onToggle={toggleTheme} icon={ThemeIcon} label={themeLabel} />
          {displayName && (
            <span className="text-body-sm text-text-secondary dark:text-text-secondary-dark">
              {displayName}
            </span>
          )}
          <Button variant="ghost" onClick={handleLogout} loading={signingOut}>
            <LogOut className="h-4 w-4" aria-hidden />
            Sign out
          </Button>
        </div>

        <div className="flex items-center gap-1 md:hidden">
          <ThemeButton theme={theme} onToggle={toggleTheme} icon={ThemeIcon} label={themeLabel} />
          <button
            type="button"
            aria-label={mobileOpen ? "Close menu" : "Open menu"}
            aria-expanded={mobileOpen}
            aria-controls="mobile-menu"
            onClick={() => setMobileOpen((v) => !v)}
            className="grid h-11 w-11 place-items-center rounded-lg text-text-secondary transition-colors duration-150 hover:bg-surface-card hover:text-text-primary dark:text-text-secondary-dark dark:hover:bg-surface-elevated-dark dark:hover:text-text-primary-dark"
          >
            {mobileOpen ? <X className="h-5 w-5" aria-hidden /> : <Menu className="h-5 w-5" aria-hidden />}
          </button>
        </div>
      </div>

      {mobileOpen && (
        <div
          id="mobile-menu"
          className="border-t border-border-subtle bg-white px-4 pb-4 pt-2 md:hidden dark:border-border-subtle-dark dark:bg-surface-card-dark"
        >
          <nav aria-label="Mobile" className="flex flex-col gap-1">
            {NAV_ITEMS.map((item) => (
              <MobileNavLink key={item.to} item={item} onNavigate={closeMobile} />
            ))}
          </nav>
          <div className="mt-3 flex items-center justify-between border-t border-border-subtle pt-3 dark:border-border-subtle-dark">
            {displayName && (
              <span className="text-body-sm text-text-secondary dark:text-text-secondary-dark">
                {displayName}
              </span>
            )}
            <Button
              variant="ghost"
              onClick={() => {
                closeMobile();
                void handleLogout();
              }}
              loading={signingOut}
            >
              <LogOut className="h-4 w-4" aria-hidden />
              Sign out
            </Button>
          </div>
        </div>
      )}
    </header>
  );
}

interface DesktopNavLinkProps {
  item: NavItem;
}

function DesktopNavLink({ item }: DesktopNavLinkProps): ReactNode {
  const { icon: Icon, to, label } = item;
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        [
          "inline-flex min-h-11 items-center gap-2 rounded-lg px-3 text-body-sm font-medium transition-colors duration-150",
          isActive
            ? "bg-primary/10 text-primary dark:bg-primary/15 dark:text-primary"
            : "text-text-secondary hover:bg-surface-card hover:text-text-primary dark:text-text-secondary-dark dark:hover:bg-surface-elevated-dark dark:hover:text-text-primary-dark",
        ].join(" ")
      }
    >
      <Icon className="h-4 w-4" aria-hidden />
      {label}
    </NavLink>
  );
}

interface MobileNavLinkProps {
  item: NavItem;
  onNavigate: () => void;
}

function MobileNavLink({ item, onNavigate }: MobileNavLinkProps): ReactNode {
  const { icon: Icon, to, label } = item;
  return (
    <NavLink
      to={to}
      onClick={onNavigate}
      className={({ isActive }) =>
        [
          "flex min-h-11 items-center gap-3 rounded-lg px-3 text-body font-medium transition-colors duration-150",
          isActive
            ? "bg-primary/10 text-primary dark:bg-primary/15 dark:text-primary"
            : "text-text-primary hover:bg-surface-card dark:text-text-primary-dark dark:hover:bg-surface-elevated-dark",
        ].join(" ")
      }
    >
      <Icon className="h-4 w-4" aria-hidden />
      {label}
    </NavLink>
  );
}

interface ThemeButtonProps {
  theme: string;
  onToggle: () => void;
  icon: LucideIcon;
  label: string;
}

function ThemeButton({ onToggle, icon: Icon, label }: ThemeButtonProps): ReactNode {
  return (
    <button
      type="button"
      onClick={onToggle}
      aria-label={label}
      title={label}
      className="grid h-11 w-11 place-items-center rounded-lg text-text-secondary transition-colors duration-150 hover:bg-surface-card hover:text-text-primary dark:text-text-secondary-dark dark:hover:bg-surface-elevated-dark dark:hover:text-text-primary-dark"
    >
      <Icon className="h-5 w-5" aria-hidden />
    </button>
  );
}
