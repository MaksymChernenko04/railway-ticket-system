import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AppLayout } from "./components/AppLayout";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { WelcomePage } from "./pages/WelcomePage";
import { AdminStationsPage } from "./pages/AdminStationsPage";
import { AdminRoutesPage } from "./pages/AdminRoutesPage.jsx";
import { AdminRouteStopsPage } from "./pages/AdminRouteStopsPage.jsx";
import { AdminTrainsPage } from "./pages/AdminTrainsPage.jsx";
import { AdminCarsPage } from "./pages/AdminCarsPage.jsx";

function HomeRedirect() {
  const { user } = useAuth();

  if (!user) {
    return <Navigate to="/home" replace />;
  }

  return <Navigate to="/welcome" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route element={<AppLayout />}>
        <Route path="/home" element={<HomePage />} />
        <Route path="/welcome" element={<WelcomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
      <Route
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/admin/stations" element={<AdminStationsPage />} />
        <Route path="/admin/routes" element={<AdminRoutesPage />} />
        <Route path="/admin/route-stops" element={<AdminRouteStopsPage />} />
        <Route path="/admin/trains" element={<AdminTrainsPage />} />
        <Route path="/admin/cars" element={<AdminCarsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
