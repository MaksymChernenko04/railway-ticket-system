import { Box, CircularProgress, Typography } from "@mui/material";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function ProtectedRoute({ children, requiredRole }) {
  const { isBootstrapping, isAuthenticated, user } = useAuth();
  const location = useLocation();

  if (isBootstrapping) {
    return (
      <Box
        sx={{
          minHeight: "100vh",
          display: "grid",
          placeItems: "center",
          bgcolor: "background.default",
          px: 3,
        }}
      >
        <Box sx={{ display: "grid", gap: 2, justifyItems: "center" }}>
          <CircularProgress />
          <Typography color="text.secondary">Loading session...</Typography>
        </Box>
      </Box>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (requiredRole && user?.role !== requiredRole) {
    return <Navigate to="/" replace />;
  }

  return children;
}
