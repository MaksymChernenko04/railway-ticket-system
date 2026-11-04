import React from "react";
import ReactDOM from "react-dom/client";
import { CssBaseline, ThemeProvider } from "@mui/material";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { AuthProvider } from "./auth/AuthContext";
import { I18nProvider } from "./i18n/I18nContext.jsx";
import { appTheme } from "./theme";
import "./styles.css";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <ThemeProvider theme={appTheme}>
      <CssBaseline />
      <BrowserRouter>
        <I18nProvider>
          <AuthProvider>
            <App />
          </AuthProvider>
        </I18nProvider>
      </BrowserRouter>
    </ThemeProvider>
  </React.StrictMode>,
);
