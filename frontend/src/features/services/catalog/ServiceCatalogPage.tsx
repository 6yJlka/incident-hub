import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../../../api/http'
import { ServiceTier } from '../../incidents/shared/ServiceTier'
import { getServiceCatalog, type ServiceCatalogItem } from '../api/servicesApi'
import styles from './ServiceCatalogPage.module.css'

export function ServiceCatalogPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [items, setItems] = useState<ServiceCatalogItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    void getServiceCatalog()
      .then((response) => {
        if (active) setItems(response)
      })
      .catch((reason: unknown) => {
        if (!active) return
        setError(
          reason instanceof ApiError
            ? (reason.problem?.detail ??
                reason.problem?.title ??
                reason.message)
            : t('services.errors.loadCatalog'),
        )
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [t])

  function openService(id?: number) {
    if (typeof id === 'number') void navigate(`/services/${id}`)
  }

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <h1>{t('services.catalog.title')}</h1>
        <p>{t('services.catalog.summary', { count: items.length })}</p>
      </header>

      {loading && <div className={styles.state}>{t('common.loading')}</div>}
      {!loading && error && (
        <div className={`${styles.state} ${styles.error}`} role="alert">
          <strong>{t('services.errors.title')}</strong>
          <span>{error}</span>
        </div>
      )}
      {!loading && !error && (
        <div className={styles.tableScroller}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>{t('services.catalog.columns.code')}</th>
                <th>{t('services.catalog.columns.name')}</th>
                <th>{t('services.catalog.columns.tier')}</th>
                <th>{t('services.catalog.columns.owner')}</th>
                <th>{t('services.catalog.columns.dependencies')}</th>
                <th>{t('services.catalog.columns.activeIncidents')}</th>
              </tr>
            </thead>
            <tbody>
              {items.map(({ service, ...counts }) => (
                <tr
                  key={service.id ?? service.code}
                  tabIndex={0}
                  aria-label={t('services.catalog.open', {
                    code: service.code,
                  })}
                  onClick={() => openService(service.id)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ')
                      openService(service.id)
                  }}
                >
                  <td>
                    <div className={styles.codeCell}>
                      <ServiceTier tier={service.tier} />
                      <strong>{service.code ?? '—'}</strong>
                    </div>
                  </td>
                  <td>{service.name ?? '—'}</td>
                  <td className={styles.tier}>{service.tier ?? '—'}</td>
                  <td>
                    <span className={styles.ownerName}>
                      {service.ownerTeamName ?? '—'}
                    </span>
                    <span className={styles.ownerCode}>
                      {service.ownerTeamCode ?? '—'}
                    </span>
                  </td>
                  <td
                    className={styles.counts}
                    title={t('services.catalog.dependenciesHint')}
                  >
                    <span>↑{counts.dependencyCount}</span>
                    <span>↓{counts.affectedCount}</span>
                  </td>
                  <td
                    className={
                      counts.activeIncidents > 0
                        ? styles.activeCount
                        : styles.zeroCount
                    }
                  >
                    {counts.activeIncidents}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
