export default function BusinessFeatureIcon({ name, size = 24, className = '' }) {
  return <svg width={size} height={size} className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {name === 'statement' && <><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6Z" /><path d="M14 2v6h6M8 12h8M8 16h3m-3 3h8" /></>}
    {name === 'payment' && <><rect x="2" y="4" width="20" height="16" rx="3" /><path d="M2 9h20M6 15h4m5 0h3" /></>}
    {name === 'schedule' && <><rect x="3" y="4" width="18" height="17" rx="2" /><path d="M7 2v4m10-4v4M3 10h18M7 14h2m4 0h4m-10 4h2m4 0h4" /></>}
    {name === 'guarantee' && <><path d="m12 2 8 3v6c0 5-8 11-8 11S4 16 4 11V5l8-3Z" /><path d="m8 11 3 3 5-5" /></>}
    {name === 'enforcement' && <><path d="m14 3 7 7-4 4-7-7 4-4Zm-3 7-8 8a2 2 0 0 0 3 3l8-8M12 22h10M10 3l11 11" /></>}
    {name === 'scales' && <><path d="M12 3v18m-5 0h10M3 7h18M6 7l-4 8h8L6 7Zm12 0-4 8h8l-4-8Z" /><circle cx="12" cy="5" r="2" /></>}
    {name === 'bank' && <><path d="m2 7 10-5 10 5H2Zm1 14h18M5 10v7m5-7v7m4-7v7m5-7v7M2 18h20" /></>}
    {name === 'property' && <><path d="m2 11 10-9 10 9M5 9v12h14V9M9 21v-7h6v7" /></>}
    {name === 'car' && <><path d="m5 6-3 7v6h20v-6l-3-7H5ZM2 13h20M5 19v3m14-3v3M6 16h2m8 0h2" /></>}
    {name === 'phone' && <><rect x="6" y="2" width="12" height="20" rx="2" /><path d="M10 5h4m-3 14h2" /></>}
    {name === 'history' && <><path d="M3 10a9 9 0 1 1 1 7M3 4v6h6M12 7v5l3 2" /></>}
    {name === 'search' && <><circle cx="10" cy="10" r="7" /><path d="m15 15 6 6" /></>}
    {name === 'refresh' && <><path d="M20 11a8 8 0 1 0-2 6M20 4v7h-7" /></>}
    {name === 'chevron' && <path d="m8 10 4 4 4-4" />}
  </svg>;
}
