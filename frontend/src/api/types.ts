export type Role = 'ATTENDEE' | 'ADMIN'

export type EventCategory = 'CONCERT' | 'SPORTS' | 'THEATER' | 'CONFERENCE' | 'FESTIVAL'
export type EventStatus = 'UPCOMING' | 'ONGOING' | 'COMPLETED' | 'CANCELLED'

export type BookingStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'CHECKED_IN'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'COMPLETING'
  | 'PAYMENT_PENDING'
  | 'PAID'
  | 'PAYMENT_FAILED'
  | 'REFUNDED'

export type TicketStatus = 'VALID' | 'USED' | 'EXPIRED' | 'CANCELLED'
export type PaymentMethod = 'CREDIT_CARD' | 'DEBIT_CARD' | 'PAYPAL' | 'WALLET'

export interface AuthResponse {
  token: string
  expiresIn: number
}

export interface JwtClaims {
  sub: string
  uid: number
  role: Role
  exp: number
  iat: number
}

export interface User {
  id: number
  name: string
  email: string
  phone: string
  role: Role
  status: 'ACTIVE' | 'DEACTIVATED'
}

export interface EventSession {
  id: number
  title: string
  speaker?: string
  startTime: string
  endTime: string
  capacity: number
  verified?: boolean
  metadata?: Record<string, unknown>
}

export interface TicketEvent {
  id: number
  name: string
  venue: string
  eventDate: string
  category: EventCategory
  status: EventStatus
  rating: number | null
  totalRatings: number | null
  details: Record<string, unknown> | null
  eventSessions?: EventSession[]
}

export interface SearchHit {
  id: string
  name: string
  category: string
  venue: string
  description?: string
  eventDate: string
  rating?: number
  status: string
}

export interface BookingMetadata {
  app?: boolean
  tier?: string
  quantity?: number
  sessionTitle?: string
  /** Transitions this app had to set itself because no backend step does. */
  appSet?: BookingStatus[]
  saleId?: number
  [key: string]: unknown
}

export interface Booking {
  id: number
  userId: number
  eventId: number
  contactEmail: string
  status: BookingStatus
  totalAmount: number | null
  metadata: BookingMetadata | null
  bookingDate: string
  confirmedAt?: string | null
}

export interface CostEstimate {
  ticketCost: number
  serviceFee: number
  estimatedTotal: number
  demandMultiplier: number
}

export interface Ticket {
  id: number
  bookingId: number
  eventId: number | null
  attendeeName: string
  ticketCode: string
  status: TicketStatus
  issuedAt: string
  metadata?: Record<string, unknown> | null
}

export interface TicketSale {
  id: number
  bookingId: number
  userId: number
  amount: number
  method: PaymentMethod | null
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED'
  transactionDetails?: Record<string, unknown> | null
  createdAt: string
}
