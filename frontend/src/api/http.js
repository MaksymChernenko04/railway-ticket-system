import { API_BASE_URL } from "../config";

const AUTH_STORAGE_KEY = "railway-ticket-system-auth";

export function loadStoredAuth() {
  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function saveStoredAuth(auth) {
  if (!auth) {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    return;
  }

  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
}

async function parseError(response) {
  let message = "Request failed";

  try {
    const data = await response.json();
    if (data?.message) {
      message = data.message;
    } else if (data?.errors) {
      const firstError = Object.values(data.errors)[0];
      if (Array.isArray(firstError) && firstError.length > 0) {
        message = firstError[0];
      }
    }
  } catch {
    if (response.statusText) {
      message = response.statusText;
    }
  }

  const error = new Error(message);
  error.status = response.status;
  throw error;
}

async function request(path, options = {}, accessToken) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };

  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    await parseError(response);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export async function postJson(path, body, accessToken) {
  return request(
    path,
    {
      method: "POST",
      body: JSON.stringify(body),
    },
    accessToken,
  );
}

export async function deleteJson(path, accessToken) {
  return request(
      path,
      {
        method: "DELETE",
      },
      accessToken,
  )
}

export async function getJson(path, accessToken) {
  return request(
    path,
    {
      method: "GET",
    },
    accessToken,
  );
}

export async function refreshAuth(refreshToken) {
  return postJson("/api/auth/refresh", { refreshToken });
}
