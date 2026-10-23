import Image from 'next/image';

// Original SVG exports from the execution-paper frames. Keep intrinsic dimensions.
const ASSETS = {
  homeOutline: ['7eae6.svg', 24], documentActive: ['26582.svg', 24],
  previous: ['feaf5.svg', 12], next: ['9dea4.svg', 12],
  checkbox: ['4bcbc.svg', 20], checked: ['1ea88.svg', 20], sort: ['d6fb1.svg', 12],
  refresh: ['3f105.svg', 20], search: ['3f18d.svg', 24], down: ['031a5.svg', 16],
  document: ['6528f.svg', 24], close: ['9726b.svg', 36], calendar: ['ae3cd.svg', 16], timer: ['b33a4.svg', 16],
  pendingOuter: ['517cd.svg', 56], pendingInner: ['2177f.svg', 40],
  activeOuter: ['fa090.svg', 56], activeInner: ['8af25.svg', 40],
  doneOuter: ['c5244.svg', 56], doneInner: ['9a285.svg', 40],
  closed: ['57918.svg', 24], auction: ['59093.svg', 24], task: ['903b0.svg', 24],
  shield: ['73ce8.svg', 24], receipt: ['27718.svg', 24], done: ['84259.svg', 40],
  pendingDot: ['1de32.svg', 6], activeDot: ['13807.svg', 6], doneDot: ['b0082.svg', 6],
  stepArrow: ['168f7.svg', 24], add: ['afb2e.svg', 20],
};

export default function PaperIcon({ name, className = '' }) {
  const [file, size] = ASSETS[name];
  return <Image className={className} src={`/images/execution-papers/${file}`} width={size} height={size} alt="" aria-hidden="true" />;
}
