import PersonAddAlt1Icon from "@mui/icons-material/PersonAddAlt1";
import ReceiptLongIcon from "@mui/icons-material/ReceiptLong";
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
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

const initialForm = {
  firstName: "",
  lastName: "",
  email: "",
  password: "",
};

export function RegisterPage() {
  const { isAuthenticated, register, user } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (isAuthenticated) {
    return <Navigate to={user?.role === "ADMIN" ? "/admin/stations" : "/"} replace />;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);

    try {
      await register(form);
      navigate("/", { replace: true });
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
          "radial-gradient(circle at top left, rgba(121,168,132,0.22), transparent 26%), linear-gradient(135deg, #f4f8f2 0%, #e8f0e6 100%)",
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
                  <ReceiptLongIcon fontSize="large" />
                </Box>
                <Box>
                  <Typography
                    variant="overline"
                    color="secondary.main"
                    sx={{ letterSpacing: "0.16em", fontWeight: 700 }}
                  >
                    Client registration
                  </Typography>
                  <Typography variant="h2" sx={{ mt: 1, maxWidth: 640 }}>
                    Create your account and start using the system.
                  </Typography>
                </Box>
                <Typography variant="body1" color="text.secondary" sx={{ maxWidth: 560 }}>
                  Registration gives you personal access to the service and prepares your account
                  for future booking and profile features.
                </Typography>
              </Stack>
            </CardContent>
          </Card>

          <Card>
            <CardContent sx={{ p: { xs: 3, md: 4.5 }, minHeight: "100%" }}>
              <Stack spacing={3} component="form" onSubmit={handleSubmit}>
                <Box>
                  <Typography variant="h4">Register</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>
                    Fill in your basic information and choose a password to create your account.
                  </Typography>
                </Box>

                <Box sx={{ display: "grid", gap: 2, gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" } }}>
                  <TextField
                    label="First name"
                    value={form.firstName}
                    onChange={(event) => setForm({ ...form, firstName: event.target.value })}
                    required
                    fullWidth
                  />
                  <TextField
                    label="Last name"
                    value={form.lastName}
                    onChange={(event) => setForm({ ...form, lastName: event.target.value })}
                    required
                    fullWidth
                  />
                </Box>

                <TextField
                  label="Email"
                  type="email"
                  value={form.email}
                  onChange={(event) => setForm({ ...form, email: event.target.value })}
                  required
                  fullWidth
                />

                <TextField
                  label="Password"
                  type="password"
                  helperText="Use at least 8 characters with one letter and one digit."
                  value={form.password}
                  onChange={(event) => setForm({ ...form, password: event.target.value })}
                  slotProps={{
                    htmlInput: { maxLength: 255 },
                  }}
                  required
                  fullWidth
                />

                {error ? <Alert severity="error">{error}</Alert> : null}

                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  startIcon={<PersonAddAlt1Icon />}
                  disabled={isSubmitting}
                >
                  {isSubmitting ? "Creating account..." : "Register"}
                </Button>

                <Typography variant="body2" color="text.secondary">
                  Already registered?{" "}
                  <MuiLink component={Link} to="/login" underline="hover">
                    Go to login
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
