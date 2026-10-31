import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { addBookingItems, createBooking, estimateCost } from '../api/endpoints'
import { keys, useEvent, useSessions } from '../api/queries'
import type { EventSession, TicketEvent } from '../api/types'
import { useSession } from '../auth/AuthContext'
import { ErrorBlock, Loading, errorText } from '../components/Feedback'
import { Halftone } from '../components/Halftone'
import { BackIcon } from '../components/Icons'
import { PixelTitle } from '../components/PixelTitle'
import { useToast } from '../components/Toasts'
import { CATEGORY_LABEL, formatLongDate, formatMoney, formatTime } from '../lib/format'

export function EventPage() {
  const id = Number(useParams().id)
  const event = useEvent(id)
  const sessions = useSessions(id)

  if (event.isLoading) return <div className="page"><Loading rows={5} label="Loading event" /></div>
  if (event.isError || !event.data)
    return (
      <div className="page">
        <BackLink />
        <ErrorBlock error={event.error ?? new Error('Event not found.')} onRetry={() => event.refetch()} />
      </div>
    )

  const e = event.data
  const description = typeof e.details?.description === 'string' ? e.details.description : null
  const list = sessions.data ?? e.eventSessions ?? []

  return (
    <div className="page event-page">
      <BackLink />
      <header className="event-head">
        <div>
          <PixelTitle text={e.name} size={64} />
          <dl className="facts">
            <div>
              <dt>Category</dt>
              <dd>{CATEGORY_LABEL[e.category] ?? e.category}</dd>
            </div>
            <div>
              <dt>When</dt>
              <dd>
                {formatLongDate(e.eventDate)}, {formatTime(e.eventDate)}
              </dd>
            </div>
            <div>
              <dt>Where</dt>
              <dd>{e.venue}</dd>
            </div>
            <div>
              <dt>Rating</dt>
              <dd>{e.totalRatings ? `${(e.rating ?? 0).toFixed(1)} from ${e.totalRatings}` : 'Not rated yet'}</dd>
            </div>
          </dl>
          {description && <p className="event-desc">{description}</p>}
          {e.details?.demo === true && <p className="muted small">Demo event created by this app.</p>}
        </div>
        <Halftone seed={e.id} cols={26} rows={20} className="event-art" />
      </header>

      <div className="event-body">
        <section aria-labelledby="sessions-heading">
          <h2 id="sessions-heading" className="section-title">
            Sessions
          </h2>
          {sessions.isLoading && <Loading rows={3} />}
          {sessions.isSuccess && list.length === 0 && (
            <p className="muted">No sessions are scheduled for this event yet.</p>
          )}
          {list.length > 0 && <SessionTable sessions={list} />}
        </section>
        <BookingPanel event={e} sessions={list} />
      </div>
    </div>
  )
}

function BackLink() {
  return (
    <Link to="/events" className="back">
      <BackIcon /> All events
    </Link>
  )
}

function SessionTable({ sessions }: { sessions: EventSession[] }) {
  return (
    <table className="table">
      <thead>
        <tr>
          <th>Time</th>
          <th>Session</th>
          <th className="num">Capacity</th>
        </tr>
      </thead>
      <tbody>
        {sessions.map((s) => (
          <tr key={s.id}>
            <td className="nowrap">
              {formatTime(s.startTime)} to {formatTime(s.endTime)}
            </td>
            <td>
              {s.title}
              {s.speaker && <span className="sub">{s.speaker}</span>}
            </td>
            <td className="num">{s.capacity.toLocaleString('en')}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

const TIERS = [
  { value: 'STANDARD', label: 'Standard', note: 'General admission' },
  { value: 'VIP', label: 'VIP', note: '2.5 × the standard price' },
]

function BookingPanel({ event, sessions }: { event: TicketEvent; sessions: EventSession[] }) {
  const { userId, email } = useSession()
  const navigate = useNavigate()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [tier, setTier] = useState('STANDARD')
  const [qty, setQty] = useState(2)
  const [sessionId, setSessionId] = useState<number | null>(sessions[0]?.id ?? null)
  const session = sessions.find((s) => s.id === sessionId) ?? sessions[0]

  const estimate = useQuery({
    queryKey: keys.estimate(event.id, qty, tier),
    queryFn: () => estimateCost(event.id, qty, tier),
    placeholderData: keepPreviousData,
  })

  const reserve = useMutation({
    mutationFn: async () => {
      const total = estimate.data!.estimatedTotal
      const booking = await createBooking({
        userId,
        eventId: event.id,
        contactEmail: email,
        totalAmount: Math.round(total * 100) / 100,
        metadata: { app: true, tier, quantity: qty, sessionTitle: session?.title },
      })
      if (session) {
        await addBookingItems(booking.id, [
          {
            sessionId: session.id,
            sessionTitle: session.title,
            quantity: qty,
            unitPrice: Math.round((estimate.data!.ticketCost / qty) * 100) / 100,
          },
        ])
      }
      return booking
    },
    onSuccess: (booking) => {
      queryClient.invalidateQueries({ queryKey: keys.bookings(userId) })
      toast(`Seats reserved. Booking #${String(booking.id).padStart(4, '0')} is waiting for confirmation.`)
      navigate(`/bookings/${booking.id}`)
    },
    onError: (err) => toast(errorText(err), 'error'),
  })

  const closed = event.status !== 'UPCOMING'
  const priced = (estimate.data?.estimatedTotal ?? 0) > 0
  const submit = (e: FormEvent) => {
    e.preventDefault()
    reserve.mutate()
  }

  return (
    <aside className="panel book-panel" aria-labelledby="book-heading">
      <h2 id="book-heading" className="section-title">
        Book seats
      </h2>
      <form onSubmit={submit} className="form">
        <fieldset className="tiers">
          <legend>Ticket type</legend>
          {TIERS.map((t) => (
            <label key={t.value} className="tier">
              <input
                type="radio"
                name="tier"
                value={t.value}
                checked={tier === t.value}
                onChange={() => setTier(t.value)}
              />
              <span className="tier-body">
                <strong>{t.label}</strong>
                <span className="muted">{t.note}</span>
              </span>
            </label>
          ))}
        </fieldset>

        <div className="field">
          <span id="qty-label">Tickets</span>
          <div className="stepper" role="group" aria-labelledby="qty-label">
            <button type="button" onClick={() => setQty((q) => Math.max(1, q - 1))} disabled={qty <= 1} aria-label="One fewer ticket">
              −
            </button>
            <output aria-live="polite">{qty}</output>
            <button type="button" onClick={() => setQty((q) => Math.min(10, q + 1))} disabled={qty >= 10} aria-label="One more ticket">
              +
            </button>
          </div>
        </div>

        {sessions.length > 1 && (
          <label className="field">
            <span>Session</span>
            <select value={session?.id} onChange={(e) => setSessionId(Number(e.target.value))}>
              {sessions.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.title}, {formatTime(s.startTime)}
                </option>
              ))}
            </select>
          </label>
        )}

        <div className="estimate" aria-live="polite">
          {estimate.isLoading && <Loading rows={2} label="Estimating price" />}
          {estimate.isError && <p className="form-error">{errorText(estimate.error)}</p>}
          {estimate.data && (
            <dl className={estimate.isFetching ? 'is-stale' : ''}>
              <div>
                <dt>Tickets</dt>
                <dd>{formatMoney(estimate.data.ticketCost)}</dd>
              </div>
              <div>
                <dt>Service fee</dt>
                <dd>{formatMoney(estimate.data.serviceFee)}</dd>
              </div>
              {estimate.data.demandMultiplier > 1 && (
                <div>
                  <dt>High demand</dt>
                  <dd>× {estimate.data.demandMultiplier}</dd>
                </div>
              )}
              <div className="estimate-total">
                <dt>Total</dt>
                <dd>{formatMoney(estimate.data.estimatedTotal)}</dd>
              </div>
            </dl>
          )}
          {estimate.data && !priced && (
            <p className="muted small">This event has no priced sessions yet, so it can’t be booked.</p>
          )}
        </div>

        <button
          className="btn btn-primary btn-block"
          disabled={closed || !priced || reserve.isPending || estimate.isFetching}
        >
          {reserve.isPending ? 'Reserving…' : closed ? 'Booking closed' : `Reserve ${qty} ${qty === 1 ? 'seat' : 'seats'}`}
        </button>
        <p className="muted small">Nothing is charged yet. You’ll confirm and pay on the next screen.</p>
      </form>
    </aside>
  )
}
