import { getAffectedServices } from '../../../api/generated/service-catalog/service-catalog'
import { getAllServices, getAllTeams } from '../api/incidentsApi'

export const getCreateIncidentReferences = () =>
  Promise.all([getAllServices(), getAllTeams()])

export const getIncidentImpactPreview = (serviceId: number) =>
  getAffectedServices(serviceId, { maxDepth: 10 })
