import { render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ServiceDetailPage } from './ServiceDetailPage'

const api = vi.hoisted(() => ({
  getServiceDetail: vi.fn(),
}))

vi.mock('../api/servicesApi', () => api)

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/services/1']}>
      <Routes>
        <Route path="/services/:id" element={<ServiceDetailPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ServiceDetailPage', () => {
  beforeEach(() => {
    api.getServiceDetail.mockResolvedValue({
      service: {
        id: 1,
        code: 'CORE_DATABASE',
        name: 'Core Database',
        tier: 'TIER_1',
        ownerTeamName: 'Platform Engineering',
        dependencies: [],
      },
      affected: [
        {
          id: 2,
          code: 'IDENTITY_API',
          name: 'Identity API',
          depth: 1,
          dependencyType: 'DATA',
        },
        {
          id: 3,
          code: 'PAYMENT_API',
          name: 'Payment API',
          depth: 1,
          dependencyType: 'DATA',
        },
        {
          id: 4,
          code: 'API_GATEWAY',
          name: 'API Gateway',
          depth: 2,
          dependencyType: 'SYNC',
        },
        {
          id: 5,
          code: 'BILLING_WORKER',
          name: 'Billing Worker',
          depth: 2,
          dependencyType: 'ASYNC',
        },
        {
          id: 6,
          code: 'CUSTOMER_PORTAL',
          name: 'Customer Portal',
          depth: 2,
          dependencyType: 'SYNC',
        },
        {
          id: 7,
          code: 'ANALYTICS',
          name: 'Operations Analytics',
          depth: 3,
          dependencyType: 'DATA',
        },
        {
          id: 8,
          code: 'SUPPORT_DESK',
          name: 'Support Desk',
          depth: 3,
          dependencyType: 'SYNC',
        },
      ],
      incidents: [],
    })
  })

  it('keeps dependency direction and shows the base-service state', async () => {
    renderPage()

    await screen.findByRole('heading', { name: 'CORE_DATABASE' })
    const dependsPanel = screen
      .getByRole('heading', { name: 'Зависит от' })
      .closest('section')
    const affectedPanel = screen
      .getByRole('heading', { name: 'Зависят от неё' })
      .closest('section')

    expect(dependsPanel).not.toBeNull()
    expect(affectedPanel).not.toBeNull()
    expect(
      within(dependsPanel!).getByText(/Базовая услуга/),
    ).toBeInTheDocument()
    expect(
      within(dependsPanel!).queryByText('PAYMENT_API'),
    ).not.toBeInTheDocument()
    expect(within(affectedPanel!).getByText('PAYMENT_API')).toBeInTheDocument()
    expect(within(affectedPanel!).getAllByRole('link')).toHaveLength(7)
    expect(within(affectedPanel!).getAllByText(/^D[123]$/)).toHaveLength(7)
  })
})
