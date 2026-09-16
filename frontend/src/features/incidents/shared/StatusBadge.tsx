import styles from './Badges.module.css'

function statusClass(value?: string): string | undefined {
  if (!value) return undefined
  const camelCase = value
    .toLowerCase()
    .replace(/_([a-z])/g, (_, letter: string) => letter.toUpperCase())
  return styles[camelCase]
}

export function StatusBadge({ value }: { value?: string }) {
  return (
    <span className={`${styles.status} ${statusClass(value) ?? ''}`}>
      {value ?? '—'}
    </span>
  )
}
