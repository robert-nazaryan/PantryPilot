import type { ReactNode } from "react";
import { Link, Navigate, useNavigate, useParams } from "react-router-dom";
import { ChevronLeft } from "lucide-react";
import { ErrorState } from "../components/ErrorState";
import { LoadingState } from "../components/LoadingState";
import { PantryItemFormPanel } from "../components/PantryItemFormPanel";
import { usePantryItemQuery } from "../hooks/usePantryItems";

interface PantryItemFormPageProps {
  mode: "create" | "edit";
}

export function PantryItemFormPage({ mode }: PantryItemFormPageProps): ReactNode {
  if (mode === "create") return <CreateForm />;
  return <EditForm />;
}

function CreateForm(): ReactNode {
  const navigate = useNavigate();
  const back = () => navigate("/pantry");
  return (
    <PageShell title="Add pantry item">
      <PantryItemFormPanel mode="create" onSuccess={back} onCancel={back} />
    </PageShell>
  );
}

function EditForm(): ReactNode {
  const navigate = useNavigate();
  const params = useParams<{ id: string }>();
  const idNum = params.id ? Number(params.id) : NaN;
  const enabled = Number.isFinite(idNum);
  const query = usePantryItemQuery(enabled ? idNum : null);

  if (!enabled) return <Navigate to="/pantry" replace />;

  const back = () => navigate("/pantry");

  return (
    <PageShell title="Edit pantry item">
      {query.isPending && <LoadingState rows={3} />}
      {query.isError && (
        <div className="rounded-lg border border-border-subtle bg-surface-card p-8 dark:border-border-subtle-dark dark:bg-surface-card-dark">
          <ErrorState onRetry={() => void query.refetch()} />
        </div>
      )}
      {query.data && (
        <PantryItemFormPanel
          mode="edit"
          initial={query.data}
          onSuccess={back}
          onCancel={back}
        />
      )}
    </PageShell>
  );
}

interface PageShellProps {
  title: string;
  children: ReactNode;
}

function PageShell({ title, children }: PageShellProps): ReactNode {
  return (
    <div className="mx-auto flex max-w-xl flex-col gap-6">
      <Link
        to="/pantry"
        className="inline-flex items-center gap-1 text-body-sm text-text-secondary transition-colors duration-150 hover:text-text-primary dark:text-text-secondary-dark dark:hover:text-text-primary-dark"
      >
        <ChevronLeft className="h-4 w-4" aria-hidden />
        Back to pantry
      </Link>
      <h1 className="text-h1 font-semibold text-text-primary dark:text-text-primary-dark">
        {title}
      </h1>
      {children}
    </div>
  );
}
