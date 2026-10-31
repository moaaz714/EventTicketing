import { createEvent, createSession, indexEvent } from '../api/endpoints'
import type { EventCategory } from '../api/types'

/*
 * The seed script creates users and bookings but no events. These demo
 * events are created through the normal events API and flagged demo: true.
 * Session capacities are chosen so the backend's estimate (average session
 * capacity / 10 per ticket) lands on plausible prices.
 */

interface DemoSession {
  title: string
  speaker?: string
  offsetHours: number
  hours: number
  capacity: number
}

interface DemoEvent {
  name: string
  venue: string
  category: EventCategory
  inDays: number
  hour: number
  description: string
  sessions: DemoSession[]
}

const DEMO: DemoEvent[] = [
  {
    name: 'Cairo Jazz Night',
    venue: 'El Sawy Culturewheel, Zamalek',
    category: 'CONCERT',
    inDays: 14,
    hour: 20,
    description: 'Two stages of Egyptian and visiting jazz trios, closing with a late jam.',
    sessions: [
      { title: 'Opening set', speaker: 'Nour Trio', offsetHours: 0, hours: 1, capacity: 3200 },
      { title: 'Big band hour', speaker: 'Cairo Big Band', offsetHours: 1, hours: 1, capacity: 3800 },
    ],
  },
  {
    name: 'Pyramids Half Marathon',
    venue: 'Giza Plateau',
    category: 'SPORTS',
    inDays: 23,
    hour: 6,
    description: '21.1 km at sunrise, starting and finishing beside the plateau.',
    sessions: [
      { title: 'Half marathon', offsetHours: 0, hours: 4, capacity: 6000 },
      { title: '5 km fun run', offsetHours: 1, hours: 2, capacity: 6000 },
    ],
  },
  {
    name: 'Hamlet, in Arabic',
    venue: 'Cairo Opera House, Small Hall',
    category: 'THEATER',
    inDays: 27,
    hour: 19,
    description: 'A new Arabic translation of Hamlet in three acts.',
    sessions: [{ title: 'Evening performance', offsetHours: 0, hours: 3, capacity: 2500 }],
  },
  {
    name: 'DevFest Cairo 2026',
    venue: 'GUC, New Cairo',
    category: 'CONFERENCE',
    inDays: 29,
    hour: 9,
    description: 'A day of talks on web, cloud and AI, run by the local developer community.',
    sessions: [
      { title: 'Keynote', speaker: 'Community organisers', offsetHours: 0, hours: 1, capacity: 1500 },
      { title: 'Event-driven systems with RabbitMQ', speaker: 'Guest speaker', offsetHours: 2, hours: 1, capacity: 1500 },
      { title: 'Shipping Spring Boot to Kubernetes', speaker: 'Guest speaker', offsetHours: 4, hours: 1, capacity: 1500 },
    ],
  },
  {
    name: 'Sandbox Sound Festival',
    venue: 'El Gouna',
    category: 'FESTIVAL',
    inDays: 34,
    hour: 16,
    description: 'Two days of electronic and alternative acts by the Red Sea.',
    sessions: [
      { title: 'Day one', offsetHours: 0, hours: 10, capacity: 24000 },
      { title: 'Day two', offsetHours: 24, hours: 10, capacity: 24000 },
    ],
  },
  {
    name: 'Wust El Balad live',
    venue: 'Cairo Jazz Club, Agouza',
    category: 'CONCERT',
    inDays: 42,
    hour: 21,
    description: 'An evening set of new songs and old favourites.',
    sessions: [{ title: 'Main set', offsetHours: 0, hours: 2, capacity: 4000 }],
  },
]

/** LocalDateTime without a zone, as the backend expects. */
function localDateTime(d: Date) {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:00`
}

export async function createDemoEvents(onProgress?: (done: number, total: number) => void) {
  let done = 0
  for (const e of DEMO) {
    const start = new Date()
    start.setDate(start.getDate() + e.inDays)
    start.setHours(e.hour, 0, 0, 0)

    const created = await createEvent({
      name: e.name,
      venue: e.venue,
      eventDate: localDateTime(start),
      category: e.category,
      status: 'UPCOMING',
      details: { description: e.description, demo: true },
    })

    for (const s of e.sessions) {
      const from = new Date(start.getTime() + s.offsetHours * 3_600_000)
      const to = new Date(from.getTime() + s.hours * 3_600_000)
      await createSession(created.id, {
        title: s.title,
        speaker: s.speaker,
        startTime: localDateTime(from),
        endTime: localDateTime(to),
        capacity: s.capacity,
      })
    }

    // Full-text search reads from Elasticsearch; indexing is best effort.
    await indexEvent(created.id).catch(() => undefined)
    onProgress?.(++done, DEMO.length)
  }
}
