import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router-dom'
import { listAlertGroups } from '../../api/generated/alert-groups/alert-groups'
import type {
  BusinessServiceListItemResponse,
  ListAlertGroupsResponse,
  ListAlertGroupsStatus,
} from '../../api/generated/model'
import { Pagination } from '../../components/Pagination'
import { getAllServices } from '../incidents/api/incidentsApi'
import { formatGroupDuration, formatGroupTime } from './groupPresentation'
import { SignalSeverity } from './SignalSeverity'
import { ServiceCode } from './ServiceCode'
import { StatusBadge } from '../incidents/shared/StatusBadge'
import { FilterSelect } from '../incidents/feed/IncidentFilters'
import feedStyles from '../incidents/feed/IncidentFeedPage.module.css'
import styles from './AlertGroups.module.css'

export function AlertGroupFeedPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const [page, setPage] = useState<ListAlertGroupsResponse | null>(null)
  const [services, setServices] = useState<BusinessServiceListItemResponse[]>(
    [],
  )
  const [error, setError] = useState(false)
  const [loading, setLoading] = useState(true)
  const status = params.get('status') ?? ''
  const root = params.get('rootServiceId') ?? ''
  const pageNumber = Math.max(0, Number(params.get('page')) || 0)
  useEffect(() => {
    let active = true
    void getAllServices()
      .then((result) => {
        if (active) setServices(result)
      })
      .catch(() => undefined)
    return () => {
      active = false
    }
  }, [])
  useEffect(() => {
    let active = true
    // Loading belongs to the current URL request.
    // oxlint-disable-next-line react/set-state-in-effect
    setLoading(true)
    setError(false)
    void listAlertGroups({
      page: pageNumber,
      size: 20,
      status: (status || undefined) as ListAlertGroupsStatus | undefined,
      rootServiceId: root ? Number(root) : undefined,
    })
      .then((result) => {
        if (active) setPage(result)
      })
      .catch(() => {
        if (active) setError(true)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [status, root, pageNumber])
  function filter(key: string, value: string) {
    setParams((current) => {
      const next = new URLSearchParams(current)
      next.delete('page')
      if (value) next.set(key, value)
      else next.delete(key)
      return next
    })
  }
  return (
    <article className={`${styles.page} ${styles.feedPage}`}>
      <header className={styles.header}>
        <h1>{t('groups.title')}</h1>
        <p>{t('groups.subtitle')}</p>
        <div className={`${feedStyles.filters} ${styles.feedFilters}`}>
          <FilterSelect
            label={t('groups.status')}
            value={status}
            options={['OPEN', 'CONFIRMED', 'DISMISSED']}
            onChange={(value) => filter('status', value)}
          />
          <FilterSelect
            label={t('groups.root')}
            value={root}
            options={services.map((service) => ({
              value: String(service.id),
              label: service.code ?? '',
            }))}
            onChange={(value) => filter('rootServiceId', value)}
          />
          {(status || root) && (
            <button className={feedStyles.reset} onClick={() => setParams({})}>
              {t('groups.reset')}
            </button>
          )}
        </div>
      </header>
      {loading ? (
        <p>{t('common.loading')}</p>
      ) : error ? (
        <p role="alert">{t('groups.loadError')}</p>
      ) : (
        <>
          <div className={styles.tableWrap}>
            <table className={`${styles.table} ${styles.groupFeedTable}`}>
              <thead>
                <tr>
                  {[
                    'id',
                    'status',
                    'probableRoot',
                    'alerts',
                    'servicesInGroup',
                    'maxSignal',
                    'first',
                    'last',
                    'shortDuration',
                    'incident',
                  ].map((key) => (
                    <th key={key}>{t(`groups.${key}`)}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {page?.items?.map((group) => (
                  <tr key={group.id}>
                    <td>
                      <Link to={`/alert-groups/${group.id}`}>
                        GRP-{group.id}
                      </Link>
                    </td>
                    <td>
                      <StatusBadge value={group.status} />
                    </td>
                    <td className={styles.code}>
                      {group.rootAssessmentStatus === 'UNDETERMINED' ? (
                        <span>{t('groups.rootUndetermined')}</span>
                      ) : (
                        <ServiceCode
                          code={group.probableRootServiceCode}
                          tier={
                            services.find(
                              (service) =>
                                service.id === group.probableRootServiceId,
                            )?.tier
                          }
                        />
                      )}
                    </td>
                    <td>{group.alertCount}</td>
                    <td className={styles.code}>
                      <span className={styles.serviceCount}>
                        {group.serviceCount}
                      </span>
                      {group.servicePreviewCodes?.join(', ')}
                      {(group.serviceCount ?? 0) >
                        (group.servicePreviewCodes?.length ?? 0) &&
                        ` +${(group.serviceCount ?? 0) - (group.servicePreviewCodes?.length ?? 0)}`}
                    </td>
                    <td>
                      <SignalSeverity value={group.worstSeverity} />
                    </td>
                    <td>{formatGroupTime(group.firstAlertAt)}</td>
                    <td>{formatGroupTime(group.lastAlertAt)}</td>
                    <td>
                      {formatGroupDuration(
                        group.firstAlertAt,
                        group.lastAlertAt,
                        t,
                      )}
                    </td>
                    <td>
                      {group.incidentId ? (
                        <Link to={`/incidents/${group.incidentId}`}>
                          INC-{group.incidentId}
                        </Link>
                      ) : (
                        '—'
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!page?.items?.length && (
            <div className={styles.empty}>
              <p>{t('groups.empty')}</p>
              <button onClick={() => setParams({})}>{t('groups.reset')}</button>
            </div>
          )}
          {page && (
            <Pagination
              page={page}
              onPageChange={(value) =>
                setParams((current) => {
                  const next = new URLSearchParams(current)
                  next.set('page', String(value))
                  return next
                })
              }
            />
          )}
        </>
      )}
    </article>
  )
}
