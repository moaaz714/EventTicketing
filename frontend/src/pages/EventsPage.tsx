import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { searchEvents } from '../api/endpoints'
import { keys, useEvents, useFromPrices } from '../api/queries'
import type { EventCategory, TicketEvent } from '../api/types'
import { BookingsRail } from '../components/BookingsRail'
import { Empty, ErrorBlock, Loading, errorText } from '../components/Feedback'
import { Halftone } from '../components/Halftone'
import { CaretIcon } from '../components/Icons'
import { PixelTitle } from '../components/PixelTitle'
import { useToast } from '../components/Toasts'
import { createDemoEvents } from '../lib/demo'
import { CATEGORIES, CATEGORY_LABEL, formatDay, formatMoney, formatTime, parseDate } from '../lib/format'

type SortKey = 'date' | 'name' | 'venue' | 'category'

const SORTERS: Record<SortKey, (a: TicketEvent, b: TicketEvent) => number> = {
  date: (a, b) => (parseDate(a.eventDate)?.getTime() ?? 0) - (parseDate(b.eventDate)?.getTime() ?? 0),
  name: (a, b) => a.name.localeCompare(b.name),
  venue: (a, b) => a.venue.localeCompare(b.venue),
  category: (a, b) => a.category.localeCompare(b.category),
}

const isOpen = (e: TicketEvent) => e.status === 'UPCOMING' || e.status === 'ONGOING'

export function EventsPage() {
  const [params, setParams] = useSearchParams()
  const q = (params.get('q') ?? '').trim()
  const category = params.get('category') as EventCategory | null
  const [sort, setSort] = useState<{ key: SortKey; dir: 1 | -1 }>({ key: 'date', dir: 1 })

  const events = useEvents()

  // Full-text search goes to Elasticsearch; if that is down, filter locally.
  const search = useQuery({
    queryKey: keys.search(q),
    queryFn: () => searchEvents(q),
    enabled: q.length > 1,
    retry: false,
  })

  const visible = useMemo(() => {
    let list = events.data ?? []
    if (q) {
      if (search.data) {
        const ids = new Set(search.data.map((h) => Number(h.id)))
        list = list.filter((e) => ids.has(e.id))
      } else {
        const needle = q.toLowerCase()
        list = list.filter((e) => `${e.name} ${e.venue}`.toLowerCase().includes(needle))
      }
    }
    if (category) list = list.filter((e) => e.category === category)
    return [...list].sort((a, b) => SORTERS[sort.key](a, b) * sort.dir)
  }, [events.data, search.data, q, category, sort])

  const lead = useMemo(
    () => [...visible].filter(isOpen).sort(SORTERS.date).find((e) => (parseDate(e.eventDate)?.getTime() ?? 0) > Date.now()),
    [visible],
  )

  const prices = useFromPrices(visible.map((e) => e.id))

  const setCategory = (c: EventCategory | null) => {
    const next = new URLSearchParams(params)
    if (c) next.set('category', c)
    else next.delete('category')
    setParams(next, { replace: true })
  }

  const clearFilters = () => setParams(new URLSearchParams(), { replace: true })

  const toggleSort = (key: SortKey) =>
    setSort((s) => (s.key === key ? { key, dir: s.dir === 1 ? -1 : 1 } : { key, dir: 1 }))

  const total = events.data?.length ?? 0

  return (
    <div className="with-rail">
      <section className="page" aria-labelledby="events-heading">
        <h1 id="events-heading" className="visually-hidden">
          Events
        </h1>

        {events.isLoading && <Loading rows={6} label="Loading events" />}
        {events.isError && <ErrorBlock error={events.error} onRetry={() => events.refetch()} />}

        {events.isSuccess && total === 0 && <NoEvents />}

        {events.isSuccess && total > 0 && (
          <>
            {lead && !q && <LeadEvent event={lead} from={prices.get(lead.id)} />}

            <div className="toolbar">
              <div className="chips" role="group" aria-label="Filter by category">
                <button className="chip" aria-pressed={!category} onClick={() => setCategory(null)}>
                  All
                </button>
                {CATEGORIES.map((c) => (
                  <button key={c} className="chip" aria-pressed={category === c} onClick={() => setCategory(c)}>
                    {CATEGORY_LABEL[c]}
                  </button>
                ))}
              </div>
              <p className="muted toolbar-count">
                {q ? (
                  <>
                    {visible.length} {visible.length === 1 ? 'result' : 'results'} for “{q}”
                    {search.isError && ' (search service unavailable, matching names locally)'}
                  </>
                ) : (
                  <>
                    {visible.length} of {total} events
                  </>
                )}
              </p>
            </div>

            {visible.length === 0 ? (
              <Empty title="Nothing matches">
                <p className="muted">Try another word, or clear the filters to see every event.</p>
                <button className="btn btn-ghost" onClick={clearFilters}>
                  Clear filters
                </button>
              </Empty>
            ) : (
              <EventTable events={visible} prices={prices} sort={sort} onSort={toggleSort} />
            )}
          </>
        )}
      </section>
      <BookingsRail />
    </div>
  )
}

function LeadEvent({ event, from }: { event: TicketEvent; from?: number }) {
  return (
    <article className="lead">
      <div className="lead-text">
        <PixelTitle text={event.name} size={96} as="h2" className="lead-title" />
        <p className="lead-meta">
          <strong>
            {formatDay(event.eventDate)}, {formatTime(event.eventDate)}
          </strong>{' '}
          at {event.venue}
          {from !== undefined && <span className="lead-price">From {formatMoney(from)}</span>}
        </p>
        <Link to={`/events/${event.id}`} className="btn btn-primary">
          Book seats
        </Link>
      </div>
      <Halftone seed={event.id} cols={30} rows={22} className="lead-art" />
    </article>
  )
}

function EventTable({
  events,
  prices,
  sort,
  onSort,
}: {
  events: TicketEvent[]
  prices: Map<number, number>
  sort: { key: SortKey; dir: 1 | -1 }
  onSort: (k: SortKey) => void
}) {
  const header = (key: SortKey, label: string, className = '') => (
    <th
      className={className}
      aria-sort={sort.key === key ? (sort.dir === 1 ? 'ascending' : 'descending') : 'none'}
    >
      <button className="th-btn" onClick={() => onSort(key)}>
        {label}
        <span className="th-caret" aria-hidden="true">
          {sort.key === key && <CaretIcon down={sort.dir === -1} />}
        </span>
      </button>
    </th>
  )

  return (
    <table className="table events-table">
      <thead>
        <tr>
          {header('date', 'Date')}
          {header('name', 'Event')}
          {header('venue', 'Venue', 'hide-sm')}
          {header('category', 'Category', 'hide-sm')}
          <th className="num">From</th>
        </tr>
      </thead>
      <tbody>
        {events.map((e) => (
          <tr key={e.id} className={isOpen(e) ? '' : 'row-closed'}>
            <td className="nowrap">
              {formatDay(e.eventDate)}
              <span className="sub">{formatTime(e.eventDate)}</span>
            </td>
            <td>
              <Link to={`/events/${e.id}`} className="row-link">
                {e.name}
              </Link>
              <span className="sub show-sm">{e.venue}</span>
              {!isOpen(e) && <span className="sub">{e.status === 'CANCELLED' ? 'Cancelled' : 'Ended'}</span>}
            </td>
            <td className="hide-sm">{e.venue}</td>
            <td className="hide-sm">{CATEGORY_LABEL[e.category] ?? e.category}</td>
            <td className="num nowrap">
              {prices.has(e.id) ? formatMoney(prices.get(e.id)) : <span className="muted">—</span>}
              {!!e.totalRatings && (
                <span className="sub">
                  Rated {(e.rating ?? 0).toFixed(1)} by {e.totalRatings}
                </span>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function NoEvents() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [progress, setProgress] = useState<string | null>(null)

  const create = async () => {
    setProgress('Creating 0 of 6')
    try {
      await createDemoEvents((done, all) => setProgress(`Creating ${done} of ${all}`))
      toast('Six demo events created.')
    } catch (err) {
      toast(errorText(err), 'error')
    } finally {
      setProgress(null)
      queryClient.invalidateQueries({ queryKey: keys.events })
    }
  }

  return (
    <Empty title="No events yet">
      <p className="muted">
        The seed script adds users and bookings but no events. Create six demo events through the events
        API to try the full booking flow.
      </p>
      <button className="btn btn-primary" onClick={create} disabled={!!progress}>
        {progress ?? 'Create demo events'}
      </button>
    </Empty>
  )
}
