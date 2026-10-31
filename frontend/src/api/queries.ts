import { useQueries, useQuery } from '@tanstack/react-query'
import { useMemo } from 'react'
import * as api from './endpoints'
import { isLive } from './journey'
import type { TicketEvent } from './types'

export const keys = {
  events: ['events'] as const,
  event: (id: number) => ['event', id] as const,
  sessions: (id: number) => ['sessions', id] as const,
  search: (q: string) => ['search', q] as const,
  bookings: (userId: number) => ['bookings', userId] as const,
  booking: (id: number) => ['booking', id] as const,
  estimate: (eventId: number, qty: number, tier: string) => ['estimate', eventId, qty, tier] as const,
  tickets: ['tickets'] as const,
}

export function useEvents() {
  return useQuery({ queryKey: keys.events, queryFn: api.listEvents, staleTime: 30_000 })
}

export function useEventMap() {
  const events = useEvents()
  const map = useMemo(() => {
    const m = new Map<number, TicketEvent>()
    for (const e of events.data ?? []) m.set(e.id, e)
    return m
  }, [events.data])
  return { map, loading: events.isLoading }
}

export function useEvent(id: number) {
  return useQuery({ queryKey: keys.event(id), queryFn: () => api.getEvent(id) })
}

export function useSessions(id: number) {
  return useQuery({ queryKey: keys.sessions(id), queryFn: () => api.listSessions(id) })
}

export function useBookings(userId: number) {
  return useQuery({
    queryKey: keys.bookings(userId),
    queryFn: () => api.listBookings(userId),
    // Keep the rail honest while anything is moving through the saga.
    refetchInterval: (q) => (q.state.data?.some(isLive) ? 4000 : false),
  })
}

export function useBooking(id: number) {
  return useQuery({
    queryKey: keys.booking(id),
    queryFn: () => api.getBooking(id),
    refetchInterval: (q) => (q.state.data && isLive(q.state.data) ? 2500 : false),
  })
}

/**
 * Events carry no price; the backend derives one per request from session
 * capacity. "From" is the standard price of one ticket, before fees.
 */
export function useFromPrices(eventIds: number[]) {
  const results = useQueries({
    queries: eventIds.map((id) => ({
      queryKey: keys.estimate(id, 1, 'STANDARD'),
      queryFn: () => api.estimateCost(id, 1, 'STANDARD'),
      staleTime: 60_000,
      retry: false,
    })),
  })
  return useMemo(() => {
    const m = new Map<number, number>()
    results.forEach((r, i) => {
      if (r.data && r.data.ticketCost > 0) m.set(eventIds[i], r.data.ticketCost)
    })
    return m
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [results.map((r) => r.dataUpdatedAt).join(), eventIds.join()])
}

export function useTickets() {
  return useQuery({ queryKey: keys.tickets, queryFn: api.listTickets })
}
