export function incidentCode(id?: number): string {
  return typeof id === 'number' ? `INC-${id}` : '—'
}

export function formatDateTime(value?: string, locale = 'ru-RU'): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  return new Intl.DateTimeFormat(locale, {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

export function compactTier(tier?: string): string {
  return tier?.replace('TIER_', 'T') ?? '—'
}
