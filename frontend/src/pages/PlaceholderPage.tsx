import { useTranslation } from 'react-i18next'
import styles from './PlaceholderPage.module.css'

interface PlaceholderPageProps {
  descriptionKey:
    | 'placeholders.incidentsDescription'
    | 'placeholders.servicesDescription'
    | 'placeholders.createIncidentDescription'
  titleKey:
    | 'placeholders.incidentsTitle'
    | 'placeholders.servicesTitle'
    | 'placeholders.createIncidentTitle'
}

export function PlaceholderPage({
  descriptionKey,
  titleKey,
}: PlaceholderPageProps) {
  const { t } = useTranslation()

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <h1>{t(titleKey)}</h1>
        <p>{t(descriptionKey)}</p>
      </header>
      <div className={styles.placeholder} aria-hidden="true" />
    </section>
  )
}
