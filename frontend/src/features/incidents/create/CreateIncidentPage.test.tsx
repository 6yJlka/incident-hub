import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../api/http'
import { AuthContext } from '../../../auth/AuthContext'
import type { AuthContextValue } from '../../../auth/AuthContext'
import { CreateIncidentPage } from './CreateIncidentPage'

const api = vi.hoisted(() => ({
  createIncident: vi.fn(),
  getCreateIncidentReferences: vi.fn(),
  getIncidentImpactPreview: vi.fn(),
}))

vi.mock('./createIncidentApi', () => api)

const auth: AuthContextValue = {
  hasToken: true,
  isRestoring: false,
  login: vi.fn(),
  logout: vi.fn(),
  user: {
    id: 5,
    email: 'olga@example.com',
    displayName: 'Olga Smirnova',
    role: 'ADMIN',
    active: true,
  },
}

function renderPage() {
  return render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={['/incidents/new']}>
        <Routes>
          <Route path="/incidents/new" element={<CreateIncidentPage />} />
          <Route
            path="/incidents/:id"
            element={<div data-testid="incident-detail" />}
          />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

async function fillValidForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Заголовок'), 'Database is slow')
  await user.type(screen.getByLabelText('Описание'), 'Connections time out')
  await user.selectOptions(screen.getByLabelText('Затронутая услуга'), '1')
}

describe('CreateIncidentPage', () => {
  beforeEach(() => {
    api.getCreateIncidentReferences.mockResolvedValue([
      [
        {
          id: 1,
          code: 'CORE_DATABASE',
          name: 'Core Database',
          tier: 'TIER_1',
          ownerTeamName: 'Platform Engineering',
          active: true,
        },
      ],
      [
        {
          id: 10,
          code: 'PLATFORM',
          name: 'Platform Engineering',
          active: true,
        },
      ],
    ])
    api.getIncidentImpactPreview.mockResolvedValue({
      items: [
        { id: 2, code: 'IDENTITY_API', tier: 'TIER_1', depth: 1 },
        { id: 3, code: 'CUSTOMER_PORTAL', tier: 'TIER_2', depth: 2 },
      ],
    })
    api.createIncident.mockResolvedValue({ incidentId: 73, status: 'OPEN' })
  })

  it('shows an impact preview after a service is selected', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.selectOptions(
      await screen.findByLabelText('Затронутая услуга'),
      '1',
    )

    expect(await screen.findByText('IDENTITY_API')).toBeInTheDocument()
    expect(screen.getByText('CUSTOMER_PORTAL')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(api.getIncidentImpactPreview).toHaveBeenCalledWith(1)
  })

  it('creates an incident and opens its card', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByLabelText('Затронутая услуга')
    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: 'Создать инцидент' }))

    await waitFor(() =>
      expect(api.createIncident).toHaveBeenCalledWith({
        title: 'Database is slow',
        description: 'Connections time out',
        affectedServiceId: 1,
        severity: 'SEV3',
        priority: 'MEDIUM',
      }),
    )
    expect(await screen.findByTestId('incident-detail')).toBeInTheDocument()
  })

  it('shows a 400 validation error next to its field', async () => {
    api.createIncident.mockRejectedValue(
      new ApiError(400, {
        detail: 'One or more request fields are invalid',
        errors: [{ field: 'title', message: 'must not be blank' }],
      }),
    )
    const user = userEvent.setup()
    renderPage()
    await screen.findByLabelText('Затронутая услуга')
    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: 'Создать инцидент' }))

    const titleField = screen.getByLabelText(/Заголовок/).closest('label')
    expect(titleField).not.toBeNull()
    expect(
      await within(titleField!).findByText('must not be blank'),
    ).toBeInTheDocument()
  })
})
