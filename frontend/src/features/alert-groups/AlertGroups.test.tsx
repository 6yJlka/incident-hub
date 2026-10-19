import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AlertGroupDetailResponse } from '../../api/generated/model'
import { ApiError } from '../../api/http'
import { AlertGroupFeedPage } from './AlertGroupFeedPage'
import { AlertGroupDetailPage } from './AlertGroupDetailPage'
import { AuthContext } from '../../auth/AuthContext'
import type { AuthContextValue } from '../../auth/AuthContext'
import { AppShell } from '../../layout/AppShell'
import { notifyGroupsChanged } from './groupEvents'

const api = vi.hoisted(() => ({
  listAlertGroups: vi.fn(),
  getAlertGroup: vi.fn(),
  confirmAlertGroup: vi.fn(),
  dismissAlertGroup: vi.fn(),
  getAffectedServices: vi.fn(),
  getService: vi.fn(),
  getAllServices: vi.fn(),
}))
vi.mock('../../api/generated/alert-groups/alert-groups', () => api)
vi.mock('../../api/generated/service-catalog/service-catalog', () => api)
vi.mock('../incidents/api/incidentsApi', () => api)

const group: AlertGroupDetailResponse = {
  id: 7,
  version: 12,
  status: 'OPEN',
  probableRootServiceId: 1,
  probableRootServiceCode: 'CORE_DATABASE',
  probableRootServiceName: 'Database',
  availableActions: ['CONFIRM', 'DISMISS'],
  alerts: [
    {
      id: 1,
      businessServiceId: 1,
      businessServiceCode: 'CORE_DATABASE',
      severity: 'CRITICAL',
      signalType: 'LATENCY',
      source: 'monitor',
      description: 'Root signal',
    },
    {
      id: 2,
      businessServiceId: 2,
      businessServiceCode: 'API_GATEWAY',
      severity: 'WARNING',
      signalType: 'LATENCY',
      source: 'monitor',
      description: 'Dependent signal',
    },
    {
      id: 3,
      businessServiceId: 3,
      businessServiceCode: 'ANALYTICS',
      severity: 'INFO',
      signalType: 'SATURATION',
      source: 'monitor',
      description: 'Noise signal',
    },
  ],
}

function auth(role: 'REPORTER' | 'ENGINEER' | 'ADMIN'): AuthContextValue {
  return {
    hasToken: true,
    isRestoring: false,
    login: vi.fn(),
    logout: vi.fn(),
    user: {
      id: 9,
      email: 'user@example.com',
      displayName: 'Engineer',
      role,
      active: true,
    },
  }
}
function LocationProbe() {
  const location = useLocation()
  return <output data-testid="location">{location.search}</output>
}
function detail(role: 'REPORTER' | 'ENGINEER' | 'ADMIN' = 'ENGINEER') {
  return render(
    <AuthContext.Provider value={auth(role)}>
      <MemoryRouter initialEntries={['/alert-groups/7']}>
        <Routes>
          <Route path="/alert-groups/:id" element={<AlertGroupDetailPage />} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}
beforeEach(() => {
  vi.resetAllMocks()
  api.getAllServices.mockResolvedValue([
    { id: 4, code: 'Z_CATALOG' },
    { id: 2, code: 'API_GATEWAY' },
    { id: 1, code: 'CORE_DATABASE' },
    { id: 3, code: 'ANALYTICS' },
  ])
  api.getAlertGroup.mockResolvedValue(group)
  api.getService.mockResolvedValue({
    id: 1,
    code: 'CORE_DATABASE',
    ownerTeamName: 'Platform',
  })
  api.getAffectedServices.mockResolvedValue({
    items: [
      {
        id: 2,
        code: 'API_GATEWAY',
        depth: 1,
        dependencies: [{ serviceId: 1, type: 'SYNC' }],
      },
    ],
  })
  api.listAlertGroups.mockResolvedValue({
    items: [
      {
        id: 7,
        status: 'CONFIRMED',
        probableRootServiceCode: 'CORE_DATABASE',
        alertCount: 8,
        serviceCount: 5,
        servicePreviewCodes: ['CORE_DATABASE', 'API_GATEWAY', 'PAYMENT_API'],
        worstSeverity: 'CRITICAL',
        incidentId: 42,
      },
    ],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
  })
  api.confirmAlertGroup.mockResolvedValue({
    ...group,
    version: 13,
    status: 'CONFIRMED',
    availableActions: [],
    incidentId: 42,
    decidedByDisplayName: 'Engineer',
    decidedAt: '2026-10-19T12:00:00Z',
  })
  api.dismissAlertGroup.mockResolvedValue({
    ...group,
    version: 13,
    status: 'DISMISSED',
    availableActions: [],
    dismissReason: 'Maintenance',
    decidedByDisplayName: 'Engineer',
    decidedAt: '2026-10-19T12:00:00Z',
  })
})

describe('alert groups', () => {
  it('focuses the first empty text field before missing choice fields', async () => {
    const user = userEvent.setup()
    detail()
    await user.click(await screen.findByRole('button', { name: 'Подтвердить' }))
    const modal = screen.getByRole('dialog')
    const title = within(modal).getByRole('textbox', { name: 'Заголовок' })
    await user.clear(title)
    await user.click(
      within(modal).getByRole('button', { name: 'Создать инцидент' }),
    )
    expect(title).toHaveFocus()
    expect(title).toHaveAttribute('aria-invalid', 'true')
    expect(title).toHaveAccessibleDescription(
      'Введите заголовок длиной до 255 символов',
    )
    expect(api.confirmAlertGroup).not.toHaveBeenCalled()
  })
  it('shows labelled group statistics and decision details', async () => {
    api.getAlertGroup.mockResolvedValue({
      ...group,
      firstAlertAt: '2026-10-19T12:00:00Z',
      lastAlertAt: '2026-10-19T12:02:05Z',
      status: 'CONFIRMED',
      availableActions: [],
      decidedByDisplayName: 'Engineer',
      decidedAt: '2026-10-19T12:03:00Z',
      incidentId: 42,
    })
    detail()
    await screen.findByRole('img', { name: 'Схема распространения отказа' })
    const sidebar = screen.getByRole('complementary')
    for (const [label, value] of [
      ['Алерты', '3'],
      ['Услуги в группе', '3'],
      ['На пути', '2'],
      ['Вне пути', '1'],
      ['Длительность', '2 мин 5 с'],
    ]) {
      expect(
        within(sidebar).getByText(label).nextElementSibling,
      ).toHaveTextContent(value)
    }
    expect(
      within(sidebar).getByText('Кто подтвердил').nextElementSibling,
    ).toHaveTextContent('Engineer')
    expect(
      within(sidebar).getByText('Когда').nextElementSibling,
    ).not.toHaveTextContent('—')
    expect(
      within(sidebar).getByText('Созданный инцидент').nextElementSibling,
    ).toHaveTextContent('INC-42')
    expect(screen.getByText('Макс. сигнал').parentElement).toHaveTextContent(
      'CRITICAL',
    )
  })

  it('labels the author and optional reason after dismissal', async () => {
    api.getAlertGroup.mockResolvedValue({
      ...group,
      status: 'DISMISSED',
      availableActions: [],
      decidedByDisplayName: 'Engineer',
    })
    detail()
    await screen.findByText('Кто отклонил')
    expect(
      screen.getByText('Причина отклонения').nextElementSibling,
    ).toHaveTextContent('Не указана')
  })

  it('supports keyboard choice and focuses the next invalid field', async () => {
    const user = userEvent.setup()
    detail()
    await user.click(await screen.findByRole('button', { name: 'Подтвердить' }))
    const modal = screen.getByRole('dialog')
    await user.click(
      within(modal).getByRole('button', { name: 'Создать инцидент' }),
    )
    expect(
      within(modal).getByRole('radiogroup', { name: 'Severity инцидента' }),
    ).toHaveAccessibleDescription('Выберите severity')
    await user.keyboard('{ArrowRight}')
    expect(within(modal).getByRole('radio', { name: 'SEV2' })).toHaveAttribute(
      'aria-checked',
      'true',
    )
    expect(
      within(modal).queryByText('Выберите severity'),
    ).not.toBeInTheDocument()
    await user.click(
      within(modal).getByRole('button', { name: 'Создать инцидент' }),
    )
    expect(within(modal).getByRole('radio', { name: 'CRITICAL' })).toHaveFocus()
    expect(
      within(modal).getByRole('radiogroup', { name: 'Приоритет' }),
    ).toHaveAccessibleDescription('Выберите priority')
    expect(within(modal).getByText(/Макс\. сигнал группы/)).toHaveTextContent(
      'CRITICAL',
    )
    expect(api.confirmAlertGroup).not.toHaveBeenCalled()
  })
  it('uses only availableActions even for an engineer', async () => {
    api.getAlertGroup.mockResolvedValue({
      ...group,
      availableActions: ['DISMISS'],
    })
    detail()
    expect(
      await screen.findByRole('button', { name: 'Отклонить' }),
    ).toBeEnabled()
    expect(
      screen.queryByRole('button', { name: 'Подтвердить' }),
    ).not.toBeInTheDocument()
  })

  it('can refresh after catalogue failure before confirming', async () => {
    api.getAllServices.mockRejectedValueOnce(new Error('offline'))
    const user = userEvent.setup()
    detail()
    await screen.findByText(
      'Не удалось загрузить каталог. Обновите карточку перед подтверждением.',
    )
    expect(screen.getByRole('button', { name: 'Подтвердить' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Обновить' }))
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Подтвердить' })).toBeEnabled(),
    )
  })
  it('renders aggregates, service preview, incident link and URL filters', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/alert-groups']}>
        <AlertGroupFeedPage />
        <LocationProbe />
      </MemoryRouter>,
    )
    expect(await screen.findByRole('link', { name: 'GRP-7' })).toHaveAttribute(
      'href',
      '/alert-groups/7',
    )
    expect(
      screen.getByText('CORE_DATABASE, API_GATEWAY, PAYMENT_API +2'),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'INC-42' })).toHaveAttribute(
      'href',
      '/incidents/42',
    )
    await user.selectOptions(screen.getByLabelText('Статус'), 'OPEN')
    await user.selectOptions(screen.getByLabelText('Корень'), '1')
    await waitFor(() =>
      expect(api.listAlertGroups).toHaveBeenLastCalledWith({
        page: 0,
        size: 20,
        status: 'OPEN',
        rootServiceId: 1,
      }),
    )
    expect(screen.getByTestId('location')).toHaveTextContent(
      'status=OPEN&rootServiceId=1',
    )
    await user.click(screen.getByRole('button', { name: 'Сбросить фильтры' }))
    await waitFor(() =>
      expect(api.listAlertGroups).toHaveBeenLastCalledWith({
        page: 0,
        size: 20,
        status: undefined,
        rootServiceId: undefined,
      }),
    )
  })

  it.each(['REPORTER', 'ENGINEER', 'ADMIN'] as const)(
    'shows actions from the API for %s',
    async (role) => {
      api.getAlertGroup.mockResolvedValue({
        ...group,
        availableActions: role === 'REPORTER' ? [] : ['CONFIRM', 'DISMISS'],
      })
      detail(role)
      await screen.findByRole('heading', { name: 'GRP-7' })
      if (role === 'REPORTER') {
        expect(screen.getByText('Ожидает решения инженера')).toBeInTheDocument()
        expect(
          screen.queryByRole('button', { name: 'Подтвердить' }),
        ).not.toBeInTheDocument()
        expect(
          screen.queryByRole('button', { name: 'Отклонить' }),
        ).not.toBeInTheDocument()
      } else
        expect(
          await screen.findByRole('button', { name: 'Подтвердить' }),
        ).toBeEnabled()
    },
  )

  it('marks noise outside the path, defaults to diagram and preserves both diamond edges', async () => {
    api.getAffectedServices.mockResolvedValue({
      items: [
        {
          id: 2,
          code: 'API_GATEWAY',
          depth: 1,
          dependencies: [{ serviceId: 1, type: 'SYNC' }],
        },
        {
          id: 5,
          code: 'PAYMENT_API',
          depth: 1,
          dependencies: [{ serviceId: 1, type: 'DATA' }],
        },
        {
          id: 6,
          code: 'CUSTOMER_PORTAL',
          depth: 2,
          dependencies: [
            { serviceId: 2, type: 'SYNC' },
            { serviceId: 5, type: 'ASYNC' },
          ],
        },
      ],
    })
    const user = userEvent.setup()
    detail()
    const graph = await screen.findByRole('img', {
      name: 'Схема распространения отказа',
    })
    expect(graph.querySelector('[data-edge="2-6"]')).toBeInTheDocument()
    expect(graph.querySelector('[data-edge="5-6"]')).toBeInTheDocument()
    for (const edge of graph.querySelectorAll('[data-edge]')) {
      const marker = edge.getAttribute('marker-end')!
      expect(marker).toMatch(/^url\(#.+\)$/)
      expect(
        graph.querySelector(`[id="${marker.slice(5, -1)}"]`),
      ).toHaveAttribute('orient', 'auto')
    }
    const noise = screen.getByRole('row', { name: /ANALYTICS.*Noise signal/ })
    expect(within(noise).getByText('Вне пути')).toBeInTheDocument()
    expect(
      within(
        screen.getByRole('row', { name: /API_GATEWAY.*Dependent signal/ }),
      ).queryByText('Вне пути'),
    ).not.toBeInTheDocument()
    expect(api.getService).toHaveBeenCalledTimes(1)
    expect(api.getAffectedServices).toHaveBeenCalledWith(1, { maxDepth: 10 })
    await user.click(screen.getByRole('button', { name: 'Подпись' }))
    expect(screen.getByText('Platform')).toBeInTheDocument()
  })

  it('does not classify services when affected loading fails', async () => {
    api.getAffectedServices.mockRejectedValue(new Error('offline'))
    detail()
    await screen.findByText(
      'Не удалось загрузить зависимости. Принадлежность пути не определена.',
    )
    expect(
      within(
        screen.getByRole('row', { name: /ANALYTICS.*Noise signal/ }),
      ).queryByText('Вне пути'),
    ).not.toBeInTheDocument()
  })

  it('requires severity and priority, orders group services first, and sends templates and viewed version', async () => {
    const user = userEvent.setup()
    detail()
    await user.click(await screen.findByRole('button', { name: 'Подтвердить' }))
    const modal = screen.getByRole('dialog')
    expect(
      within(modal)
        .getAllByRole('radio')
        .every((button) => button.getAttribute('aria-checked') === 'false'),
    ).toBe(true)
    expect(
      within(within(modal).getByLabelText('Услуга инцидента'))
        .getAllByRole('option')
        .map((option) => option.textContent),
    ).toEqual([
      'CORE_DATABASE',
      'API_GATEWAY',
      'ANALYTICS',
      '──── Остальной каталог ────',
      'Z_CATALOG',
    ])
    await user.click(
      within(modal).getByRole('button', { name: 'Создать инцидент' }),
    )
    expect(api.confirmAlertGroup).not.toHaveBeenCalled()
    expect(within(modal).getByText('Выберите severity')).toBeInTheDocument()
    expect(within(modal).getByText('Выберите priority')).toBeInTheDocument()
    expect(within(modal).getByRole('radio', { name: 'SEV1' })).toHaveFocus()
    expect(
      within(modal).getByRole('radiogroup', { name: 'Severity инцидента' }),
    ).toHaveAttribute('aria-invalid', 'true')
    await user.click(within(modal).getByRole('radio', { name: 'SEV2' }))
    await user.click(
      within(modal).getByRole('button', { name: 'Создать инцидент' }),
    )
    expect(api.confirmAlertGroup).not.toHaveBeenCalled()
    expect(within(modal).getByRole('radio', { name: 'CRITICAL' })).toHaveFocus()
    await user.click(within(modal).getByRole('radio', { name: 'HIGH' }))
    await user.click(
      within(modal).getByRole('button', { name: 'Создать инцидент' }),
    )
    await waitFor(() =>
      expect(api.confirmAlertGroup).toHaveBeenCalledWith(7, {
        expectedVersion: 12,
        businessServiceId: 1,
        severity: 'SEV2',
        priority: 'HIGH',
        title: 'CORE_DATABASE: алерты от 3 услуг',
        description:
          'Группа GRP-7: 3 алертов. Вероятный корень — CORE_DATABASE. Услуги группы: CORE_DATABASE, API_GATEWAY, ANALYTICS.',
      }),
    )
    expect(
      await screen.findByRole('link', { name: /^INC-42/ }),
    ).toHaveAttribute('href', '/incidents/42')
    expect(screen.getByText('Engineer')).toBeInTheDocument()
  })

  it.each(['', 'Maintenance'])(
    'dismisses with viewed version and optional reason %s',
    async (reason) => {
      const user = userEvent.setup()
      detail()
      await user.click(await screen.findByRole('button', { name: 'Отклонить' }))
      const modal = screen.getByRole('dialog')
      if (reason)
        await user.type(
          within(modal).getByLabelText('Причина (необязательно)'),
          reason,
        )
      await user.click(within(modal).getByRole('button', { name: 'Отклонить' }))
      await waitFor(() =>
        expect(api.dismissAlertGroup).toHaveBeenCalledWith(7, {
          expectedVersion: 12,
          reason: reason || undefined,
        }),
      )
      expect(await screen.findByText('Engineer')).toBeInTheDocument()
      expect(screen.getByText('Maintenance')).toBeInTheDocument()
    },
  )

  it('blocks decisions after concurrent modification until explicit successful refresh', async () => {
    api.dismissAlertGroup.mockRejectedValue(
      new ApiError(409, {
        type: 'urn:incident-hub:problem:concurrent-modification',
      }),
    )
    const user = userEvent.setup()
    detail()
    await user.click(await screen.findByRole('button', { name: 'Отклонить' }))
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', {
        name: 'Отклонить',
      }),
    )
    await screen.findByText(
      'Группа изменилась. Обновите карточку перед решением.',
    )
    expect(screen.getByRole('button', { name: 'Подтвердить' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Отклонить' })).toBeDisabled()
    expect(api.getAlertGroup).toHaveBeenCalledTimes(1)
    api.getAlertGroup.mockRejectedValueOnce(new Error('offline'))
    await user.click(screen.getByRole('button', { name: 'Обновить' }))
    await waitFor(() => expect(api.getAlertGroup).toHaveBeenCalledTimes(2))
    expect(screen.getByRole('button', { name: 'Подтвердить' })).toBeDisabled()
    api.getAlertGroup.mockResolvedValue({ ...group, version: 15 })
    await user.click(screen.getByRole('button', { name: 'Обновить' }))
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Подтвердить' })).toBeEnabled(),
    )
    await user.click(screen.getByRole('button', { name: 'Отклонить' }))
    api.dismissAlertGroup.mockResolvedValue({
      ...group,
      status: 'DISMISSED',
      availableActions: [],
    })
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', {
        name: 'Отклонить',
      }),
    )
    await waitFor(() =>
      expect(api.dismissAlertGroup).toHaveBeenLastCalledWith(7, {
        expectedVersion: 15,
        reason: undefined,
      }),
    )
  })

  it('reports a state conflict and reloads the card automatically', async () => {
    api.dismissAlertGroup.mockRejectedValue(
      new ApiError(409, { type: 'urn:incident-hub:problem:conflict' }),
    )
    const user = userEvent.setup()
    detail()
    await user.click(await screen.findByRole('button', { name: 'Отклонить' }))
    api.getAlertGroup.mockResolvedValue({
      ...group,
      status: 'DISMISSED',
      availableActions: [],
      decidedByDisplayName: 'Another engineer',
    })
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', {
        name: 'Отклонить',
      }),
    )
    expect(
      await screen.findByText('Действие уже недоступно. Карточка обновлена.'),
    ).toBeInTheDocument()
    expect(await screen.findByText('Another engineer')).toBeInTheDocument()
    expect(api.getAlertGroup).toHaveBeenCalledTimes(2)
    expect(
      screen.queryByRole('button', { name: 'Подтвердить' }),
    ).not.toBeInTheDocument()
  })

  it('loads the OPEN total for section navigation and refreshes it after a decision', async () => {
    api.listAlertGroups.mockResolvedValue({ totalElements: 8 })
    render(
      <AuthContext.Provider value={auth('REPORTER')}>
        <MemoryRouter>
          <AppShell />
        </MemoryRouter>
      </AuthContext.Provider>,
    )
    const counter = await screen.findByLabelText('OPEN-групп: 8')
    expect(counter.closest('a')).toHaveAttribute('href', '/alert-groups')
    expect(api.listAlertGroups).toHaveBeenCalledWith({
      status: 'OPEN',
      page: 0,
      size: 1,
    })
    api.listAlertGroups.mockResolvedValue({ totalElements: 7 })
    act(() => notifyGroupsChanged())
    expect(await screen.findByLabelText('OPEN-групп: 7')).toBeInTheDocument()
  })
})
