import { createContext, useContext, useEffect, useMemo, useState } from "react";
import {deleteJson, getJson, loadStoredAuth, postJson, refreshAuth, saveStoredAuth} from "../api/http";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [authState, setAuthState] = useState(() => loadStoredAuth());
  const [isBootstrapping, setIsBootstrapping] = useState(true);

  useEffect(() => {
    let isActive = true;

    async function bootstrap() {
      if (!authState?.accessToken) {
        setIsBootstrapping(false);
        return;
      }

      try {
        const user = await getJson("/api/auth/me", authState.accessToken);
        if (isActive) {
          const nextState = { ...authState, user };
          setAuthState(nextState);
          saveStoredAuth(nextState);
        }
      } catch (error) {
        if (error.status === 401 && authState.refreshToken) {
          try {
            const refreshed = await refreshAuth(authState.refreshToken);
            const user = await getJson("/api/auth/me", refreshed.accessToken);
            const nextState = { ...refreshed, user };
            if (isActive) {
              setAuthState(nextState);
              saveStoredAuth(nextState);
            }
          } catch {
            if (isActive) {
              setAuthState(null);
              saveStoredAuth(null);
            }
          }
        } else if (isActive) {
          setAuthState(null);
          saveStoredAuth(null);
        }
      } finally {
        if (isActive) {
          setIsBootstrapping(false);
        }
      }
    }

    bootstrap();

    return () => {
      isActive = false;
    };
  }, []);

  const processError = async (error) => {
    if (error.status !== 401 || !authState?.refreshToken) {
      throw error;
    }

    const refreshed = await refreshAuth(authState.refreshToken);

    const nextState = {
      ...refreshed,
      user: refreshed.user ?? authState.user,
    };

    setAuthState(nextState);
    saveStoredAuth(nextState);

    return refreshed.accessToken;
  };

  const authorizedRequest = async (requestFn) => {
    let token = authState?.accessToken;

    if (!token) {
      throw new Error("Authentication is required");
    }

    try {
      return await requestFn(token);
    } catch (error) {
      token = await processError(error);
      return requestFn(token);
    }
  };

  const value = useMemo(
    () => ({
      accessToken: authState?.accessToken ?? null,
      refreshToken: authState?.refreshToken ?? null,
      user: authState?.user ?? null,
      isAuthenticated: Boolean(authState?.accessToken),
      isBootstrapping,
      async login(credentials) {
        const auth = await postJson("/api/auth/login", credentials);
        setAuthState(auth);
        saveStoredAuth(auth);
        return auth;
      },
      async register(payload) {
        const auth = await postJson("/api/auth/register", payload);
        setAuthState(auth);
        saveStoredAuth(auth);
        return auth;
      },
      logout() {
        setAuthState(null);
        saveStoredAuth(null);
      },
      authorizedGet(path) {
        return authorizedRequest((token) => getJson(path, token));
      },

      authorizedPost(path, body) {
        return authorizedRequest((token) => postJson(path, body, token));
      },

      authorizedDelete(path) {
        return authorizedRequest((token) => deleteJson(path, token));
      },
    }),
    [authState, isBootstrapping],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
