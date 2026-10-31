import { useMemo } from 'react'
import { seeded } from '../lib/format'

interface Props {
  seed: number
  cols?: number
  rows?: number
  className?: string
}

/**
 * Event imagery: a halftone field generated from the event id. There is no
 * photography in the backend, so every event gets its own stable plate.
 */
export function Halftone({ seed, cols = 24, rows = 16, className = '' }: Props) {
  const dots = useMemo(() => {
    const rand = seeded(seed + 7)
    const blobs = Array.from({ length: 3 }, () => ({
      x: rand() * cols,
      y: rand() * rows,
      r: (0.25 + rand() * 0.45) * Math.max(cols, rows),
    }))
    const out: { x: number; y: number; r: number }[] = []
    for (let y = 0; y < rows; y++) {
      for (let x = 0; x < cols; x++) {
        let v = 0
        for (const b of blobs) {
          const d = Math.hypot(x - b.x, y - b.y) / b.r
          v += Math.exp(-d * d * 2.2)
        }
        v = Math.min(1, v * 0.9 + (rand() - 0.5) * 0.12)
        // Cap the dot below the cell so the screen never closes into a solid.
        if (v > 0.08) out.push({ x, y, r: 0.42 * Math.sqrt(v) })
      }
    }
    return out
  }, [seed, cols, rows])

  return (
    <svg
      className={`halftone ${className}`}
      viewBox={`0 0 ${cols} ${rows}`}
      preserveAspectRatio="xMidYMid slice"
      aria-hidden="true"
    >
      {dots.map((d, i) => (
        <rect
          key={i}
          x={d.x + 0.5 - d.r}
          y={d.y + 0.5 - d.r}
          width={d.r * 2}
          height={d.r * 2}
        />
      ))}
    </svg>
  )
}
