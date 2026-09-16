import { affected } from '../../../api/generated/service-catalog/service-catalog'
import { create3 } from '../../../api/generated/incidents/incidents'
import type { CreateIncidentRequest } from '../../../api/generated/model'
import { getAllServices, getAllTeams } from '../api/incidentsApi'

export const createIncident = (request: CreateIncidentRequest) =>
  create3(request)

export const getCreateIncidentReferences = () =>
  Promise.all([getAllServices(), getAllTeams()])

export const getIncidentImpactPreview = (serviceId: number) =>
  affected(serviceId, { maxDepth: 10 })
