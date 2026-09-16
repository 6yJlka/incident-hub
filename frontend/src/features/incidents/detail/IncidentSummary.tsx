import { useTranslation } from 'react-i18next'
import type { IncidentResponse } from '../../../api/generated/model'
import { formatDateTime } from '../model/format'
import styles from './IncidentDetailPage.module.css'

export function IncidentSummary({
  incident,
  scope,
}: {
  incident: IncidentResponse
  scope: number
}) {
  const { i18n, t } = useTranslation()
  const rows = [
    [
      t('incidents.detail.team'),
      incident.responsibleTeamCode ?? incident.responsibleTeamName ?? '—',
    ],
    [
      t('incidents.detail.assignee'),
      incident.assigneeDisplayName ?? t('incidents.unassigned'),
    ],
    [
      t('incidents.detail.created'),
      formatDateTime(incident.createdAt, i18n.language),
    ],
    [t('incidents.detail.source'), incident.source ?? '—'],
    [
      t('incidents.detail.scope'),
      t('incidents.detail.scopeValue', { count: scope }),
    ],
  ]
  return (
    <div className={styles.summary}>
      {rows.map(([label, value]) => (
        <div key={label}>
          <span>{label}</span>
          <strong>{value}</strong>
        </div>
      ))}
    </div>
  )
}
