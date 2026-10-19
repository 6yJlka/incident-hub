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
export const EDGE_LANE_SPACING = 8
const CHANNEL_MARGIN = 12

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
  const columnIndex = new Map(
    depths.flatMap((depth, index) =>
      columns.get(depth)!.map((node) => [node.id, index] as const),
    ),
  )
  const validEdges = edges.filter(
    (edge) => byId.has(edge.from) && byId.has(edge.to),
  )
  const incoming = new Map<number, number[]>(),
    outgoing = new Map<number, number[]>()
  const channels = new Map<number, number[]>()
  const bypasses: number[] = []
  validEdges.forEach((edge, index) => {
    incoming.set(edge.to, [...(incoming.get(edge.to) ?? []), index])
    outgoing.set(edge.from, [...(outgoing.get(edge.from) ?? []), index])
    const exit = columnIndex.get(edge.from)!,
      entry = columnIndex.get(edge.to)! - 1
    for (const channel of new Set([exit, entry]))
      channels.set(channel, [...(channels.get(channel) ?? []), index])
    if (entry !== exit) bypasses.push(index)
  })
  const nodeHeight = (node: GraphNode) =>
    Math.max(
      NODE_HEIGHT,
      (Math.max(
        incoming.get(node.id)?.length ?? 0,
        outgoing.get(node.id)?.length ?? 0,
      ) +
        2) *
        EDGE_LANE_SPACING,
    )
  // Keep node tops and heights on an eight-pixel grid. Outgoing ports use phase 0,
  // incoming ports phase 4, so opposing horizontal stubs cannot merge in a channel.
  const roundedHeight = (node: GraphNode) =>
    Math.ceil(nodeHeight(node) / EDGE_LANE_SPACING) * EDGE_LANE_SPACING
  const columnHeights = depths.map((depth) =>
    columns
      .get(depth)!
      .reduce((sum, node) => sum + roundedHeight(node) + ROW_GAP, -ROW_GAP),
  )
  const tallest = Math.max(0, ...columnHeights)
  const gap = (channel: number) =>
    Math.max(
      COLUMN_GAP,
      CHANNEL_MARGIN * 2 +
        ((channels.get(channel)?.length ?? 1) - 1) * EDGE_LANE_SPACING,
    )
  const columnX: number[] = []
  let nextX = 24 + (channels.has(-1) ? gap(-1) : 0)
  depths.forEach((_, index) => {
    columnX.push(nextX)
    nextX += NODE_WIDTH + gap(index)
  })
  const top = bypasses.length ? 64 + bypasses.length * EDGE_LANE_SPACING : 40
  const nodes: PlacedNode[] = depths.flatMap((depth, index) => {
    let y =
      top +
      Math.floor((tallest - columnHeights[index]) / (2 * EDGE_LANE_SPACING)) *
        EDGE_LANE_SPACING
    return columns.get(depth)!.map((node) => {
      const placed = {
        ...node,
        x: columnX[index],
        y,
        width: NODE_WIDTH,
        height: roundedHeight(node),
      }
      y += placed.height + ROW_GAP
      return placed
    })
  })
  const placed = new Map(nodes.map((node) => [node.id, node]))
  const port = (
    node: PlacedNode,
    list: number[],
    edgeIndex: number,
    entry: boolean,
  ): Point => {
    const offset =
      Math.floor(
        (node.height - (list.length - 1) * EDGE_LANE_SPACING) /
          (2 * EDGE_LANE_SPACING),
      ) * EDGE_LANE_SPACING
    return {
      x: entry ? node.x : node.x + node.width,
      y:
        node.y +
        offset +
        list.indexOf(edgeIndex) * EDGE_LANE_SPACING +
        (entry ? 4 : 0),
    }
  }
  const lane = (channel: number, edgeIndex: number) => {
    const left = channel < 0 ? 24 : columnX[channel] + NODE_WIDTH
    return (
      left +
      CHANNEL_MARGIN +
      channels.get(channel)!.indexOf(edgeIndex) * EDGE_LANE_SPACING
    )
  }
  const routes: RoutedEdge[] = validEdges.map((edge, index) => {
    const from = placed.get(edge.from)!,
      to = placed.get(edge.to)!
    const start = port(from, outgoing.get(edge.from)!, index, false)
    const end = port(to, incoming.get(edge.to)!, index, true)
    const exitChannel = columnIndex.get(edge.from)!,
      entryChannel = columnIndex.get(edge.to)! - 1
    const exitX = lane(exitChannel, index),
      entryX = lane(entryChannel, index)
    const points: Point[] = [start, { x: exitX, y: start.y }]
    if (exitChannel !== entryChannel) {
      // Long, same-column and returning edges each have a private corridor above all nodes.
      const bridgeY = 40 + bypasses.indexOf(index) * EDGE_LANE_SPACING
      points.push({ x: exitX, y: bridgeY }, { x: entryX, y: bridgeY })
    }
    points.push({ x: entryX, y: end.y }, end)
    return { ...edge, points }
  })
  const last = depths.length - 1
  const width =
    last < 0
      ? 240
      : columnX[last] + NODE_WIDTH + (channels.has(last) ? gap(last) : 24)
  return { nodes, edges: routes, width, height: top + tallest + 24 }
}
