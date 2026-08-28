import { useMemo, useState } from "react";
import type { ReactNode } from "react";
import { useQueries } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router-dom";
import {
  BookOpen,
  ChefHat,
  ChevronRight,
  ClipboardList,
  Package,
  Plus,
  Sparkles,
} from "lucide-react";
import { CreateShoppingListModal } from "../components/CreateShoppingListModal";
import { DashboardExpiryCard } from "../components/DashboardExpiryCard";
import { DashboardShoppingListSummaryCard } from "../components/DashboardShoppingListSummaryCard";
import { ErrorState } from "../components/ErrorState";
import { PantryItemFormShell } from "../components/PantryItemFormShell";
import { StatTile } from "../components/StatTile";
import { useAuth } from "../context/useAuth";
import { MD_BREAKPOINT_QUERY, useMediaQuery } from "../hooks/useMediaQuery";
import {
  useExpiringPantryItemsQuery,
  usePantryItemsQuery,
} from "../hooks/usePantryItems";
import { useRecipesQuery } from "../hooks/useRecipes";
import {
  SHOPPING_LISTS_QUERY_KEY,
  useShoppingListsQuery,
} from "../hooks/useShoppingLists";
import * as shoppingListsApi from "../api/shoppingLists";
import type { PantryItemResponse } from "../types/pantry";
import type { ShoppingListResponse } from "../types/shoppingList";

const MAX_EXPIRY_PREVIEW = 6;
const MAX_ACTIVE_LISTS_PREVIEW = 4;
const ACTIVE_LISTS_FETCH_SIZE = 10;

export function DashboardPage(): ReactNode {
  const isDesktop = useMediaQuery(MD_BREAKPOINT_QUERY);
  const navigate = useNavigate();
  const { user } = useAuth();
  const displayName =
    user?.displayName?.trim() || user?.email?.split("@")[0] || "friend";

  const [pantryFormIntent, setPantryFormIntent] = useState<
    | { mode: "create" }
    | { mode: "edit"; item: PantryItemResponse }
    | null
  >(null);
  const [shoppingListModalOpen, setShoppingListModalOpen] = useState(false);

  const expiring = useExpiringPantryItemsQuery();
  const shoppingLists = useShoppingListsQuery({
    page: 0,
    size: ACTIVE_LISTS_FETCH_SIZE,
  });
  const pantryCount = usePantryItemsQuery({ page: 0, size: 1 });
  const recipeCount = useRecipesQuery({ page: 0, size: 1 });
  const shoppingListCount = shoppingLists;

  const shoppingListSummaries = useMemo(
    () => shoppingLists.data?.content ?? [],
    [shoppingLists.data],
  );
  const shoppingListDetails = useQueries({
    queries: shoppingListSummaries.map((summary) => ({
      queryKey: [...SHOPPING_LISTS_QUERY_KEY, "detail", summary.id],
      queryFn: (): Promise<ShoppingListResponse> =>
        shoppingListsApi.getShoppingList(summary.id),
    })),
  });
  const listsWithUnchecked = useMemo(() => {
    const rows: { id: number; name: string; total: number; checked: number }[] = [];
    shoppingListSummaries.forEach((summary, i) => {
      const detail = shoppingListDetails[i]?.data;
      if (!detail) return;
      const total = detail.items.length;
      const checked = detail.items.reduce(
        (n, item) => (item.checked ? n + 1 : n),
        0,
      );
      if (total - checked > 0) {
        rows.push({ id: summary.id, name: summary.name, total, checked });
      }
    });
    return rows;
  }, [shoppingListSummaries, shoppingListDetails]);
  const shoppingListDetailsPending = shoppingListDetails.some((q) => q.isPending);

  function handleAddPantryItem(): void {
    if (isDesktop) {
      setPantryFormIntent({ mode: "create" });
    } else {
      navigate("/pantry/new");
    }
  }

  function handleEditPantryItem(item: PantryItemResponse): void {
    if (isDesktop) {
      setPantryFormIntent({ mode: "edit", item });
    } else {
      navigate(`/pantry/${item.id}/edit`);
    }
  }

  function handleShoppingListCreated(id: number): void {
    setShoppingListModalOpen(false);
    navigate(`/shopping-lists/${id}`);
  }

  const expiringItems = expiring.data ?? [];
  const previewExpiring = expiringItems.slice(0, MAX_EXPIRY_PREVIEW);
  const hasMoreExpiring = expiringItems.length > MAX_EXPIRY_PREVIEW;

  const previewActiveLists = listsWithUnchecked.slice(0, MAX_ACTIVE_LISTS_PREVIEW);
  const hasMoreActiveLists = listsWithUnchecked.length > MAX_ACTIVE_LISTS_PREVIEW;

  const needsAttentionLoading =
    expiring.isPending ||
    shoppingLists.isPending ||
    (shoppingListSummaries.length > 0 && shoppingListDetailsPending);
  const needsAttentionError = expiring.isError && shoppingLists.isError;
  const nothingUrgent =
    !needsAttentionLoading &&
    !needsAttentionError &&
    expiringItems.length === 0 &&
    listsWithUnchecked.length === 0;

  return (
    <>
      <div className="flex flex-col gap-8">
        <header
          className="dashboard-section-in"
          style={{ animationDelay: "0ms" }}
        >
          <h1 className="text-h1 font-semibold text-text-primary dark:text-text-primary-dark">
            {greeting()}, {displayName}.
          </h1>
          <p className="mt-1 text-body text-text-secondary dark:text-text-secondary-dark">
            Here&rsquo;s what needs your attention today.
          </p>
        </header>

        <section
          aria-labelledby="dashboard-attention-heading"
          className="dashboard-section-in flex flex-col gap-4"
          style={{ animationDelay: "60ms" }}
        >
          <div className="flex items-center justify-between">
            <h2
              id="dashboard-attention-heading"
              className="text-h2 font-semibold text-text-primary dark:text-text-primary-dark"
            >
              Needs attention
            </h2>
          </div>

          {needsAttentionLoading ? (
            <AttentionLoading />
          ) : needsAttentionError ? (
            <div className="rounded-lg border border-border-subtle bg-white p-8 dark:border-border-subtle-dark dark:bg-surface-card-dark">
              <ErrorState
                onRetry={() => {
                  void expiring.refetch();
                  void shoppingLists.refetch();
                }}
              />
            </div>
          ) : nothingUrgent ? (
            <NothingUrgent />
          ) : (
            <div className="flex flex-col gap-6">
              {previewExpiring.length > 0 && (
                <ExpiringGroup
                  items={previewExpiring}
                  hasMore={hasMoreExpiring}
                  onEdit={handleEditPantryItem}
                />
              )}
              {previewActiveLists.length > 0 && (
                <ActiveListsGroup
                  lists={previewActiveLists}
                  hasMore={hasMoreActiveLists}
                />
              )}
            </div>
          )}
        </section>

        <section
          aria-labelledby="dashboard-quickactions-heading"
          className="dashboard-section-in flex flex-col gap-4"
          style={{ animationDelay: "120ms" }}
        >
          <h2
            id="dashboard-quickactions-heading"
            className="text-h2 font-semibold text-text-primary dark:text-text-primary-dark"
          >
            Quick actions
          </h2>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <QuickAction
              icon={Package}
              label="Add pantry item"
              description="Log something new in the kitchen"
              onClick={handleAddPantryItem}
              testId="dashboard-quick-add-pantry-item"
            />
            <QuickAction
              icon={ClipboardList}
              label="New shopping list"
              description="Start planning a grocery run"
              onClick={() => setShoppingListModalOpen(true)}
              testId="dashboard-quick-new-shopping-list"
            />
            <QuickAction
              icon={ChefHat}
              label="Add recipe"
              description="Save a new dish"
              onClick={() => navigate("/recipes/new")}
              testId="dashboard-quick-add-recipe"
            />
          </div>
        </section>

        <section
          aria-labelledby="dashboard-overview-heading"
          className="dashboard-section-in flex flex-col gap-3"
          style={{ animationDelay: "180ms" }}
        >
          <h2
            id="dashboard-overview-heading"
            className="text-body font-medium text-text-secondary dark:text-text-secondary-dark"
          >
            Overview
          </h2>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <StatTile
              icon={Package}
              label="Pantry items"
              value={pantryCount.data?.totalElements}
              loading={pantryCount.isPending}
              data-testid="dashboard-stat-pantry"
            />
            <StatTile
              icon={BookOpen}
              label="Recipes"
              value={recipeCount.data?.totalElements}
              loading={recipeCount.isPending}
              data-testid="dashboard-stat-recipes"
            />
            <StatTile
              icon={ClipboardList}
              label="Shopping lists"
              value={shoppingListCount.data?.totalElements}
              loading={shoppingListCount.isPending}
              data-testid="dashboard-stat-shopping-lists"
            />
          </div>
        </section>
      </div>

      {pantryFormIntent?.mode === "create" && (
        <PantryItemFormShell
          open
          mode="create"
          onClose={() => setPantryFormIntent(null)}
          onSuccess={() => setPantryFormIntent(null)}
        />
      )}
      {pantryFormIntent?.mode === "edit" && (
        <PantryItemFormShell
          open
          mode="edit"
          initial={pantryFormIntent.item}
          onClose={() => setPantryFormIntent(null)}
          onSuccess={() => setPantryFormIntent(null)}
        />
      )}

      <CreateShoppingListModal
        open={shoppingListModalOpen}
        onClose={() => setShoppingListModalOpen(false)}
        onCreated={handleShoppingListCreated}
      />
    </>
  );
}

function greeting(): string {
  const hour = new Date().getHours();
  if (hour < 5) return "Good evening";
  if (hour < 12) return "Good morning";
  if (hour < 18) return "Good afternoon";
  return "Good evening";
}

function AttentionLoading(): ReactNode {
  return (
    <div className="flex flex-col gap-3" aria-busy="true" aria-label="Loading">
      <div className="h-28 animate-pulse rounded-lg border border-border-subtle bg-surface-card dark:border-border-subtle-dark dark:bg-surface-card-dark" />
      <div className="h-20 animate-pulse rounded-lg border border-border-subtle bg-surface-card dark:border-border-subtle-dark dark:bg-surface-card-dark" />
    </div>
  );
}

function NothingUrgent(): ReactNode {
  return (
    <div
      data-testid="dashboard-nothing-urgent"
      className="flex flex-col items-center gap-3 rounded-lg border border-border-subtle bg-white p-8 text-center dark:border-border-subtle-dark dark:bg-surface-card-dark"
    >
      <span className="grid h-12 w-12 place-items-center rounded-lg bg-success/10 text-success dark:bg-success/20">
        <Sparkles className="h-6 w-6" aria-hidden />
      </span>
      <h3 className="text-h3 font-semibold text-text-primary dark:text-text-primary-dark">
        Nothing urgent
      </h3>
      <p className="max-w-sm text-body text-text-secondary dark:text-text-secondary-dark">
        Your kitchen&rsquo;s in good shape. Nothing&rsquo;s about to expire and
        no shopping lists are waiting on you.
      </p>
    </div>
  );
}

interface ExpiringGroupProps {
  items: PantryItemResponse[];
  hasMore: boolean;
  onEdit: (item: PantryItemResponse) => void;
}

function ExpiringGroup({
  items,
  hasMore,
  onEdit,
}: ExpiringGroupProps): ReactNode {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <h3 className="text-body font-medium text-text-primary dark:text-text-primary-dark">
          Expiring soon
        </h3>
        {hasMore && (
          <ViewAllLink to="/pantry" testId="dashboard-view-all-expiring" />
        )}
      </div>
      <div className="-mx-4 overflow-x-auto px-4 md:mx-0 md:px-0">
        <div className="flex gap-3 md:grid md:grid-cols-2 md:gap-3 lg:grid-cols-3">
          {items.map((item) => (
            <DashboardExpiryCard key={item.id} item={item} onClick={onEdit} />
          ))}
        </div>
      </div>
    </div>
  );
}

interface ActiveListsGroupProps {
  lists: { id: number; name: string; total: number; checked: number }[];
  hasMore: boolean;
}

function ActiveListsGroup({
  lists,
  hasMore,
}: ActiveListsGroupProps): ReactNode {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <h3 className="text-body font-medium text-text-primary dark:text-text-primary-dark">
          Active shopping lists
        </h3>
        {hasMore && (
          <ViewAllLink
            to="/shopping-lists"
            testId="dashboard-view-all-shopping-lists"
          />
        )}
      </div>
      <ul className="flex flex-col gap-2">
        {lists.map((list) => (
          <DashboardShoppingListSummaryCard
            key={list.id}
            listId={list.id}
            name={list.name}
            total={list.total}
            checked={list.checked}
          />
        ))}
      </ul>
    </div>
  );
}

function ViewAllLink({
  to,
  testId,
}: {
  to: string;
  testId: string;
}): ReactNode {
  return (
    <Link
      to={to}
      data-testid={testId}
      className="inline-flex items-center gap-0.5 text-body-sm font-medium text-primary transition-colors duration-150 hover:text-primary-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
    >
      View all
      <ChevronRight className="h-4 w-4" aria-hidden />
    </Link>
  );
}

interface QuickActionProps {
  icon: typeof Package;
  label: string;
  description: string;
  onClick: () => void;
  testId: string;
}

function QuickAction({
  icon: Icon,
  label,
  description,
  onClick,
  testId,
}: QuickActionProps): ReactNode {
  return (
    <button
      type="button"
      onClick={onClick}
      data-testid={testId}
      className={
        "group flex items-start gap-3 rounded-lg border border-border-subtle bg-white p-4 text-left " +
        "transition-colors duration-150 " +
        "hover:border-primary/40 hover:bg-surface-card " +
        "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary " +
        "dark:border-border-subtle-dark dark:bg-surface-card-dark " +
        "dark:hover:border-primary/40 dark:hover:bg-surface-elevated-dark"
      }
    >
      <span className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-primary/10 text-primary dark:bg-primary/20">
        <Icon className="h-5 w-5" aria-hidden />
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex items-center gap-1 text-body font-semibold text-text-primary dark:text-text-primary-dark">
          {label}
          <Plus
            className="h-4 w-4 text-text-secondary transition-transform duration-150 group-hover:text-primary dark:text-text-secondary-dark"
            aria-hidden
          />
        </span>
        <span className="mt-0.5 block text-body-sm text-text-secondary dark:text-text-secondary-dark">
          {description}
        </span>
      </span>
    </button>
  );
}
