import { useTranslation } from 'react-i18next'
import type { ListIncidentsResponse } from '../../../api/generated/model'
import styles from './IncidentFeedPage.module.css'

interface IncidentPaginationProps {
  onPageChange: (page: number) => void
  page: Pick<
    ListIncidentsResponse,
    'hasNext' | 'hasPrevious' | 'page' | 'size' | 'totalElements' | 'totalPages'
  >
}

export function IncidentPagination({
  onPageChange,
  page,
}: IncidentPaginationProps) {
  const { t } = useTranslation()
  const current = page.page ?? 0
  const totalPages = page.totalPages ?? 0
  const firstItem =
    (page.totalElements ?? 0) === 0 ? 0 : current * (page.size ?? 0) + 1
  const lastItem = Math.min(
    (current + 1) * (page.size ?? 0),
    page.totalElements ?? 0,
  )

  return (
    <div className={styles.pagination}>
      <span>
        {t('incidents.pagination.summary', {
          first: firstItem,
          last: lastItem,
          total: page.totalElements ?? 0,
        })}
      </span>
      <div className={styles.pageButtons}>
        <button
          type="button"
          disabled={!page.hasPrevious}
          onClick={() => onPageChange(current - 1)}
          aria-label={t('incidents.pagination.previous')}
        >
          ←
        </button>
        <span>
          {t('incidents.pagination.page', {
            current: current + 1,
            total: Math.max(totalPages, 1),
          })}
        </span>
        <button
          type="button"
          disabled={!page.hasNext}
          onClick={() => onPageChange(current + 1)}
          aria-label={t('incidents.pagination.next')}
        >
          →
        </button>
      </div>
    </div>
  )
}
