import {
  clearAccessToken,
  getAccessToken,
  notifyAuthenticationExpired,
} from '../auth/tokenStorage'

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

export interface ApiProblem {
  detail?: string
  errors?: Array<{ field?: string; message?: string }>
  instance?: string
  status?: number
  title?: string
  type?: string
}

export class ApiError extends Error {
  readonly status: number
  readonly problem?: ApiProblem

  constructor(status: number, problem?: ApiProblem) {
    super(problem?.detail ?? `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

function requestUrl(generatedUrl: string): string {
  const url = new URL(generatedUrl, window.location.origin)
  return `${apiBaseUrl}${url.pathname}${url.search}`
}

async function responseBody(response: Response): Promise<unknown> {
  if (response.status === 204) {
    return undefined
  }

  const contentType = response.headers.get('content-type')
  return contentType?.includes('json') ? response.json() : response.text()
}

export async function apiFetch<T>(
  url: string,
  options: RequestInit,
): Promise<T> {
  const token = getAccessToken()
  const headers = new Headers(options.headers)

  headers.set('Accept', 'application/json')
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(requestUrl(url), { ...options, headers })
  const body = await responseBody(response)

  if (!response.ok) {
    if (response.status === 401) {
      clearAccessToken()
      notifyAuthenticationExpired()
    }
    throw new ApiError(response.status, body as ApiProblem | undefined)
  }

  return body as T
}
