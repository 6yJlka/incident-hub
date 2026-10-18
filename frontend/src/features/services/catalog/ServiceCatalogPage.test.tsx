import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ServiceCatalogPage } from './ServiceCatalogPage'

const api = vi.hoisted(() => ({
  fetch: vi.fn(),
}))

function renderPage(path = '/services') {
  return render(
    <MemoryRouter initialEntries={[path]}>
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
    vi.stubGlobal('fetch', api.fetch)
    api.fetch.mockReset()
    api.fetch.mockImplementation(async () =>
      Response.json({
        items: Array.from({ length: 10 }, (_, index) => ({
          id: index + 1,
          code: index === 0 ? 'CORE_DATABASE' : `SERVICE_${index + 1}`,
          name: index === 0 ? 'Core Database' : `Service ${index + 1}`,
          tier: index < 4 ? 'TIER_1' : 'TIER_2',
          ownerTeamName: 'Platform Engineering',
          ownerTeamCode: 'PLATFORM',
          dependencyCount: 0,
          dependentCount: index === 0 ? 2 : 0,
          activeIncidentCount: index === 0 ? 1 : 0,
        })),
        page: 0,
        size: 20,
        totalElements: 30,
        totalPages: 2,
        hasNext: true,
        hasPrevious: false,
      }),
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
    expect(row).toHaveTextContent('↓2')
    expect(row).toHaveTextContent('1')
    expect(api.fetch).toHaveBeenCalledTimes(1)
    expect(api.fetch).toHaveBeenCalledWith(
      '/api/v1/services?page=0&size=20',
      expect.objectContaining({ method: 'GET' }),
    )
  })

  it('requests only the selected page and never preloads remaining pages', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('CORE_DATABASE')
    expect(api.fetch).toHaveBeenCalledTimes(1)
    await user.click(screen.getByRole('button', { name: 'Следующая страница' }))
    await waitFor(() => expect(api.fetch).toHaveBeenCalledTimes(2))
    expect(api.fetch).toHaveBeenLastCalledWith(
      '/api/v1/services?page=1&size=20',
      expect.objectContaining({ method: 'GET' }),
    )
  })

  it('restores the page from the URL', async () => {
    renderPage('/services?page=1')
    await screen.findByText('CORE_DATABASE')
    expect(api.fetch).toHaveBeenCalledTimes(1)
    expect(api.fetch).toHaveBeenCalledWith(
      '/api/v1/services?page=1&size=20',
      expect.objectContaining({ method: 'GET' }),
    )
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
