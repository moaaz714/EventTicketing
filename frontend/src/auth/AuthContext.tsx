import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { readToken, setUnauthorizedHandler, writeToken } from '../api/client'
import * as endpoints from '../api/endpoints'
import type { JwtClaims } from '../api/types'

interface Session {
  token: string
  userId: number
  email: string
  role: JwtClaims['role']
}

interface AuthValue {
  session: Session | null
  signIn: (email: string, password: string) => Promise<void>
  signUp: (input: { name: string; email: string; password: string; phone: string }) => Promise<void>
  signOut: () => void
}

const AuthContext = createContext<AuthValue | null>(null)

// The login response carries only the token; the user id and role live in it.
function decode(token: string): Session | null {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const claims = JSON.parse(atob(payload)) as JwtClaims
    if (!claims.uid || claims.exp * 1000 < Date.now()) return null
    return { token, userId: claims.uid, email: claims.sub, role: claims.role }
  } catch {
    return null
  }
}

function initialSession(): Session | null {
  const token = readToken()
  const session = token ? decode(token) : null
  if (token && !session) writeToken(null)
  return session
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(initialSession)
  const queryClient = useQueryClient()

  const adopt = useCallback((token: string) => {
    const next = decode(token)
    if (!next) throw new Error('The server returned a token this app can’t read.')
    writeToken(token)
    setSession(next)
  }, [])

  const signOut = useCallback(() => {
    writeToken(null)
    setSession(null)
    queryClient.clear()
  }, [queryClient])

  useEffect(() => setUnauthorizedHandler(signOut), [signOut])

  const value = useMemo<AuthValue>(
    () => ({
      session,
      signIn: async (email, password) => adopt((await endpoints.login(email, password)).token),
      signUp: async (input) => adopt((await endpoints.register(input)).token),
      signOut,
    }),
    [session, adopt, signOut],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth outside AuthProvider')
  return ctx
}

/** For screens behind RequireAuth, where a session always exists. */
export function useSession() {
  const { session } = useAuth()
  if (!session) throw new Error('useSession without a session')
  return session
}
