import { useState } from "react";
import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { PackageOpen, Plus } from "lucide-react";
import { Button } from "../components/Button";
import { EmptyState } from "../components/EmptyState";
import { ErrorState } from "../components/ErrorState";
import { LoadingState } from "../components/LoadingState";
import { Pagination } from "../components/Pagination";
import { PantryItemCard } from "../components/PantryItemCard";
import { PantryItemFormShell } from "../components/PantryItemFormShell";
import { usePantryItemsQuery } from "../hooks/usePantryItems";
import { MD_BREAKPOINT_QUERY, useMediaQuery } from "../hooks/useMediaQuery";
import type { PantryItemResponse } from "../types/pantry";

export function PantryPage(): ReactNode {
  const isDesktop = useMediaQuery(MD_BREAKPOINT_QUERY);
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [formIntent, setFormIntent] = useState<
    | { mode: "create" }
    | { mode: "edit"; item: PantryItemResponse }
    | null
  >(null);

  const query = usePantryItemsQuery({ page });

  function handleAddClick() {
    if (isDesktop) {
      setFormIntent({ mode: "create" });
    } else {
      navigate("/pantry/new");
    }
  }

  function handleEditClick(item: PantryItemResponse) {
    if (isDesktop) {
      setFormIntent({ mode: "edit", item });
    } else {
      navigate(`/pantry/${item.id}/edit`);
    }
  }

  function closeForm() {
    setFormIntent(null);
  }

  return (
    <>
      <div className="flex flex-col gap-6">
        <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-h1 font-semibold text-text-primary dark:text-text-primary-dark">
              Your pantry
            </h1>
            <p className="mt-1 text-body text-text-secondary dark:text-text-secondary-dark">
              Track what you have, and what&rsquo;s about to expire.
            </p>
          </div>
          <Button onClick={handleAddClick} className="self-start md:self-auto">
            <Plus className="h-4 w-4" aria-hidden />
            Add item
          </Button>
        </div>

        <PantryContent
          state={query}
          onEdit={handleEditClick}
          onAdd={handleAddClick}
          page={page}
          onPageChange={setPage}
        />
      </div>

      {formIntent?.mode === "create" && (
        <PantryItemFormShell
          open
          mode="create"
          onClose={closeForm}
          onSuccess={closeForm}
        />
      )}
      {formIntent?.mode === "edit" && (
        <PantryItemFormShell
          open
          mode="edit"
          initial={formIntent.item}
          onClose={closeForm}
          onSuccess={closeForm}
        />
      )}
    </>
  );
}

interface PantryContentProps {
  state: ReturnType<typeof usePantryItemsQuery>;
  onEdit: (item: PantryItemResponse) => void;
  onAdd: () => void;
  page: number;
  onPageChange: (page: number) => void;
}

function PantryContent({
  state,
  onEdit,
  onAdd,
  page,
  onPageChange,
}: PantryContentProps): ReactNode {
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
          icon={PackageOpen}
          title="Your pantry is empty"
          description="Add what's in your kitchen to start tracking expiry dates and building recipes."
          action={
            <Button onClick={onAdd}>
              <Plus className="h-4 w-4" aria-hidden />
              Add your first item
            </Button>
          }
        />
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-4">
      <ul className="flex flex-col gap-3">
        {data.content.map((item) => (
          <PantryItemCard key={item.id} item={item} onEdit={onEdit} />
        ))}
      </ul>
      <Pagination page={page} totalPages={data.totalPages} onChange={onPageChange} />
    </div>
  );
}
