import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, expect, it, vi } from 'vitest'
import { AuthContext } from '../../../auth/AuthContext'
import type { AuthContextValue } from '../../../auth/AuthContext'
import { IncidentDetailPage } from './IncidentDetailPage'

const api = vi.hoisted(() => ({
  getIncident: vi.fn(),
  getIncidentHistory: vi.fn(),
  getIncidentServiceContext: vi.fn(),
}))
vi.mock('../../../api/generated/incidents/incidents', () => api)
vi.mock('../api/incidentsApi', () => api)

const auth: AuthContextValue = {
  hasToken: true,
  isRestoring: false,
  login: vi.fn(),
  logout: vi.fn(),
  user: {
    id: 1,
    email: 'reporter@example.com',
    displayName: 'Reporter',
    role: 'REPORTER',
    active: true,
  },
}
beforeEach(() => {
  vi.resetAllMocks()
  api.getIncidentHistory.mockResolvedValue({ items: [] })
  api.getIncidentServiceContext.mockResolvedValue([
    { id: 1, code: 'CORE_DATABASE', dependencies: [] },
    { items: [] },
    [],
  ])
})
it.each([7, undefined])(
  'links to originating group only when alertGroupId is present (%s)',
  async (alertGroupId) => {
    api.getIncident.mockResolvedValue({
      id: 42,
      title: 'Database issue',
      affectedServiceId: 1,
      status: 'OPEN',
      severity: 'SEV2',
      source: 'AUTOMATIC',
      priority: 'HIGH',
      availableActions: [],
      alertGroupId,
    })
    render(
      <AuthContext.Provider value={auth}>
        <MemoryRouter initialEntries={['/incidents/42']}>
          <Routes>
            <Route path="/incidents/:id" element={<IncidentDetailPage />} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )
    await screen.findByText('Database issue')
    if (alertGroupId) {
      const origin = screen.getByRole('link', {
        name: /Создан из группы GRP-7/,
      })
      expect(origin).toHaveAttribute('href', '/alert-groups/7')
      expect(origin.closest('header')).toBeInTheDocument()
      expect(origin.previousElementSibling).toHaveTextContent('AUTOMATIC')
    } else
      expect(
        screen.queryByRole('link', { name: /Создан из группы/ }),
      ).not.toBeInTheDocument()
  },
)
