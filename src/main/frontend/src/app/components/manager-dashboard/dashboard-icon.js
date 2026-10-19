import Image from 'next/image';

// Original SVG exports from Figma frame 324:8876. Preserve their intrinsic dimensions.
const ASSETS = {
  home: ['ca095.svg', 24], document: ['15c18.svg', 24], lock: ['b8a33.svg', 24],
  receipt: ['5aa04.svg', 24], shield: ['407bd.svg', 24], list: ['085c0.svg', 24],
  gavel: ['dc23d.svg', 24], reports: ['1498b.svg', 24], settings: ['50a2a.svg', 24],
  close: ['ecc4e.svg', 36], logo: ['02f46.svg', 52], user: ['465aa.svg', 48],
  down: ['8a76f.svg', 24], bell: ['f2bed.svg', 32], calendar: ['50359.svg', 20],
  briefcase: ['0084c.svg', 20], alert: ['8bed5.svg', 20], check: ['7b0a9.svg', 20],
  archive: ['96333.svg', 20], info: ['b8b36.svg', 24], chart: ['af45e.svg', 24],
  up: ['15ef1.svg', 16], calendarFilled: ['3a2ec.svg', 16], tick: ['770cf.svg', 24],
  blueBackground: ['67fc3.svg', 32], redBackground: ['1fe22.svg', 32],
  greenBackground: ['4a00a.svg', 32], purpleBackground: ['a2653.svg', 32],
  activityLock: ['ca9d6.svg', 24], activityReceipt: ['ecfe3.svg', 24],
  activityShield: ['9fa1a.svg', 24], activityGavel: ['ac6e2.svg', 24],
};

export default function DashboardIcon({ name, className = '' }) {
  const asset = ASSETS[name];
  if (!asset) return <span className={className} aria-hidden="true">☰</span>;
  return <Image className={className} src={`/images/dashboard/${asset[0]}`} width={asset[1]} height={asset[1]} alt="" aria-hidden="true" />;
}
