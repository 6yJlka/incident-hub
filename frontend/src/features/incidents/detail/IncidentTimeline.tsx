import { useTranslation } from 'react-i18next'
import type { IncidentHistoryItemResponse } from '../../../api/generated/model'
import { formatDateTime } from '../model/format'
import styles from './IncidentDetailPage.module.css'

export function IncidentTimeline({
  items,
}: {
  items: IncidentHistoryItemResponse[]
}) {
  const { i18n, t } = useTranslation()
  return (
    <section>
      <h2 className={styles.sectionLabel}>{t('incidents.detail.timeline')}</h2>
      <div className={styles.timeline}>
        {items.map((item) => (
          <div className={styles.timelineItem} key={item.id}>
            <span className={styles.timelineDot} />
            <p>
              <strong>
                {item.actorDisplayName ?? t('incidents.detail.systemActor')}
              </strong>
              <span>
                {item.eventType ?? '—'}
                {item.toStatus ? ` · ${item.toStatus}` : ''}
              </span>
            </p>
            <time>{formatDateTime(item.createdAt, i18n.language)}</time>
          </div>
        ))}
      </div>
    </section>
  )
}
