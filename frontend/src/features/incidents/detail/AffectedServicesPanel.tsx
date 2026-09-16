import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type {
  AffectedServiceResponse,
  BusinessServiceListItemResponse,
  BusinessServiceResponse,
} from '../../../api/generated/model'
import { CascadeDepthGroups } from './cascade/CascadeDepthGroups'
import { CascadeTree } from './cascade/CascadeTree'
import { CatalogImpactMap } from './cascade/CatalogImpactMap'
import styles from './IncidentDetailPage.module.css'

type CascadeView = 'tree' | 'depth' | 'map'

interface AffectedServicesPanelProps {
  affected: AffectedServiceResponse[]
  catalog: BusinessServiceListItemResponse[]
  root: BusinessServiceResponse
}

export function AffectedServicesPanel({
  affected,
  catalog,
  root,
}: AffectedServicesPanelProps) {
  const { t } = useTranslation()
  const [view, setView] = useState<CascadeView>('tree')
  const maxDepth = Math.max(0, ...affected.map((item) => item.depth ?? 0))
  const tierOne = affected.filter((item) => item.tier === 'TIER_1').length
  const tabs: CascadeView[] = ['tree', 'depth', 'map']

  return (
    <section className={styles.cascadePanel}>
      <header className={styles.cascadeHeader}>
        <div>
          <h2>{t('incidents.cascade.title')}</h2>
          <p>
            {t('incidents.cascade.summary', {
              count: affected.length,
              depth: maxDepth,
              tierOne,
            })}
          </p>
        </div>
        <div
          className={styles.tabs}
          role="tablist"
          aria-label={t('incidents.cascade.views')}
        >
          {tabs.map((tab) => (
            <button
              key={tab}
              type="button"
              role="tab"
              aria-selected={view === tab}
              onClick={() => setView(tab)}
            >
              {t(`incidents.cascade.tabs.${tab}`)}
            </button>
          ))}
        </div>
      </header>
      {affected.length === 0 ? (
        <div className={styles.cascadeEmpty}>
          {t('incidents.cascade.empty')}
        </div>
      ) : (
        <>
          {view === 'tree' && <CascadeTree root={root} items={affected} />}
          {view === 'depth' && <CascadeDepthGroups items={affected} />}
          {view === 'map' && (
            <CatalogImpactMap
              root={root}
              affected={affected}
              catalog={catalog}
            />
          )}
        </>
      )}
    </section>
  )
}
