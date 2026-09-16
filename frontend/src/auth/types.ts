import type {
  CurrentUserResponse,
  CurrentUserResponseRole,
} from '../api/generated/model'

export interface CurrentUser {
  active: boolean
  displayName: string
  email: string
  id: number
  role: CurrentUserResponseRole
}

export function toCurrentUser(response: CurrentUserResponse): CurrentUser {
  if (
    typeof response.id !== 'number' ||
    typeof response.email !== 'string' ||
    typeof response.displayName !== 'string' ||
    typeof response.role !== 'string' ||
    typeof response.active !== 'boolean'
  ) {
    throw new Error('Current user response is incomplete')
  }

  return {
    id: response.id,
    email: response.email,
    displayName: response.displayName,
    role: response.role,
    active: response.active,
  }
}
