import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../../../api/http'
import { incidentCode } from '../../incidents/model/format'
import { ServiceTier } from '../../incidents/shared/ServiceTier'
import { SeverityBadge } from '../../incidents/shared/SeverityBadge'
import { StatusBadge } from '../../incidents/shared/StatusBadge'
import { getServiceDetail, type ServiceDetailData } from '../api/servicesApi'
import styles from './ServiceDetailPage.module.css'

export function ServiceDetailPage() {
  const { t } = useTranslation()
  const { id } = useParams()
  const serviceId = Number(id)
  const [data, setData] = useState<ServiceDetailData | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    if (!Number.isInteger(serviceId)) return

    void getServiceDetail(serviceId)
      .then((response) => {
        if (active) setData(response)
      })
      .catch((reason: unknown) => {
        if (!active) return
        setError(
          reason instanceof ApiError
            ? (reason.problem?.detail ??
                reason.problem?.title ??
                reason.message)
            : t('services.errors.loadDetail'),
        )
      })

    return () => {
      active = false
    }
  }, [serviceId, t])

  if (!Number.isInteger(serviceId)) {
    return (
      <div className={`${styles.state} ${styles.error}`} role="alert">
        <strong>{t('services.errors.title')}</strong>
        <span>{t('services.errors.notFound')}</span>
      </div>
    )
  }

  if (error) {
    return (
      <div className={`${styles.state} ${styles.error}`} role="alert">
        <strong>{t('services.errors.title')}</strong>
        <span>{error}</span>
      </div>
    )
  }

  if (!data || data.service.id !== serviceId) {
    return <div className={styles.state}>{t('common.loading')}</div>
  }

  const { service, affected, incidents } = data

  return (
    <article className={styles.page}>
      <header className={styles.header}>
        <Link to="/services">{t('services.detail.back')}</Link>
        <div className={styles.heading}>
          <ServiceTier tier={service.tier} />
          <h1>{service.code ?? '—'}</h1>
          <span>{service.name ?? '—'}</span>
          <strong>{service.tier ?? '—'}</strong>
          <small>
            {t('services.detail.owner', {
              owner: service.ownerTeamName ?? '—',
            })}
          </small>
        </div>
        {service.description && (
          <p className={styles.description}>{service.description}</p>
        )}
      </header>

      <div className={styles.grid}>
        <section className={styles.panel}>
          <h2>{t('services.detail.dependsOn.title')}</h2>
          <p>
            {t('services.detail.dependsOn.description', {
              code: service.code,
            })}
          </p>
          {(service.dependencies?.length ?? 0) > 0 ? (
            <div className={styles.rows}>
              {service.dependencies?.map((dependency) => (
                <Link
                  key={dependency.relationshipId ?? dependency.serviceId}
                  className={styles.serviceRow}
                  to={`/services/${dependency.serviceId}`}
                >
                  <ServiceTier tier={dependency.tier} />
                  <strong>{dependency.code ?? '—'}</strong>
                  <span>{dependency.name ?? '—'}</span>
                  <em className={styles[dependency.type?.toLowerCase() ?? '']}>
                    {dependency.type ?? '—'}
                  </em>
                </Link>
              ))}
            </div>
          ) : (
            <div className={styles.empty}>
              {t('services.detail.baseService')}
            </div>
          )}
        </section>

        <section className={styles.panel}>
          <h2>{t('services.detail.affected.title')}</h2>
          <p>
            {t('services.detail.affected.description', {
              code: service.code,
            })}
          </p>
          {affected.length > 0 ? (
            <div className={styles.rows}>
              {affected.map((item) => (
                <Link
                  key={item.id}
                  className={styles.serviceRow}
                  style={{
                    paddingLeft: `${Math.max(0, (item.depth ?? 1) - 1) * 20}px`,
                  }}
                  to={`/services/${item.id}`}
                >
                  <span className={styles.branch} aria-hidden="true">
                    {item.depth === 1 ? '├─' : '└─'}
                  </span>
                  <strong>{item.code ?? '—'}</strong>
                  <span>{item.name ?? '—'}</span>
                  <em
                    className={styles[item.dependencyType?.toLowerCase() ?? '']}
                  >
                    {item.dependencyType ?? '—'}
                  </em>
                  <small>D{item.depth ?? '—'}</small>
                </Link>
              ))}
            </div>
          ) : (
            <div className={styles.empty}>
              {t('services.detail.affected.empty')}
            </div>
          )}
        </section>

        <section className={styles.panel}>
          <h2>{t('services.detail.incidents.title')}</h2>
          {incidents.length > 0 ? (
            <div className={styles.rows}>
              {incidents.map((incident) => (
                <Link
                  key={incident.id}
                  className={styles.incidentRow}
                  to={`/incidents/${incident.id}`}
                >
                  <span className={styles.incidentId}>
                    {incidentCode(incident.id)}
                  </span>
                  <SeverityBadge value={incident.severity} />
                  <strong>{incident.title ?? '—'}</strong>
                  <StatusBadge value={incident.status} />
                </Link>
              ))}
            </div>
          ) : (
            <div className={styles.empty}>
              {t('services.detail.incidents.empty')}
            </div>
          )}
        </section>
      </div>
    </article>
  )
}
