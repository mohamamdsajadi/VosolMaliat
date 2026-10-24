export default function RestrictionIcon({ name, size = 24, className = '' }) {
  return <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {name === 'company' && <><rect x="5" y="3" width="14" height="18" rx="2" /><path d="M9 7h1m4 0h1M9 11h1m4 0h1M9 15h1m4 0h1M10 21v-3h4v3M3 21h18" /></>}
    {name === 'debt' && <><rect x="3" y="5" width="18" height="14" rx="3" /><path d="M3 10h18M7 15h4m5 0h1" /></>}
    {name === 'plane' && <path d="m22 12-8-4V3a2 2 0 0 0-4 0v5l-8 4v3l8-2v6l-3 2v1l5-1 5 1v-1l-3-2v-6l8 2Z" />}
    {name === 'shield' && <><path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6l8-3Z" /><path d="m8 12 3 3 5-6" /></>}
    {name === 'clock' && <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>}
    {name === 'check' && <><circle cx="12" cy="12" r="9" /><path d="m8 12 3 3 5-6" /></>}
    {name === 'alert' && <><circle cx="12" cy="12" r="9" /><path d="M12 7v6m0 4h.01" /></>}
    {name === 'documents' && <><path d="M8 3h9l4 4v12a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z" /><path d="M16 3v5h5M3 6v11m7-5h7m-7 4h5" /></>}
    {name === 'search' && <><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 5 5" /></>}
    {name === 'refresh' && <><path d="M20 11a8 8 0 1 0-2 6M20 4v7h-7" /></>}
    {name === 'chevron' && <path d="m8 10 4 4 4-4" />}
    {name === 'close' && <path d="m6 6 12 12M18 6 6 18" />}
    {name === 'arrow' && <path d="M5 12h14m-5-5 5 5-5 5" />}
    {name === 'people' && <><circle cx="9" cy="8" r="3" /><path d="M3 21v-3a6 6 0 0 1 12 0v3M17 5a3 3 0 0 1 0 6m1 4a5 5 0 0 1 3 4v2" /></>}
    {name === 'history' && <><path d="M3 10a9 9 0 1 1 1 7M3 4v6h6M12 7v5l3 2" /></>}
  </svg>;
}
