import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import PersonAddAlt1Icon from "@mui/icons-material/PersonAddAlt1";
import TrainIcon from "@mui/icons-material/Train";
import {
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Stack,
  Typography,
} from "@mui/material";
import { Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useI18n } from "../i18n/I18nContext.jsx";

export function HomePage() {
  const { isAuthenticated } = useAuth();
  const { t } = useI18n();
  const navigate = useNavigate();

  if (isAuthenticated) {
    return <Navigate to="/welcome" replace />;
  }

  return (
    <Box
      sx={{
        minHeight: "calc(100vh - 64px)",
        display: "flex",
        alignItems: "center",
        background:
            "radial-gradient(circle at top left, rgba(92,133,255,0.18), transparent 26%), linear-gradient(135deg, #e8eef2 0%, #dde9e3 100%)",
        py: 6,
      }}
    >
      <Container maxWidth="lg">
        <Card sx={{ bgcolor: "rgba(255,255,255,0.84)", backdropFilter: "blur(12px)" }}>
          <CardContent sx={{ p: { xs: 3, md: 6 } }}>
            <Stack spacing={4} alignItems="flex-start">
              <Box
                sx={{
                  width: 72,
                  height: 72,
                  borderRadius: 4,
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
                  {t("Home")}
                </Typography>
                <Typography variant="h1" sx={{ mt: 1, fontSize: { xs: "2.8rem", md: "4.4rem" }, maxWidth: 1000 }}>
                  {t("Railway access starts here.")}
                </Typography>
              </Box>

              <Typography variant="h6" color="text.secondary" sx={{ maxWidth: 760, fontWeight: 400 }}>
                {t("Plan future journeys, manage your account, and use railway services in one place. Sign in if you already have an account or register to get started.")}
              </Typography>

              <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
                <Button
                  variant="contained"
                  size="large"
                  endIcon={<ArrowForwardIcon />}
                  onClick={() => navigate("/login")}
                >
                  {t("Login")}
                </Button>
                <Button
                  variant="outlined"
                  size="large"
                  startIcon={<PersonAddAlt1Icon />}
                  onClick={() => navigate("/register")}
                >
                  {t("Register")}
                </Button>
              </Stack>
            </Stack>
          </CardContent>
        </Card>
      </Container>
    </Box>
  );
}
