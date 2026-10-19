import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import {
  confirmAlertGroup,
  dismissAlertGroup,
  getAlertGroup,
} from '../../api/generated/alert-groups/alert-groups'
import {
  getAffectedServices,
  getService,
} from '../../api/generated/service-catalog/service-catalog'
import type {
  AlertGroupDetailResponse,
  AffectedServiceResponse,
  BusinessServiceListItemResponse,
  BusinessServiceResponse,
  ConfirmAlertGroupRequest,
} from '../../api/generated/model'
import { ApiError } from '../../api/http'
import { getAllServices } from '../incidents/api/incidentsApi'
import { GroupRootPanel } from './GroupRootPanel'
import { GroupDecisionDialog } from './GroupDecisionDialog'
import {
  formatGroupDuration,
  formatGroupTime,
  groupServices,
  maximumSignal,
  reachedServiceIds,
} from './groupPresentation'
import { SignalSeverity } from './SignalSeverity'
import { ServiceCode } from './ServiceCode'
import { StatusBadge } from '../incidents/shared/StatusBadge'
import { notifyGroupsChanged } from './groupEvents'
import styles from './AlertGroups.module.css'

export function AlertGroupDetailPage() {
  const { t } = useTranslation()
  const { id } = useParams()
  const groupId = Number(id)
  const [group, setGroup] = useState<AlertGroupDetailResponse | null>(null)
  const [affected, setAffected] = useState<AffectedServiceResponse[] | null>(
    null,
  )
  const [root, setRoot] = useState<BusinessServiceResponse | null>(null)
  const [catalog, setCatalog] = useState<BusinessServiceListItemResponse[]>([])
  const [catalogFailed, setCatalogFailed] = useState(false)
  const [graphFailed, setGraphFailed] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [revision, setRevision] = useState(0)
  const [stale, setStale] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [modal, setModal] = useState<'confirm' | 'dismiss' | null>(null)
  const [busy, setBusy] = useState(false)
  const [decisionError, setDecisionError] = useState<string | null>(null)
  useEffect(() => {
    let active = true
    // Card data is explicitly refreshed after a conflict or a user action.
    // oxlint-disable-next-line react/set-state-in-effect
    setLoading(true)
    setLoadError(false)
    setAffected(null)
    setGraphFailed(false)
    setRoot(null)
    void getAlertGroup(groupId)
      .then((result) => {
        if (!active) return
        setGroup(result)
        setStale(false)
        void getService(result.probableRootServiceId!)
          .then((value) => {
            if (active) setRoot(value)
          })
          .catch(() => undefined)
        void getAffectedServices(result.probableRootServiceId!, {
          maxDepth: 10,
        })
          .then((value) => {
            if (active) setAffected(value.items ?? [])
          })
          .catch(() => {
            if (active) setGraphFailed(true)
          })
      })
      .catch(() => {
        if (active) setLoadError(true)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    void getAllServices()
      .then((value) => {
        if (active) {
          setCatalog(value)
          setCatalogFailed(false)
        }
      })
      .catch(() => {
        if (active) setCatalogFailed(true)
      })
    return () => {
      active = false
    }
  }, [groupId, revision])
  async function decide(operation: () => Promise<AlertGroupDetailResponse>) {
    if (busy || stale || loading) return
    setBusy(true)
    setDecisionError(null)
    try {
      const result = await operation()
      setGroup(result)
      setModal(null)
      setNotice(null)
      notifyGroupsChanged()
    } catch (error) {
      if (
        error instanceof ApiError &&
        error.status === 409 &&
        error.problem?.type ===
          'urn:incident-hub:problem:concurrent-modification'
      ) {
        setStale(true)
        setModal(null)
        setNotice(null)
      } else if (
        error instanceof ApiError &&
        error.status === 409 &&
        error.problem?.type === 'urn:incident-hub:problem:conflict'
      ) {
        setModal(null)
        setNotice(t('groups.unavailable'))
        setStale(true)
        setRevision((value) => value + 1)
        notifyGroupsChanged()
      } else setDecisionError(t('groups.decisionError'))
    } finally {
      setBusy(false)
    }
  }
  function open(kind: 'confirm' | 'dismiss') {
    setDecisionError(null)
    setModal(kind)
  }
  const blocked = stale || busy || loading || group?.version === undefined
  const reached =
    group && affected !== null
      ? reachedServiceIds(group.probableRootServiceId!, affected)
      : null
  if (loadError && group?.id !== groupId)
    return (
      <p role="alert">
        {t('groups.loadError')}{' '}
        <button onClick={() => setRevision((value) => value + 1)}>
          {t('groups.refresh')}
        </button>
      </p>
    )
  if (!group || group.id !== groupId) return <p>{t('common.loading')}</p>
  return (
    <article className={styles.page}>
      <Link to="/alert-groups">← {t('groups.title')}</Link>
      <header className={`${styles.header} ${styles.groupHeader}`}>
        <div>
          <div className={styles.groupHeading}>
            <span>{t('groups.suspectedGroup')}</span>
            <StatusBadge value={group.status} />
          </div>
          <h1>GRP-{group.id}</h1>
          <p>
            {formatGroupTime(group.firstAlertAt)} —{' '}
            {formatGroupTime(group.lastAlertAt)}
          </p>
        </div>
        <div className={styles.headerFacts}>
          <div>
            <span>{t('groups.probableRoot')}</span>
            <ServiceCode
              code={group.probableRootServiceCode}
              tier={root?.tier}
            />
            <small>{root?.tier}</small>
          </div>
          <div>
            <span>{t('groups.maxSignal')}</span>
            <SignalSeverity value={maximumSignal(group.alerts)} />
          </div>
        </div>
      </header>
      {(stale || notice || loadError) && (
        <div role="alert" className={styles.banner}>
          <strong>
            {stale ? t('groups.changed') : (notice ?? t('groups.loadError'))}
          </strong>
          <button
            disabled={loading}
            onClick={() => {
              setNotice(null)
              setRevision((value) => value + 1)
            }}
          >
            {t('groups.refresh')}
          </button>
        </div>
      )}
      <div className={styles.detailGrid}>
        <main className={styles.mainColumn}>
          <GroupRootPanel
            group={group}
            affected={affected}
            root={root}
            failed={graphFailed}
          />
          <section className={styles.panel}>
            <h2>{t('groups.alerts')}</h2>
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    {[
                      'occurred',
                      'service',
                      'signal',
                      'signalSeverity',
                      'source',
                      'description',
                    ].map((key) => (
                      <th key={key}>{t(`groups.${key}`)}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {group.alerts?.map((alert) => (
                    <tr
                      key={alert.id}
                      className={
                        reached && !reached.has(alert.businessServiceId!)
                          ? styles.offPath
                          : undefined
                      }
                    >
                      <td>{formatGroupTime(alert.occurredAt)}</td>
                      <td>
                        <Link to={`/services/${alert.businessServiceId}`}>
                          {alert.businessServiceCode}
                        </Link>
                        {reached && !reached.has(alert.businessServiceId!) && (
                          <small>{t('groups.offPath')}</small>
                        )}
                      </td>
                      <td>{alert.signalType}</td>
                      <td>
                        <SignalSeverity value={alert.severity} />
                      </td>
                      <td>
                        {alert.source}
                        <small>{alert.externalId}</small>
                      </td>
                      <td>{alert.description}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </main>
        <aside className={`${styles.panel} ${styles.groupSidebar}`}>
          <h2>{t('groups.decision')}</h2>
          {(group.availableActions?.length ?? 0) > 0 ? (
            <>
              <p>{t('groups.decisionHint')}</p>
              <div className={styles.actions}>
                {group.availableActions?.includes('CONFIRM') && (
                  <button
                    className={styles.primary}
                    disabled={blocked || catalogFailed || catalog.length === 0}
                    onClick={() => open('confirm')}
                  >
                    {t('groups.confirm')}
                  </button>
                )}
                {group.availableActions?.includes('DISMISS') && (
                  <button disabled={blocked} onClick={() => open('dismiss')}>
                    {t('groups.dismiss')}
                  </button>
                )}
              </div>
              {catalogFailed && (
                <div role="alert">
                  <p>{t('groups.catalogError')}</p>
                  <button
                    disabled={loading}
                    onClick={() => setRevision((value) => value + 1)}
                  >
                    {t('groups.refresh')}
                  </button>
                </div>
              )}
            </>
          ) : group.status === 'OPEN' ? (
            <p>{t('groups.awaitingEngineer')}</p>
          ) : (
            <dl className={styles.decisionResult}>
              <dt>
                {t(
                  group.status === 'CONFIRMED'
                    ? 'groups.confirmedBy'
                    : 'groups.dismissedBy',
                )}
              </dt>
              <dd>{group.decidedByDisplayName ?? '—'}</dd>
              <dt>{t('groups.decidedAt')}</dt>
              <dd>{formatGroupTime(group.decidedAt)}</dd>
              {group.status === 'DISMISSED' && (
                <>
                  <dt>{t('groups.dismissReason')}</dt>
                  <dd>{group.dismissReason || t('groups.noReason')}</dd>
                </>
              )}
              {group.incidentId && (
                <>
                  <dt>{t('groups.createdIncident')}</dt>
                  <dd>
                    <Link to={`/incidents/${group.incidentId}`}>
                      INC-{group.incidentId} →
                    </Link>
                  </dd>
                </>
              )}
            </dl>
          )}
          <dl className={styles.groupStats}>
            <div>
              <dt>{t('groups.alerts')}</dt>
              <dd>{group.alerts?.length ?? 0}</dd>
            </div>
            <div>
              <dt>{t('groups.servicesInGroup')}</dt>
              <dd>{groupServices(group).length}</dd>
            </div>
            <div>
              <dt>{t('groups.onPath')}</dt>
              <dd>
                {reached
                  ? groupServices(group).filter((service) =>
                      reached.has(service.id!),
                    ).length
                  : '—'}
              </dd>
            </div>
            <div>
              <dt>{t('groups.offPath')}</dt>
              <dd>
                {reached
                  ? groupServices(group).filter(
                      (service) => !reached.has(service.id!),
                    ).length
                  : '—'}
              </dd>
            </div>
            <div>
              <dt>{t('groups.duration')}</dt>
              <dd>
                {formatGroupDuration(group.firstAlertAt, group.lastAlertAt, t)}
              </dd>
            </div>
          </dl>
          <p className={styles.hint}>{t('groups.signalScaleHint')}</p>
        </aside>
      </div>
      {modal && !stale && !loading && (
        <GroupDecisionDialog
          kind={modal}
          group={group}
          catalog={catalog}
          busy={busy}
          error={decisionError}
          onClose={() => setModal(null)}
          onConfirm={(body: ConfirmAlertGroupRequest) =>
            void decide(() => confirmAlertGroup(groupId, body))
          }
          onDismiss={(reason) =>
            void decide(() =>
              dismissAlertGroup(groupId, {
                expectedVersion: group.version!,
                reason,
              }),
            )
          }
        />
      )}
    </article>
  )
}
