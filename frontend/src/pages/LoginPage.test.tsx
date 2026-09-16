import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/http'
import { AuthContext } from '../auth/AuthContext'
import type { AuthContextValue } from '../auth/AuthContext'
import { LoginPage } from './LoginPage'

function renderLogin(login: AuthContextValue['login']) {
  const value: AuthContextValue = {
    hasToken: false,
    isRestoring: false,
    login,
    logout: vi.fn(),
    user: null,
  }

  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/incidents"
            element={<div data-testid="destination" />}
          />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('LoginPage', () => {
  it('submits entered credentials', async () => {
    const login = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    renderLogin(login)

    await user.type(screen.getByLabelText('Email'), 'user@example.com')
    await user.type(screen.getByLabelText('Пароль'), 'secret-password')
    await user.click(screen.getByRole('button', { name: 'Войти' }))

    expect(login).toHaveBeenCalledWith({
      email: 'user@example.com',
      password: 'secret-password',
    })
    expect(await screen.findByTestId('destination')).toBeInTheDocument()
  })

  it('shows an authentication error', async () => {
    const login = vi
      .fn()
      .mockRejectedValue(new ApiError(401, { detail: 'Invalid credentials' }))
    const user = userEvent.setup()
    renderLogin(login)

    await user.type(screen.getByLabelText('Email'), 'user@example.com')
    await user.type(screen.getByLabelText('Пароль'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Войти' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Неверный email или пароль',
    )
  })
})
