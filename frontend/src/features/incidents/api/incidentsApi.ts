import {
  affected,
  get as getService,
  list2,
} from '../../../api/generated/service-catalog/service-catalog'
import {
  assign,
  cancel,
  close,
  get1,
  history,
  list3,
  reopen,
  resolve,
  start,
} from '../../../api/generated/incidents/incidents'
import { list1 } from '../../../api/generated/teams/teams'
import { list as listUsers } from '../../../api/generated/users/users'
import type {
  BusinessServiceListItemResponse,
  IncidentResponseAvailableActionsItem,
  List3Params,
  TeamListItemResponse,
  UserListItemResponse,
} from '../../../api/generated/model'

const REFERENCE_PAGE_SIZE = 100

export const getIncidentPage = list3
export const getIncident = get1
export const getIncidentHistory = history

export async function getIncidentServiceContext(serviceId: number) {
  return Promise.all([
    getService(serviceId),
    affected(serviceId, { maxDepth: 10 }),
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
    const response = await list2({ page, size: REFERENCE_PAGE_SIZE })
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
    const response = await list1({ page, size: REFERENCE_PAGE_SIZE })
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
      return assign(incidentId, { assigneeId })
    case 'START':
      return start(incidentId)
    case 'RESOLVE':
      return resolve(incidentId)
    case 'CLOSE':
      return close(incidentId)
    case 'REOPEN':
      return reopen(incidentId)
    case 'CANCEL':
      return cancel(incidentId)
  }
}

export type IncidentFeedParams = List3Params
