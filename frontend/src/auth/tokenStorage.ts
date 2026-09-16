const ACCESS_TOKEN_KEY = 'incident-hub.access-token'

export const AUTH_EXPIRED_EVENT = 'incident-hub:auth-expired'

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function setAccessToken(token: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, token)
}

export function clearAccessToken(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
}

export function notifyAuthenticationExpired(): void {
  window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT))
}
