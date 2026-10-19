import { render, screen } from '@testing-library/react'
import { expect, it } from 'vitest'
import { SignalSeverity } from './SignalSeverity'

it.each([
  ['INFO', 1],
  ['WARNING', 2],
  ['CRITICAL', 3],
] as const)('shows %s with %s monochrome divisions', (value, active) => {
  const { container } = render(<SignalSeverity value={value} />)
  expect(screen.getByText(value)).toBeInTheDocument()
  expect(container.querySelectorAll('[data-active]')).toHaveLength(3)
  expect(container.querySelectorAll('[data-active=true]')).toHaveLength(active)
  expect(container.querySelector('[aria-hidden=true]')).toBeInTheDocument()
})
