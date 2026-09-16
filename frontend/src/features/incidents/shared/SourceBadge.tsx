import styles from './Badges.module.css'

export function SourceBadge({ value }: { value?: string }) {
  if (!value) return null
  return <span className={styles.source}>{value}</span>
}
