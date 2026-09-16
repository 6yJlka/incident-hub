import { useTranslation } from 'react-i18next'
import type {
  AffectedServiceResponse,
  BusinessServiceResponse,
} from '../../../../api/generated/model'
import { ServiceTier } from '../../shared/ServiceTier'
import styles from '../IncidentDetailPage.module.css'

export function CascadeTree({
  items,
  root,
}: {
  items: AffectedServiceResponse[]
  root: BusinessServiceResponse
}) {
  const { t } = useTranslation()
  return (
    <div className={styles.tree}>
      <div className={styles.treeRoot}>
        <span className={styles.failureDot} />
        <strong>{root.code ?? '—'}</strong>
        <span>{t('incidents.cascade.failureSource')}</span>
      </div>
      {[...items]
        .sort((a, b) => (a.depth ?? 0) - (b.depth ?? 0))
        .map((item) => (
          <div
            key={item.id}
            className={styles.treeRow}
            style={{
              paddingInlineStart: `calc(${Math.max((item.depth ?? 1) - 1, 0)} * var(--cascade-indent))`,
            }}
          >
            <span className={styles.branch} aria-hidden="true">
              └
            </span>
            <strong>{item.code ?? '—'}</strong>
            <span className={styles.grow}>{item.name ?? '—'}</span>
            <span className={styles.dependencyType}>
              {item.dependencyType ?? '—'}
            </span>
            <ServiceTier tier={item.tier} />
            <span className={styles.depth}>D{item.depth ?? '—'}</span>
          </div>
        ))}
    </div>
  )
}
