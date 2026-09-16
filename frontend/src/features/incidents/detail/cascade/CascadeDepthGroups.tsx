import { useTranslation } from 'react-i18next'
import type { AffectedServiceResponse } from '../../../../api/generated/model'
import { ServiceTier } from '../../shared/ServiceTier'
import styles from '../IncidentDetailPage.module.css'

export function CascadeDepthGroups({
  items,
}: {
  items: AffectedServiceResponse[]
}) {
  const { t } = useTranslation()
  const depths = [
    ...new Set(
      items
        .map((item) => item.depth)
        .filter((depth): depth is number => typeof depth === 'number'),
    ),
  ].sort((a, b) => a - b)
  return (
    <div className={styles.depthGrid}>
      {depths.map((depth) => {
        const services = items.filter((item) => item.depth === depth)
        return (
          <section className={styles.depthGroup} key={depth}>
            <header>
              <strong>{t('incidents.cascade.depth', { depth })}</strong>
              <span>
                {t('incidents.cascade.serviceCount', {
                  count: services.length,
                })}
              </span>
            </header>
            <div className={styles.depthBar} />
            {services.map((service) => (
              <div className={styles.depthCard} key={service.id}>
                <div>
                  <ServiceTier tier={service.tier} />
                  <strong>{service.code ?? '—'}</strong>
                </div>
                <span>{service.name ?? '—'}</span>
                <span className={styles.dependencyType}>
                  {service.dependencyType ?? '—'}
                </span>
              </div>
            ))}
          </section>
        )
      })}
    </div>
  )
}
