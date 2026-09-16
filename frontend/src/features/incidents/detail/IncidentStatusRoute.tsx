import { useTranslation } from 'react-i18next'
import { IncidentResponseStatus } from '../../../api/generated/model'
import styles from './IncidentDetailPage.module.css'

export function IncidentStatusRoute({ current }: { current?: string }) {
  const { t } = useTranslation()
  return (
    <section>
      <h2 className={styles.sectionLabel}>
        {t('incidents.detail.statusRoute')}
      </h2>
      <div className={styles.statusRoute}>
        {Object.values(IncidentResponseStatus).map((status) => (
          <div
            key={status}
            className={status === current ? styles.currentStatus : ''}
          >
            <span />
            {status}
          </div>
        ))}
      </div>
    </section>
  )
}
