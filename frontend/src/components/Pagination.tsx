import { useTranslation } from 'react-i18next'
import styles from './Pagination.module.css'

interface PaginationProps {
  onPageChange: (page: number) => void
  page: {
    hasNext?: boolean
    hasPrevious?: boolean
    page?: number
    size?: number
    totalElements?: number
    totalPages?: number
  }
}

export function Pagination({ onPageChange, page }: PaginationProps) {
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
        {t('common.pagination.summary', {
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
          aria-label={t('common.pagination.previous')}
        >
          ←
        </button>
        <span>
          {t('common.pagination.page', {
            current: current + 1,
            total: Math.max(totalPages, 1),
          })}
        </span>
        <button
          type="button"
          disabled={!page.hasNext}
          onClick={() => onPageChange(current + 1)}
          aria-label={t('common.pagination.next')}
        >
          →
        </button>
      </div>
    </div>
  )
}
