import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router-dom'
import type {
  BusinessServiceListItemResponse,
  IncidentListItemResponse,
} from '../../../api/generated/model'
import { formatDateTime, incidentCode } from '../model/format'
import { ServiceTier } from '../shared/ServiceTier'
import { SeverityBadge } from '../shared/SeverityBadge'
import { SourceBadge } from '../shared/SourceBadge'
import { StatusBadge } from '../shared/StatusBadge'
import styles from './IncidentFeedPage.module.css'

interface IncidentTableProps {
  items: IncidentListItemResponse[]
  services: BusinessServiceListItemResponse[]
}

export function IncidentTable({ items, services }: IncidentTableProps) {
  const { i18n, t } = useTranslation()
  const navigate = useNavigate()
  const location = useLocation()
  const tiers = new Map(services.map((service) => [service.id, service.tier]))

  function openIncident(id?: number) {
    if (typeof id !== 'number') return
    void navigate(`/incidents/${id}`, {
      state: { feedSearch: location.search },
    })
  }

  return (
    <div className={styles.tableScroller}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>{t('incidents.table.id')}</th>
            <th>{t('incidents.table.title')}</th>
            <th>{t('incidents.table.service')}</th>
            <th>{t('incidents.table.severity')}</th>
            <th>{t('incidents.table.priority')}</th>
            <th>{t('incidents.table.status')}</th>
            <th>{t('incidents.table.team')}</th>
            <th>{t('incidents.table.assignee')}</th>
            <th>{t('incidents.table.created')}</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr
              key={item.id}
              tabIndex={0}
              aria-label={t('incidents.table.open', {
                id: incidentCode(item.id),
              })}
              onClick={() => openIncident(item.id)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ')
                  openIncident(item.id)
              }}
            >
              <td className={styles.monoMuted}>{incidentCode(item.id)}</td>
              <td>
                <div className={styles.titleCell}>
                  {item.source === 'AUTOMATIC' && (
                    <SourceBadge value={item.source} />
                  )}
                  <span>{item.title ?? '—'}</span>
                </div>
              </td>
              <td>
                <div className={styles.serviceCell}>
                  <ServiceTier tier={tiers.get(item.affectedServiceId)} />
                  <span>{item.affectedServiceCode ?? '—'}</span>
                </div>
              </td>
              <td>
                <SeverityBadge value={item.severity} />
              </td>
              <td className={styles.priority}>{item.priority ?? '—'}</td>
              <td>
                <StatusBadge value={item.status} />
              </td>
              <td className={styles.code}>{item.responsibleTeamCode ?? '—'}</td>
              <td className={item.assigneeDisplayName ? '' : styles.muted}>
                {item.assigneeDisplayName ?? t('incidents.unassigned')}
              </td>
              <td className={styles.date}>
                {formatDateTime(item.createdAt, i18n.language)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
