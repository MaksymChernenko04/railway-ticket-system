import LoginIcon from "@mui/icons-material/Login";
import TrainIcon from "@mui/icons-material/Train";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Link as MuiLink,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useI18n } from "../i18n/I18nContext.jsx";

export function LoginPage() {
  const { isAuthenticated, login, user } = useAuth();
  const { t } = useI18n();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (isAuthenticated) {
    return <Navigate to={user?.role === "ADMIN" ? "/admin/stations" : "/"} replace />;
  }

  const redirectTo = location.state?.from?.pathname || "/";

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);

    try {
      const auth = await login(form);
      navigate(auth.user?.role === "ADMIN" ? "/admin/stations" : redirectTo, { replace: true });
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        background:
          "radial-gradient(circle at top left, rgba(121,168,132,0.22), transparent 26%), linear-gradient(135deg, #e7efe5 0%, #dce7d9 100%)",
        py: 6,
      }}
    >
      <Container maxWidth="lg">
        <Box
          sx={{
            display: "grid",
            gap: 3,
            gridTemplateColumns: { xs: "1fr", md: "1.05fr 0.95fr" },
            alignItems: "stretch",
          }}
        >
          <Card sx={{ bgcolor: "rgba(255,255,255,0.86)", backdropFilter: "blur(12px)" }}>
            <CardContent sx={{ p: { xs: 3, md: 4.5 }, minHeight: "100%" }}>
              <Stack spacing={3}>
                <Box
                  sx={{
                    width: 64,
                    height: 64,
                    borderRadius: 3,
                    display: "grid",
                    placeItems: "center",
                    bgcolor: "primary.main",
                    color: "primary.contrastText",
                  }}
                >
                  <TrainIcon fontSize="large" />
                </Box>
                <Box>
                  <Typography
                    variant="overline"
                    color="primary.main"
                    sx={{ letterSpacing: "0.16em", fontWeight: 700 }}
                  >
                    {t("Sign in")}
                  </Typography>
                  <Typography variant="h2" sx={{ mt: 1, maxWidth: 500 }}>
                    {t("Sign in to continue your railway journey.")}
                  </Typography>
                </Box>
                <Typography variant="body1" color="text.secondary" sx={{ maxWidth: 500 }}>
                  {t("Use your email and password to open your account and manage your trips.")}
                </Typography>
              </Stack>
            </CardContent>
          </Card>

          <Card>
            <CardContent sx={{ p: { xs: 3, md: 4.5 }, minHeight: "100%" }}>
              <Stack spacing={3} component="form" onSubmit={handleSubmit}>
                <Box>
                  <Typography variant="h4">{t("Login")}</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>
                    {t("Enter the email address and password you used when creating your account.")}
                  </Typography>
                </Box>

                <TextField
                  label={t("Email")}
                  type="email"
                  value={form.email}
                  onChange={(event) => setForm({ ...form, email: event.target.value })}
                  required
                  fullWidth
                />

                <TextField
                  label={t("Password")}
                  type="password"
                  value={form.password}
                  onChange={(event) => setForm({ ...form, password: event.target.value })}
                  required
                  fullWidth
                />

                {error ? <Alert severity="error">{error}</Alert> : null}

                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  startIcon={<LoginIcon />}
                  disabled={isSubmitting}
                >
                  {isSubmitting ? t("Signing in...") : t("Sign in")}
                </Button>

                <Typography variant="body2" color="text.secondary">
                  {t("Need an account?")}{" "}
                  <MuiLink component={Link} to="/register" underline="hover">
                    {t("Create one")}
                  </MuiLink>
                </Typography>
              </Stack>
            </CardContent>
          </Card>
        </Box>
      </Container>
    </Box>
  );
}
