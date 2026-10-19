import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import type {
  AlertGroupDetailResponse,
  AffectedServiceResponse,
  BusinessServiceResponse,
} from '../../api/generated/model'
import { groupServices, reachedServiceIds } from './groupPresentation'
import styles from './AlertGroups.module.css'

export function GroupRootPanel({
  group,
  affected,
  root,
  failed,
}: {
  group: AlertGroupDetailResponse
  affected: AffectedServiceResponse[] | null
  root: BusinessServiceResponse | null
  failed: boolean
}) {
  const { t } = useTranslation()
  const [view, setView] = useState<'graph' | 'label'>('graph')
  const services = groupServices(group)
  const reached = reachedServiceIds(
    group.probableRootServiceId!,
    affected ?? [],
  )
  const outside =
    affected === null
      ? []
      : services.filter((service) => !reached.has(service.id!))
  const counts = new Map(
    services.map((service) => [
      service.id,
      group.alerts?.filter((alert) => alert.businessServiceId === service.id)
        .length ?? 0,
    ]),
  )
  const maxDepth = Math.max(
    0,
    ...(affected ?? []).map((service) => service.depth ?? 0),
  )
  const nodes = [
    {
      id: group.probableRootServiceId,
      code: group.probableRootServiceCode,
      depth: 0,
      off: false,
    },
    ...(affected ?? []).map((service) => ({ ...service, off: false })),
    ...outside.map((service) => ({
      ...service,
      depth: maxDepth + 1,
      off: true,
    })),
  ]
  const positions = new Map(
    nodes.map((node) => {
      const column = nodes.filter((other) => other.depth === node.depth)
      return [
        node.id,
        {
          x: (node.depth ?? 0) * 184 + 12,
          y: column.findIndex((other) => other.id === node.id) * 76 + 36,
        },
      ]
    }),
  )
  const width = (maxDepth + (outside.length ? 2 : 1)) * 184 + 12
  const height =
    Math.max(
      1,
      ...nodes.map(
        (node) => nodes.filter((other) => other.depth === node.depth).length,
      ),
    ) *
      76 +
    40
  return (
    <section className={styles.panel}>
      <div className={styles.sectionHead}>
        <div>
          <h2>{t('groups.probableRoot')}</h2>
          <p>
            {affected !== null
              ? t('groups.pathSummary', {
                  on: services.length - outside.length,
                  total: services.length,
                })
              : !failed && t('common.loading')}
          </p>
        </div>
        <div className={styles.tabs}>
          {(['graph', 'label'] as const).map((key) => (
            <button
              key={key}
              aria-pressed={view === key}
              onClick={() => setView(key)}
            >
              {t(`groups.${key}`)}
            </button>
          ))}
        </div>
      </div>
      {view === 'label' ? (
        <div className={styles.rootLabel}>
          <strong>{group.probableRootServiceCode}</strong>
          <span>{group.probableRootServiceName}</span>
          <span>{root?.tier}</span>
          <span>{root?.ownerTeamName}</span>
          <Link to={`/services/${group.probableRootServiceId}`}>
            {t('groups.openService')}
          </Link>
        </div>
      ) : affected !== null ? (
        <div className={styles.graphWrap}>
          <svg
            width={width}
            height={height}
            role="img"
            aria-label={t('groups.graphTitle')}
          >
            {(affected ?? []).flatMap((service) =>
              (service.dependencies ?? []).map((edge) => {
                const from = positions.get(edge.serviceId),
                  to = positions.get(service.id)
                return (
                  from &&
                  to && (
                    <path
                      key={`${edge.serviceId}-${service.id}`}
                      data-edge={`${edge.serviceId}-${service.id}`}
                      d={`M${from.x + 152},${from.y + 26} C${from.x + 170},${from.y + 26} ${to.x - 18},${to.y + 26} ${to.x},${to.y + 26}`}
                      fill="none"
                      stroke="#69757b"
                      strokeDasharray={
                        edge.type === 'ASYNC'
                          ? '5 4'
                          : edge.type === 'DATA'
                            ? '2 3'
                            : undefined
                      }
                    />
                  )
                )
              }),
            )}
            {nodes.map((node) => {
              const position = positions.get(node.id)!
              return (
                <g
                  key={node.id}
                  transform={`translate(${position.x},${position.y})`}
                >
                  <rect
                    width="152"
                    height="54"
                    rx="4"
                    fill={node.depth === 0 ? '#321b1e' : '#151a1d'}
                    stroke={node.depth === 0 ? '#ef4444' : '#556169'}
                    strokeDasharray={counts.has(node.id) ? undefined : '4 3'}
                  />
                  <text x="10" y="21" fill="#e6e9ea" fontSize="11">
                    {node.code}
                  </text>
                  <text x="10" y="41" fill="#a9b3b7" fontSize="10">
                    {node.off
                      ? t('groups.offPath')
                      : node.depth === 0
                        ? t('groups.root')
                        : t('groups.depth', { depth: node.depth })}{' '}
                    ·{' '}
                    {t('groups.alertCount', {
                      count: counts.get(node.id) ?? 0,
                    })}
                  </text>
                </g>
              )
            })}
          </svg>
        </div>
      ) : (
        <p role={failed ? 'alert' : undefined}>
          {t(failed ? 'groups.graphError' : 'common.loading')}
        </p>
      )}
      {affected !== null && (
        <p className={styles.hint}>
          {t('groups.graphLimit')}
          {outside.length > 0 &&
            ` ${t('groups.outsideServices', { services: outside.map((service) => service.code).join(', ') })}`}
        </p>
      )}
    </section>
  )
}
