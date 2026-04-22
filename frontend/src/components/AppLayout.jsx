import MenuIcon from "@mui/icons-material/Menu";
import TrainIcon from "@mui/icons-material/Train";
import {
  AppBar,
  Avatar,
  Box,
  Button,
  Container,
  IconButton,
  Stack,
  Toolbar,
  Typography,
} from "@mui/material";
import { useState } from "react";
import { Link as RouterLink, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

function NavButton({ to, children }) {
  return (
    <Button
      component={NavLink}
      to={to}
      color="inherit"
      variant="text"
      sx={{
        borderRadius: 0,
        px: 1.5,
        minHeight: 64,
        alignSelf: "stretch",
        transition: "background-color 160ms ease, box-shadow 160ms ease",
        "&:hover": {
          bgcolor: "rgba(255,255,255,0.16)",
        },
        "&.active": {
          bgcolor: "transparent",
          boxShadow: "inset 0 -3px 0 rgba(255,255,255,0.92)",
        },
      }}
    >
      {children}
    </Button>
  );
}

export function AppLayout({ children }) {
  const { isAuthenticated, logout, user } = useAuth();
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const initials = user?.firstName?.[0] || user?.email?.[0] || "U";

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      <AppBar position="sticky" color="primary" elevation={0} square>
        <Container maxWidth="xl">
          <Toolbar disableGutters sx={{ gap: 2 }}>
            <Stack
              component={RouterLink}
              to={isAuthenticated ? "/welcome" : "/home"}
              direction="row"
              spacing={1.25}
              alignItems="center"
              sx={{ color: "inherit", textDecoration: "none", flexGrow: 1, minWidth: 0 }}
            >
              <TrainIcon />
              <Box sx={{ minWidth: 0 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                  Railway Ticket System
                </Typography>
              </Box>
            </Stack>

            <Stack
              direction="row"
              spacing={1}
              alignItems="center"
              sx={{ display: { xs: "none", md: "flex" } }}
            >
              {!isAuthenticated ? <NavButton to="/home">Home</NavButton> : null}
              {!isAuthenticated ? <NavButton to="/login">Login</NavButton> : null}
              {!isAuthenticated ? <NavButton to="/register">Register</NavButton> : null}
              {isAuthenticated ? <NavButton to="/welcome">Welcome</NavButton> : null}
              {user?.role === "ADMIN" ? <NavButton to="/admin/stations">Stations</NavButton> : null}
            </Stack>

            <Stack direction="row" spacing={1} alignItems="center">
              {isAuthenticated ? (
                <>
                  <Avatar sx={{ width: 36, height: 36, bgcolor: "secondary.main" }}>
                    {String(initials).toUpperCase()}
                  </Avatar>
                  <Button
                    color="inherit"
                    variant="text"
                    onClick={logout}
                    sx={{
                      display: { xs: "none", sm: "inline-flex" },
                      borderRadius: 0,
                      "&:hover": {
                        bgcolor: "rgba(255,255,255,0.16)",
                      },
                    }}
                  >
                    Logout
                  </Button>
                </>
              ) : null}

              <IconButton
                color="inherit"
                sx={{ display: { xs: "inline-flex", md: "none" } }}
                onClick={() => setIsMenuOpen((current) => !current)}
              >
                <MenuIcon />
              </IconButton>
            </Stack>
          </Toolbar>

          {isMenuOpen ? (
            <Stack spacing={1.25} sx={{ display: { xs: "flex", md: "none" }, pb: 2 }}>
              {!isAuthenticated ? <NavButton to="/home">Home</NavButton> : null}
              {!isAuthenticated ? <NavButton to="/login">Login</NavButton> : null}
              {!isAuthenticated ? <NavButton to="/register">Register</NavButton> : null}
              {isAuthenticated ? <NavButton to="/welcome">Welcome</NavButton> : null}
              {user?.role === "ADMIN" ? <NavButton to="/admin/stations">Stations</NavButton> : null}
              {isAuthenticated ? (
                <Button
                  color="inherit"
                  onClick={logout}
                  sx={{
                    justifyContent: "flex-start",
                    borderRadius: 0,
                    "&:hover": {
                      bgcolor: "rgba(255,255,255,0.16)",
                    },
                  }}
                >
                  Logout
                </Button>
              ) : null}
            </Stack>
          ) : null}
        </Container>
      </AppBar>

      {children ?? <Outlet />}
    </Box>
  );
}
