import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AppLayout } from "./components/AppLayout";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { WelcomePage } from "./pages/WelcomePage";
import { ProfilePage } from "./pages/ProfilePage";
import { AdminStationsPage } from "./pages/AdminStationsPage";
import { AdminRoutesPage } from "./pages/AdminRoutesPage.jsx";
import { AdminRouteStopsPage } from "./pages/AdminRouteStopsPage.jsx";
import { AdminTrainsPage } from "./pages/AdminTrainsPage.jsx";
import { AdminCarsPage } from "./pages/AdminCarsPage.jsx";
import { AdminTripsPage } from "./pages/AdminTripsPage.jsx";
import { AdminAnalyticsPage } from "./pages/AdminAnalyticsPage.jsx";
import { SearchTripsPage } from "./pages/SearchTripsPage.jsx";
import { SelectSeatsPage } from "./pages/SelectSeatsPage.jsx";
import { PaymentPage } from "./pages/PaymentPage.jsx";
import { MyTicketsPage } from "./pages/MyTicketsPage.jsx";
import { MyOrdersPage } from "./pages/MyOrdersPage.jsx";
import { TravelAssistantPage } from "./pages/TravelAssistantPage.jsx";

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
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/trips/search" element={<SearchTripsPage />} />
        <Route path="/assistant" element={<TravelAssistantPage />} />
        <Route path="/trips/:tripId/seats" element={<SelectSeatsPage />} />
        <Route path="/payments/:paymentId" element={<PaymentPage />} />
        <Route
          path="/tickets"
          element={
            <ProtectedRoute forbiddenRole="ADMIN">
              <MyTicketsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/orders"
          element={
            <ProtectedRoute forbiddenRole="ADMIN">
              <MyOrdersPage />
            </ProtectedRoute>
          }
        />
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
        <Route path="/admin/trips" element={<AdminTripsPage />} />
        <Route path="/admin/analytics" element={<AdminAnalyticsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
