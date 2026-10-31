import { Link } from 'react-router-dom'
import { view } from '../api/journey'
import { useBookings, useEventMap } from '../api/queries'
import type { Booking } from '../api/types'
import { useSession } from '../auth/AuthContext'
import { formatMoney } from '../lib/format'
import { ErrorBlock, Loading } from './Feedback'
import { JourneyStrip } from './JourneyStrip'
import { StatusWord } from './StatusWord'

const order = (a: Booking, b: Booking) => {
  const live = Number(view(b).phase !== 'settled') - Number(view(a).phase !== 'settled')
  return live || b.id - a.id
}

export function BookingsRail() {
  const { userId } = useSession()
  const bookings = useBookings(userId)
  const { map } = useEventMap()
  const list = [...(bookings.data ?? [])].sort(order).slice(0, 5)

  return (
    <aside className="rail" aria-labelledby="rail-heading">
      <div className="rail-head">
        <h2 id="rail-heading">Your bookings</h2>
        {(bookings.data?.length ?? 0) > 5 && <Link to="/bookings">See all</Link>}
      </div>
      {bookings.isLoading && <Loading rows={3} label="Loading bookings" />}
      {bookings.isError && <ErrorBlock error={bookings.error} onRetry={() => bookings.refetch()} />}
      {bookings.isSuccess && list.length === 0 && (
        <p className="muted rail-empty">No bookings yet. Pick an event to reserve seats.</p>
      )}
      <ul className="rail-list">
        {list.map((b) => {
          const v = view(b)
          const event = map.get(b.eventId)
          return (
            <li key={b.id} className={`rail-item ${v.phase !== 'settled' ? 'rail-item-live' : ''}`}>
              <Link to={`/bookings/${b.id}`} className="rail-link">
                <span className="rail-meta">
                  {event ? (
                    <span className="rail-event">{event.name}</span>
                  ) : (
                    <span className="rail-event rail-event-missing">Event no longer listed</span>
                  )}
                  <span className="muted">#{String(b.id).padStart(4, '0')}</span>
                </span>
                <StatusWord label={v.label} resolved={v.phase === 'settled'} failed={v.failed} size="sm" />
                {v.phase !== 'settled' && <JourneyStrip view={v} />}
                <span className="rail-foot muted">
                  {formatMoney(b.totalAmount)}
                  {v.phase === 'you' && <span className="rail-cta">Continue</span>}
                </span>
              </Link>
            </li>
          )
        })}
      </ul>
    </aside>
  )
}
