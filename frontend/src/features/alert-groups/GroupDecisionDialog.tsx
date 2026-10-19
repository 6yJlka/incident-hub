import { useEffect, useRef, useState } from 'react'
import type { FormEvent, KeyboardEvent } from 'react'
import { useTranslation } from 'react-i18next'
import type {
  AlertGroupDetailResponse,
  BusinessServiceListItemResponse,
  ConfirmAlertGroupRequest,
} from '../../api/generated/model'
import { groupServices } from './groupPresentation'
import styles from './AlertGroups.module.css'

export function GroupDecisionDialog({
  kind,
  group,
  catalog,
  busy,
  error,
  onClose,
  onConfirm,
  onDismiss,
}: {
  kind: 'confirm' | 'dismiss'
  group: AlertGroupDetailResponse
  catalog: BusinessServiceListItemResponse[]
  busy: boolean
  error: string | null
  onClose: () => void
  onConfirm: (body: ConfirmAlertGroupRequest) => void
  onDismiss: (reason?: string) => void
}) {
  const { t } = useTranslation()
  const ref = useRef<HTMLDivElement>(null)
  const [severity, setSeverity] = useState('')
  const [priority, setPriority] = useState('')
  const [serviceId, setServiceId] = useState(
    String(group.probableRootServiceId ?? ''),
  )
  const [title, setTitle] = useState(() =>
    t('groups.titleTemplate', {
      root: group.probableRootServiceCode,
      count: groupServices(group).length,
    }),
  )
  const [description, setDescription] = useState(() =>
    t('groups.descriptionTemplate', {
      id: group.id,
      count: group.alerts?.length ?? 0,
      root: group.probableRootServiceCode,
      services: groupServices(group)
        .map((service) => service.code)
        .join(', '),
    }),
  )
  const [reason, setReason] = useState('')
  const [invalid, setInvalid] = useState(false)
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
    if (
      !severity ||
      !priority ||
      !serviceId ||
      !title.trim() ||
      !description.trim() ||
      title.length > 255
    ) {
      setInvalid(true)
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
                  {t('groups.incidentSeverity')}
                  <select
                    required
                    aria-invalid={invalid && !severity}
                    value={severity}
                    onChange={(event) => setSeverity(event.target.value)}
                  >
                    <option value="">{t('groups.choose')}</option>
                    {['SEV1', 'SEV2', 'SEV3', 'SEV4'].map((value) => (
                      <option key={value}>{value}</option>
                    ))}
                  </select>
                </label>
                <label>
                  {t('groups.priority')}
                  <select
                    required
                    aria-invalid={invalid && !priority}
                    value={priority}
                    onChange={(event) => setPriority(event.target.value)}
                  >
                    <option value="">{t('groups.choose')}</option>
                    {['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((value) => (
                      <option key={value}>{value}</option>
                    ))}
                  </select>
                </label>
                <p className={styles.hint}>{t('groups.independentScales')}</p>
                <label>
                  {t('groups.incidentService')}
                  <select
                    required
                    value={serviceId}
                    onChange={(event) => setServiceId(event.target.value)}
                  >
                    {services.map((service) => (
                      <option key={service.id} value={service.id}>
                        {service.code}
                      </option>
                    ))}
                    {remaining.length > 0 && (
                      <option disabled value="">
                        {t('groups.catalogDivider')}
                      </option>
                    )}
                    {remaining.map((service) => (
                      <option key={service.id} value={service.id}>
                        {service.code}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  {t('groups.incidentTitle')}
                  <input
                    required
                    maxLength={255}
                    value={title}
                    onChange={(event) => setTitle(event.target.value)}
                  />
                </label>
                <label>
                  {t('groups.incidentDescription')}
                  <textarea
                    required
                    rows={4}
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                  />
                </label>
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
          {(invalid || error) && (
            <p role="alert">{error ?? t('groups.validation')}</p>
          )}
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
