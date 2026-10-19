export interface GraphNode {
  id: number
  depth: number
}
export interface GraphEdge {
  from: number
  to: number
  type?: string
}
export interface Point {
  x: number
  y: number
}
export interface PlacedNode extends GraphNode, Point {
  width: number
  height: number
}
export interface RoutedEdge extends GraphEdge {
  points: Point[]
}

const NODE_WIDTH = 140
const NODE_HEIGHT = 44
const COLUMN_GAP = 38
const ROW_GAP = 24
const CLEARANCE = 6

// Edges and node rectangles use the same coordinates; SVG rendering does not smooth corners.
export function layoutGroupGraph(input: GraphNode[], edges: GraphEdge[]) {
  const columns = new Map<number, GraphNode[]>()
  for (const node of input)
    columns.set(node.depth, [...(columns.get(node.depth) ?? []), node])
  const depths = [...columns.keys()].sort((a, b) => a - b)
  const ranks = new Map<number, number>()
  const byId = new Map(input.map((node) => [node.id, node]))
  function rank() {
    for (const column of columns.values())
      column.forEach((node, index) => ranks.set(node.id, index))
  }
  rank()
  // Alternate parent and child sweeps. Only adjacent columns affect the barycentre.
  for (let pass = 0; pass < 8; pass++) {
    const forward = pass % 2 === 0
    for (const depth of forward ? depths : [...depths].reverse()) {
      const column = columns.get(depth)!
      const scores = new Map(
        column.map((node) => {
          const neighbours = edges.flatMap((edge) =>
            forward
              ? edge.to === node.id && byId.get(edge.from)?.depth === depth - 1
                ? [edge.from]
                : []
              : edge.from === node.id && byId.get(edge.to)?.depth === depth + 1
                ? [edge.to]
                : [],
          )
          return [
            node.id,
            neighbours.length
              ? neighbours.reduce((sum, id) => sum + ranks.get(id)!, 0) /
                neighbours.length
              : ranks.get(node.id)!,
          ]
        }),
      )
      column.sort(
        (a, b) =>
          scores.get(a.id)! - scores.get(b.id)! ||
          ranks.get(a.id)! - ranks.get(b.id)!,
      )
      rank()
    }
  }
  const rows = Math.max(
    1,
    ...[...columns.values()].map((column) => column.length),
  )
  const nodes: PlacedNode[] = depths.flatMap((depth) => {
    const column = columns.get(depth)!
    return column.map((node, index) => ({
      ...node,
      width: NODE_WIDTH,
      height: NODE_HEIGHT,
      x: 24 + depth * (NODE_WIDTH + COLUMN_GAP),
      y: 36 + ((rows - column.length) / 2 + index) * (NODE_HEIGHT + ROW_GAP),
    }))
  })
  const width = Math.max(240, ...nodes.map((node) => node.x + node.width + 24))
  const height = rows * (NODE_HEIGHT + ROW_GAP) + 48
  const placed = new Map(nodes.map((node) => [node.id, node]))
  const obstacles = nodes.map((node) => ({
    x: node.x - CLEARANCE,
    y: node.y - CLEARANCE,
    width: node.width + CLEARANCE * 2,
    height: node.height + CLEARANCE * 2,
  }))
  const xs = [
    ...new Set([
      8,
      width - 8,
      ...nodes.flatMap((node) => [
        node.x - CLEARANCE - 2,
        node.x + node.width + CLEARANCE + 2,
      ]),
    ]),
  ].sort((a, b) => a - b)
  const ys = [
    ...new Set([
      8,
      height - 8,
      ...nodes.flatMap((node) => [
        node.y - CLEARANCE - 2,
        node.y + node.height / 2,
        node.y + node.height + CLEARANCE + 2,
      ]),
    ]),
  ].sort((a, b) => a - b)
  const points = ys.flatMap((y) => xs.map((x) => ({ x, y })))
  const valid = points.map(
    (point) =>
      !obstacles.some(
        (rect) =>
          point.x > rect.x &&
          point.x < rect.x + rect.width &&
          point.y > rect.y &&
          point.y < rect.y + rect.height,
      ),
  )
  const adjacency: Array<
    Array<{ to: number; length: number; direction: number }>
  > = points.map(() => [])
  for (let index = 0; index < points.length; index++) {
    if (!valid[index]) continue
    for (const other of [
      (index % xs.length) + 1 < xs.length ? index + 1 : -1,
      index + xs.length < points.length ? index + xs.length : -1,
    ]) {
      if (
        other < 0 ||
        !valid[other] ||
        obstacles.some((rect) =>
          segmentIntersectsRectangle(points[index], points[other], rect),
        )
      )
        continue
      const direction = points[index].y === points[other].y ? 0 : 1
      const length =
        Math.abs(points[index].x - points[other].x) +
        Math.abs(points[index].y - points[other].y)
      adjacency[index].push({ to: other, length, direction })
      adjacency[other].push({ to: index, length, direction })
    }
  }
  const routes: RoutedEdge[] = edges.flatMap((edge) => {
    const from = placed.get(edge.from),
      to = placed.get(edge.to)
    if (!from || !to) return []
    const start = {
      x: from.x + from.width + CLEARANCE + 2,
      y: from.y + from.height / 2,
    }
    const end = { x: to.x - CLEARANCE - 2, y: to.y + to.height / 2 }
    const startIndex = ys.indexOf(start.y) * xs.length + xs.indexOf(start.x)
    const endIndex = ys.indexOf(end.y) * xs.length + xs.indexOf(end.x)
    const distance = new Map<number, number>([[startIndex * 2, 0]])
    const previous = new Map<number, number>()
    const heap = new MinHeap()
    heap.push(startIndex * 2, 0)
    let finish: number | undefined
    while (heap.size) {
      const { state, cost } = heap.pop()!
      if (cost !== distance.get(state)) continue
      const current = Math.floor(state / 2)
      if (current === endIndex) {
        finish = state
        break
      }
      for (const step of adjacency[current]) {
        const next = step.to * 2 + step.direction
        const candidate =
          cost + step.length + (state % 2 === step.direction ? 0 : 16)
        if (candidate >= (distance.get(next) ?? Infinity)) continue
        distance.set(next, candidate)
        previous.set(next, state)
        heap.push(next, candidate)
      }
    }
    if (finish === undefined)
      throw new Error(`No obstacle-free route for ${edge.from} -> ${edge.to}`)
    const path: Point[] = []
    for (
      let state: number | undefined = finish;
      state !== undefined;
      state = previous.get(state)
    )
      path.push(points[Math.floor(state / 2)])
    path.reverse()
    path.unshift({ x: from.x + from.width, y: start.y })
    path.push({ x: to.x, y: end.y })
    return [
      {
        ...edge,
        points: path.filter((point, index) => {
          const before = path[index - 1],
            after = path[index + 1]
          return (
            !before ||
            !after ||
            !(
              (before.x === point.x && point.x === after.x) ||
              (before.y === point.y && point.y === after.y)
            )
          )
        }),
      },
    ]
  })
  return { nodes, edges: routes, width, height }
}

export function segmentIntersectsRectangle(
  a: Point,
  b: Point,
  rect: Point & { width: number; height: number },
) {
  if (a.x === b.x)
    return (
      a.x > rect.x &&
      a.x < rect.x + rect.width &&
      Math.max(a.y, b.y) > rect.y &&
      Math.min(a.y, b.y) < rect.y + rect.height
    )
  if (a.y === b.y)
    return (
      a.y > rect.y &&
      a.y < rect.y + rect.height &&
      Math.max(a.x, b.x) > rect.x &&
      Math.min(a.x, b.x) < rect.x + rect.width
    )
  throw new Error('Graph routes must use axis-aligned segments')
}

class MinHeap {
  private items: Array<{ state: number; cost: number }> = []
  get size() {
    return this.items.length
  }
  push(state: number, cost: number) {
    this.items.push({ state, cost })
    let index = this.items.length - 1
    while (index > 0) {
      const parent = Math.floor((index - 1) / 2)
      if (this.items[parent].cost <= cost) break
      ;[this.items[parent], this.items[index]] = [
        this.items[index],
        this.items[parent],
      ]
      index = parent
    }
  }
  pop() {
    const result = this.items[0],
      last = this.items.pop()!
    if (!this.items.length) return result
    this.items[0] = last
    let index = 0
    while (index * 2 + 1 < this.items.length) {
      let child = index * 2 + 1
      if (
        child + 1 < this.items.length &&
        this.items[child + 1].cost < this.items[child].cost
      )
        child++
      if (this.items[index].cost <= this.items[child].cost) break
      ;[this.items[index], this.items[child]] = [
        this.items[child],
        this.items[index],
      ]
      index = child
    }
    return result
  }
}
