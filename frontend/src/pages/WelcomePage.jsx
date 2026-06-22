import CelebrationIcon from "@mui/icons-material/Celebration";
import {
  Box,
  Card,
  CardContent,
  Chip,
  Container,
  Stack,
  Typography,
} from "@mui/material";
import { Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function WelcomePage() {
  const { isAuthenticated, user } = useAuth();
    useNavigate();

    if (!isAuthenticated) {
        return <Navigate to="/home" replace />;
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
                <Card>
                    <CardContent sx={{ p: { xs: 3, md: 6 } }}>
                        <Stack spacing={4} alignItems="flex-start">
                            <Stack direction="row" spacing={2} alignItems="center">
                                <Box
                                    sx={{
                                        width: 72,
                                        height: 72,
                                        borderRadius: 4,
                                        display: "grid",
                                        placeItems: "center",
                                        bgcolor: "secondary.main",
                                        color: "common.white",
                                    }}
                                >
                                    <CelebrationIcon fontSize="large" />
                                </Box>
                                <Chip color="primary" label={user?.role || "USER"} />
                            </Stack>

                            <Box>
                                <Typography
                                    variant="overline"
                                    color="secondary.main"
                                    sx={{ letterSpacing: "0.16em", fontWeight: 700 }}
                                >
                                    Welcome
                                </Typography>
                                <Typography variant="h1" sx={{ mt: 1, fontSize: { xs: "2.8rem", md: "4.2rem" }, maxWidth: 820 }}>
                                    Welcome back, {user?.firstName || user?.email}.
                                </Typography>
                            </Box>

                            <Typography variant="h6" color="text.secondary" sx={{ maxWidth: 760, fontWeight: 400 }}>
                                Your account is ready. Use the navigation bar to continue.
                            </Typography>
                        </Stack>
                    </CardContent>
                </Card>
            </Container>
        </Box>
    );
}

