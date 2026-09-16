import { useTranslation } from 'react-i18next'
import type { IncidentResponse } from '../../../api/generated/model'
import { incidentCode } from '../model/format'
import { SeverityBadge } from '../shared/SeverityBadge'
import { SourceBadge } from '../shared/SourceBadge'
import { StatusBadge } from '../shared/StatusBadge'
import styles from './IncidentDetailPage.module.css'

export function IncidentHeader({ incident }: { incident: IncidentResponse }) {
  const { t } = useTranslation()
  return (
    <header className={styles.detailHeader}>
      <div className={styles.heading}>
        <div>
          <span>{incidentCode(incident.id)}</span>
          <StatusBadge value={incident.status} />
          <SourceBadge value={incident.source} />
        </div>
        <h1>{incident.title ?? '—'}</h1>
      </div>
      <div className={styles.scales}>
        <div>
          <span>{t('incidents.detail.severity')}</span>
          <SeverityBadge value={incident.severity} />
        </div>
        <div>
          <span>{t('incidents.detail.priority')}</span>
          <strong>{incident.priority ?? '—'}</strong>
        </div>
      </div>
    </header>
  )
}
