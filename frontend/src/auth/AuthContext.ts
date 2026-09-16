import { createContext } from 'react'
import type { LoginRequest } from '../api/generated/model'
import type { CurrentUser } from './types'

export interface AuthContextValue {
  hasToken: boolean
  isRestoring: boolean
  login: (credentials: LoginRequest) => Promise<void>
  logout: () => void
  user: CurrentUser | null
}

export const AuthContext = createContext<AuthContextValue | null>(null)
