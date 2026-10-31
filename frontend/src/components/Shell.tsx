import { Link, NavLink, Navigate, Outlet, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { useAuth } from '../auth/AuthContext'
import { SearchIcon } from './Icons'

export function RequireAuth() {
  const { session } = useAuth()
  const location = useLocation()
  if (!session) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Shell />
}

function HeaderSearch() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [value, setValue] = useState(params.get('q') ?? '')
  const input = useRef<HTMLInputElement>(null)

  useEffect(() => setValue(params.get('q') ?? ''), [params])

  // "/" focuses search, as on most catalogue sites. Never animated.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const t = e.target as HTMLElement
      if (e.key === '/' && !['INPUT', 'TEXTAREA', 'SELECT'].includes(t.tagName)) {
        e.preventDefault()
        input.current?.focus()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  const submit = (e: FormEvent) => {
    e.preventDefault()
    const q = value.trim()
    navigate(q ? `/events?q=${encodeURIComponent(q)}` : '/events')
  }

  return (
    <form className="search" role="search" onSubmit={submit}>
      <SearchIcon />
      <label htmlFor="global-search" className="visually-hidden">
        Search events
      </label>
      <input
        id="global-search"
        ref={input}
        type="search"
        placeholder="Search events, venues"
        value={value}
        onChange={(e) => setValue(e.target.value)}
      />
      <kbd aria-hidden="true">/</kbd>
    </form>
  )
}

function Shell() {
  const { session, signOut } = useAuth()
  return (
    <div className="app">
      <a className="skip" href="#main">
        Skip to content
      </a>
      <header className="topbar">
        <Link to="/events" className="wordmark pixel" aria-label="Event Ticketing, home">
          Event Ticketing
        </Link>
        <nav className="nav" aria-label="Main">
          <NavLink to="/events">Events</NavLink>
          <NavLink to="/bookings">My bookings</NavLink>
          <NavLink to="/tickets">Tickets</NavLink>
        </nav>
        <HeaderSearch />
        <div className="account">
          <span className="account-email" title={session?.email}>
            {session?.email}
          </span>
          <button className="link-btn" onClick={signOut}>
            Sign out
          </button>
        </div>
      </header>
      <main id="main" className="main">
        <Outlet />
      </main>
    </div>
  )
}
