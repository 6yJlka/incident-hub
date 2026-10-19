import type {
  AlertGroupDetailResponse,
  AffectedServiceResponse,
  GroupAlertResponse,
} from '../../api/generated/model'
import type { TFunction } from 'i18next'

export function maximumSignal(alerts: GroupAlertResponse[] = []) {
  return alerts.some((alert) => alert.severity === 'CRITICAL')
    ? 'CRITICAL'
    : alerts.some((alert) => alert.severity === 'WARNING')
      ? 'WARNING'
      : alerts.some((alert) => alert.severity === 'INFO')
        ? 'INFO'
        : undefined
}

export function formatGroupDuration(
  first: string | undefined,
  last: string | undefined,
  t: TFunction,
) {
  if (!first || !last) return '—'
  const seconds = Math.max(
    0,
    Math.floor((Date.parse(last) - Date.parse(first)) / 1000),
  )
  if (!Number.isFinite(seconds)) return '—'
  return t('groups.durationValue', {
    minutes: Math.floor(seconds / 60),
    seconds: seconds % 60,
  })
}

export function groupServices(group: AlertGroupDetailResponse) {
  return [
    ...new Map(
      (group.alerts ?? []).map((alert) => [
        alert.businessServiceId,
        {
          id: alert.businessServiceId,
          code: alert.businessServiceCode,
          name: alert.businessServiceName,
        },
      ]),
    ).values(),
  ]
}

export function reachedServiceIds(
  rootId: number,
  affected: AffectedServiceResponse[],
) {
  return new Set([
    rootId,
    ...affected.flatMap((service) =>
      service.id === undefined ? [] : [service.id],
    ),
  ])
}

export function formatGroupTime(value?: string) {
  return value ? new Date(value).toLocaleString('ru-RU') : '—'
}
