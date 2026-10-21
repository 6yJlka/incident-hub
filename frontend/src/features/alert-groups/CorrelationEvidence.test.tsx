import { render, screen } from '@testing-library/react'
import { expect, it } from 'vitest'
import { CorrelationEvidence } from './CorrelationEvidence'

it('renders the generated PageRank evidence union with its numerical explanation', () => {
  render(
    <CorrelationEvidence
      serviceCode={(id) => `SERVICE_${id}`}
      evidence={[
        {
          type: 'PAGERANK_SCORE',
          serviceId: 42,
          score: 0.75,
          anomalyWeight: 3,
          damping: 0.85,
          iterations: 50,
          residual: 1e-13,
        },
      ]}
    />,
  )
  expect(
    screen.getByText(/PageRank SERVICE_42: score 0.7500/),
  ).toHaveTextContent(
    'вес сигналов 3, damping 0.85, итераций 50, невязка 1.00e-13',
  )
})
