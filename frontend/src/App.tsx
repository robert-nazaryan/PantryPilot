import type { ReactNode } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { AuthenticatedLayout } from "./components/AuthenticatedLayout";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AuthCallbackPage } from "./pages/AuthCallbackPage";
import { DashboardPage } from "./pages/DashboardPage";
import { LoginPage } from "./pages/LoginPage";
import { PantryItemFormPage } from "./pages/PantryItemFormPage";
import { PantryPage } from "./pages/PantryPage";
import { RecipeDetailPage } from "./pages/RecipeDetailPage";
import { RecipeFormPage } from "./pages/RecipeFormPage";
import { RecipesPage } from "./pages/RecipesPage";
import { RegisterPage } from "./pages/RegisterPage";
import { ShoppingListDetailPage } from "./pages/ShoppingListDetailPage";
import { ShoppingListsPage } from "./pages/ShoppingListsPage";
import { useAuth } from "./context/useAuth";

function RedirectIfAuthenticated({ children }: { children: ReactNode }): ReactNode {
  const { status } = useAuth();
  if (status === "authenticated") return <Navigate to="/dashboard" replace />;
  return children;
}

function App(): ReactNode {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <RedirectIfAuthenticated>
            <LoginPage />
          </RedirectIfAuthenticated>
        }
      />
      <Route
        path="/register"
        element={
          <RedirectIfAuthenticated>
            <RegisterPage />
          </RedirectIfAuthenticated>
        }
      />
      <Route path="/auth/callback" element={<AuthCallbackPage />} />
      <Route
        element={
          <ProtectedRoute>
            <AuthenticatedLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/pantry" element={<PantryPage />} />
        <Route path="/pantry/new" element={<PantryItemFormPage mode="create" />} />
        <Route path="/pantry/:id/edit" element={<PantryItemFormPage mode="edit" />} />
        <Route path="/recipes" element={<RecipesPage />} />
        <Route path="/recipes/new" element={<RecipeFormPage mode="create" />} />
        <Route path="/recipes/:id" element={<RecipeDetailPage />} />
        <Route path="/recipes/:id/edit" element={<RecipeFormPage mode="edit" />} />
        <Route path="/shopping-lists" element={<ShoppingListsPage />} />
        <Route path="/shopping-lists/:id" element={<ShoppingListDetailPage />} />
      </Route>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default App;
