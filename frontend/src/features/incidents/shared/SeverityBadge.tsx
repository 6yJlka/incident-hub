import styles from './Badges.module.css'

export function SeverityBadge({ value }: { value?: string }) {
  const className = value ? styles[value.toLowerCase()] : undefined
  return (
    <span className={`${styles.severity} ${className ?? ''}`}>
      {value ?? '—'}
    </span>
  )
}
