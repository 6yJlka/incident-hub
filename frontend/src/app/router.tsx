import { createBrowserRouter, Navigate } from 'react-router-dom'
import { ProtectedRoute } from '../auth/ProtectedRoute'
import { AppShell } from '../layout/AppShell'
import { LoginPage } from '../pages/LoginPage'
import { IncidentFeedPage } from '../features/incidents/feed/IncidentFeedPage'
import { IncidentDetailPage } from '../features/incidents/detail/IncidentDetailPage'
import { CreateIncidentPage } from '../features/incidents/create/CreateIncidentPage'
import { ServiceCatalogPage } from '../features/services/catalog/ServiceCatalogPage'
import { ServiceDetailPage } from '../features/services/detail/ServiceDetailPage'
import { AlertGroupFeedPage } from '../features/alert-groups/AlertGroupFeedPage'
import { AlertGroupDetailPage } from '../features/alert-groups/AlertGroupDetailPage'

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppShell />,
        children: [
          { path: '/alert-groups', element: <AlertGroupFeedPage /> },
          { path: '/alert-groups/:id', element: <AlertGroupDetailPage /> },
          {
            index: true,
            element: <Navigate to="/incidents" replace />,
          },
          {
            path: '/incidents',
            element: <IncidentFeedPage />,
          },
          {
            path: '/incidents/:id',
            element: <IncidentDetailPage />,
          },
          {
            path: '/services',
            element: <ServiceCatalogPage />,
          },
          {
            path: '/services/:id',
            element: <ServiceDetailPage />,
          },
          {
            path: '/incidents/new',
            element: <CreateIncidentPage />,
          },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/incidents" replace />,
  },
])
