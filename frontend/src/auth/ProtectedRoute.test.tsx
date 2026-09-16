import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext } from './AuthContext'
import type { AuthContextValue } from './AuthContext'
import { ProtectedRoute } from './ProtectedRoute'

describe('ProtectedRoute', () => {
  it('redirects to login without a token', async () => {
    const value: AuthContextValue = {
      hasToken: false,
      isRestoring: false,
      login: vi.fn(),
      logout: vi.fn(),
      user: null,
    }

    render(
      <AuthContext.Provider value={value}>
        <MemoryRouter initialEntries={['/incidents']}>
          <Routes>
            <Route element={<ProtectedRoute />}>
              <Route
                path="/incidents"
                element={<div data-testid="protected-page" />}
              />
            </Route>
            <Route path="/login" element={<div data-testid="login-page" />} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    expect(await screen.findByTestId('login-page')).toBeInTheDocument()
    expect(screen.queryByTestId('protected-page')).not.toBeInTheDocument()
  })
})
