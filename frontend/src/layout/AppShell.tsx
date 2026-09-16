import { useTranslation } from 'react-i18next'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import styles from './AppShell.module.css'

const navigation = [
  { to: '/incidents', key: 'navigation.incidents', end: true },
  { to: '/services', key: 'navigation.services', end: false },
  { to: '/incidents/new', key: 'navigation.createIncident', end: false },
] as const

function initials(displayName: string): string {
  return displayName
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')
}

export function AppShell() {
  const { t } = useTranslation()
  const { logout, user } = useAuth()
  const location = useLocation()

  if (!user) {
    return null
  }

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <div className={styles.brand}>
          <span className={styles.brandMark} aria-hidden="true" />
          <span>{t('common.brand')}</span>
        </div>

        <nav className={styles.navigation} aria-label={t('navigation.section')}>
          <div className={styles.sectionLabel}>{t('navigation.section')}</div>
          {navigation.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => {
                const isIncidentDetail =
                  item.to === '/incidents' &&
                  /^\/incidents\/\d+$/.test(location.pathname)
                return isActive || isIncidentDetail
                  ? `${styles.link} ${styles.activeLink}`
                  : styles.link
              }}
            >
              {t(item.key)}
            </NavLink>
          ))}
        </nav>

        <div className={styles.userPanel}>
          <div className={styles.userLabel}>{t('navigation.currentUser')}</div>
          <div className={styles.userRow}>
            <div className={styles.avatar} aria-hidden="true">
              {initials(user.displayName)}
            </div>
            <div className={styles.userDetails}>
              <div className={styles.userName}>{user.displayName}</div>
              <div className={styles.userRole}>{user.role}</div>
            </div>
          </div>
          <button className={styles.logout} type="button" onClick={logout}>
            {t('navigation.logout')}
          </button>
        </div>
      </aside>

      <main className={styles.content}>
        <Outlet />
      </main>
    </div>
  )
}
