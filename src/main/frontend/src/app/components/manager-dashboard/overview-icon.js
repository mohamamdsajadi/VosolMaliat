// Stroke icons for light report cards, matching the application's existing icon weight.
const PATHS = {
  refresh: <><path d="M20 7v5h-5M4 17v-5h5" /><path d="M6.1 7a7 7 0 0 1 11.5-1L20 9M4 15l2.4 3A7 7 0 0 0 17.9 17" /></>,
  user: <><circle cx="12" cy="8" r="3" /><path d="M5 21v-3a7 7 0 0 1 14 0v3" /></>,
  document: <><path d="M14 3H5v18h14V8Z" /><path d="M14 3v5h5M8 12h8M8 16h6" /></>,
  receipt: <><path d="M5 3v18l3-2 4 2 4-2 3 2V3Z" /><path d="M8 7h8M8 11h8M8 15h5" /></>,
  check: <><circle cx="12" cy="12" r="9" /><path d="m8 12 3 3 5-6" /></>,
  chart: <><path d="M4 4v16h16M8 16v-4M12 16V8M16 16v-6" /></>,
  reports: <><path d="M4 4v16h16M8 16l4-6 4 2 4-7" /></>,
  lock: <><rect x="5" y="10" width="14" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3" /></>,
  shield: <><path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6Z" /><path d="m8 12 3 3 5-6" /></>,
  gavel: <><path d="m9 4 7 7M6 7l7 7M7 6l3-3 7 7-3 3ZM11 12l-7 7M12 21h9" /></>,
  info: <><circle cx="12" cy="12" r="9" /><path d="M12 11v6M12 7v.1" /></>,
};
export default function OverviewIcon({ name }) {
  return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{PATHS[name] || PATHS.chart}</svg>;
}
