import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cancelBooking, getUser } from '../api/endpoints'
import * as journey from '../api/journey'
import { keys, useBooking, useEvent, useTickets } from '../api/queries'
import type { Booking, PaymentMethod } from '../api/types'
import { useSession } from '../auth/AuthContext'
import { ErrorBlock, Loading, errorText } from '../components/Feedback'
import { BackIcon } from '../components/Icons'
import { JourneyStrip } from '../components/JourneyStrip'
import { PixelTitle } from '../components/PixelTitle'
import { StatusWord } from '../components/StatusWord'
import { TicketStub } from '../components/TicketStub'
import { useToast } from '../components/Toasts'
import { formatLongDate, formatMoney, formatTime, parseDate } from '../lib/format'

export function BookingPage() {
  const id = Number(useParams().id)
  const booking = useBooking(id)
  const [working, setWorking] = useState<string | null>(null)

  if (booking.isLoading) return <div className="page"><Loading rows={5} label="Loading booking" /></div>
  if (booking.isError || !booking.data)
    return (
      <div className="page">
        <BackLink />
        <ErrorBlock error={booking.error} onRetry={() => booking.refetch()} />
      </div>
    )

  return <BookingView booking={booking.data} working={working} setWorking={setWorking} />
}

function BackLink() {
  return (
    <Link to="/bookings" className="back">
      <BackIcon /> My bookings
    </Link>
  )
}

interface ViewProps {
  booking: Booking
  working: string | null
  setWorking: (s: string | null) => void
}

function BookingView({ booking: b, working, setWorking }: ViewProps) {
  const event = useEvent(b.eventId)
  const v = journey.view(b)
  const appSet = b.metadata?.appSet ?? []
  const name = event.data?.name ?? `Event ${b.eventId}`

  // While an action runs, the status shows what the system is doing now.
  const label = working ?? v.label
  const resolved = !working && v.phase === 'settled'

  return (
    <div className="page booking-page">
      <BackLink />
      <header className="booking-head">
        <PixelTitle text={name} size={48} smooth={v.phase === 'settled' && !working} />
        <div className="booking-status">
          <StatusWord label={label} resolved={resolved} failed={v.failed && !working} size="lg" live />
          {(working || v.phase === 'system') && <span className="muted">Waiting on the backend</span>}
        </div>
        <JourneyStrip view={working ? { ...v, phase: 'system' } : v} labelled appSet={appSet} />
      </header>

      <div className="booking-body">
        <section className="panel step-panel" aria-label="Next step">
          <NextStep booking={b} working={working} setWorking={setWorking} />
        </section>
        <section aria-labelledby="summary-heading">
          <h2 id="summary-heading" className="section-title">
            Summary
          </h2>
          <dl className="facts facts-grid">
            <div>
              <dt>Booking</dt>
              <dd>#{String(b.id).padStart(4, '0')}</dd>
            </div>
            <div>
              <dt>Event</dt>
              <dd>{event.data ? <Link to={`/events/${b.eventId}`}>{name}</Link> : name}</dd>
            </div>
            {event.data && (
              <div>
                <dt>When</dt>
                <dd>
                  {formatLongDate(event.data.eventDate)}, {formatTime(event.data.eventDate)}
                </dd>
              </div>
            )}
            {event.data && (
              <div>
                <dt>Where</dt>
                <dd>{event.data.venue}</dd>
              </div>
            )}
            <div>
              <dt>Tickets</dt>
              <dd>
                {b.metadata?.quantity ?? '—'} {b.metadata?.tier === 'VIP' ? 'VIP' : 'Standard'}
                {b.metadata?.sessionTitle ? `, ${b.metadata.sessionTitle}` : ''}
              </dd>
            </div>
            <div>
              <dt>Total</dt>
              <dd>{formatMoney(b.totalAmount)}</dd>
            </div>
            <div>
              <dt>Contact</dt>
              <dd>{b.contactEmail}</dd>
            </div>
            <div>
              <dt>Booked</dt>
              <dd>{parseDate(b.bookingDate)?.toLocaleString('en-GB') ?? '—'}</dd>
            </div>
          </dl>
          {appSet.length > 0 && (
            <p className="footnote">
              * Updated by this app, not by the booking service: the backend has no endpoint or saga step for
              this transition yet, so the app wrote it through the generic booking update.
            </p>
          )}
        </section>
      </div>

      {(b.status === 'PAID' || (b.status === 'COMPLETED' && b.metadata?.saleId) || b.status === 'PAYMENT_FAILED' ||
        b.status === 'CANCELLED') && <BookingTickets booking={b} eventName={name} />}
    </div>
  )
}

function NextStep({ booking: b, working, setWorking }: ViewProps) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const { userId } = useSession()

  const run = useMutation({
    mutationFn: async (step: () => Promise<Booking | void>) => step(),
    onSettled: () => {
      setWorking(null)
      queryClient.invalidateQueries({ queryKey: keys.booking(b.id) })
      queryClient.invalidateQueries({ queryKey: keys.bookings(userId) })
      queryClient.invalidateQueries({ queryKey: keys.tickets })
    },
    onError: (err) => toast(errorText(err), 'error'),
  })

  const go = (label: string, step: () => Promise<Booking | void>, done?: string) => {
    setWorking(label)
    run.mutate(step, { onSuccess: () => done && toast(done) })
  }

  const busy = Boolean(working) || run.isPending
  const v = journey.view(b)

  const cancel = (
    <CancelControl
      disabled={busy}
      onConfirm={() => go('Cancelling', () => cancelBooking(b.id), 'Booking cancelled.')}
    />
  )

  switch (b.status) {
    case 'PENDING':
      return (
        <>
          <h2 className="step-title">Confirm your booking</h2>
          <p>Confirming checks with the event service that the event is still upcoming, then holds your seats.</p>
          <button className="btn btn-primary" disabled={busy} onClick={() => go('Confirming', () => journey.confirm(b), 'Booking confirmed.')}>
            Confirm booking
          </button>
          {cancel}
        </>
      )
    case 'CONFIRMED':
      return (
        <>
          <h2 className="step-title">Check in</h2>
          <p>
            This backend takes payment after check-in, when the booking is completed. It has no check-in
            endpoint, so the app sets the status directly.
          </p>
          <button className="btn btn-primary" disabled={busy} onClick={() => go('Checking in', () => journey.checkIn(b))}>
            Check in
          </button>
          {cancel}
        </>
      )
    case 'CHECKED_IN':
      return (
        <>
          <h2 className="step-title">Open payment</h2>
          <p>
            Completing the booking publishes <code>booking.completed</code>. The sales service opens a pending
            sale, and the booking should move to payment.
          </p>
          <button className="btn btn-primary" disabled={busy} onClick={() => go('Completing', () => journey.startPayment(b, setWorking))}>
            Continue to payment
          </button>
        </>
      )
    case 'PAYMENT_PENDING':
      return <PaymentForm booking={b} busy={busy} onPay={(input) => go('Charging', () => journey.pay(b, input, setWorking))} />
    default:
      if (v.phase === 'system')
        return (
          <>
            <h2 className="step-title">Waiting for payment to open</h2>
            <p>
              The booking is completed and <code>booking.completed</code> is published. If the saga doesn’t
              move it to payment within a few seconds, the app opens payment itself.
            </p>
            {!busy && (
              <button className="btn btn-primary" onClick={() => go('Opening payment', () => journey.openPayment(b))}>
                Open payment now
              </button>
            )}
          </>
        )
      return <Outcome booking={b} />
  }
}

/** Destructive action kept apart, and asked twice in place rather than in a dialog. */
function CancelControl({ disabled, onConfirm }: { disabled: boolean; onConfirm: () => void }) {
  const [asking, setAsking] = useState(false)
  return (
    <div className="danger-zone">
      {asking ? (
        <div className="confirm-row" role="group" aria-label="Confirm cancellation">
          <span>Release your seats?</span>
          <button className="btn btn-danger" disabled={disabled} onClick={onConfirm} autoFocus>
            Yes, cancel booking
          </button>
          <button className="btn btn-ghost" onClick={() => setAsking(false)}>
            Keep booking
          </button>
        </div>
      ) : (
        <button className="btn btn-danger-outline" disabled={disabled} onClick={() => setAsking(true)}>
          Cancel booking
        </button>
      )}
    </div>
  )
}

const METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'CREDIT_CARD', label: 'Credit card' },
  { value: 'DEBIT_CARD', label: 'Debit card' },
  { value: 'WALLET', label: 'Mobile wallet' },
  { value: 'PAYPAL', label: 'PayPal' },
]

function PaymentForm({
  booking: b,
  busy,
  onPay,
}: {
  booking: Booking
  busy: boolean
  onPay: (input: journey.PayInput) => void
}) {
  const { userId } = useSession()
  const user = useQuery({ queryKey: ['user', userId], queryFn: () => getUser(userId) })
  const [method, setMethod] = useState<PaymentMethod>('CREDIT_CARD')
  const [last4, setLast4] = useState('')
  const [attendee, setAttendee] = useState<string | null>(null)
  const [fail, setFail] = useState(false)
  const [problem, setProblem] = useState<string | null>(null)
  const card = method === 'CREDIT_CARD' || method === 'DEBIT_CARD'
  const name = attendee ?? user.data?.name ?? ''

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (card && !/^\d{4}$/.test(last4)) return setProblem('Enter the last four digits of the card.')
    if (!name.trim()) return setProblem('Enter the name to print on the tickets.')
    setProblem(null)
    onPay({ method, cardLastFour: card ? last4 : undefined, simulateFailure: fail, attendeeName: name.trim() })
  }

  return (
    <>
      <h2 className="step-title">Pay {formatMoney(b.totalAmount)}</h2>
      <form className="form" onSubmit={submit} noValidate>
        <label className="field">
          <span>Payment method</span>
          <select value={method} onChange={(e) => setMethod(e.target.value as PaymentMethod)}>
            {METHODS.map((m) => (
              <option key={m.value} value={m.value}>
                {m.label}
              </option>
            ))}
          </select>
        </label>
        {card && (
          <label className="field">
            <span>Last four digits of the card</span>
            <input
              inputMode="numeric"
              maxLength={4}
              value={last4}
              onChange={(e) => setLast4(e.target.value.replace(/\D/g, ''))}
              aria-invalid={problem?.includes('four') || undefined}
            />
            <small>This demo never asks for a full card number.</small>
          </label>
        )}
        <label className="field">
          <span>Name on the tickets</span>
          <input value={name} onChange={(e) => setAttendee(e.target.value)} autoComplete="name" />
        </label>
        <label className="check">
          <input type="checkbox" checked={fail} onChange={(e) => setFail(e.target.checked)} />
          <span>
            Simulate a declined payment
            <small>Runs the compensation path: the payment fails and the booking is cancelled.</small>
          </span>
        </label>
        {problem && (
          <p className="form-error" role="alert">
            {problem}
          </p>
        )}
        <button className={`btn btn-block ${fail ? 'btn-danger' : 'btn-primary'}`} disabled={busy}>
          {fail ? 'Pay and decline' : `Pay ${formatMoney(b.totalAmount)}`}
        </button>
      </form>
    </>
  )
}

function Outcome({ booking: b }: { booking: Booking }) {
  const appSet = b.metadata?.appSet ?? []
  switch (b.status) {
    case 'PAID':
    case 'COMPLETED':
      return (
        <>
          <h2 className="step-title">You’re going</h2>
          <p>{b.metadata?.saleId ? 'Payment went through and your tickets are issued.' : 'This booking is complete.'}</p>
          <Link to="/tickets" className="btn btn-primary">
            View tickets
          </Link>
        </>
      )
    case 'PAYMENT_FAILED':
    case 'REFUNDED':
      return (
        <>
          <h2 className="step-title">Payment declined</h2>
          <p>
            {appSet.includes('PAYMENT_FAILED')
              ? 'The sales service recorded the declined payment, but no service cancelled the booking, so this app marked it failed.'
              : 'The saga ran its compensation: the payment failed and the booking was refunded.'}
          </p>
          <Link to="/events" className="btn btn-ghost">
            Find another event
          </Link>
        </>
      )
    default:
      return (
        <>
          <h2 className="step-title">Booking cancelled</h2>
          <p>
            {b.metadata?.saleId
              ? 'The payment was declined, so the saga cancelled the booking and voided its tickets.'
              : 'Your seats were released.'}
          </p>
          <Link to="/events" className="btn btn-ghost">
            Find another event
          </Link>
        </>
      )
  }
}

function BookingTickets({ booking, eventName }: { booking: Booking; eventName: string }) {
  const tickets = useTickets()
  const event = useEvent(booking.eventId)
  const mine = (tickets.data ?? []).filter((t) => t.bookingId === booking.id)
  if (!mine.length) return null
  return (
    <section className="booking-tickets" aria-labelledby="tickets-heading">
      <h2 id="tickets-heading" className="section-title">
        Tickets
      </h2>
      <div className="stubs">
        {mine.map((t) => (
          <TicketStub key={t.id} ticket={t} eventName={eventName} event={event.data} />
        ))}
      </div>
    </section>
  )
}
