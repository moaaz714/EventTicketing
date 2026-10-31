import { useEffect, useRef, useState } from 'react'

interface Props {
  label: string
  resolved: boolean
  failed?: boolean
  size?: 'sm' | 'md' | 'lg'
  /** Announce changes to screen readers. Only for the one status a page is about. */
  live?: boolean
}

/**
 * The signature move: unresolved states print in pixels; when a state
 * settles, the pixel word dithers out and the smooth word fades in.
 */
export function StatusWord({ label, resolved, failed = false, size = 'md', live = false }: Props) {
  const prev = useRef({ label, resolved })
  const [leaving, setLeaving] = useState<string | null>(null)

  useEffect(() => {
    const was = prev.current
    prev.current = { label, resolved }
    if (!was.resolved && resolved && was.label !== label) {
      setLeaving(was.label)
      const t = setTimeout(() => setLeaving(null), 420)
      return () => clearTimeout(t)
    }
  }, [label, resolved])

  const tone = failed ? 'is-failed' : resolved ? 'is-settled' : 'is-live'
  return (
    <span className={`status status-${size} ${tone}`} role={live ? 'status' : undefined}>
      {leaving && (
        <span className="status-word pixel status-out" aria-hidden="true">
          {leaving}
        </span>
      )}
      <span className={`status-word ${resolved ? 'status-smooth' : 'pixel'} ${leaving ? 'status-in' : ''}`}>
        {label}
      </span>
    </span>
  )
}
