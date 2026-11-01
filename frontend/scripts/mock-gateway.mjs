// A stand-in for the API gateway, for working on the UI without the full
// Docker stack. It mirrors the backend's real behaviour, gaps included: no
// events are seeded, completing a booking leaves it COMPLETED (nothing moves
// it to PAYMENT_PENDING or PAID), and a declined payment cancels the booking.
//
//   npm run mock          # serves on :8090
//   npm run dev:mock      # Vite proxied to the mock

import http from 'node:http'

const PORT = Number(process.env.MOCK_PORT ?? 8090)
const now = () => new Date().toISOString().slice(0, 19)

const db = {
  users: [
    { id: 1, name: 'Ahmed Hassan', email: 'ahmed@example.com', phone: '0100000001', role: 'ATTENDEE' },
    { id: 2, name: 'Bob', email: 'bob@example.com', phone: '0100000002', role: 'ATTENDEE' },
    { id: 5, name: 'Eve', email: 'eve@example.com', phone: '0100000005', role: 'ADMIN' },
  ],
  events: [],
  sessions: [],
  bookings: [
    { id: 1, userId: 1, eventId: 101, contactEmail: 'ahmed@example.com', status: 'COMPLETED', totalAmount: 300, metadata: {}, bookingDate: now() },
    { id: 2, userId: 1, eventId: 102, contactEmail: 'ahmed@example.com', status: 'COMPLETED', totalAmount: 500, metadata: {}, bookingDate: now() },
    { id: 3, userId: 1, eventId: 103, contactEmail: 'ahmed@example.com', status: 'COMPLETED', totalAmount: 700, metadata: {}, bookingDate: now() },
    { id: 4, userId: 1, eventId: 104, contactEmail: 'ahmed@example.com', status: 'CANCELLED', totalAmount: 50, metadata: {}, bookingDate: now() },
    { id: 5, userId: 1, eventId: 105, contactEmail: 'ahmed@example.com', status: 'PENDING', totalAmount: 99, metadata: {}, bookingDate: now() },
  ],
  items: [],
  tickets: [],
  sales: [],
}
const seq = { users: 6, events: 1, sessions: 1, bookings: 6, items: 1, tickets: 1, sales: 1 }

const b64 = (o) => Buffer.from(JSON.stringify(o)).toString('base64url')
const tokenFor = (u) =>
  `${b64({ alg: 'none' })}.${b64({ sub: u.email, uid: u.id, role: u.role, iat: Date.now() / 1000 | 0, exp: (Date.now() / 1000 | 0) + 86400 })}.mock`

const uidFrom = (req) => {
  const m = /^Bearer (.+)$/.exec(req.headers.authorization ?? '')
  if (!m) return null
  try {
    return JSON.parse(Buffer.from(m[1].split('.')[1], 'base64url').toString()).uid
  } catch {
    return null
  }
}

class HttpError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}
const fail = (status, message) => {
  throw new HttpError(status, message)
}
const find = (list, id, what) => list.find((x) => x.id === Number(id)) ?? fail(404, `${what} not found with id: ${id}`)
const later = (ms, fn) => setTimeout(fn, ms)

const withSessions = (e) => ({ ...e, eventSessions: db.sessions.filter((s) => s.eventId === e.id) })

const routes = [
  ['POST', /^\/api\/auth\/login$/, (_, body) => {
    const u = db.users.find((x) => x.email === body.email)
    if (!u || body.password !== 'password123') fail(401, 'Invalid credentials')
    return { token: tokenFor(u), expiresIn: 86400000 }
  }],
  ['POST', /^\/api\/auth\/register$/, (_, body) => {
    if (!body.name || !body.email || !body.password || !body.phone) fail(400, 'All fields are required')
    if (db.users.some((u) => u.email === body.email)) fail(409, 'Email already registered')
    if (db.users.some((u) => u.phone === body.phone)) fail(409, 'Phone already registered')
    const u = { id: seq.users++, name: body.name, email: body.email, phone: body.phone, role: 'ATTENDEE' }
    db.users.push(u)
    return [201, { token: tokenFor(u), expiresIn: 86400000 }]
  }],
  ['GET', /^\/api\/users\/(\d+)$/, ([id]) => ({ ...find(db.users, id, 'User'), status: 'ACTIVE' })],

  ['GET', /^\/api\/events$/, () => db.events.map(withSessions)],
  ['POST', /^\/api\/events$/, (_, body) => {
    const e = { id: seq.events++, rating: 0, totalRatings: 0, createdAt: now(), ...body }
    db.events.push(e)
    return [201, e]
  }],
  ['GET', /^\/api\/events\/search\/full-text$/, (_, __, q) => {
    const needle = (q.get('query') ?? '').toLowerCase()
    return db.events
      .filter((e) => `${e.name} ${e.venue} ${e.details?.description ?? ''}`.toLowerCase().includes(needle))
      .map((e) => ({ id: String(e.id), name: e.name, category: e.category, venue: e.venue, eventDate: e.eventDate, status: e.status }))
  }],
  ['GET', /^\/api\/events\/(\d+)$/, ([id]) => withSessions(find(db.events, id, 'Event'))],
  ['POST', /^\/api\/events\/(\d+)\/index$/, () => undefined],
  ['GET', /^\/api\/events\/(\d+)\/sessions$/, ([id]) => db.sessions.filter((s) => s.eventId === Number(id))],
  ['POST', /^\/api\/events\/(\d+)\/sessions$/, ([id], body) => {
    const s = { id: seq.sessions++, eventId: Number(id), verified: false, ...body }
    db.sessions.push(s)
    return [201, s]
  }],

  ['POST', /^\/api\/bookings\/estimate$/, (_, body) => {
    const caps = db.sessions.filter((s) => s.eventId === body.eventId).map((s) => s.capacity)
    const avg = caps.length ? caps.reduce((a, b) => a + b, 0) / caps.length : 0
    const ticketCost = (avg / 10) * (String(body.ticketTier).toUpperCase() === 'VIP' ? 2.5 : 1) * body.ticketCount
    const serviceFee = ticketCost * 0.15
    return { ticketCost, serviceFee, estimatedTotal: ticketCost + serviceFee, demandMultiplier: 1 }
  }],
  ['GET', /^\/api\/bookings$/, (_, __, q) =>
    q.get('userId') ? db.bookings.filter((b) => b.userId === Number(q.get('userId'))) : db.bookings],
  ['POST', /^\/api\/bookings$/, (_, body) => {
    const b = { id: seq.bookings++, bookingDate: now(), confirmedAt: null, metadata: {}, ...body }
    db.bookings.push(b)
    return [201, b]
  }],
  ['GET', /^\/api\/bookings\/(\d+)$/, ([id]) => find(db.bookings, id, 'Booking')],
  ['PUT', /^\/api\/bookings\/(\d+)$/, ([id], body) => {
    const b = find(db.bookings, id, 'Booking')
    for (const k of ['contactEmail', 'status', 'totalAmount', 'metadata', 'confirmedAt']) if (body[k] != null) b[k] = body[k]
    return b
  }],
  ['POST', /^\/api\/bookings\/(\d+)\/items$/, ([id], body) => {
    const b = find(db.bookings, id, 'Booking')
    for (const it of body) db.items.push({ id: seq.items++, bookingId: b.id, status: 'RESERVED', ...it })
    return b
  }],
  ['PUT', /^\/api\/bookings\/(\d+)\/confirm$/, ([id], _, q) => {
    const b = find(db.bookings, id, 'Booking')
    if (b.status !== 'PENDING') fail(400, 'Only PENDING bookings can be confirmed')
    const e = find(db.events, q.get('eventId'), 'Event')
    if (e.status !== 'UPCOMING') fail(400, 'Event is not upcoming')
    Object.assign(b, { status: 'CONFIRMED', confirmedAt: now() })
    return b
  }],
  ['PUT', /^\/api\/bookings\/(\d+)\/cancel$/, ([id]) => {
    const b = find(db.bookings, id, 'Booking')
    if (!['PENDING', 'CONFIRMED'].includes(b.status)) fail(400, 'Only PENDING or CONFIRMED bookings can be cancelled')
    b.status = 'CANCELLED'
  }],
  ['PUT', /^\/api\/bookings\/(\d+)\/complete$/, ([id]) => {
    const b = find(db.bookings, id, 'Booking')
    if (b.status !== 'CHECKED_IN') fail(400, 'Only CHECKED_IN bookings can be completed')
    b.status = 'COMPLETED'
    // booking.completed → sales-service opens a pending sale. Nothing moves the booking on.
    later(300, () => db.sales.push({ id: seq.sales++, bookingId: b.id, userId: b.userId, amount: b.totalAmount, method: null, status: 'PENDING', createdAt: now() }))
    return b
  }],

  ['GET', /^\/api\/tickets$/, () => db.tickets],
  ['POST', /^\/api\/tickets\/batch$/, (_, body) => {
    const b = find(db.bookings, body.bookingId, 'Booking')
    for (const t of body.tickets) {
      if (db.tickets.some((x) => x.ticketCode === t.ticketCode)) fail(400, `Ticket code already exists: ${t.ticketCode}`)
      db.tickets.push({ id: seq.tickets++, bookingId: b.id, eventId: b.eventId, status: 'VALID', issuedAt: now(), metadata: {}, ...t })
    }
    return [201, { count: body.tickets.length }]
  }],

  ['POST', /^\/api\/sales\/booking\/(\d+)$/, ([id], body, q) => {
    const b = find(db.bookings, id, 'Booking')
    if (b.status !== 'PAYMENT_PENDING') fail(400, `Booking is not awaiting payment. Status: ${b.status}`)
    const sale = db.sales.find((s) => s.bookingId === b.id && s.status === 'PENDING') ?? fail(404, `Pending ticket sale not found for booking: ${id}`)
    const method = String(body.method).toUpperCase()
    if (q.get('simulateFailure') === 'true') {
      Object.assign(sale, { status: 'FAILED', method, transactionDetails: { gatewayResponse: 'declined' } })
      // payment.failed → booking-service cancels; ticket-service voids tickets.
      later(600, () => {
        b.status = 'CANCELLED'
        for (const t of db.tickets) if (t.bookingId === b.id && t.status === 'VALID') t.status = 'CANCELLED'
      })
      return [201, sale]
    }
    Object.assign(sale, { status: 'COMPLETED', method, transactionDetails: { gatewayResponse: 'approved', cardLastFour: body.cardLastFour } })
    return [201, sale]
  }],
]

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://x')
  const send = (status, body) => {
    res.writeHead(status, { 'Content-Type': 'application/json' })
    res.end(body === undefined ? '' : JSON.stringify(body))
  }
  try {
    if (!url.pathname.startsWith('/api/auth/') && !uidFrom(req)) return send(401)
    let raw = ''
    for await (const chunk of req) raw += chunk
    const body = raw ? JSON.parse(raw) : undefined
    for (const [method, re, handler] of routes) {
      const m = re.exec(url.pathname)
      if (m && method === req.method) {
        await new Promise((r) => setTimeout(r, 120)) // feel like a network
        const out = handler(m.slice(1), body, url.searchParams)
        return Array.isArray(out) && typeof out[0] === 'number' ? send(out[0], out[1]) : send(out === undefined ? 200 : 200, out)
      }
    }
    send(404, { status: 404, error: 'Not Found', path: url.pathname })
  } catch (err) {
    const status = err instanceof HttpError ? err.status : 500
    // Only booking-service sets server.error.include-message; the other
    // services send Spring's default body with no message.
    const message = url.pathname.startsWith('/api/bookings') ? err.message : undefined
    send(status, { status, error: http.STATUS_CODES[status], message, path: url.pathname })
  }
})

server.listen(PORT, () => console.log(`Mock gateway on http://localhost:${PORT} (sign in as ahmed@example.com / password123)`))
