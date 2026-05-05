import { API_BASE_URL } from "../config";

const AUTH_STORAGE_KEY = "railway-ticket-system-auth";

export function loadStoredAuth() {
  const raw = globalThis.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch {
    globalThis.localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function saveStoredAuth(auth) {
  if (!auth) {
    globalThis.localStorage.removeItem(AUTH_STORAGE_KEY);
    return;
  }

  globalThis.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
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

async function request(path, accessToken, options) {
  const headers = {
    "Content-Type": "application/json",
  };

  if (options?.headers) {
    Object.assign(headers, options.headers);
  }

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

  const text = await response.text();
  if (!text) {
    return null;
  }

  return JSON.parse(text);
}

export async function postJson(path, body, accessToken) {
  return request(
    path,
    accessToken,
    {
      method: "POST",
      body: JSON.stringify(body),
    }
  );
}

export async function deleteJson(path, accessToken) {
  return request(
      path,
      accessToken,
      {
        method: "DELETE",
      }
  )
}

export async function getJson(path, accessToken) {
  return request(
    path,
    accessToken,
    {
      method: "GET",
    }
  );
}

export async function refreshAuth(refreshToken) {
  return postJson("/api/auth/refresh", { refreshToken });
}
