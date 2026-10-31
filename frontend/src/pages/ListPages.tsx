import { Link } from 'react-router-dom'
import { view } from '../api/journey'
import { useBookings, useEventMap, useTickets } from '../api/queries'
import type { Ticket } from '../api/types'
import { useSession } from '../auth/AuthContext'
import { Empty, ErrorBlock, Loading } from '../components/Feedback'
import { JourneyStrip } from '../components/JourneyStrip'
import { StatusWord } from '../components/StatusWord'
import { TicketStub } from '../components/TicketStub'
import { formatDay, formatMoney, parseDate } from '../lib/format'

export function BookingsPage() {
  const { userId } = useSession()
  const bookings = useBookings(userId)
  const { map } = useEventMap()
  const list = [...(bookings.data ?? [])].sort((a, b) => b.id - a.id)

  return (
    <section className="page" aria-labelledby="bookings-heading">
      <h1 id="bookings-heading" className="page-title">
        My bookings
      </h1>
      {bookings.isLoading && <Loading rows={5} label="Loading bookings" />}
      {bookings.isError && <ErrorBlock error={bookings.error} onRetry={() => bookings.refetch()} />}
      {bookings.isSuccess && list.length === 0 && (
        <Empty title="No bookings yet">
          <p className="muted">Reserve seats for an event and the booking shows up here.</p>
          <Link to="/events" className="btn btn-primary">
            Browse events
          </Link>
        </Empty>
      )}
      {list.length > 0 && (
        <table className="table bookings-table">
          <thead>
            <tr>
              <th>Booking</th>
              <th>Event</th>
              <th className="hide-sm">Booked</th>
              <th className="num">Total</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {list.map((b) => {
              const v = view(b)
              const event = map.get(b.eventId)
              return (
                <tr key={b.id}>
                  <td>
                    <Link to={`/bookings/${b.id}`} className="row-link">
                      #{String(b.id).padStart(4, '0')}
                    </Link>
                  </td>
                  <td>
                    {event?.name ?? <span className="muted">Event {b.eventId} (not in event service)</span>}
                    {event && <span className="sub">{formatDay(event.eventDate)}</span>}
                  </td>
                  <td className="hide-sm nowrap">{parseDate(b.bookingDate)?.toLocaleDateString('en-GB') ?? '—'}</td>
                  <td className="num">{formatMoney(b.totalAmount)}</td>
                  <td className="status-cell">
                    <StatusWord label={v.label} resolved={v.phase === 'settled'} failed={v.failed} size="sm" />
                    {v.phase !== 'settled' && <JourneyStrip view={v} />}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )}
    </section>
  )
}

export function TicketsPage() {
  const { userId } = useSession()
  const bookings = useBookings(userId)
  const tickets = useTickets()
  const { map } = useEventMap()

  // The ticket service has no per-user endpoint; match tickets to your bookings.
  const bookingIds = new Set((bookings.data ?? []).map((b) => b.id))
  const eventOf = new Map((bookings.data ?? []).map((b) => [b.id, b.eventId]))
  const mine = (tickets.data ?? []).filter((t) => bookingIds.has(t.bookingId))
  const order = (t: Ticket) => (t.status === 'VALID' ? 0 : 1)
  const sorted = [...mine].sort((a, b) => order(a) - order(b) || b.id - a.id)

  const loading = bookings.isLoading || tickets.isLoading
  const error = bookings.error ?? tickets.error

  return (
    <section className="page" aria-labelledby="tickets-page-heading">
      <h1 id="tickets-page-heading" className="page-title">
        Tickets
      </h1>
      {loading && <Loading rows={4} label="Loading tickets" />}
      {error && <ErrorBlock error={error} onRetry={() => { bookings.refetch(); tickets.refetch() }} />}
      {!loading && !error && sorted.length === 0 && (
        <Empty title="No tickets yet">
          <p className="muted">Tickets are issued when a booking is paid.</p>
          <Link to="/bookings" className="btn btn-primary">
            Go to my bookings
          </Link>
        </Empty>
      )}
      <div className="stubs">
        {sorted.map((t) => {
          const event = map.get(t.eventId ?? eventOf.get(t.bookingId) ?? -1)
          return <TicketStub key={t.id} ticket={t} event={event} eventName={event?.name ?? 'Event'} />
        })}
      </div>
    </section>
  )
}
