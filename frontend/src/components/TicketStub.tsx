import type { Ticket, TicketEvent } from '../api/types'
import { formatDay, formatTime } from '../lib/format'
import { Halftone } from './Halftone'
import { StatusWord } from './StatusWord'

const LABEL: Record<Ticket['status'], string> = {
  VALID: 'Valid',
  USED: 'Used',
  EXPIRED: 'Expired',
  CANCELLED: 'Cancelled',
}

/** A ticket printed the way box offices did: code in dot-matrix, the rest in type. */
export function TicketStub({ ticket, eventName, event }: { ticket: Ticket; eventName: string; event?: TicketEvent }) {
  const void_ = ticket.status === 'CANCELLED' || ticket.status === 'EXPIRED'
  return (
    <article className={`stub ${void_ ? 'stub-void' : ''}`}>
      <div className="stub-main">
        <p className="stub-event">{eventName}</p>
        {event && (
          <p className="muted">
            {formatDay(event.eventDate)}, {formatTime(event.eventDate)}
            <br />
            {event.venue}
          </p>
        )}
        <p className="stub-code pixel" aria-label={`Ticket code ${ticket.ticketCode}`}>
          {ticket.ticketCode}
        </p>
        <p className="stub-holder">
          {ticket.attendeeName}
          <span className="muted"> booking #{String(ticket.bookingId).padStart(4, '0')}</span>
        </p>
      </div>
      <div className="stub-side">
        <StatusWord label={LABEL[ticket.status]} resolved failed={void_} size="sm" />
        <Halftone seed={ticket.id * 31 + ticket.bookingId} cols={8} rows={10} className="stub-art" />
      </div>
    </article>
  )
}
