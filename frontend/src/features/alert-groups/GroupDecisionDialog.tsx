import { useEffect, useRef, useState } from 'react'
import type { FormEvent, KeyboardEvent } from 'react'
import { useTranslation } from 'react-i18next'
import type {
  AlertGroupDetailResponse,
  BusinessServiceListItemResponse,
  ConfirmAlertGroupRequest,
} from '../../api/generated/model'
import { groupServices, maximumSignal } from './groupPresentation'
import { DecisionChoices } from './DecisionChoices'
import { SignalSeverity } from './SignalSeverity'
import styles from './AlertGroups.module.css'

export function GroupDecisionDialog({
  kind,
  group,
  catalog,
  busy,
  error,
  serviceRequired,
  onClose,
  onConfirm,
  onDismiss,
}: {
  kind: 'confirm' | 'dismiss'
  group: AlertGroupDetailResponse
  catalog: BusinessServiceListItemResponse[]
  busy: boolean
  error: string | null
  serviceRequired: boolean
  onClose: () => void
  onConfirm: (body: ConfirmAlertGroupRequest) => void
  onDismiss: (reason?: string) => void
}) {
  const { t } = useTranslation()
  const undetermined = group.rootAssessment?.status === 'UNDETERMINED'
  const ref = useRef<HTMLDivElement>(null)
  const [severity, setSeverity] = useState('')
  const [priority, setPriority] = useState('')
  const [serviceId, setServiceId] = useState(
    undetermined ? '' : String(group.probableRootServiceId ?? ''),
  )
  const [title, setTitle] = useState(() =>
    t(
      undetermined
        ? 'groups.undeterminedTitleTemplate'
        : 'groups.titleTemplate',
      {
        root: group.probableRootServiceCode,
        count: groupServices(group).length,
      },
    ),
  )
  const [description, setDescription] = useState(() =>
    t(
      undetermined
        ? 'groups.undeterminedDescriptionTemplate'
        : 'groups.descriptionTemplate',
      {
        id: group.id,
        count: group.alerts?.length ?? 0,
        root: group.probableRootServiceCode,
        services: groupServices(group)
          .map((service) => service.code)
          .join(', '),
      },
    ),
  )
  const [reason, setReason] = useState('')
  type Field = 'severity' | 'priority' | 'serviceId' | 'title' | 'description'
  const [errors, setErrors] = useState<Partial<Record<Field, string>>>({})
  function clear(field: Field) {
    setErrors((current) => ({ ...current, [field]: undefined }))
  }
  const services = groupServices(group)
  const ids = new Set(services.map((service) => service.id))
  const remaining = catalog.filter((service) => !ids.has(service.id))
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null
    ref.current
      ?.querySelector<HTMLElement>('select, textarea, input, button')
      ?.focus()
    return () => {
      previous?.focus()
    }
  }, [])
  useEffect(() => {
    if (serviceRequired)
      ref.current
        ?.querySelector<HTMLElement>('[data-field="serviceId"]')
        ?.focus()
  }, [serviceRequired])
  const serviceError =
    errors.serviceId ||
    (serviceRequired ? t('groups.serviceRequired') : undefined)
  function keyboard(event: KeyboardEvent) {
    if (event.key === 'Escape' && !busy) {
      event.preventDefault()
      onClose()
    }
    if (event.key !== 'Tab') return
    const items = ref.current?.querySelectorAll<HTMLElement>(
      'button:not(:disabled), input:not(:disabled), textarea:not(:disabled), select:not(:disabled)',
    )
    const first = items?.[0],
      last = items?.[items.length - 1]
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault()
      last?.focus()
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault()
      first?.focus()
    }
  }
  function submit(event: FormEvent) {
    event.preventDefault()
    if (busy) return
    if (kind === 'dismiss') {
      onDismiss(reason.trim() || undefined)
      return
    }
    const invalid: Partial<Record<Field, string>> = {}
    if (!title.trim() || title.length > 255)
      invalid.title = t('groups.titleRequired')
    if (!description.trim())
      invalid.description = t('groups.descriptionRequired')
    if (!serviceId) invalid.serviceId = t('groups.serviceRequired')
    if (!severity) invalid.severity = t('groups.severityRequired')
    if (!priority) invalid.priority = t('groups.priorityRequired')
    setErrors(invalid)
    const first = Object.keys(invalid)[0]
    if (first) {
      ref.current
        ?.querySelector<HTMLElement>(`[data-field="${first}"]`)
        ?.focus()
      return
    }
    onConfirm({
      expectedVersion: group.version!,
      severity: severity as ConfirmAlertGroupRequest['severity'],
      priority: priority as ConfirmAlertGroupRequest['priority'],
      businessServiceId: Number(serviceId),
      title: title.trim(),
      description: description.trim(),
    })
  }
  return (
    <div className={styles.backdrop}>
      <div
        ref={ref}
        role="dialog"
        aria-modal="true"
        aria-labelledby="decision-title"
        className={styles.dialog}
        onKeyDown={keyboard}
      >
        <h2 id="decision-title">
          {t(
            kind === 'confirm' ? 'groups.confirmTitle' : 'groups.dismissTitle',
            { id: group.id },
          )}
        </h2>
        <form onSubmit={submit} noValidate>
          <fieldset disabled={busy} className={styles.formFields}>
            {kind === 'confirm' ? (
              <>
                <label>
                  {t('groups.incidentTitle')}
                  <input
                    required
                    data-field="title"
                    aria-invalid={Boolean(errors.title)}
                    aria-describedby={errors.title ? 'title-error' : undefined}
                    maxLength={255}
                    value={title}
                    onChange={(event) => {
                      setTitle(event.target.value)
                      clear('title')
                    }}
                  />
                  {errors.title && (
                    <span
                      id="title-error"
                      role="alert"
                      className={styles.fieldError}
                    >
                      {errors.title}
                    </span>
                  )}
                </label>
                <label>
                  {t('groups.incidentDescription')}
                  <textarea
                    required
                    data-field="description"
                    aria-invalid={Boolean(errors.description)}
                    aria-describedby={
                      errors.description ? 'description-error' : undefined
                    }
                    rows={4}
                    value={description}
                    onChange={(event) => {
                      setDescription(event.target.value)
                      clear('description')
                    }}
                  />
                  {errors.description && (
                    <span
                      id="description-error"
                      role="alert"
                      className={styles.fieldError}
                    >
                      {errors.description}
                    </span>
                  )}
                </label>
                <label>
                  <span id="incident-service-label">
                    {t('groups.incidentService')}
                  </span>
                  <select
                    required
                    data-field="serviceId"
                    aria-labelledby="incident-service-label"
                    aria-invalid={Boolean(serviceError)}
                    aria-describedby={
                      serviceError ? 'serviceId-error' : undefined
                    }
                    value={serviceId}
                    onChange={(event) => {
                      setServiceId(event.target.value)
                      clear('serviceId')
                    }}
                  >
                    <option value="">{t('groups.selectService')}</option>
                    {services.map((service) => (
                      <option key={service.id} value={service.id}>
                        {service.code}
                      </option>
                    ))}
                    {remaining.length > 0 && (
                      <option disabled value="catalog-divider">
                        {t('groups.catalogDivider')}
                      </option>
                    )}
                    {remaining.map((service) => (
                      <option key={service.id} value={service.id}>
                        {service.code}
                      </option>
                    ))}
                  </select>
                  {serviceError && (
                    <span
                      id="serviceId-error"
                      role="alert"
                      className={styles.fieldError}
                    >
                      {serviceError}
                    </span>
                  )}
                </label>
                <div className={styles.choiceColumns}>
                  <DecisionChoices
                    name="severity"
                    label={t('groups.incidentSeverity')}
                    options={['SEV1', 'SEV2', 'SEV3', 'SEV4']}
                    value={severity}
                    error={errors.severity}
                    onChange={(value) => {
                      setSeverity(value)
                      clear('severity')
                    }}
                  />
                  <DecisionChoices
                    name="priority"
                    label={t('groups.priority')}
                    options={['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']}
                    value={priority}
                    error={errors.priority}
                    onChange={(value) => {
                      setPriority(value)
                      clear('priority')
                    }}
                  />
                </div>
                <p className={styles.hint}>
                  {t('groups.maxSignalHint')}{' '}
                  <SignalSeverity value={maximumSignal(group.alerts)} />.{' '}
                  {t('groups.signalNotIncident')}
                </p>
              </>
            ) : (
              <label>
                {t('groups.reason')}
                <textarea
                  maxLength={2000}
                  rows={4}
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                />
              </label>
            )}
          </fieldset>
          {error && <p role="alert">{error}</p>}
          <div className={styles.actions}>
            <button className={styles.primary} disabled={busy} type="submit">
              {t(
                busy
                  ? 'groups.saving'
                  : kind === 'confirm'
                    ? 'groups.createIncident'
                    : 'groups.dismiss',
              )}
            </button>
            <button disabled={busy} type="button" onClick={onClose}>
              {t('groups.cancel')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
