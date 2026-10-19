import { useState } from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { IncidentResponse } from '../../../api/generated/model'
import { ApiError } from '../../../api/http'
import type { CurrentUser } from '../../../auth/types'
import { IncidentActions } from './IncidentActions'

const api = vi.hoisted(() => ({
  executeIncidentAction: vi.fn(),
  getAllUsers: vi.fn(),
}))
vi.mock('../api/incidentsApi', () => api)

const engineer: CurrentUser = {
  id: 1,
  email: 'boris@example.com',
  displayName: 'Boris Petrov',
  role: 'ENGINEER',
  active: true,
}
const reporter: CurrentUser = {
  ...engineer,
  id: 2,
  role: 'REPORTER',
  displayName: 'Anna Ivanova',
}
const baseIncident: IncidentResponse = {
  id: 1041,
  title: 'Database issue',
  status: 'ASSIGNED',
  severity: 'SEV1',
  priority: 'CRITICAL',
  source: 'AUTOMATIC',
  availableActions: ['START', 'CANCEL'],
}

function ActionHarness({
  initial = baseIncident,
}: {
  initial?: IncidentResponse
}) {
  const [incident, setIncident] = useState(initial)
  return (
    <>
      <output data-testid="status">{incident.status}</output>
      <IncidentActions
        incident={incident}
        user={engineer}
        onUpdated={setIncident}
      />
    </>
  )
}

describe('IncidentActions', () => {
  it.each(['CLOSED', 'CANCELLED'] as const)(
    'names the actual terminal status %s',
    (status) => {
      render(
        <IncidentActions
          incident={{ ...baseIncident, status, availableActions: [] }}
          user={engineer}
          onUpdated={vi.fn()}
        />,
      )
      expect(
        screen.getByText(`В статусе ${status} действий нет.`),
      ).toBeInTheDocument()
      expect(screen.queryByRole('button')).not.toBeInTheDocument()
    },
  )

  it('explains the role for a reporter even in a terminal status', () => {
    render(
      <IncidentActions
        incident={{
          ...baseIncident,
          status: 'CANCELLED',
          availableActions: [],
        }}
        user={reporter}
        onUpdated={vi.fn()}
      />,
    )
    expect(
      screen.getByText('Ваша роль не меняет состояние инцидентов.'),
    ).toBeInTheDocument()
  })
  beforeEach(() => {
    api.getAllUsers.mockResolvedValue([])
  })

  it('renders only actions returned by availableActions', () => {
    render(
      <IncidentActions
        incident={baseIncident}
        user={engineer}
        onUpdated={vi.fn()}
      />,
    )
    expect(
      screen.getByRole('button', { name: 'Начать работу' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Отменить' })).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Закрыть' }),
    ).not.toBeInTheDocument()
  })

  it('distinguishes role and terminal-status explanations', () => {
    const { rerender } = render(
      <IncidentActions
        incident={{ ...baseIncident, status: 'OPEN', availableActions: [] }}
        user={reporter}
        onUpdated={vi.fn()}
      />,
    )
    expect(
      screen.getByText('Ваша роль не меняет состояние инцидентов.'),
    ).toBeInTheDocument()
    rerender(
      <IncidentActions
        incident={{ ...baseIncident, status: 'CLOSED', availableActions: [] }}
        user={engineer}
        onUpdated={vi.fn()}
      />,
    )
    expect(
      screen.getByText('В статусе CLOSED действий нет.'),
    ).toBeInTheDocument()
  })

  it('replaces the card action state with the action response', async () => {
    api.executeIncidentAction.mockResolvedValue({
      ...baseIncident,
      status: 'IN_PROGRESS',
      availableActions: ['RESOLVE'],
    })
    const user = userEvent.setup()
    render(<ActionHarness />)
    await user.click(screen.getByRole('button', { name: 'Начать работу' }))
    expect(await screen.findByText('IN_PROGRESS')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Устранить' }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Начать работу' }),
    ).not.toBeInTheDocument()
  })

  it('shows ProblemDetail title and detail for a conflict', async () => {
    api.executeIncidentAction.mockRejectedValue(
      new ApiError(409, {
        title: 'Conflict',
        detail: 'Incident status changed',
      }),
    )
    const user = userEvent.setup()
    render(
      <IncidentActions
        incident={baseIncident}
        user={engineer}
        onUpdated={vi.fn()}
      />,
    )
    await user.click(screen.getByRole('button', { name: 'Начать работу' }))
    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Conflict')
    expect(alert).toHaveTextContent('Incident status changed')
  })
})
