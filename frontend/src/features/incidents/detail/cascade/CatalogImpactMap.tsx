import { useTranslation } from 'react-i18next'
import type {
  AffectedServiceResponse,
  BusinessServiceListItemResponse,
  BusinessServiceResponse,
} from '../../../../api/generated/model'
import styles from '../IncidentDetailPage.module.css'

interface CatalogImpactMapProps {
  affected: AffectedServiceResponse[]
  catalog: BusinessServiceListItemResponse[]
  root: BusinessServiceResponse
}

export function CatalogImpactMap({
  affected,
  catalog,
  root,
}: CatalogImpactMapProps) {
  const { t } = useTranslation()
  const affectedById = new Map(affected.map((item) => [item.id, item]))
  const allServices = catalog.some((item) => item.id === root.id)
    ? catalog
    : [root, ...catalog]
  return (
    <div className={styles.mapWrap}>
      <div className={styles.catalogMap}>
        {allServices.map((service) => {
          const impact = affectedById.get(service.id)
          const isRoot = service.id === root.id
          return (
            <div
              key={service.id}
              className={`${styles.mapCell} ${isRoot ? styles.mapRoot : ''} ${impact ? styles.mapAffected : ''} ${!isRoot && !impact ? styles.mapMuted : ''}`}
            >
              <div>
                <strong>{service.code ?? '—'}</strong>
                <span>
                  {isRoot
                    ? t('incidents.cascade.source')
                    : impact
                      ? `D${impact.depth ?? '—'}`
                      : '—'}
                </span>
              </div>
              <p>{service.name ?? '—'}</p>
              {impact && <small>{impact.dependencyType}</small>}
            </div>
          )
        })}
      </div>
      <p className={styles.mapLegend}>{t('incidents.cascade.mapLegend')}</p>
    </div>
  )
}
