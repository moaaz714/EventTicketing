import type { EventCategory } from '../api/types'

const money = new Intl.NumberFormat('en-EG', { maximumFractionDigits: 0 })

export function formatMoney(amount: number | null | undefined) {
  if (amount === null || amount === undefined || Number.isNaN(amount)) return 'EGP —'
  return `EGP ${money.format(amount)}`
}

// Backend LocalDateTime values arrive without a zone; treat them as local.
export function parseDate(value: string | null | undefined) {
  if (!value) return null
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? null : d
}

export function formatDay(value: string) {
  const d = parseDate(value)
  return d ? d.toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' }) : '—'
}

export function formatTime(value: string) {
  const d = parseDate(value)
  return d ? d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }) : ''
}

export function formatLongDate(value: string) {
  const d = parseDate(value)
  return d
    ? d.toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
    : '—'
}

export const CATEGORY_LABEL: Record<EventCategory, string> = {
  CONCERT: 'Concert',
  SPORTS: 'Sports',
  THEATER: 'Theater',
  CONFERENCE: 'Conference',
  FESTIVAL: 'Festival',
}

export const CATEGORIES = Object.keys(CATEGORY_LABEL) as EventCategory[]

/** Small deterministic PRNG so generated patterns are stable per id. */
export function seeded(seed: number) {
  let s = (seed * 2654435761) >>> 0 || 1
  return () => {
    s ^= s << 13
    s ^= s >>> 17
    s ^= s << 5
    return ((s >>> 0) % 10000) / 10000
  }
}

export function hashString(value: string) {
  let h = 2166136261
  for (let i = 0; i < value.length; i++) {
    h ^= value.charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  return h >>> 0
}
