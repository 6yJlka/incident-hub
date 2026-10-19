import type { GroupAlertResponseSeverity } from '../../api/generated/model'
import styles from './AlertGroups.module.css'

export function SignalSeverity({
  value,
}: {
  value?: GroupAlertResponseSeverity
}) {
  const level =
    value === 'CRITICAL'
      ? 3
      : value === 'WARNING'
        ? 2
        : value === 'INFO'
          ? 1
          : 0
  return (
    <span className={styles.signalSeverity}>
      <span className={styles.signalMeter} aria-hidden="true">
        {[1, 2, 3].map((bar) => (
          <span
            key={bar}
            data-active={bar <= level}
            style={{ height: `${bar * 3 + 1}px` }}
          />
        ))}
      </span>
      <span>{value ?? '—'}</span>
    </span>
  )
}
