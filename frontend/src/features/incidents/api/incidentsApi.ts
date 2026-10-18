import {
  getAffectedServices,
  getService,
  listServices,
} from '../../../api/generated/service-catalog/service-catalog'
import {
  assignIncident,
  cancelIncident,
  closeIncident,
  reopenIncident,
  resolveIncident,
  startIncidentProgress,
} from '../../../api/generated/incidents/incidents'
import { listTeams } from '../../../api/generated/teams/teams'
import { listUsers } from '../../../api/generated/users/users'
import type {
  BusinessServiceListItemResponse,
  IncidentResponseAvailableActionsItem,
  TeamListItemResponse,
  UserListItemResponse,
} from '../../../api/generated/model'

const REFERENCE_PAGE_SIZE = 100

export async function getIncidentServiceContext(serviceId: number) {
  return Promise.all([
    getService(serviceId),
    getAffectedServices(serviceId, { maxDepth: 10 }),
    getAllServices(),
  ])
}

export async function getAllServices(): Promise<
  BusinessServiceListItemResponse[]
> {
  const result: BusinessServiceListItemResponse[] = []
  let page = 0
  let hasNext = true

  while (hasNext) {
    const response = await listServices({ page, size: REFERENCE_PAGE_SIZE })
    result.push(...(response.items ?? []))
    hasNext = response.hasNext ?? false
    page += 1
  }

  return result
}

export async function getAllTeams(): Promise<TeamListItemResponse[]> {
  const result: TeamListItemResponse[] = []
  let page = 0
  let hasNext = true

  while (hasNext) {
    const response = await listTeams({ page, size: REFERENCE_PAGE_SIZE })
    result.push(...(response.items ?? []))
    hasNext = response.hasNext ?? false
    page += 1
  }

  return result
}

export async function getAllUsers(): Promise<UserListItemResponse[]> {
  const result: UserListItemResponse[] = []
  let page = 0
  let hasNext = true

  while (hasNext) {
    const response = await listUsers({
      page,
      size: REFERENCE_PAGE_SIZE,
      active: true,
    })
    result.push(...(response.items ?? []))
    hasNext = response.hasNext ?? false
    page += 1
  }

  return result
}

export function executeIncidentAction(
  incidentId: number,
  action: IncidentResponseAvailableActionsItem,
  assigneeId?: number,
) {
  switch (action) {
    case 'ASSIGN':
      if (assigneeId === undefined) {
        throw new Error('ASSIGN requires an assignee')
      }
      return assignIncident(incidentId, { assigneeId })
    case 'START':
      return startIncidentProgress(incidentId)
    case 'RESOLVE':
      return resolveIncident(incidentId)
    case 'CLOSE':
      return closeIncident(incidentId)
    case 'REOPEN':
      return reopenIncident(incidentId)
    case 'CANCEL':
      return cancelIncident(incidentId)
  }
}
