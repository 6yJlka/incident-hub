import { useState } from 'react'
import type { FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/http'
import { FullPageLoader } from '../components/FullPageLoader'
import { useAuth } from '../auth/useAuth'
import styles from './LoginPage.module.css'

interface LoginLocationState {
  from?: string
}

export function LoginPage() {
  const { t } = useTranslation()
  const { hasToken, isRestoring, login, user } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (isRestoring || (hasToken && !user)) {
    return <FullPageLoader />
  }

  if (user) {
    return <Navigate to="/incidents" replace />
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setErrorKey(null)
    setIsSubmitting(true)

    try {
      await login({ email, password })
      const state = location.state as LoginLocationState | null
      navigate(state?.from ?? '/incidents', { replace: true })
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setErrorKey('auth.invalidCredentials')
      } else if (error instanceof ApiError && error.status === 400) {
        setErrorKey('auth.validationError')
      } else {
        setErrorKey('auth.unexpectedError')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className={styles.page}>
      <div className={styles.wrapper}>
        <div className={styles.brand}>
          <span className={styles.brandMark} aria-hidden="true" />
          <span className={styles.brandName}>{t('common.brand')}</span>
          <span className={styles.platform}>{t('common.platform')}</span>
        </div>

        <form className={styles.card} onSubmit={handleSubmit}>
          <h1 className={styles.title}>{t('auth.title')}</h1>
          <p className={styles.subtitle}>{t('auth.subtitle')}</p>

          {errorKey && (
            <div className={styles.error} role="alert">
              <span className={styles.errorDot} aria-hidden="true" />
              <span>{t(errorKey)}</span>
            </div>
          )}

          <label className={styles.field}>
            <span className={styles.label}>{t('auth.email')}</span>
            <input
              className={styles.input}
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
              disabled={isSubmitting}
            />
          </label>

          <label className={styles.field}>
            <span className={styles.label}>{t('auth.password')}</span>
            <input
              className={styles.input}
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              disabled={isSubmitting}
            />
          </label>

          <button
            className={styles.submit}
            type="submit"
            disabled={isSubmitting}
          >
            {t(isSubmitting ? 'auth.submitting' : 'auth.submit')}
          </button>
        </form>
      </div>
    </main>
  )
}
