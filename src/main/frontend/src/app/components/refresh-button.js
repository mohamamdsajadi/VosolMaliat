'use client'

export default function RefreshButton({ onClick, disabled, loading, children = 'بروزرسانی', className = '' }) {
  return (
    <button
      type="button"
      className={`ghost-action refresh-action ${className}`.trim()}
      onClick={onClick}
      disabled={disabled || loading}
    >
      <span className="refresh-action__icon" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <path d="M20 6v5h-5" />
          <path d="M4 18v-5h5" />
          <path d="M6.1 9A7 7 0 0 1 18.4 6.6L20 11" />
          <path d="M17.9 15A7 7 0 0 1 5.6 17.4L4 13" />
        </svg>
      </span>
      <span>{loading ? 'در حال بروزرسانی' : children}</span>
    </button>
  )
}
