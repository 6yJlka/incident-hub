export const GROUPS_CHANGED = 'incident-hub:groups-changed'

export function notifyGroupsChanged() {
  window.dispatchEvent(new Event(GROUPS_CHANGED))
}
