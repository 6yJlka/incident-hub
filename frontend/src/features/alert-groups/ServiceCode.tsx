import styles from './AlertGroups.module.css'

export function ServiceCode({ code, tier }: { code?: string; tier?: string }) {
  return (
    <span className={styles.serviceCode}>
      <span
        className={`${styles.tierDot} ${tier ? styles[tier.toLowerCase()] : ''}`}
        aria-hidden="true"
      />
      {code ?? '—'}
    </span>
  )
}
