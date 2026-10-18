import {
  getAffectedServices,
  getService,
} from '../../../api/generated/service-catalog/service-catalog'
import { listIncidents } from '../../../api/generated/incidents/incidents'
import type {
  AffectedServiceResponse,
  BusinessServiceResponse,
  IncidentListItemResponse,
} from '../../../api/generated/model'

const PAGE_SIZE = 100
const ACTIVE_INCIDENT_STATUSES = new Set(['OPEN', 'ASSIGNED', 'IN_PROGRESS'])

export interface ServiceDetailData {
  affected: AffectedServiceResponse[]
  incidents: IncidentListItemResponse[]
  service: BusinessServiceResponse
}

async function getIncidentsForService(
  serviceId: number,
): Promise<IncidentListItemResponse[]> {
  const items: IncidentListItemResponse[] = []
  let page = 0
  let hasNext = true

  while (hasNext) {
    const response = await listIncidents({
      affectedServiceId: serviceId,
      page,
      size: PAGE_SIZE,
    })
    items.push(...(response.items ?? []))
    hasNext = response.hasNext ?? false
    page += 1
  }

  return items
}

function isActiveIncident(incident: IncidentListItemResponse): boolean {
  return Boolean(
    incident.status && ACTIVE_INCIDENT_STATUSES.has(incident.status),
  )
}

export async function getServiceDetail(
  serviceId: number,
): Promise<ServiceDetailData> {
  const [service, affectedResponse, incidents] = await Promise.all([
    getService(serviceId),
    getAffectedServices(serviceId, { maxDepth: 10 }),
    getIncidentsForService(serviceId),
  ])

  return {
    service,
    affected: affectedResponse.items ?? [],
    incidents: incidents.filter(isActiveIncident),
  }
}
