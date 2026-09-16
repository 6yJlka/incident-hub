import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ListIncidentsResponse } from '../../../api/generated/model'
import { IncidentFeedPage } from './IncidentFeedPage'

const api = vi.hoisted(() => ({
  getAllServices: vi.fn(),
  getAllTeams: vi.fn(),
  getIncidentPage: vi.fn(),
}))

vi.mock('../api/incidentsApi', () => api)

const populatedPage: ListIncidentsResponse = {
  items: [
    {
      id: 1041,
      title: 'Database connection pool saturation',
      affectedServiceId: 7,
      affectedServiceCode: 'CORE_DATABASE',
      source: 'AUTOMATIC',
      priority: 'CRITICAL',
      severity: 'SEV1',
      status: 'IN_PROGRESS',
      responsibleTeamCode: 'PLATFORM',
      assigneeDisplayName: 'Boris Petrov',
      createdAt: '2026-09-14T12:41:00Z',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  hasNext: false,
  hasPrevious: false,
}

function LocationProbe() {
  const location = useLocation()
  return <output data-testid="location">{location.search}</output>
}

function renderPage(initialEntry = '/incidents') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route
          path="/incidents"
          element={
            <>
              <IncidentFeedPage />
              <LocationProbe />
            </>
          }
        />
      </Routes>
    </MemoryRouter>,
  )
}

describe('IncidentFeedPage', () => {
  beforeEach(() => {
    api.getAllServices.mockResolvedValue([
      { id: 7, code: 'CORE_DATABASE', name: 'Core database', tier: 'TIER_1' },
    ])
    api.getAllTeams.mockResolvedValue([
      { id: 2, code: 'PLATFORM', name: 'Platform' },
    ])
    api.getIncidentPage.mockResolvedValue(populatedPage)
  })

  it('renders incident rows', async () => {
    renderPage()
    expect(
      await screen.findByText('Database connection pool saturation'),
    ).toBeInTheDocument()
    const row = screen.getByRole('row', {
      name: 'Открыть инцидент INC-1041',
    })
    expect(within(row).getByText('CORE_DATABASE')).toBeInTheDocument()
    expect(within(row).getByText('SEV1')).toBeInTheDocument()
    expect(within(row).getByText('AUTOMATIC')).toBeInTheDocument()
  })

  it('writes a selected filter to the URL and sends it to the API', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('Database connection pool saturation')
    await user.selectOptions(screen.getByLabelText('СТАТУС'), 'OPEN')
    await waitFor(() =>
      expect(api.getIncidentPage).toHaveBeenLastCalledWith(
        expect.objectContaining({ status: 'OPEN', page: 0 }),
      ),
    )
    expect(screen.getByTestId('location')).toHaveTextContent('?status=OPEN')
  })

  it('shows the empty result state', async () => {
    api.getIncidentPage.mockResolvedValue({
      items: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      hasNext: false,
      hasPrevious: false,
    })
    renderPage('/incidents?severity=SEV4')
    expect(await screen.findByText('Ничего не найдено')).toBeInTheDocument()
  })
})
