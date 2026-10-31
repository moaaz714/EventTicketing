const TOKEN_KEY = 'et.token'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export function readToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function writeToken(token: string | null) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token)
    else localStorage.removeItem(TOKEN_KEY)
  } catch {
    // Storage can be unavailable (private mode); the session then lasts one tab.
  }
}

let onUnauthorized: () => void = () => {}

/** Registered by the auth provider so a 401 anywhere signs the user out. */
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

// Services answer errors in three shapes: Spring's default
// { error, message?, status }, the JWT filter's { error }, or plain text.
async function readError(res: Response): Promise<string> {
  const text = await res.text().catch(() => '')
  if (!text) return defaultMessage(res.status)
  try {
    const body = JSON.parse(text) as { message?: string; error?: string }
    return body.message || body.error || defaultMessage(res.status)
  } catch {
    return text.length < 200 ? text : defaultMessage(res.status)
  }
}

function defaultMessage(status: number): string {
  if (status === 401) return 'Your session has ended. Sign in again.'
  if (status === 403) return 'You don’t have access to this.'
  if (status === 404) return 'Not found.'
  if (status === 502 || status === 503 || status === 504)
    return 'A backend service is unavailable. Check that the stack is running.'
  return `Request failed (${status}).`
}

type Query = Record<string, string | number | boolean | undefined | null>

export interface RequestOptions {
  method?: string
  body?: unknown
  query?: Query
}

export async function api<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const url = new URL(path, window.location.origin)
  for (const [k, v] of Object.entries(opts.query ?? {})) {
    if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, String(v))
  }

  const headers: Record<string, string> = { Accept: 'application/json' }
  const token = readToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json'

  let res: Response
  try {
    res = await fetch(url, {
      method: opts.method ?? 'GET',
      headers,
      body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
    })
  } catch {
    throw new ApiError(0, 'Can’t reach the gateway on :8080. Is the backend running?')
  }

  if (res.status === 401 && token) onUnauthorized()
  if (!res.ok) throw new ApiError(res.status, await readError(res))

  const text = await res.text()
  if (!text) return undefined as T
  try {
    return JSON.parse(text) as T
  } catch {
    return text as T
  }
}
