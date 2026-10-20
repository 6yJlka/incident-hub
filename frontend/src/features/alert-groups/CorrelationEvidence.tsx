import { useTranslation } from 'react-i18next'
import type { EvidenceResponse } from '../../api/generated/model'

function durationSeconds(value: string) {
  const match =
    /^(-)?P(?:(-?[\d.]+)D)?(?:T(?:(-?[\d.]+)H)?(?:(-?[\d.]+)M)?(?:(-?[\d.]+)S)?)?$/.exec(
      value,
    )
  if (!match) return null
  return (
    (match[1] ? -1 : 1) *
    (Number(match[2] ?? 0) * 86400 +
      Number(match[3] ?? 0) * 3600 +
      Number(match[4] ?? 0) * 60 +
      Number(match[5] ?? 0))
  )
}

export function CorrelationEvidence({
  evidence = [],
  serviceCode,
}: {
  evidence?: EvidenceResponse[]
  serviceCode: (id: number) => string
}) {
  const { t } = useTranslation()
  return (
    <ul>
      {evidence.map((item, index) => {
        let text: string
        if (item.type === 'TEMPORAL_PROXIMITY' && 'distanceToGroup' in item) {
          const seconds = durationSeconds(item.distanceToGroup ?? '')
          text =
            seconds === null
              ? (item.distanceToGroup ?? '')
              : t(
                  seconds < 0
                    ? 'groups.evidence.before'
                    : seconds > 0
                      ? 'groups.evidence.after'
                      : 'groups.evidence.within',
                  { seconds: Math.abs(seconds) },
                )
        } else if (
          item.type === 'EARLIEST_ALERT' &&
          'serviceId' in item &&
          'alertId' in item
        ) {
          text = t('groups.evidence.earliest', {
            alertId: item.alertId,
            service: serviceCode(item.serviceId!),
          })
        } else if (item.type === 'DEPENDENCY_PATH' && 'serviceIds' in item) {
          text = t('groups.evidence.path', {
            services: item.serviceIds?.map(serviceCode).join(' → '),
          })
        } else if (item.type === 'MIGRATED') {
          text = t('groups.evidence.migrated')
        } else text = item.type
        return <li key={index}>{text}</li>
      })}
    </ul>
  )
}
