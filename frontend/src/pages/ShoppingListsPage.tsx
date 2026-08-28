import { useState } from "react";
import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { ClipboardList, Plus } from "lucide-react";
import { Button } from "../components/Button";
import { CreateShoppingListModal } from "../components/CreateShoppingListModal";
import { EmptyState } from "../components/EmptyState";
import { ErrorState } from "../components/ErrorState";
import { LoadingState } from "../components/LoadingState";
import { Pagination } from "../components/Pagination";
import { ShoppingListCard } from "../components/ShoppingListCard";
import { useShoppingListsQuery } from "../hooks/useShoppingLists";

export function ShoppingListsPage(): ReactNode {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [createOpen, setCreateOpen] = useState(false);

  const query = useShoppingListsQuery({ page });

  function handleCreated(id: number): void {
    setCreateOpen(false);
    navigate(`/shopping-lists/${id}`);
  }

  return (
    <>
      <div className="flex flex-col gap-6">
        <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-h1 font-semibold text-text-primary dark:text-text-primary-dark">
              Shopping lists
            </h1>
            <p className="mt-1 text-body text-text-secondary dark:text-text-secondary-dark">
              Build lists as you plan, then tick things off at the store.
            </p>
          </div>
          <Button
            onClick={() => setCreateOpen(true)}
            className="self-start md:self-auto"
            data-testid="new-shopping-list-button"
          >
            <Plus className="h-4 w-4" aria-hidden />
            New list
          </Button>
        </div>

        <ShoppingListsContent
          state={query}
          onNew={() => setCreateOpen(true)}
          page={page}
          onPageChange={setPage}
        />
      </div>

      <CreateShoppingListModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={handleCreated}
      />
    </>
  );
}

interface ShoppingListsContentProps {
  state: ReturnType<typeof useShoppingListsQuery>;
  onNew: () => void;
  page: number;
  onPageChange: (page: number) => void;
}

function ShoppingListsContent({
  state,
  onNew,
  page,
  onPageChange,
}: ShoppingListsContentProps): ReactNode {
  if (state.isPending) {
    return <LoadingState rows={4} />;
  }
  if (state.isError) {
    return (
      <div className="rounded-lg border border-border-subtle bg-surface-card p-8 dark:border-border-subtle-dark dark:bg-surface-card-dark">
        <ErrorState onRetry={() => void state.refetch()} />
      </div>
    );
  }
  const data = state.data;
  if (data.totalElements === 0) {
    return (
      <div className="rounded-lg border border-border-subtle bg-surface-card p-8 dark:border-border-subtle-dark dark:bg-surface-card-dark">
        <EmptyState
          icon={ClipboardList}
          title="No shopping lists yet"
          description="Start a list for your next grocery run, or generate one from a recipe."
          action={
            <Button onClick={onNew}>
              <Plus className="h-4 w-4" aria-hidden />
              Create your first list
            </Button>
          }
        />
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-4">
      <ul className="flex flex-col gap-3">
        {data.content.map((list) => (
          <ShoppingListCard key={list.id} list={list} />
        ))}
      </ul>
      <Pagination page={page} totalPages={data.totalPages} onChange={onPageChange} />
    </div>
  );
}
