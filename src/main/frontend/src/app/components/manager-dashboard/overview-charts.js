'use client';

import { useState } from 'react';
import { Cell, Pie, PieChart, ResponsiveContainer, Sector, Tooltip } from 'recharts';
import { formatDashboardNumber as number } from '../../lib/manager-dashboard';
import { chartWeights } from '../../lib/dashboard-reporting';
import styles from './overview-dashboard.module.css';
import { REPORT_COLORS } from './overview-colors';

export function ReportPieChart({ items, money = false, onSelect, label, secondaryItems = [], onSecondarySelect, summaryCount }) {
  const [activeIndex, setActiveIndex] = useState(null);
  const [activeSecondary, setActiveSecondary] = useState(null);
  const weights = chartWeights(items.map(item => item.value));
  const secondaryWeights = chartWeights(secondaryItems.map(item => item.value));
  if (!weights || !weights.some(value => value > 0)) return <div className={styles.chartEmpty} role="status">{weights ? 'داده‌ای برای رسم نمودار وجود ندارد.' : 'تفکیک نمودار در دسترس نیست.'}</div>;
  const chartData = items.map((item, index) => ({ ...item, weight: weights[index], color: item.color || REPORT_COLORS[index % REPORT_COLORS.length] }));
  const ringData = secondaryItems.map((item, index) => ({ ...item, weight: secondaryWeights?.[index] || 0, color: item.color || REPORT_COLORS[index % REPORT_COLORS.length] }));
  const hasRing = Boolean(secondaryWeights?.some(value => value > 0));
  const shape = (props, highlighted, callback, layer = 'primary') => {
    const entry = props.payload || props;
    return <g className={styles.pieSector} data-pie-key={entry.key} data-pie-layer={layer} data-inner-radius={props.innerRadius} data-outer-radius={props.outerRadius} role={callback ? 'button' : 'img'} tabIndex={callback ? 0 : undefined} aria-label={`${entry.label}: ${number(entry.value)} ${money ? 'ریال' : 'برگ'}`}
      onKeyDown={event => { if (callback && (event.key === 'Enter' || event.key === ' ')) { event.preventDefault(); event.stopPropagation(); callback(entry); } }}>
      <Sector {...props} tabIndex={-1} outerRadius={props.outerRadius + (highlighted ? 3 : 0)} strokeWidth={highlighted ? 3 : 2} />
    </g>;
  };
  return <figure className={styles.pieFrame} data-chart-kind="pie" data-chart-style="doughnut" data-category-count={items.length} aria-label={label}>
    <div className={styles.pieCanvas}>
      <ResponsiveContainer width="100%" height="100%"><PieChart>
        <Pie data={chartData} dataKey="weight" nameKey="label" innerRadius={hasRing ? '45%' : '56%'} outerRadius={hasRing ? '75%' : '94%'} startAngle={90} endAngle={-270} paddingAngle={items.length > 5 ? .4 : .7} cornerRadius={0} stroke="white" strokeWidth={2} isAnimationActive={false}
          activeIndex={activeIndex ?? -1} activeShape={props => shape(props, true, onSelect)} inactiveShape={props => shape(props, false, onSelect)}
          onMouseEnter={(_entry, index) => setActiveIndex(index)} onMouseLeave={() => setActiveIndex(null)}
          onClick={entry => onSelect?.(entry.payload || entry)} style={{ cursor: onSelect ? 'pointer' : 'default' }}>
          {chartData.map(item => <Cell key={item.key} fill={item.color} />)}
        </Pie>
        {hasRing && <Pie data={ringData} dataKey="weight" nameKey="label" innerRadius="82%" outerRadius="96%" startAngle={90} endAngle={-270} paddingAngle={.7} cornerRadius={0} stroke="white" strokeWidth={2} isAnimationActive={false}
          activeIndex={activeSecondary ?? -1} activeShape={props => shape(props, true, onSecondarySelect, 'notification')} inactiveShape={props => shape(props, false, onSecondarySelect, 'notification')}
          onMouseEnter={(_entry, index) => setActiveSecondary(index)} onMouseLeave={() => setActiveSecondary(null)} onClick={entry => onSecondarySelect?.(entry.payload || entry)} style={{ cursor: onSecondarySelect ? 'pointer' : 'default' }}>
          {ringData.map(item => <Cell key={item.key} fill={item.color} />)}
        </Pie>}
        <Tooltip wrapperStyle={{ zIndex: 5 }} content={({ active, payload }) => {
          const entry = payload?.[0]?.payload;
          return active && entry ? <div className={styles.pieTooltip}><strong><span style={{ background: entry.color }} />{entry.label}</strong><span>{number(entry.value)} {money ? 'ریال' : 'برگ'}</span><small>{number(entry.weight.toFixed(1))}٪ از مقادیر نمایش‌داده‌شده</small>{entry.balance !== undefined && <><span>مانده: {number(entry.balance)} ریال</span><span>پرداخت: {number(entry.payments)} ریال</span></>}</div> : null;
        }} />
      </PieChart></ResponsiveContainer>
    </div>
    {summaryCount != null && <figcaption className={`${styles.pieCaption} ${styles.chartSummary}`} data-chart-summary><strong>{number(summaryCount)}</strong><span>برگ اجرا</span></figcaption>}
  </figure>;
}

export function OfficePieChart({ items, onSelect }) {
  if (!items.length) return <div className={styles.chartEmpty}>واحدی برای مقایسه وجود ندارد.</div>;
  const chartItems = items.map((item, index) => ({ ...item, value: item.count, balance: item.amount, payments: item.paid, color: REPORT_COLORS[index % REPORT_COLORS.length] }));
  return <div className={styles.officePieLayout}>
    <ReportPieChart items={chartItems} summaryCount={items.reduce((total, item) => total + item.count, 0)} onSelect={onSelect} label="سهم واحدها از برگ‌های اجرای واحدهای نمایش‌داده‌شده" />
  </div>;
}
