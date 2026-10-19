import styles from './AlertGroups.module.css'

export function DecisionChoices({
  name,
  label,
  options,
  value,
  error,
  onChange,
}: {
  name: string
  label: string
  options: string[]
  value: string
  error?: string
  onChange: (value: string) => void
}) {
  return (
    <div className={styles.choiceField}>
      <span id={`${name}-label`} className={styles.fieldLabel}>
        {label}
      </span>
      <div
        role="radiogroup"
        aria-required="true"
        aria-labelledby={`${name}-label`}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${name}-error` : undefined}
        className={styles.choices}
      >
        {options.map((option, index) => (
          <button
            key={option}
            type="button"
            role="radio"
            aria-checked={value === option}
            data-field={index === 0 ? name : undefined}
            tabIndex={value === option || (!value && index === 0) ? 0 : -1}
            className={
              name === 'severity' ? styles[option.toLowerCase()] : undefined
            }
            onClick={() => onChange(option)}
            onKeyDown={(event) => {
              if (
                ![
                  'ArrowLeft',
                  'ArrowRight',
                  'ArrowUp',
                  'ArrowDown',
                  'Home',
                  'End',
                ].includes(event.key)
              )
                return
              event.preventDefault()
              const next =
                event.key === 'Home'
                  ? 0
                  : event.key === 'End'
                    ? options.length - 1
                    : (index +
                        (event.key === 'ArrowLeft' || event.key === 'ArrowUp'
                          ? -1
                          : 1) +
                        options.length) %
                      options.length
              onChange(options[next])
              ;(
                event.currentTarget.parentElement?.children[next] as HTMLElement
              )?.focus()
            }}
          >
            {option}
          </button>
        ))}
      </div>
      {error && (
        <span id={`${name}-error`} role="alert" className={styles.fieldError}>
          {error}
        </span>
      )}
    </div>
  )
}
