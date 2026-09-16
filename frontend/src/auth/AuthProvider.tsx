import { useCallback, useEffect, useMemo, useState } from 'react'
import type { PropsWithChildren } from 'react'
import { login as loginRequest } from '../api/generated/authentication/authentication'
import type { LoginRequest } from '../api/generated/model'
import { current as currentUserRequest } from '../api/generated/users/users'
import { AuthContext } from './AuthContext'
import {
  AUTH_EXPIRED_EVENT,
  clearAccessToken,
  getAccessToken,
  setAccessToken,
} from './tokenStorage'
import type { CurrentUser } from './types'
import { toCurrentUser } from './types'

export function AuthProvider({ children }: PropsWithChildren) {
  const [hasToken, setHasToken] = useState(() => Boolean(getAccessToken()))
  const [isRestoring, setIsRestoring] = useState(() =>
    Boolean(getAccessToken()),
  )
  const [user, setUser] = useState<CurrentUser | null>(null)

  const logout = useCallback(() => {
    clearAccessToken()
    setHasToken(false)
    setIsRestoring(false)
    setUser(null)
  }, [])

  useEffect(() => {
    const handleExpiredAuthentication = () => logout()
    window.addEventListener(AUTH_EXPIRED_EVENT, handleExpiredAuthentication)
    return () =>
      window.removeEventListener(
        AUTH_EXPIRED_EVENT,
        handleExpiredAuthentication,
      )
  }, [logout])

  useEffect(() => {
    if (!hasToken || user) {
      return
    }

    let active = true

    void currentUserRequest()
      .then((response) => {
        if (active) {
          setUser(toCurrentUser(response))
        }
      })
      .catch(() => {
        if (active) {
          logout()
        }
      })
      .finally(() => {
        if (active) {
          setIsRestoring(false)
        }
      })

    return () => {
      active = false
    }
  }, [hasToken, logout, user])

  const login = useCallback(async (credentials: LoginRequest) => {
    const response = await loginRequest(credentials)
    if (!response.accessToken) {
      throw new Error('Login response does not contain an access token')
    }

    setAccessToken(response.accessToken)

    try {
      setUser(toCurrentUser(await currentUserRequest()))
      setHasToken(true)
    } catch (error) {
      clearAccessToken()
      setHasToken(false)
      setUser(null)
      throw error
    }
  }, [])

  const value = useMemo(
    () => ({ hasToken, isRestoring, login, logout, user }),
    [hasToken, isRestoring, login, logout, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
