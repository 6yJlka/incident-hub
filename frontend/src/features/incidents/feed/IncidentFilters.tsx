import { useTranslation } from 'react-i18next'
import {
  List3Priority,
  List3Severity,
  List3Status,
} from '../../../api/generated/model'
import type {
  BusinessServiceListItemResponse,
  TeamListItemResponse,
} from '../../../api/generated/model'
import styles from './IncidentFeedPage.module.css'

export interface FeedFilterValues {
  priority: string
  serviceId: string
  severity: string
  status: string
  teamId: string
}

interface IncidentFiltersProps {
  onChange: (name: keyof FeedFilterValues, value: string) => void
  onReset: () => void
  services: BusinessServiceListItemResponse[]
  teams: TeamListItemResponse[]
  values: FeedFilterValues
}

const contractOptions = {
  status: Object.values(List3Status),
  severity: Object.values(List3Severity),
  priority: Object.values(List3Priority),
}

export function IncidentFilters({
  onChange,
  onReset,
  services,
  teams,
  values,
}: IncidentFiltersProps) {
  const { t } = useTranslation()
  const isDirty = Object.values(values).some(Boolean)

  return (
    <div className={styles.filters} aria-label={t('incidents.filters.label')}>
      <FilterSelect
        label={t('incidents.filters.status')}
        value={values.status}
        options={contractOptions.status}
        onChange={(value) => onChange('status', value)}
      />
      <FilterSelect
        label={t('incidents.filters.severity')}
        value={values.severity}
        options={contractOptions.severity}
        onChange={(value) => onChange('severity', value)}
      />
      <FilterSelect
        label={t('incidents.filters.priority')}
        value={values.priority}
        options={contractOptions.priority}
        onChange={(value) => onChange('priority', value)}
      />
      <FilterSelect
        label={t('incidents.filters.service')}
        value={values.serviceId}
        options={services
          .filter(
            (item): item is BusinessServiceListItemResponse & { id: number } =>
              typeof item.id === 'number',
          )
          .map((item) => ({
            value: String(item.id),
            label: item.code ?? item.name ?? String(item.id),
          }))}
        onChange={(value) => onChange('serviceId', value)}
      />
      <FilterSelect
        label={t('incidents.filters.team')}
        value={values.teamId}
        options={teams
          .filter(
            (item): item is TeamListItemResponse & { id: number } =>
              typeof item.id === 'number',
          )
          .map((item) => ({
            value: String(item.id),
            label: item.code ?? item.name ?? String(item.id),
          }))}
        onChange={(value) => onChange('teamId', value)}
      />
      {isDirty && (
        <button type="button" className={styles.reset} onClick={onReset}>
          {t('incidents.filters.reset')}
        </button>
      )}
    </div>
  )
}

interface FilterSelectProps {
  label: string
  onChange: (value: string) => void
  options: readonly string[] | Array<{ label: string; value: string }>
  value: string
}

function FilterSelect({ label, onChange, options, value }: FilterSelectProps) {
  const { t } = useTranslation()
  return (
    <label className={`${styles.filter} ${value ? styles.activeFilter : ''}`}>
      <span>{label}</span>
      <select
        aria-label={label}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      >
        <option value="">{t('incidents.filters.all')}</option>
        {options.map((option) => {
          const item =
            typeof option === 'string'
              ? { value: option, label: option }
              : option
          return (
            <option key={item.value} value={item.value}>
              {item.label}
            </option>
          )
        })}
      </select>
    </label>
  )
}
