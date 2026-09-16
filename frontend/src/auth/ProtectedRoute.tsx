import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { FullPageLoader } from '../components/FullPageLoader'
import { useAuth } from './useAuth'

export function ProtectedRoute() {
  const { hasToken, isRestoring, user } = useAuth()
  const location = useLocation()

  if (isRestoring || (hasToken && !user)) {
    return <FullPageLoader />
  }

  if (!hasToken || !user) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: `${location.pathname}${location.search}` }}
      />
    )
  }

  return <Outlet />
}
