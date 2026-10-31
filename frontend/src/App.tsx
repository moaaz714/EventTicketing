import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ApiError } from './api/client'
import { AuthProvider } from './auth/AuthContext'
import { RequireAuth } from './components/Shell'
import { ToastProvider } from './components/Toasts'
import { LoginPage, RegisterPage } from './pages/AuthPages'
import { BookingPage } from './pages/BookingPage'
import { EventPage } from './pages/EventPage'
import { EventsPage } from './pages/EventsPage'
import { BookingsPage, TicketsPage } from './pages/ListPages'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Client errors won't fix themselves; only retry flaky upstreams.
      retry: (count, error) => count < 2 && !(error instanceof ApiError && error.status >= 400 && error.status < 500),
      refetchOnWindowFocus: false,
    },
  },
})

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <BrowserRouter>
          <AuthProvider>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route element={<RequireAuth />}>
                <Route path="/events" element={<EventsPage />} />
                <Route path="/events/:id" element={<EventPage />} />
                <Route path="/bookings" element={<BookingsPage />} />
                <Route path="/bookings/:id" element={<BookingPage />} />
                <Route path="/tickets" element={<TicketsPage />} />
              </Route>
              <Route path="*" element={<Navigate to="/events" replace />} />
            </Routes>
          </AuthProvider>
        </BrowserRouter>
      </ToastProvider>
    </QueryClientProvider>
  )
}
