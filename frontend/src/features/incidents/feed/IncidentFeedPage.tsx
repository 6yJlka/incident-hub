import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import type {
  BusinessServiceListItemResponse,
  ListIncidentsResponse,
  TeamListItemResponse,
} from '../../../api/generated/model'
import { ApiError } from '../../../api/http'
import {
  getAllServices,
  getAllTeams,
  getIncidentPage,
} from '../api/incidentsApi'
import type { IncidentFeedParams } from '../api/incidentsApi'
import { IncidentFilters } from './IncidentFilters'
import type { FeedFilterValues } from './IncidentFilters'
import { IncidentPagination } from './IncidentPagination'
import { IncidentTable } from './IncidentTable'
import styles from './IncidentFeedPage.module.css'

const PAGE_SIZE = 20

function positiveNumber(value: string | null): number | undefined {
  if (!value) return undefined
  const number = Number(value)
  return Number.isInteger(number) && number >= 0 ? number : undefined
}

export function IncidentFeedPage() {
  const { t } = useTranslation()
  const [searchParams, setSearchParams] = useSearchParams()
  const [page, setPage] = useState<ListIncidentsResponse | null>(null)
  const [services, setServices] = useState<BusinessServiceListItemResponse[]>(
    [],
  )
  const [teams, setTeams] = useState<TeamListItemResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const values: FeedFilterValues = {
    status: searchParams.get('status') ?? '',
    severity: searchParams.get('severity') ?? '',
    priority: searchParams.get('priority') ?? '',
    serviceId: searchParams.get('serviceId') ?? '',
    teamId: searchParams.get('teamId') ?? '',
  }
  const currentPage = positiveNumber(searchParams.get('page')) ?? 0
  const requestParams = useMemo<IncidentFeedParams>(
    () => ({
      page: currentPage,
      size: PAGE_SIZE,
      status: values.status
        ? (values.status as IncidentFeedParams['status'])
        : undefined,
      severity: values.severity
        ? (values.severity as IncidentFeedParams['severity'])
        : undefined,
      priority: values.priority
        ? (values.priority as IncidentFeedParams['priority'])
        : undefined,
      affectedServiceId: positiveNumber(values.serviceId),
      responsibleTeamId: positiveNumber(values.teamId),
    }),
    [
      currentPage,
      values.priority,
      values.serviceId,
      values.severity,
      values.status,
      values.teamId,
    ],
  )

  useEffect(() => {
    let active = true
    void Promise.all([getAllServices(), getAllTeams()])
      .then(([serviceItems, teamItems]) => {
        if (active) {
          setServices(serviceItems)
          setTeams(teamItems)
        }
      })
      .catch(() => undefined)
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    let active = true
    // Loading is request state, so it is reset whenever URL parameters change.
    // oxlint-disable-next-line react/set-state-in-effect
    setLoading(true)
    setError(null)
    void getIncidentPage(requestParams)
      .then((response) => {
        if (active) setPage(response)
      })
      .catch((reason: unknown) => {
        if (active)
          setError(
            reason instanceof ApiError
              ? (reason.problem?.detail ??
                  reason.problem?.title ??
                  reason.message)
              : t('incidents.errors.load'),
          )
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [requestParams, t])

  function updateFilter(name: keyof FeedFilterValues, value: string) {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      if (value) next.set(name, value)
      else next.delete(name)
      next.delete('page')
      return next
    })
  }
  function resetFilters() {
    setSearchParams(new URLSearchParams())
  }
  function changePage(nextPage: number) {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      if (nextPage > 0) next.set('page', String(nextPage))
      else next.delete('page')
      return next
    })
  }

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <div>
          <h1>{t('incidents.feed.title')}</h1>
          <p>
            {t('incidents.feed.summary', { count: page?.totalElements ?? 0 })}
          </p>
        </div>
        <IncidentFilters
          values={values}
          services={services}
          teams={teams}
          onChange={updateFilter}
          onReset={resetFilters}
        />
      </header>
      {loading && <div className={styles.state}>{t('common.loading')}</div>}
      {!loading && error && (
        <div className={`${styles.state} ${styles.error}`} role="alert">
          <strong>{t('incidents.errors.title')}</strong>
          <span>{error}</span>
        </div>
      )}
      {!loading && !error && page && (page.items?.length ?? 0) > 0 && (
        <>
          <IncidentTable items={page.items ?? []} services={services} />
          <IncidentPagination page={page} onPageChange={changePage} />
        </>
      )}
      {!loading && !error && page && (page.items?.length ?? 0) === 0 && (
        <div className={styles.empty}>
          <span className={styles.emptyMark}>0</span>
          <strong>{t('incidents.empty.title')}</strong>
          <p>{t('incidents.empty.description')}</p>
          <button type="button" onClick={resetFilters}>
            {t('incidents.empty.reset')}
          </button>
        </div>
      )}
    </section>
  )
}
