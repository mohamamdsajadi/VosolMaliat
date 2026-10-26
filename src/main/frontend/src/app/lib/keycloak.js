import { clearAuthTokens, setAuthTokens } from './api';
import { fetchIdentityConfiguration, loginWithMockIdentity, logoutMockIdentity, safeLoginNext } from './identity-provider';

const DEFAULT_REALM = 'vosol-maliat';

export function getKeycloakConfig() {
  const baseUrl = process.env.NEXT_PUBLIC_KEYCLOAK_URL || 'https://keycloack.parand.mparsict.com/auth';
  const realm = process.env.NEXT_PUBLIC_KEYCLOAK_REALM || DEFAULT_REALM;
  const clientId = process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID || 'vosol-frontend';
  const appUrl = process.env.NEXT_PUBLIC_APP_URL || currentOrigin();

  return {
    baseUrl: baseUrl.replace(/\/$/, ''),
    realm,
    clientId,
    redirectUri: `${appUrl.replace(/\/$/, '')}/auth/callback`
  };
}

export async function startKeycloakLogin(nextPath = '/', mockUsername) {
  const identity = await fetchIdentityConfiguration();
  if (identity.provider === 'mock') {
    return loginWithMockIdentity(mockUsername, nextPath);
  }
  const config = getKeycloakConfig();
  const verifier = createCodeVerifier();
  const challenge = await createCodeChallenge(verifier);
  const state = crypto.randomUUID();

  window.sessionStorage.setItem('pkce_verifier', verifier);
  window.sessionStorage.setItem('oauth_state', state);
  window.sessionStorage.setItem('login_next', safeLoginNext(nextPath));

  const authorizeUrl = new URL(`${config.baseUrl}/realms/${config.realm}/protocol/openid-connect/auth`);
  authorizeUrl.searchParams.set('client_id', config.clientId);
  authorizeUrl.searchParams.set('redirect_uri', config.redirectUri);
  authorizeUrl.searchParams.set('response_type', 'code');
  authorizeUrl.searchParams.set('scope', 'openid profile email');
  authorizeUrl.searchParams.set('state', state);
  authorizeUrl.searchParams.set('code_challenge', challenge);
  authorizeUrl.searchParams.set('code_challenge_method', 'S256');

  window.location.assign(authorizeUrl.toString());
}

export async function completeKeycloakLogin(code, state) {
  const identity = await fetchIdentityConfiguration();
  if (identity.provider !== 'keycloak') throw new Error('ورود سازمانی غیرفعال است؛ از صفحه ورود وارد شوید.');
  const expectedState = window.sessionStorage.getItem('oauth_state');
  const verifier = window.sessionStorage.getItem('pkce_verifier');
  if (!code || !state || state !== expectedState || !verifier) {
    throw new Error('پاسخ ورود معتبر نیست.');
  }

  const config = getKeycloakConfig();
  const body = new URLSearchParams();
  body.set('grant_type', 'authorization_code');
  body.set('client_id', config.clientId);
  body.set('redirect_uri', config.redirectUri);
  body.set('code', code);
  body.set('code_verifier', verifier);

  const response = await fetch(`${config.baseUrl}/realms/${config.realm}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body
  });

  if (!response.ok) {
    throw new Error('دریافت توکن از Keycloak ناموفق بود.');
  }

  const token = await response.json();
  setAuthTokens({
    accessToken: token.access_token,
    refreshToken: token.refresh_token,
    expiresIn: token.expires_in
  });
  window.sessionStorage.removeItem('oauth_state');
  window.sessionStorage.removeItem('pkce_verifier');
}

export function consumeLoginNext() {
  if (typeof window === 'undefined') {
    return '/';
  }
  const next = window.sessionStorage.getItem('login_next') || '/';
  window.sessionStorage.removeItem('login_next');
  return safeLoginNext(next);
}

export async function logoutFromKeycloak() {
  try {
    const identity = await fetchIdentityConfiguration();
    if (identity.provider === 'mock') return await logoutMockIdentity();
    logoutRealIdentity();
  } catch (error) {
    window.alert(error.message || 'خروج ناموفق بود؛ دوباره تلاش کنید.');
  }
}

function logoutRealIdentity() {
  const config = getKeycloakConfig();
  clearAuthTokens();
  const logoutUrl = new URL(`${config.baseUrl}/realms/${config.realm}/protocol/openid-connect/logout`);
  logoutUrl.searchParams.set('client_id', config.clientId);
  logoutUrl.searchParams.set('post_logout_redirect_uri', `${currentOrigin()}/login`);
  window.location.assign(logoutUrl.toString());
}

function currentOrigin() {
  if (typeof window === 'undefined') {
    return 'http://localhost:3000';
  }
  return window.location.origin;
}

function createCodeVerifier() {
  const values = new Uint8Array(64);
  crypto.getRandomValues(values);
  return base64Url(values);
}

async function createCodeChallenge(verifier) {
  const data = new TextEncoder().encode(verifier);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return base64Url(new Uint8Array(digest));
}

function base64Url(bytes) {
  return btoa(String.fromCharCode(...bytes))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}
