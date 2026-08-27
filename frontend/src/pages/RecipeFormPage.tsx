import type { ReactNode } from "react";
import { Link, Navigate, useNavigate, useParams } from "react-router-dom";
import { ChevronLeft } from "lucide-react";
import { ErrorState } from "../components/ErrorState";
import { LoadingState } from "../components/LoadingState";
import { RecipeFormPanel } from "../components/RecipeFormPanel";
import { useRecipeQuery } from "../hooks/useRecipes";
import { ApiError } from "../api/client";
import type { RecipeResponse } from "../types/recipe";

interface RecipeFormPageProps {
  mode: "create" | "edit";
}

export function RecipeFormPage({ mode }: RecipeFormPageProps): ReactNode {
  if (mode === "create") return <CreateForm />;
  return <EditForm />;
}

function CreateForm(): ReactNode {
  const navigate = useNavigate();
  const cancel = () => navigate("/recipes");
  const onCreated = (recipe: RecipeResponse) => navigate(`/recipes/${recipe.id}`);
  return (
    <PageShell title="Add recipe" backTo="/recipes" backLabel="Back to recipes">
      <RecipeFormPanel mode="create" onCreated={onCreated} onCancel={cancel} />
    </PageShell>
  );
}

function EditForm(): ReactNode {
  const navigate = useNavigate();
  const params = useParams<{ id: string }>();
  const idNum = params.id ? Number(params.id) : NaN;
  const enabled = Number.isFinite(idNum);
  const query = useRecipeQuery(enabled ? idNum : null);

  if (!enabled) return <Navigate to="/recipes" replace />;

  const back = () => navigate(`/recipes/${idNum}`);
  const onSaved = (_recipe: RecipeResponse) => back();

  const notFound =
    query.isError && query.error instanceof ApiError && query.error.status === 404;

  return (
    <PageShell
      title="Edit recipe"
      backTo={`/recipes/${idNum}`}
      backLabel="Back to recipe"
    >
      {query.isPending && <LoadingState rows={3} />}
      {notFound && (
        <div className="rounded-lg border border-border-subtle bg-surface-card p-8 dark:border-border-subtle-dark dark:bg-surface-card-dark">
          <ErrorState
            title="Recipe not found"
            description="This recipe doesn't exist or was removed."
          />
        </div>
      )}
      {query.isError && !notFound && (
        <div className="rounded-lg border border-border-subtle bg-surface-card p-8 dark:border-border-subtle-dark dark:bg-surface-card-dark">
          <ErrorState onRetry={() => void query.refetch()} />
        </div>
      )}
      {query.data && (
        <RecipeFormPanel
          mode="edit"
          initial={query.data}
          onSaved={onSaved}
          onCancel={back}
        />
      )}
    </PageShell>
  );
}

interface PageShellProps {
  title: string;
  backTo: string;
  backLabel: string;
  children: ReactNode;
}

function PageShell({ title, backTo, backLabel, children }: PageShellProps): ReactNode {
  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <Link
        to={backTo}
        className="inline-flex items-center gap-1 text-body-sm text-text-secondary transition-colors duration-150 hover:text-text-primary dark:text-text-secondary-dark dark:hover:text-text-primary-dark"
      >
        <ChevronLeft className="h-4 w-4" aria-hidden />
        {backLabel}
      </Link>
      <h1 className="text-h1 font-semibold text-text-primary dark:text-text-primary-dark">
        {title}
      </h1>
      {children}
    </div>
  );
}
