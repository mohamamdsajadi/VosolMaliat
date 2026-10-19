'use client';

import { Bar, BarChart, CartesianGrid, LabelList, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { formatDashboardNumber } from '../../lib/manager-dashboard';

export default function CollectionChart({ data }) {
  return (
    <div style={{ width: '100%', height: 310, direction: 'ltr' }} aria-label="نمودار عملکرد دوره‌ای واحد">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 35, right: 20, left: 0, bottom: 5 }} barGap={4}>
          <CartesianGrid stroke="#d5d9df" strokeDasharray="3 3" />
          <XAxis dataKey="label" tick={{ fill: '#183350', fontSize: 11 }} axisLine={false} tickLine={false} />
          <YAxis domain={[0, 100]} ticks={[0, 20, 40, 60, 80, 100]} tickFormatter={formatDashboardNumber} tick={{ fill: '#183350', fontSize: 12 }} axisLine={false} tickLine={false} width={38} />
          <Tooltip formatter={(value, name) => [formatDashboardNumber(value), name]} contentStyle={{ direction: 'rtl', borderRadius: 10, fontSize: 12 }} cursor={{ fill: '#eff4fa' }} />
          <Legend verticalAlign="top" align="right" iconType="square" iconSize={12} wrapperStyle={{ top: 0, fontSize: 13, direction: 'rtl' }} />
          <Bar dataKey="collected" name="وصول شده" fill="#5c93e2" maxBarSize={28}>
            <LabelList dataKey="collected" position="top" formatter={formatDashboardNumber} fill="#2c5282" fontSize={11} />
          </Bar>
          <Bar dataKey="total" name="کل" fill="#b0cdf3" maxBarSize={28}>
            <LabelList dataKey="total" position="top" formatter={formatDashboardNumber} fill="#2c5282" fontSize={11} />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
