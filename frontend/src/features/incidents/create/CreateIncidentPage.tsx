import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import type {
  AffectedServiceResponse,
  BusinessServiceListItemResponse,
  CreateIncidentRequestPriority,
  CreateIncidentRequestSeverity,
  TeamListItemResponse,
} from '../../../api/generated/model'
import { ApiError } from '../../../api/http'
import { useAuth } from '../../../auth/useAuth'
import {
  createIncident,
  getCreateIncidentReferences,
  getIncidentImpactPreview,
} from './createIncidentApi'
import styles from './CreateIncidentPage.module.css'

const SEVERITIES: CreateIncidentRequestSeverity[] = [
  'SEV1',
  'SEV2',
  'SEV3',
  'SEV4',
]
const PRIORITIES: CreateIncidentRequestPriority[] = [
  'CRITICAL',
  'HIGH',
  'MEDIUM',
  'LOW',
]

type FieldName =
  | 'title'
  | 'description'
  | 'affectedServiceId'
  | 'severity'
  | 'priority'
  | 'responsibleTeamId'

type FieldErrors = Partial<Record<FieldName, string>>

export function CreateIncidentPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [services, setServices] = useState<BusinessServiceListItemResponse[]>(
    [],
  )
  const [teams, setTeams] = useState<TeamListItemResponse[]>([])
  const [referencesLoading, setReferencesLoading] = useState(true)
  const [referencesError, setReferencesError] = useState<string | null>(null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [serviceId, setServiceId] = useState('')
  const [severity, setSeverity] =
    useState<CreateIncidentRequestSeverity>('SEV3')
  const [priority, setPriority] =
    useState<CreateIncidentRequestPriority>('MEDIUM')
  const [teamId, setTeamId] = useState('')
  const [impact, setImpact] = useState<AffectedServiceResponse[] | null>(null)
  const [impactLoading, setImpactLoading] = useState(false)
  const [impactError, setImpactError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    let active = true
    void getCreateIncidentReferences()
      .then(([serviceItems, teamItems]) => {
        if (!active) return
        setServices(serviceItems.filter((item) => item.active !== false))
        setTeams(teamItems.filter((item) => item.active !== false))
      })
      .catch(() => {
        if (active) setReferencesError(t('createIncident.errors.references'))
      })
      .finally(() => {
        if (active) setReferencesLoading(false)
      })
    return () => {
      active = false
    }
  }, [t])

  useEffect(() => {
    let active = true
    const numericServiceId = Number(serviceId)

    if (!Number.isInteger(numericServiceId) || numericServiceId <= 0) {
      return
    }

    void getIncidentImpactPreview(numericServiceId)
      .then((response) => {
        if (active) setImpact(response.items ?? [])
      })
      .catch(() => {
        if (active) setImpactError(t('createIncident.errors.preview'))
      })
      .finally(() => {
        if (active) setImpactLoading(false)
      })

    return () => {
      active = false
    }
  }, [serviceId, t])

  const selectedService = services.find(
    (service) => service.id === Number(serviceId),
  )

  const impactGroups = useMemo(() => {
    const groups = new Map<number, string[]>()
    for (const item of impact ?? []) {
      const depth = item.depth ?? 0
      groups.set(depth, [...(groups.get(depth) ?? []), item.code ?? '—'])
    }
    return [...groups.entries()].sort(([left], [right]) => left - right)
  }, [impact])

  function clearFieldError(field: FieldName) {
    setFieldErrors((current) => {
      if (!current[field]) return current
      const next = { ...current }
      delete next[field]
      return next
    })
  }

  function validate(): FieldErrors {
    const errors: FieldErrors = {}
    if (!title.trim())
      errors.title = t('createIncident.validation.titleRequired')
    else if (title.length > 255)
      errors.title = t('createIncident.validation.titleTooLong')
    if (!description.trim())
      errors.description = t('createIncident.validation.descriptionRequired')
    if (!Number.isInteger(Number(serviceId)) || Number(serviceId) <= 0)
      errors.affectedServiceId = t('createIncident.validation.serviceRequired')
    return errors
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const clientErrors = validate()
    setFieldErrors(clientErrors)
    setSubmitError(null)
    if (Object.keys(clientErrors).length > 0) return

    setSubmitting(true)
    try {
      const response = await createIncident({
        title: title.trim(),
        description: description.trim(),
        affectedServiceId: Number(serviceId),
        severity,
        priority,
        ...(teamId ? { responsibleTeamId: Number(teamId) } : {}),
      })
      if (typeof response.incidentId !== 'number') {
        throw new Error('Create incident response has no identifier')
      }
      void navigate(`/incidents/${response.incidentId}`, { replace: true })
    } catch (reason: unknown) {
      if (reason instanceof ApiError && reason.status === 400) {
        const serverErrors: FieldErrors = {}
        for (const error of reason.problem?.errors ?? []) {
          if (error.field && error.message)
            serverErrors[error.field as FieldName] = error.message
        }
        if (Object.keys(serverErrors).length > 0) setFieldErrors(serverErrors)
        else
          setSubmitError(
            reason.problem?.detail ?? t('createIncident.errors.submit'),
          )
      } else {
        setSubmitError(t('createIncident.errors.submit'))
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <article className={styles.page}>
      <header className={styles.header}>
        <Link to="/incidents">{t('createIncident.back')}</Link>
        <h1>{t('createIncident.title')}</h1>
        <p>
          {t('createIncident.author', {
            name: user?.displayName ?? '—',
            role: user?.role ?? '—',
          })}
        </p>
      </header>

      <div className={styles.layout}>
        <form
          className={styles.form}
          onSubmit={(event) => void handleSubmit(event)}
        >
          {referencesError && (
            <div className={styles.formError} role="alert">
              {referencesError}
            </div>
          )}

          <label className={styles.field}>
            <span>{t('createIncident.fields.title')}</span>
            <input
              value={title}
              maxLength={255}
              aria-invalid={Boolean(fieldErrors.title)}
              onChange={(event) => {
                setTitle(event.target.value)
                clearFieldError('title')
              }}
              placeholder={t('createIncident.placeholders.title')}
            />
            {fieldErrors.title && (
              <small className={styles.fieldError}>{fieldErrors.title}</small>
            )}
          </label>

          <label className={styles.field}>
            <span>{t('createIncident.fields.description')}</span>
            <textarea
              value={description}
              aria-invalid={Boolean(fieldErrors.description)}
              onChange={(event) => {
                setDescription(event.target.value)
                clearFieldError('description')
              }}
              placeholder={t('createIncident.placeholders.description')}
            />
            {fieldErrors.description && (
              <small className={styles.fieldError}>
                {fieldErrors.description}
              </small>
            )}
          </label>

          <label className={styles.field}>
            <span>{t('createIncident.fields.service')}</span>
            <select
              value={serviceId}
              disabled={referencesLoading || Boolean(referencesError)}
              aria-invalid={Boolean(fieldErrors.affectedServiceId)}
              onChange={(event) => {
                const value = event.target.value
                setImpact(null)
                setImpactError(null)
                setImpactLoading(Boolean(value))
                setServiceId(value)
                clearFieldError('affectedServiceId')
              }}
            >
              <option value="">
                {t('createIncident.placeholders.service')}
              </option>
              {services.map((service) => (
                <option key={service.id} value={service.id}>
                  {service.code} · {service.name} · {service.tier}
                </option>
              ))}
            </select>
            {fieldErrors.affectedServiceId && (
              <small className={styles.fieldError}>
                {fieldErrors.affectedServiceId}
              </small>
            )}
          </label>

          <div className={styles.scales}>
            <fieldset>
              <legend>{t('createIncident.fields.severity')}</legend>
              <div className={styles.choiceRow}>
                {SEVERITIES.map((value) => (
                  <button
                    key={value}
                    type="button"
                    aria-pressed={severity === value}
                    className={styles[value.toLowerCase()]}
                    onClick={() => {
                      setSeverity(value)
                      clearFieldError('severity')
                    }}
                  >
                    {value}
                  </button>
                ))}
              </div>
              <small>{t(`createIncident.severityHints.${severity}`)}</small>
              {fieldErrors.severity && (
                <small className={styles.fieldError}>
                  {fieldErrors.severity}
                </small>
              )}
            </fieldset>

            <fieldset>
              <legend>{t('createIncident.fields.priority')}</legend>
              <div className={styles.choiceRow}>
                {PRIORITIES.map((value) => (
                  <button
                    key={value}
                    type="button"
                    aria-pressed={priority === value}
                    onClick={() => {
                      setPriority(value)
                      clearFieldError('priority')
                    }}
                  >
                    {value}
                  </button>
                ))}
              </div>
              <small>{t('createIncident.independentScales')}</small>
              {fieldErrors.priority && (
                <small className={styles.fieldError}>
                  {fieldErrors.priority}
                </small>
              )}
            </fieldset>
          </div>

          <label className={styles.field}>
            <span>{t('createIncident.fields.team')}</span>
            <select
              value={teamId}
              disabled={referencesLoading || Boolean(referencesError)}
              aria-invalid={Boolean(fieldErrors.responsibleTeamId)}
              onChange={(event) => {
                setTeamId(event.target.value)
                clearFieldError('responsibleTeamId')
              }}
            >
              <option value="">
                {selectedService?.ownerTeamName
                  ? t('createIncident.ownerFallbackSelected', {
                      owner: selectedService.ownerTeamName,
                    })
                  : t('createIncident.ownerFallback')}
              </option>
              {teams.map((team) => (
                <option key={team.id} value={team.id}>
                  {team.name} ({team.code})
                </option>
              ))}
            </select>
            {fieldErrors.responsibleTeamId && (
              <small className={styles.fieldError}>
                {fieldErrors.responsibleTeamId}
              </small>
            )}
          </label>

          {submitError && (
            <div className={styles.formError} role="alert">
              {submitError}
            </div>
          )}

          <div className={styles.actions}>
            <button type="submit" disabled={submitting || referencesLoading}>
              {submitting
                ? t('createIncident.submitting')
                : t('createIncident.submit')}
            </button>
            <Link to="/incidents">{t('createIncident.cancel')}</Link>
            <span>{t('createIncident.source')}</span>
          </div>
        </form>

        <aside className={styles.preview}>
          <h2>{t('createIncident.preview.title')}</h2>
          {!serviceId && (
            <div className={styles.previewEmpty}>
              {t('createIncident.preview.empty')}
            </div>
          )}
          {serviceId && impactLoading && <p>{t('common.loading')}</p>}
          {serviceId && impactError && (
            <p className={styles.previewError} role="alert">
              {impactError}
            </p>
          )}
          {serviceId && !impactLoading && !impactError && impact && (
            <>
              <div className={styles.impactTotal}>
                <strong>{impact.length}</strong>
                <span>{t('createIncident.preview.affectedCount')}</span>
              </div>
              <p>
                {t('createIncident.preview.summary', {
                  code: selectedService?.code ?? '—',
                  depth: impactGroups.at(-1)?.[0] ?? 0,
                })}
              </p>
              <div className={styles.depthGroups}>
                {impactGroups.map(([depth, codes]) => (
                  <div key={depth}>
                    <span>D{depth}</span>
                    <strong>{codes.join(', ')}</strong>
                  </div>
                ))}
              </div>
              <dl>
                <div>
                  <dt>{t('createIncident.preview.tierOne')}</dt>
                  <dd>
                    {impact.filter((item) => item.tier === 'TIER_1').length}
                  </dd>
                </div>
                <div>
                  <dt>{t('createIncident.preview.owner')}</dt>
                  <dd>{selectedService?.ownerTeamName ?? '—'}</dd>
                </div>
              </dl>
            </>
          )}
        </aside>
      </div>
    </article>
  )
}
