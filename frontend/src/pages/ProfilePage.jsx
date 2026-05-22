import AccountCircleIcon from "@mui/icons-material/AccountCircle";
import BadgeIcon from "@mui/icons-material/Badge";
import EmailIcon from "@mui/icons-material/Email";
import PersonIcon from "@mui/icons-material/Person";
import {
  Avatar,
  Box,
  Card,
  CardContent,
  Chip,
  Container,
  Divider,
  Stack,
  Typography,
} from "@mui/material";
import { Navigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

function ProfileField({ icon, label, value }) {
  return (
    <Stack
      direction="row"
      spacing={2}
      alignItems="center"
      sx={{
        minHeight: 72,
        px: { xs: 0, sm: 1 },
      }}
    >
      <Box
        sx={{
          width: 44,
          height: 44,
          borderRadius: 2,
          display: "grid",
          placeItems: "center",
          bgcolor: "rgba(79, 138, 91, 0.12)",
          color: "primary.main",
          flexShrink: 0,
        }}
      >
        {icon}
      </Box>
      <Box sx={{ minWidth: 0 }}>
        <Typography variant="body2" color="text.secondary">
          {label}
        </Typography>
        <Typography variant="h6" sx={{ overflowWrap: "anywhere" }}>
          {value || "Not specified"}
        </Typography>
      </Box>
    </Stack>
  );
}

export function ProfilePage() {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/home" replace />;
  }

  const fullName = [user?.firstName, user?.lastName].filter(Boolean).join(" ");
  const initials = user?.firstName?.[0] || user?.email?.[0] || "U";

  return (
    <Box
      sx={{
        minHeight: "calc(100vh - 64px)",
        py: 5,
        background:
          "radial-gradient(circle at top left, rgba(92,133,255,0.18), transparent 26%), linear-gradient(135deg, #f4f7fb 0%, #edf7f3 100%)",
      }}
    >
      <Container maxWidth="lg">
        <Stack spacing={3}>
          <Card sx={{ bgcolor: "rgba(255,255,255,0.88)", backdropFilter: "blur(12px)" }}>
            <CardContent sx={{ p: { xs: 3, md: 4 } }}>
              <Stack
                direction={{ xs: "column", sm: "row" }}
                spacing={3}
                alignItems={{ xs: "flex-start", sm: "center" }}
              >
                <Avatar
                  sx={{
                    width: 88,
                    height: 88,
                    bgcolor: "secondary.main",
                    fontSize: "2rem",
                    fontWeight: 700,
                  }}
                >
                  {String(initials).toUpperCase()}
                </Avatar>

                <Box sx={{ minWidth: 0, flexGrow: 1 }}>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                    <AccountCircleIcon color="primary" />
                    <Typography
                      variant="overline"
                      color="primary.main"
                      sx={{ letterSpacing: "0.16em", fontWeight: 700 }}
                    >
                      Profile
                    </Typography>
                  </Stack>
                  <Typography variant="h4" sx={{ overflowWrap: "anywhere" }}>
                    {fullName || user?.email}
                  </Typography>
                </Box>

                <Chip color="primary" label={user?.role || "USER"} />
              </Stack>
            </CardContent>
          </Card>

          <Card>
            <CardContent sx={{ p: { xs: 3, md: 4 } }}>
              <Stack spacing={2}>
                <Box>
                  <Typography variant="h5">Account details</Typography>
                </Box>

                <Divider />

                <Box
                  sx={{
                    display: "grid",
                    gap: { xs: 1.5, md: 2 },
                    gridTemplateColumns: { xs: "1fr", md: "1fr 1fr" },
                  }}
                >
                  <ProfileField icon={<PersonIcon />} label="First name" value={user?.firstName} />
                  <ProfileField icon={<PersonIcon />} label="Last name" value={user?.lastName} />
                  <ProfileField icon={<EmailIcon />} label="Email" value={user?.email} />
                  <ProfileField icon={<BadgeIcon />} label="User ID" value={user?.id} />
                </Box>
              </Stack>
            </CardContent>
          </Card>
        </Stack>
      </Container>
    </Box>
  );
}
