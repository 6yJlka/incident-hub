import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ServiceCatalogPage } from './ServiceCatalogPage'

const api = vi.hoisted(() => ({
  getServiceCatalog: vi.fn(),
}))

vi.mock('../api/servicesApi', () => api)

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/services']}>
      <Routes>
        <Route path="/services" element={<ServiceCatalogPage />} />
        <Route
          path="/services/:id"
          element={<div data-testid="service-detail" />}
        />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ServiceCatalogPage', () => {
  beforeEach(() => {
    api.getServiceCatalog.mockResolvedValue(
      Array.from({ length: 10 }, (_, index) => ({
        service: {
          id: index + 1,
          code: index === 0 ? 'CORE_DATABASE' : `SERVICE_${index + 1}`,
          name: index === 0 ? 'Core Database' : `Service ${index + 1}`,
          tier: index < 4 ? 'TIER_1' : 'TIER_2',
          ownerTeamName: 'Platform Engineering',
          ownerTeamCode: 'PLATFORM',
        },
        dependencyCount: 0,
        affectedCount: index === 0 ? 7 : 0,
        activeIncidents: index === 0 ? 1 : 0,
      })),
    )
  })

  it('renders the service list with dependency and incident counts', async () => {
    renderPage()

    expect(await screen.findByText('CORE_DATABASE')).toBeInTheDocument()
    expect(screen.getAllByRole('row')).toHaveLength(11)
    const row = screen.getByRole('row', {
      name: 'Открыть услугу CORE_DATABASE',
    })
    expect(row).toHaveTextContent('↑0')
    expect(row).toHaveTextContent('↓7')
    expect(row).toHaveTextContent('1')
  })

  it('opens a service card from a row', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(
      await screen.findByRole('row', {
        name: 'Открыть услугу CORE_DATABASE',
      }),
    )
    expect(screen.getByTestId('service-detail')).toBeInTheDocument()
  })
})
