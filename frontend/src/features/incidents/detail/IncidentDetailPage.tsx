import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation, useParams } from 'react-router-dom'
import type {
  AffectedServiceResponse,
  BusinessServiceListItemResponse,
  BusinessServiceResponse,
  IncidentHistoryItemResponse,
  IncidentResponse,
} from '../../../api/generated/model'
import { ApiError } from '../../../api/http'
import { useAuth } from '../../../auth/useAuth'
import {
  getIncident,
  getIncidentHistory,
  getIncidentServiceContext,
} from '../api/incidentsApi'
import { ServiceTier } from '../shared/ServiceTier'
import { AffectedServicesPanel } from './AffectedServicesPanel'
import { IncidentActions } from './IncidentActions'
import { IncidentHeader } from './IncidentHeader'
import { IncidentStatusRoute } from './IncidentStatusRoute'
import { IncidentSummary } from './IncidentSummary'
import { IncidentTimeline } from './IncidentTimeline'
import styles from './IncidentDetailPage.module.css'

interface DetailData {
  affected: AffectedServiceResponse[]
  catalog: BusinessServiceListItemResponse[]
  history: IncidentHistoryItemResponse[]
  incident: IncidentResponse
  service: BusinessServiceResponse
}

export function IncidentDetailPage() {
  const { t } = useTranslation()
  const { id } = useParams()
  const location = useLocation()
  const { user } = useAuth()
  const [data, setData] = useState<DetailData | null>(null)
  const [error, setError] = useState<string | null>(null)
  const incidentId = Number(id)
  const feedSearch =
    (location.state as { feedSearch?: string } | null)?.feedSearch ?? ''

  useEffect(() => {
    let active = true
    if (!Number.isInteger(incidentId)) return
    void getIncident(incidentId)
      .then(async (incident) => {
        if (typeof incident.affectedServiceId !== 'number')
          throw new Error('Incident response has no affected service')
        const [historyResponse, [service, affectedResponse, catalog]] =
          await Promise.all([
            getIncidentHistory(incidentId),
            getIncidentServiceContext(incident.affectedServiceId),
          ])
        if (active)
          setData({
            incident,
            service,
            affected: affectedResponse.items ?? [],
            catalog,
            history: historyResponse.items ?? [],
          })
      })
      .catch((reason: unknown) => {
        if (active)
          setError(
            reason instanceof ApiError
              ? (reason.problem?.detail ??
                  reason.problem?.title ??
                  reason.message)
              : t('incidents.errors.loadDetail'),
          )
      })
    return () => {
      active = false
    }
  }, [incidentId, t])

  async function handleUpdated(incident: IncidentResponse) {
    setData((current) => (current ? { ...current, incident } : current))
    if (typeof incident.id === 'number') {
      try {
        const response = await getIncidentHistory(incident.id)
        setData((current) =>
          current ? { ...current, history: response.items ?? [] } : current,
        )
      } catch {
        /* The updated card remains usable if history refresh fails. */
      }
    }
  }

  if (!Number.isInteger(incidentId))
    return (
      <div className={`${styles.state} ${styles.error}`} role="alert">
        <strong>{t('incidents.errors.title')}</strong>
        <span>{t('incidents.errors.notFound')}</span>
      </div>
    )
  if (error)
    return (
      <div className={`${styles.state} ${styles.error}`} role="alert">
        <strong>{t('incidents.errors.title')}</strong>
        <span>{error}</span>
      </div>
    )
  if (!data || data.incident.id !== incidentId || !user)
    return <div className={styles.state}>{t('common.loading')}</div>

  return (
    <article className={styles.page}>
      <div className={styles.backRow}>
        <Link to={{ pathname: '/incidents', search: feedSearch }}>
          {t('incidents.detail.back')}
        </Link>
      </div>
      <IncidentHeader incident={data.incident} />
      <div className={styles.detailGrid}>
        <main className={styles.mainColumn}>
          <section>
            <h2 className={styles.sectionLabel}>
              {t('incidents.detail.description')}
            </h2>
            <p className={styles.description}>
              {data.incident.description ?? t('incidents.detail.noDescription')}
            </p>
          </section>
          <section className={styles.servicePanel}>
            <h2 className={styles.sectionLabel}>
              {t('incidents.detail.affectedService')}
            </h2>
            <div>
              <ServiceTier tier={data.service.tier} />
              <strong>
                {data.incident.affectedServiceCode ?? data.service.code}
              </strong>
              <span>
                {data.incident.affectedServiceName ?? data.service.name}
              </span>
              <span>{data.service.ownerTeamName}</span>
              <Link to={`/services/${data.service.id}`}>
                {t('incidents.detail.openService')}
              </Link>
            </div>
          </section>
          <AffectedServicesPanel
            root={data.service}
            affected={data.affected}
            catalog={data.catalog}
          />
          <IncidentStatusRoute current={data.incident.status} />
          <IncidentTimeline items={data.history} />
        </main>
        <aside className={styles.sidebar}>
          <IncidentActions
            incident={data.incident}
            user={user}
            onUpdated={(incident) => void handleUpdated(incident)}
          />
          <IncidentSummary
            incident={data.incident}
            scope={data.affected.length}
          />
        </aside>
      </div>
    </article>
  )
}
