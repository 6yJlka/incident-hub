import { describe, expect, it } from 'vitest'
import { layoutGroupGraph } from './groupGraphLayout'
import type {
  GraphEdge,
  GraphNode,
  Point,
  PlacedNode,
} from './groupGraphLayout'

// Independent segment/rectangle test, including boundary contact, using slab clipping.
function hits(a: Point, b: Point, rect: PlacedNode) {
  let lower = 0,
    upper = 1
  for (const [start, delta, min, max] of [
    [a.x, b.x - a.x, rect.x, rect.x + rect.width],
    [a.y, b.y - a.y, rect.y, rect.y + rect.height],
  ]) {
    if (delta === 0) {
      if (start < min || start > max) return false
    } else {
      lower = Math.max(
        lower,
        Math.min((min - start) / delta, (max - start) / delta),
      )
      upper = Math.min(
        upper,
        Math.max((min - start) / delta, (max - start) / delta),
      )
      if (lower > upper) return false
    }
  }
  return true
}
function assertClear(nodes: GraphNode[], edges: GraphEdge[]) {
  const layout = layoutGroupGraph(nodes, edges)
  expect(layout.edges).toHaveLength(edges.length)
  for (const edge of layout.edges) {
    const source = layout.nodes.find((node) => node.id === edge.from)!,
      target = layout.nodes.find((node) => node.id === edge.to)!
    const start = edge.points[0],
      end = edge.points.at(-1)!
    expect(start.x).toBe(source.x + source.width)
    expect(end.x).toBe(target.x)
    expect(start.y).toBeGreaterThan(source.y)
    expect(start.y).toBeLessThan(source.y + source.height)
    expect(end.y).toBeGreaterThan(target.y)
    expect(end.y).toBeLessThan(target.y + target.height)
    expect(edge.points[1].x).toBeGreaterThan(start.x)
    expect(edge.points[1].y).toBe(start.y)
    expect(edge.points.at(-2)!.x).toBeLessThan(end.x)
    expect(edge.points.at(-2)!.y).toBe(end.y)
    for (let i = 1; i < edge.points.length; i++) {
      const a = edge.points[i - 1],
        b = edge.points[i]
      expect(a.x === b.x || a.y === b.y).toBe(true)
      for (const node of layout.nodes.filter(
        (node) => node.id !== edge.from && node.id !== edge.to,
      )) {
        expect(
          hits(a, b, node),
          `${edge.from} -> ${edge.to} intersects node ${node.id}`,
        ).toBe(false)
      }
    }
  }
  for (let i = 0; i < layout.edges.length; i++) {
    const first = layout.edges[i]
    for (const second of layout.edges.slice(i + 1)) {
      if (first.from === second.from)
        expect(
          Math.abs(first.points[0].y - second.points[0].y),
        ).toBeGreaterThanOrEqual(8)
      if (first.to === second.to)
        expect(
          Math.abs(first.points.at(-1)!.y - second.points.at(-1)!.y),
        ).toBeGreaterThanOrEqual(8)
      for (let a = 1; a < first.points.length; a++) {
        for (let b = 1; b < second.points.length; b++) {
          const p = first.points[a - 1],
            q = first.points[a]
          const r = second.points[b - 1],
            s = second.points[b]
          const overlap = (v: number, w: number, x: number, y: number) =>
            Math.min(Math.max(v, w), Math.max(x, y)) -
            Math.max(Math.min(v, w), Math.min(x, y))
          if (p.x === q.x && r.x === s.x && overlap(p.y, q.y, r.y, s.y) > 0)
            expect(
              Math.abs(p.x - r.x),
              'vertical lanes must be separated',
            ).toBeGreaterThanOrEqual(8)
          if (p.y === q.y && r.y === s.y && p.y === r.y)
            expect(
              overlap(p.x, q.x, r.x, s.x),
              'horizontal edges must not merge',
            ).toBeLessThanOrEqual(4)
        }
      }
    }
  }
  return layout
}

describe('group graph geometry', () => {
  it('routes the demo graph without interpreting API_GATEWAY -> CUSTOMER_PORTAL as BILLING_WORKER', () => {
    // 1 database, 2 identity, 3 payment, 4 gateway, 5 billing, 6 portal, 7 analytics, 8 support, 9 noise.
    assertClear(
      [
        { id: 1, depth: 0 },
        { id: 2, depth: 1 },
        { id: 3, depth: 1 },
        { id: 4, depth: 2 },
        { id: 5, depth: 2 },
        { id: 6, depth: 2 },
        { id: 7, depth: 3 },
        { id: 8, depth: 3 },
        { id: 9, depth: 4 },
      ],
      [
        [1, 2],
        [1, 3],
        [2, 4],
        [3, 5],
        [3, 6],
        [4, 6],
        [5, 7],
        [6, 8],
      ].map(([from, to]) => ({ from, to })),
    )
  })
  it('routes a wide layer with twelve nodes and interleaved connections', () => {
    const nodes = [
      { id: 1, depth: 0 },
      ...Array.from({ length: 12 }, (_, i) => ({ id: i + 2, depth: 1 })),
      ...Array.from({ length: 8 }, (_, i) => ({ id: i + 20, depth: 2 })),
    ]
    const edges = [
      ...Array.from({ length: 12 }, (_, i) => ({ from: 1, to: i + 2 })),
      ...Array.from({ length: 24 }, (_, i) => ({
        from: (i % 12) + 2,
        to: ((i * 3) % 8) + 20,
      })),
    ]
    const layout = assertClear(nodes, edges)
    const root = layout.nodes.find((node) => node.id === 1)!
    const next = layout.nodes.find((node) => node.id === 2)!
    expect(next.x - root.x - root.width).toBeGreaterThanOrEqual(12 * 8)
  })
  it('routes four consecutive diamonds', () => {
    const nodes = [{ id: 1, depth: 0 }],
      edges: GraphEdge[] = []
    let root = 1
    for (let i = 0; i < 4; i++) {
      const left = root + 1,
        right = root + 2,
        end = root + 3
      nodes.push(
        { id: left, depth: i * 2 + 1 },
        { id: right, depth: i * 2 + 1 },
        { id: end, depth: i * 2 + 2 },
      )
      edges.push(
        { from: root, to: left },
        { from: root, to: right },
        { from: left, to: end },
        { from: right, to: end },
      )
      root = end
    }
    assertClear(nodes, edges)
  })
  it('routes an edge from depth one to depth three around the intermediate column', () => {
    assertClear(
      [
        { id: 1, depth: 0 },
        { id: 2, depth: 1 },
        { id: 3, depth: 2 },
        { id: 4, depth: 2 },
        { id: 5, depth: 2 },
        { id: 6, depth: 3 },
      ],
      [
        [1, 2],
        [2, 3],
        [2, 4],
        [2, 5],
        [3, 6],
        [5, 6],
        [2, 6],
      ].map(([from, to]) => ({ from, to })),
    )
  })
  it('routes cycle and same-column edges', () => {
    assertClear(
      [
        { id: 1, depth: 0 },
        { id: 2, depth: 1 },
        { id: 3, depth: 1 },
        { id: 4, depth: 2 },
      ],
      [
        [1, 2],
        [1, 3],
        [2, 3],
        [3, 4],
        [4, 1],
        [4, 2],
      ].map(([from, to]) => ({ from, to })),
    )
  })
  it('uses repeated barycentric sweeps to align nodes with their parents', () => {
    const nodes = [
      { id: 1, depth: 0 },
      { id: 2, depth: 1 },
      { id: 3, depth: 1 },
      { id: 4, depth: 2 },
      { id: 5, depth: 2 },
    ]
    const edges = [
      [1, 2],
      [1, 3],
      [2, 5],
      [3, 4],
    ].map(([from, to]) => ({ from, to }))
    const layout = assertClear(nodes, edges)
    expect(layout.nodes.find((node) => node.id === 5)!.y).toBeLessThan(
      layout.nodes.find((node) => node.id === 4)!.y,
    )
    expect(
      nodes.filter((node) => node.depth === 2).map((node) => node.id),
    ).toEqual([4, 5])
    expect(layoutGroupGraph(nodes, edges)).toEqual(layout)
  })
})
