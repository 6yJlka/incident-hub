import { compactTier } from '../model/format'
import styles from './Badges.module.css'

export function ServiceTier({ tier }: { tier?: string }) {
  const className = tier
    ? styles[tier.toLowerCase().replace('_', '')]
    : undefined
  return (
    <span className={`${styles.tier} ${className ?? ''}`}>
      {compactTier(tier)}
    </span>
  )
}
