import { createTheme } from "@mui/material/styles";

export const appTheme = createTheme({
  palette: {
    mode: "light",
    primary: {
      main: "#4f8a5b",
      dark: "#3b6b46",
      light: "#79a884",
    },
    secondary: {
      main: "#5f8f66",
    },
    background: {
      default: "#e2ebe0",
      paper: "#fbfdf9",
    },
    success: {
      main: "#5d9764",
    },
    text: {
      primary: "#223128",
      secondary: "#5a6c5f",
    },
  },
  shape: {
    borderRadius: 16,
  },
  typography: {
    fontFamily: '"Segoe UI", Tahoma, Geneva, Verdana, sans-serif',
    h1: {
      fontWeight: 800,
      letterSpacing: "-0.04em",
    },
    h2: {
      fontWeight: 700,
      letterSpacing: "-0.03em",
    },
    h4: {
      fontWeight: 700,
      letterSpacing: "-0.03em",
    },
    button: {
      textTransform: "none",
      fontWeight: 600,
    },
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 24,
          boxShadow: "0 18px 38px rgba(53, 86, 58, 0.08)",
          border: "1px solid rgba(95, 143, 102, 0.14)",
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          borderRadius: 20,
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          borderRadius: 0,
          backgroundImage: "none",
          boxShadow: "none",
          borderBottom: "1px solid rgba(255,255,255,0.16)",
        },
      },
    },
    MuiButton: {
      defaultProps: {
        disableElevation: true,
      },
      styleOverrides: {
        root: {
          borderRadius: 12,
          minHeight: 44,
          transition: "background-color 160ms ease, border-color 160ms ease, color 160ms ease, transform 160ms ease",
          "&:hover": {
            transform: "translateY(-1px)",
          },
        },
        containedPrimary: {
          backgroundColor: "#4f8a5b",
          "&:hover": {
            backgroundColor: "#3f744a",
          },
        },
        outlinedPrimary: {
          borderColor: "rgba(79, 138, 91, 0.42)",
          color: "#3f744a",
          "&:hover": {
            borderColor: "#4f8a5b",
            backgroundColor: "rgba(79, 138, 91, 0.10)",
          },
        },
        textPrimary: {
          "&:hover": {
            backgroundColor: "rgba(79, 138, 91, 0.12)",
          },
        },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        root: {
          transition: "background-color 160ms ease, color 160ms ease",
          "&:hover": {
            backgroundColor: "rgba(79, 138, 91, 0.14)",
          },
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          transition: "border-color 160ms ease, box-shadow 160ms ease, background-color 160ms ease",
          "&:hover .MuiOutlinedInput-notchedOutline": {
            borderColor: "rgba(79, 138, 91, 0.55)",
          },
          "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
            borderColor: "#4f8a5b",
            borderWidth: 2,
          },
        },
      },
    },
  },
});
