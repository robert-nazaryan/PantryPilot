import type { ReactNode } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { AuthenticatedLayout } from "./components/AuthenticatedLayout";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AuthCallbackPage } from "./pages/AuthCallbackPage";
import { DashboardPage } from "./pages/DashboardPage";
import { LoginPage } from "./pages/LoginPage";
import { PantryItemFormPage } from "./pages/PantryItemFormPage";
import { PantryPage } from "./pages/PantryPage";
import { RegisterPage } from "./pages/RegisterPage";
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
      </Route>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default App;
