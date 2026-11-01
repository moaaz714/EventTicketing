import { ApiError, api } from './client'
import type {
  AuthResponse,
  Booking,
  BookingMetadata,
  BookingStatus,
  CostEstimate,
  EventCategory,
  EventSession,
  PaymentMethod,
  SearchHit,
  Ticket,
  TicketEvent,
  TicketSale,
  User,
} from './types'

/**
 * user-service sends Spring's default error body without a message, so the
 * client only sees the status text ("Unauthorized", "Conflict"). Name the
 * failure instead; a real message from the backend still wins.
 */
const explain = (status: number, statusText: string, message: string) => (err: unknown): never => {
  if (err instanceof ApiError && err.status === status && err.message === statusText)
    throw new ApiError(status, message)
  throw err
}

// Auth (user-service, public through the gateway)
export const login = (email: string, password: string) =>
  api<AuthResponse>('/api/auth/login', { method: 'POST', body: { email, password } }).catch(
    explain(401, 'Unauthorized', 'Wrong email or password.'),
  )

export const register = (body: { name: string; email: string; password: string; phone: string }) =>
  api<AuthResponse>('/api/auth/register', { method: 'POST', body }).catch(
    explain(409, 'Conflict', 'That email or phone number is already registered.'),
  )

export const getUser = (id: number) => api<User>(`/api/users/${id}`)

// Events (event-service)
export const listEvents = () => api<TicketEvent[]>('/api/events')
export const getEvent = (id: number) => api<TicketEvent>(`/api/events/${id}`)
export const listSessions = (eventId: number) =>
  api<EventSession[]>(`/api/events/${eventId}/sessions`)

export const searchEvents = (query: string, category?: EventCategory) =>
  api<SearchHit[]>('/api/events/search/full-text', { query: { query, category } })

export const createEvent = (body: {
  name: string
  venue: string
  eventDate: string
  category: EventCategory
  status: 'UPCOMING'
  details?: Record<string, unknown>
}) => api<TicketEvent>('/api/events', { method: 'POST', body })

export const createSession = (
  eventId: number,
  body: { title: string; speaker?: string; startTime: string; endTime: string; capacity: number },
) => api<EventSession>(`/api/events/${eventId}/sessions`, { method: 'POST', body })

export const indexEvent = (eventId: number) =>
  api<void>(`/api/events/${eventId}/index`, { method: 'POST' })

// Bookings (booking-service)
export const listBookings = (userId: number) =>
  api<Booking[]>('/api/bookings', { query: { userId } })

export const getBooking = (id: number) => api<Booking>(`/api/bookings/${id}`)

export const estimateCost = (eventId: number, ticketCount: number, ticketTier: string) =>
  api<CostEstimate>('/api/bookings/estimate', {
    method: 'POST',
    body: { eventId, ticketCount, ticketTier },
  })

export const createBooking = (body: {
  userId: number
  eventId: number
  contactEmail: string
  totalAmount: number
  metadata: BookingMetadata
}) => api<Booking>('/api/bookings', { method: 'POST', body: { ...body, status: 'PENDING' } })

export const addBookingItems = (
  bookingId: number,
  items: { sessionId: number; sessionTitle: string; quantity: number; unitPrice: number }[],
) => api<Booking>(`/api/bookings/${bookingId}/items`, { method: 'POST', body: items })

export const confirmBooking = (bookingId: number, eventId: number) =>
  api<Booking>(`/api/bookings/${bookingId}/confirm`, { method: 'PUT', query: { eventId } })

export const cancelBooking = (bookingId: number) =>
  api<void>(`/api/bookings/${bookingId}/cancel`, { method: 'PUT' })

export const completeBooking = (bookingId: number) =>
  api<Booking>(`/api/bookings/${bookingId}/complete`, { method: 'PUT' })

/** The generic update; the only way the backend exposes some transitions. */
export const patchBooking = (
  bookingId: number,
  patch: { status?: BookingStatus; metadata?: BookingMetadata },
) => api<Booking>(`/api/bookings/${bookingId}`, { method: 'PUT', body: patch })

// Tickets (ticket-service)
export const listTickets = () => api<Ticket[]>('/api/tickets')

export const issueTickets = (
  bookingId: number,
  tickets: { attendeeName: string; ticketCode: string }[],
) => api<{ count: number }>('/api/tickets/batch', { method: 'POST', body: { bookingId, tickets } })

// Payment (sales-service)
export const payForBooking = (
  bookingId: number,
  body: { method: PaymentMethod; cardLastFour?: string },
  simulateFailure: boolean,
) =>
  api<TicketSale>(`/api/sales/booking/${bookingId}`, {
    method: 'POST',
    body,
    query: { simulateFailure: simulateFailure || undefined },
  })
