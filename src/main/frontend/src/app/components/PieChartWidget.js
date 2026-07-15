'use client'

import { PieChart, Pie, Cell, Tooltip } from 'recharts'

function toPersianDigits(str) {
  const persianDigits = '۰۱۲۳۴۵۶۷۸۹'
  return String(str).replace(/\d/g, d => persianDigits[d])
}

const SEGMENTS = [
  {
    key: 'notInformedExecutionPapers',
    label: 'برگ اجرا ابلاغ نشده',
    color: '#f97316',
  },
  {
    key: 'informedExecutionPapers',
    label: 'برگ اجرا ابلاغ شده',
    color: '#22c55e',
  },
  {
    key: 'rejectedDebtPapers',
    label: 'برگ بدهی رد شده',
    color: '#ef4444',
  },
  {
    key: 'totalIssuedExecutionPapers',
    label: 'کل برگ اجرا صادر شده',
    color: '#64748b',
  },
]

const RADIAN = Math.PI / 180

function renderCustomLabel({ cx, cy, midAngle, innerRadius, outerRadius, percent }) {
  if (percent < 0.04) return null
  const radius = innerRadius + (outerRadius - innerRadius) * 0.55
  const x = cx + radius * Math.cos(-midAngle * RADIAN)
  const y = cy + radius * Math.sin(-midAngle * RADIAN)
  return (
    <text
      x={x}
      y={y}
      fill="white"
      textAnchor="middle"
      dominantBaseline="central"
      fontSize={12}
      fontWeight={700}
    >
      {`${(percent * 100).toFixed(0)}٪`}
    </text>
  )
}

function CustomTooltip({ active, payload }) {
  if (!active || !payload || !payload.length) return null
  const { name, value, fill } = payload[0].payload
  return (
    <div
      style={{
        background: 'white',
        border: '1px solid #e5e7eb',
        borderRadius: 8,
        padding: '8px 12px',
        fontSize: 13,
        direction: 'rtl',
        boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span
          style={{
            width: 10,
            height: 10,
            borderRadius: 2,
            background: fill,
            display: 'inline-block',
          }}
        />
        <span style={{ color: '#374151' }}>{name}</span>
      </div>
      <div style={{ fontWeight: 700, color: '#1f2937', marginTop: 2 }}>
        {toPersianDigits(value.toLocaleString('en-US'))}
      </div>
    </div>
  )
}

export default function PieChartWidget({ stats }) {
  const data = SEGMENTS.map(seg => ({
    name: seg.label,
    value: Number(stats?.[seg.key] || 0),
    color: seg.color,
  }))

  const total = data.reduce((s, d) => s + d.value, 0)

  return (
    <div style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      {total === 0 ? (
        <div
          style={{
            height: 240,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#9ca3af',
            fontSize: 14,
          }}
        >
          داده‌ای برای نمایش وجود ندارد
        </div>
      ) : (
        <PieChart width={320} height={260}>
          <Pie
            data={data}
            cx={160}
            cy={125}
            outerRadius={110}
            labelLine={false}
            label={renderCustomLabel}
            dataKey="value"
            isAnimationActive={true}
          >
            {data.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={entry.color} />
            ))}
          </Pie>
          <Tooltip content={<CustomTooltip />} />
        </PieChart>
      )}

      {/* Custom legend */}
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '8px',
          marginTop: 8,
          width: '100%',
          direction: 'rtl',
          padding: '0 8px',
        }}
      >
        {data.map((item, index) => (
          <div
            key={index}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <span
              style={{
                width: 12,
                height: 12,
                borderRadius: 3,
                background: SEGMENTS[index].color,
                flexShrink: 0,
              }}
            />
            <span style={{ fontSize: 13, color: '#374151', flex: 1 }}>{item.name}</span>
            <span style={{ fontSize: 13, fontWeight: 700, color: '#1f2937' }}>
              {toPersianDigits(item.value.toLocaleString('en-US'))}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
