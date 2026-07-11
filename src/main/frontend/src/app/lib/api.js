export function getAccessToken() {
  if (typeof window === "undefined") {
    return null;
  }
  return window.localStorage.getItem("access_token");
}

export function getRefreshToken() {
  if (typeof window === "undefined") {
    return null;
  }
  return window.localStorage.getItem("refresh_token");
}

export function setAccessToken(token) {
  if (typeof window === "undefined") {
    return;
  }
  if (token) {
    window.localStorage.setItem("access_token", token);
  } else {
    window.localStorage.removeItem("access_token");
  }
}

export function setAuthTokens({ accessToken, refreshToken, expiresIn }) {
  if (typeof window === "undefined") {
    return;
  }
  setAccessToken(accessToken);
  if (refreshToken) {
    window.localStorage.setItem("refresh_token", refreshToken);
  }
  if (expiresIn) {
    window.localStorage.setItem("token_expires_at", String(Date.now() + expiresIn * 1000));
  }
}

export function clearAuthTokens() {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.removeItem("access_token");
  window.localStorage.removeItem("refresh_token");
  window.localStorage.removeItem("token_expires_at");
}

export async function apiFetch(path, options = {}) {
  const token = getAccessToken();
  const headers = new Headers(options.headers || {});
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  return fetch(path, { ...options, headers });
}

export async function apiJson(path, options = {}) {
  const response = await apiFetch(path, options);
  if (!response.ok) {
    const details = await response.text();
    if (response.status === 401 && typeof window !== "undefined") {
      clearAuthTokens();
      window.location.replace(`/login?next=${encodeURIComponent(window.location.pathname + window.location.search)}`);
    }
    throw new Error(readErrorMessage(details, response.status));
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

function readErrorMessage(details, status) {
  if (!details) {
    return status === 403
      ? "شما مجوز لازم برای مشاهده یا انجام این عملیات را ندارید."
      : `درخواست ناموفق بود: ${status}`;
  }
  try {
    const parsed = JSON.parse(details);
    return parsed.message || parsed.error || details;
  } catch {
    return details;
  }
}
