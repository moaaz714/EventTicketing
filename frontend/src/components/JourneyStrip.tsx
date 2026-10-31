import { STEPS } from '../api/journey'
import type { JourneyView } from '../api/journey'
import type { BookingStatus } from '../api/types'

interface Props {
  view: JourneyView
  labelled?: boolean
  appSet?: BookingStatus[]
}

const STEP_STATUS: (BookingStatus | null)[] = [null, null, 'CHECKED_IN', 'PAYMENT_PENDING', 'PAID']

export function JourneyStrip({ view, labelled = false, appSet = [] }: Props) {
  const { reached, phase, failed } = view
  const summary = failed
    ? `${view.label}`
    : `Step ${Math.min(reached + (phase === 'settled' ? 0 : 1), STEPS.length)} of ${STEPS.length}: ${view.label}`

  return (
    <ol className={`strip ${labelled ? 'strip-labelled' : ''}`} aria-label={summary}>
      {STEPS.map((step, i) => {
        const done = i < reached
        const next = i === reached && phase !== 'settled'
        const broke = failed && i === reached
        const cls = broke ? 'cell-failed' : done ? 'cell-done' : next ? `cell-next cell-${phase}` : ''
        const byApp = STEP_STATUS[i] && appSet.includes(STEP_STATUS[i]!)
        return (
          <li key={step} className={`cell ${cls}`}>
            <span className="cell-block" />
            {labelled && (
              <span className="cell-label">
                {step}
                {byApp && <sup title="Set by this app; the backend has no step for it">*</sup>}
              </span>
            )}
          </li>
        )
      })}
    </ol>
  )
}
