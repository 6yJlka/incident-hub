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
import { formatGroupTime } from './groupPresentation'
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
    <article className={styles.page}>
      <header className={styles.header}>
        <h1>{t('groups.title')}</h1>
        <p>{t('groups.subtitle')}</p>
      </header>
      <div className={styles.filters}>
        <label>
          {t('groups.status')}
          <select
            value={status}
            onChange={(event) => filter('status', event.target.value)}
          >
            <option value="">{t('groups.all')}</option>
            {['OPEN', 'CONFIRMED', 'DISMISSED'].map((value) => (
              <option key={value}>{value}</option>
            ))}
          </select>
        </label>
        <label>
          {t('groups.root')}
          <select
            value={root}
            onChange={(event) => filter('rootServiceId', event.target.value)}
          >
            <option value="">{t('groups.all')}</option>
            {services.map((service) => (
              <option key={service.id} value={service.id}>
                {service.code}
              </option>
            ))}
          </select>
        </label>
        <button onClick={() => setParams({})}>{t('groups.reset')}</button>
      </div>
      {loading ? (
        <p>{t('common.loading')}</p>
      ) : error ? (
        <p role="alert">{t('groups.loadError')}</p>
      ) : (
        <>
          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  {[
                    'group',
                    'status',
                    'root',
                    'services',
                    'alerts',
                    'signalSeverity',
                    'lastAlert',
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
                    <td>{group.status}</td>
                    <td className={styles.code}>
                      {group.probableRootServiceCode}
                    </td>
                    <td className={styles.code}>
                      {group.servicePreviewCodes?.join(', ')}
                      {(group.serviceCount ?? 0) >
                        (group.servicePreviewCodes?.length ?? 0) &&
                        ` +${(group.serviceCount ?? 0) - (group.servicePreviewCodes?.length ?? 0)}`}
                    </td>
                    <td>{group.alertCount}</td>
                    <td>
                      <span className={styles[group.worstSeverity ?? 'INFO']}>
                        {group.worstSeverity ?? '—'}
                      </span>
                    </td>
                    <td>{formatGroupTime(group.lastAlertAt)}</td>
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
            <p className={styles.empty}>{t('groups.empty')}</p>
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
