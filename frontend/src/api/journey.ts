import {
  completeBooking,
  confirmBooking,
  getBooking,
  issueTickets,
  patchBooking,
  payForBooking,
} from './endpoints'
import type { Booking, BookingMetadata, BookingStatus, PaymentMethod, TicketSale } from './types'

/*
 * The booking journey as the backend actually implements it:
 *
 *   PENDING → confirm → CONFIRMED → (check in) → CHECKED_IN → complete → COMPLETED
 *     → booking.completed: sales-service opens a pending sale
 *     → PAYMENT_PENDING → pay → PAID, or payment.failed → CANCELLED
 *
 * No endpoint sets CHECKED_IN, and no consumer moves a booking to
 * PAYMENT_PENDING or PAID, so when the saga does not move the booking within
 * a few seconds this app sets the status through the generic update and
 * records it in metadata.appSet. The UI shows which steps were set that way.
 */

export const STEPS = ['Reserved', 'Confirmed', 'Checked in', 'Payment', 'Paid'] as const

export type Phase = 'you' | 'system' | 'settled'

export interface JourneyView {
  label: string
  /** Number of steps reached, 0–5. */
  reached: number
  phase: Phase
  failed: boolean
}

const SAGA_WAIT_MS = 5000
const POLL_MS = 700

export function isAppBooking(b: Booking) {
  return Boolean(b.metadata?.app)
}

export function view(b: Booking): JourneyView {
  const app = isAppBooking(b)
  switch (b.status) {
    case 'PENDING':
      return { label: 'Reserved', reached: 1, phase: 'you', failed: false }
    case 'CONFIRMED':
      return { label: 'Confirmed', reached: 2, phase: 'you', failed: false }
    case 'CHECKED_IN':
      return { label: 'Checked in', reached: 3, phase: 'you', failed: false }
    case 'COMPLETING':
      return { label: 'Completing', reached: 3, phase: 'system', failed: false }
    case 'COMPLETED':
      // Seeded bookings and paid-then-ticketed ones are finished; an app
      // booking without a sale is waiting for the saga to open payment.
      if (app && !b.metadata?.saleId)
        return { label: 'Completing', reached: 3, phase: 'system', failed: false }
      return { label: app ? 'Paid' : 'Completed', reached: 5, phase: 'settled', failed: false }
    case 'PAYMENT_PENDING':
      return { label: 'Payment pending', reached: 3, phase: 'you', failed: false }
    case 'PAID':
      return { label: 'Paid', reached: 5, phase: 'settled', failed: false }
    case 'PAYMENT_FAILED':
      return { label: 'Payment failed', reached: 3, phase: 'settled', failed: true }
    case 'REFUNDED':
      return { label: 'Refunded', reached: 3, phase: 'settled', failed: true }
    case 'CANCELLED':
      // Mark the step the booking stopped at: payment if a sale was tried,
      // otherwise the step after the last one it reached.
      return {
        label: 'Cancelled',
        reached: b.metadata?.saleId ? 3 : b.confirmedAt ? 2 : 1,
        phase: 'settled',
        failed: true,
      }
  }
}

export const isLive = (b: Booking) => view(b).phase !== 'settled'

function sleep(ms: number) {
  return new Promise((r) => setTimeout(r, ms))
}

async function waitFor(id: number, done: (b: Booking) => boolean): Promise<Booking | null> {
  const until = Date.now() + SAGA_WAIT_MS
  while (Date.now() < until) {
    await sleep(POLL_MS)
    const b = await getBooking(id)
    if (done(b)) return b
  }
  return null
}

function withMeta(b: Booking, extra: BookingMetadata, setByApp?: BookingStatus): BookingMetadata {
  const meta = { ...(b.metadata ?? {}), ...extra }
  if (setByApp) meta.appSet = [...new Set([...(meta.appSet ?? []), setByApp])]
  return meta
}

export async function confirm(b: Booking) {
  return confirmBooking(b.id, b.eventId)
}

/** There is no check-in endpoint; the generic update is the only route. */
export async function checkIn(b: Booking) {
  return patchBooking(b.id, { status: 'CHECKED_IN', metadata: withMeta(b, {}, 'CHECKED_IN') })
}

export async function startPayment(b: Booking, onPhase: (label: string) => void) {
  onPhase('Completing')
  const completed = await completeBooking(b.id)
  onPhase('Waiting for the saga')
  const moved = await waitFor(b.id, (x) => x.status === 'PAYMENT_PENDING')
  if (moved) return moved
  return patchBooking(b.id, {
    status: 'PAYMENT_PENDING',
    metadata: withMeta(completed, {}, 'PAYMENT_PENDING'),
  })
}

/** For a booking left completed without payment open (e.g. after a reload). */
export async function openPayment(b: Booking) {
  return patchBooking(b.id, {
    status: 'PAYMENT_PENDING',
    metadata: withMeta(b, {}, 'PAYMENT_PENDING'),
  })
}

export interface PayInput {
  method: PaymentMethod
  cardLastFour?: string
  simulateFailure: boolean
  attendeeName: string
}

export async function pay(b: Booking, input: PayInput, onPhase: (label: string) => void) {
  onPhase('Charging')
  const sale: TicketSale = await payForBooking(
    b.id,
    { method: input.method, cardLastFour: input.cardLastFour || undefined },
    input.simulateFailure,
  )

  if (sale.status === 'FAILED') {
    onPhase('Compensating')
    const settled = await waitFor(b.id, (x) =>
      ['CANCELLED', 'REFUNDED', 'PAYMENT_FAILED'].includes(x.status),
    )
    const latest = settled ?? (await getBooking(b.id))
    if (settled) {
      return patchBooking(b.id, { metadata: withMeta(latest, { saleId: sale.id }) })
    }
    return patchBooking(b.id, {
      status: 'PAYMENT_FAILED',
      metadata: withMeta(latest, { saleId: sale.id }, 'PAYMENT_FAILED'),
    })
  }

  onPhase('Issuing tickets')
  const qty = Math.max(1, Number(b.metadata?.quantity ?? 1))
  const stamp = Date.now().toString(36).slice(-4).toUpperCase()
  await issueTickets(
    b.id,
    Array.from({ length: qty }, (_, i) => ({
      attendeeName: input.attendeeName,
      ticketCode: `ET-${b.id}-${i + 1}-${stamp}`,
    })),
  )

  const paid = await waitFor(b.id, (x) => x.status === 'PAID' || x.status === 'COMPLETED')
  const latest = paid ?? (await getBooking(b.id))
  if (paid) return patchBooking(b.id, { metadata: withMeta(latest, { saleId: sale.id }) })
  return patchBooking(b.id, {
    status: 'PAID',
    metadata: withMeta(latest, { saleId: sale.id }, 'PAID'),
  })
}
