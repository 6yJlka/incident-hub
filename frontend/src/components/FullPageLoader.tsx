import { useTranslation } from 'react-i18next'
import styles from './FullPageLoader.module.css'

export function FullPageLoader() {
  const { t } = useTranslation()

  return (
    <div className={styles.root} role="status">
      <span className={styles.indicator} aria-hidden="true" />
      {t('common.loading')}
    </div>
  )
}
