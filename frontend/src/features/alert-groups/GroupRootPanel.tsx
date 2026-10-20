import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import type {
  AlertGroupDetailResponse,
  AffectedServiceResponse,
  BusinessServiceResponse,
} from '../../api/generated/model'
import { groupServices, reachedServiceIds } from './groupPresentation'
import { layoutGroupGraph } from './groupGraphLayout'
import { CorrelationEvidence } from './CorrelationEvidence'
import { ServiceCode } from './ServiceCode'
import styles from './AlertGroups.module.css'

export function GroupRootPanel({
  group,
  affected,
  root,
  failed,
  serviceCode,
}: {
  group: AlertGroupDetailResponse
  affected: AffectedServiceResponse[] | null
  root: BusinessServiceResponse | null
  failed: boolean
  serviceCode: (id: number) => string
}) {
  const { t } = useTranslation()
  const arrowId = `group-arrow-${useId().replace(/[^a-zA-Z0-9_-]/g, '')}`
  const [view, setView] = useState<'graph' | 'label'>('graph')
  const undetermined = group.rootAssessment?.status === 'UNDETERMINED'
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
  const nodes = undetermined
    ? services.map((service) => ({ ...service, depth: 0, off: false }))
    : [
        {
          id: group.probableRootServiceId!,
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
  const geometry = layoutGroupGraph(
    nodes.map((node) => ({ id: node.id!, depth: node.depth ?? 0 })),
    (undetermined ? [] : (affected ?? [])).flatMap((service) =>
      (service.dependencies ?? []).map((edge) => ({
        from: edge.serviceId!,
        to: service.id!,
        type: edge.type,
      })),
    ),
  )
  const positions = new Map(geometry.nodes.map((node) => [node.id, node]))
  return (
    <section className={styles.panel}>
      <div className={styles.sectionHead}>
        <div>
          <h2>{t('groups.probableRoot')}</h2>
          <p>
            {undetermined
              ? t('groups.neutralGraphHint')
              : affected !== null
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
      {undetermined ? (
        <div className={styles.rootLabel}>
          <strong>{t('groups.rootUndetermined')}</strong>
          <p>
            {t(`groups.rootReasons.${group.rootAssessment?.reason}`, {
              defaultValue: group.rootAssessment?.reason ?? '',
            })}
          </p>
        </div>
      ) : (
        <div className={styles.candidates}>
          <ol>
            {group.rootAssessment?.candidates?.map((candidate) => (
              <li key={candidate.serviceId}>
                <Link to={`/services/${candidate.serviceId}`}>
                  {candidate.serviceCode ?? serviceCode(candidate.serviceId!)}
                </Link>
                {' · '}
                <span>
                  {t('groups.candidateScore', { score: candidate.score })}
                </span>
              </li>
            ))}
          </ol>
          <CorrelationEvidence
            evidence={group.rootAssessment?.evidence}
            serviceCode={serviceCode}
          />
        </div>
      )}
      {view === 'label' ? (
        undetermined ? null : (
          <div className={styles.rootLabel}>
            <ServiceCode
              code={group.probableRootServiceCode}
              tier={root?.tier}
            />
            <span>{group.probableRootServiceName}</span>
            <span>{root?.tier}</span>
            <span>{root?.ownerTeamName}</span>
            <Link to={`/services/${group.probableRootServiceId}`}>
              {t('groups.openService')}
            </Link>
          </div>
        )
      ) : affected !== null || undetermined ? (
        <div className={styles.graphWrap}>
          <svg
            width={geometry.width}
            height={geometry.height}
            role="img"
            aria-label={t('groups.graphTitle')}
          >
            <defs>
              <marker
                id={arrowId}
                viewBox="0 0 8 8"
                refX="8"
                refY="4"
                markerWidth="8"
                markerHeight="8"
                markerUnits="userSpaceOnUse"
                orient="auto"
              >
                <path d="M0,0 L8,4 L0,8 Z" fill="#69757b" />
              </marker>
            </defs>
            {[...new Set(nodes.map((node) => node.depth))].map((depth) => {
              const node = nodes.find((item) => item.depth === depth)!
              return (
                <text
                  key={depth}
                  x={positions.get(node.id!)!.x}
                  y={16}
                  fill="#7d878b"
                  fontSize="9"
                >
                  {undetermined
                    ? t('groups.servicesInGroup')
                    : node.off
                      ? t('groups.offPath')
                      : depth === 0
                        ? t('groups.root')
                        : t('groups.depth', { depth })}
                </text>
              )
            })}
            {geometry.edges.map((edge) => (
              <path
                key={`${edge.from}-${edge.to}`}
                data-edge={`${edge.from}-${edge.to}`}
                d={edge.points
                  .map(
                    (point, index) =>
                      `${index === 0 ? 'M' : 'L'}${point.x},${point.y}`,
                  )
                  .join(' ')}
                fill="none"
                stroke="#69757b"
                strokeLinejoin="round"
                markerEnd={`url(#${arrowId})`}
                strokeDasharray={
                  edge.type === 'ASYNC'
                    ? '5 4'
                    : edge.type === 'DATA'
                      ? '2 3'
                      : undefined
                }
              />
            ))}
            {nodes.map((node) => {
              const position = positions.get(node.id!)!
              return (
                <g
                  key={node.id}
                  transform={`translate(${position.x},${position.y})`}
                >
                  <rect
                    width={position.width}
                    height={position.height}
                    rx="4"
                    fill={
                      !undetermined && node.depth === 0 ? '#321b1e' : '#151a1d'
                    }
                    stroke={
                      !undetermined && node.depth === 0 ? '#ef4444' : '#556169'
                    }
                    strokeDasharray={counts.has(node.id) ? undefined : '4 3'}
                  />
                  <title>{node.code}</title>
                  <text x="10" y="18" fill="#e6e9ea" fontSize="10">
                    {node.code && node.code.length > 19
                      ? `${node.code.slice(0, 18)}…`
                      : node.code}
                  </text>
                  <text x="10" y="34" fill="#a9b3b7" fontSize="9">
                    {undetermined
                      ? ''
                      : node.off
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
      {!undetermined && affected !== null && (
        <p className={styles.hint}>
          {t('groups.graphLimit')}
          {outside.length > 0 &&
            ` ${t('groups.outsideServices', { services: outside.map((service) => service.code).join(', ') })}`}
        </p>
      )}
    </section>
  )
}
