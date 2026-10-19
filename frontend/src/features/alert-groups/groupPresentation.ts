import type {
  AlertGroupDetailResponse,
  AffectedServiceResponse,
} from '../../api/generated/model'

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
