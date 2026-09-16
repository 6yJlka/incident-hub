import {
  affected,
  get as getService,
  list2,
} from '../../../api/generated/service-catalog/service-catalog'
import { list3 } from '../../../api/generated/incidents/incidents'
import type {
  AffectedServiceResponse,
  BusinessServiceListItemResponse,
  BusinessServiceResponse,
  IncidentListItemResponse,
} from '../../../api/generated/model'

const PAGE_SIZE = 100
const ACTIVE_INCIDENT_STATUSES = new Set(['OPEN', 'ASSIGNED', 'IN_PROGRESS'])

export interface ServiceCatalogItem {
  activeIncidents: number
  affectedCount: number
  dependencyCount: number
  service: BusinessServiceListItemResponse
}

export interface ServiceDetailData {
  affected: AffectedServiceResponse[]
  incidents: IncidentListItemResponse[]
  service: BusinessServiceResponse
}

async function getAllServices(): Promise<BusinessServiceListItemResponse[]> {
  const items: BusinessServiceListItemResponse[] = []
  let page = 0
  let hasNext = true

  while (hasNext) {
    const response = await list2({ page, size: PAGE_SIZE })
    items.push(...(response.items ?? []))
    hasNext = response.hasNext ?? false
    page += 1
  }

  return items
}

async function getIncidentsForService(
  serviceId: number,
): Promise<IncidentListItemResponse[]> {
  const items: IncidentListItemResponse[] = []
  let page = 0
  let hasNext = true

  while (hasNext) {
    const response = await list3({
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

export async function getServiceCatalog(): Promise<ServiceCatalogItem[]> {
  const services = await getAllServices()

  return Promise.all(
    services.map(async (service) => {
      if (typeof service.id !== 'number') {
        return {
          service,
          dependencyCount: 0,
          affectedCount: 0,
          activeIncidents: 0,
        }
      }

      const [details, affectedResponse, incidents] = await Promise.all([
        getService(service.id),
        affected(service.id, { maxDepth: 10 }),
        getIncidentsForService(service.id),
      ])

      return {
        service,
        dependencyCount: details.dependencies?.length ?? 0,
        affectedCount: affectedResponse.items?.length ?? 0,
        activeIncidents: incidents.filter(isActiveIncident).length,
      }
    }),
  )
}

export async function getServiceDetail(
  serviceId: number,
): Promise<ServiceDetailData> {
  const [service, affectedResponse, incidents] = await Promise.all([
    getService(serviceId),
    affected(serviceId, { maxDepth: 10 }),
    getIncidentsForService(serviceId),
  ])

  return {
    service,
    affected: affectedResponse.items ?? [],
    incidents: incidents.filter(isActiveIncident),
  }
}
