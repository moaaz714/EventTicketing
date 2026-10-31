import type { ReactNode } from 'react'
import { ApiError } from '../api/client'

export function errorText(error: unknown) {
  if (error instanceof ApiError || error instanceof Error) return error.message
  return 'Something went wrong.'
}

export function Empty({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="empty">
      <p className="pixel empty-title">{title}</p>
      {children}
    </div>
  )
}

export function ErrorBlock({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  return (
    <div className="error-block" role="alert">
      <p>{errorText(error)}</p>
      {onRetry && (
        <button className="btn btn-ghost" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  )
}

/** Loading rows drawn as dither bars, not spinners. */
export function Loading({ rows = 4, label = 'Loading' }: { rows?: number; label?: string }) {
  return (
    <div className="loading" role="status" aria-label={label}>
      {Array.from({ length: rows }, (_, i) => (
        <span key={i} className="loading-row" style={{ width: `${92 - i * 11}%` }} />
      ))}
    </div>
  )
}
