import { createBrowserRouter, Navigate } from 'react-router-dom'
import { ProtectedRoute } from '../auth/ProtectedRoute'
import { AppShell } from '../layout/AppShell'
import { LoginPage } from '../pages/LoginPage'
import { PlaceholderPage } from '../pages/PlaceholderPage'
import { IncidentFeedPage } from '../features/incidents/feed/IncidentFeedPage'
import { IncidentDetailPage } from '../features/incidents/detail/IncidentDetailPage'

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
            element: (
              <PlaceholderPage
                titleKey="placeholders.servicesTitle"
                descriptionKey="placeholders.servicesDescription"
              />
            ),
          },
          {
            path: '/services/:id',
            element: (
              <PlaceholderPage
                titleKey="placeholders.servicesTitle"
                descriptionKey="placeholders.servicesDescription"
              />
            ),
          },
          {
            path: '/incidents/new',
            element: (
              <PlaceholderPage
                titleKey="placeholders.createIncidentTitle"
                descriptionKey="placeholders.createIncidentDescription"
              />
            ),
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
