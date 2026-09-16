import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type {
  IncidentResponse,
  IncidentResponseAvailableActionsItem,
  UserListItemResponse,
} from '../../../api/generated/model'
import { ApiError } from '../../../api/http'
import type { CurrentUser } from '../../../auth/types'
import { executeIncidentAction, getAllUsers } from '../api/incidentsApi'
import styles from './IncidentDetailPage.module.css'

interface IncidentActionsProps {
  incident: IncidentResponse
  onUpdated: (incident: IncidentResponse) => void
  user: CurrentUser
}

export function IncidentActions({
  incident,
  onUpdated,
  user,
}: IncidentActionsProps) {
  const { t } = useTranslation()
  const actions = useMemo(
    () => incident.availableActions ?? [],
    [incident.availableActions],
  )
  const [users, setUsers] = useState<UserListItemResponse[]>([])
  const [assigneeId, setAssigneeId] = useState('')
  const [pending, setPending] =
    useState<IncidentResponseAvailableActionsItem | null>(null)
  const [problem, setProblem] = useState<{
    detail?: string
    title?: string
  } | null>(null)

  useEffect(() => {
    if (!actions.includes('ASSIGN')) return
    let active = true
    void getAllUsers()
      .then((items) => {
        if (active) setUsers(items)
      })
      .catch(() => undefined)
    return () => {
      active = false
    }
  }, [actions])

  async function run(action: IncidentResponseAvailableActionsItem) {
    if (typeof incident.id !== 'number') return
    setPending(action)
    setProblem(null)
    try {
      const updated = await executeIncidentAction(
        incident.id,
        action,
        action === 'ASSIGN' ? Number(assigneeId) : undefined,
      )
      onUpdated(updated)
    } catch (reason) {
      if (
        reason instanceof ApiError &&
        (reason.status === 403 || reason.status === 409)
      ) {
        setProblem({
          title: reason.problem?.title,
          detail: reason.problem?.detail,
        })
      } else {
        setProblem({
          title: t('incidents.errors.actionTitle'),
          detail: t('incidents.errors.action'),
        })
      }
    } finally {
      setPending(null)
    }
  }

  return (
    <section>
      <h2 className={styles.sectionLabel}>{t('incidents.actions.title')}</h2>
      {actions.length > 0 ? (
        <div className={styles.actionList}>
          {actions.map((action) =>
            action === 'ASSIGN' ? (
              <div className={styles.assignAction} key={action}>
                <select
                  aria-label={t('incidents.actions.assignee')}
                  value={assigneeId}
                  onChange={(event) => setAssigneeId(event.target.value)}
                >
                  <option value="">
                    {t('incidents.actions.chooseAssignee')}
                  </option>
                  {users
                    .filter((item) => typeof item.id === 'number')
                    .map((item) => (
                      <option key={item.id} value={item.id}>
                        {item.displayName ?? item.email}
                      </option>
                    ))}
                </select>
                <button
                  type="button"
                  disabled={!assigneeId || pending !== null}
                  onClick={() => void run(action)}
                >
                  {pending === action
                    ? t('incidents.actions.pending')
                    : t(`incidents.actions.labels.${action}`)}
                </button>
              </div>
            ) : (
              <button
                key={action}
                type="button"
                disabled={pending !== null}
                onClick={() => void run(action)}
              >
                {pending === action
                  ? t('incidents.actions.pending')
                  : t(`incidents.actions.labels.${action}`)}
              </button>
            ),
          )}
        </div>
      ) : (
        <div className={styles.noActions}>
          {user.role === 'REPORTER'
            ? t('incidents.actions.reporterReason')
            : incident.status === 'CLOSED' || incident.status === 'CANCELLED'
              ? t('incidents.actions.terminalReason')
              : t('incidents.actions.none')}
        </div>
      )}
      {problem && (
        <div className={styles.actionError} role="alert">
          <strong>{problem.title ?? t('incidents.errors.actionTitle')}</strong>
          {problem.detail && <span>{problem.detail}</span>}
        </div>
      )}
    </section>
  )
}
