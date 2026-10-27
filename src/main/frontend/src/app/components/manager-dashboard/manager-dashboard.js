'use client';

import FigmaShell from './figma-shell';
import DashboardOverview from './overview-dashboard';
import styles from './overview-dashboard.module.css';

export default function ManagerDashboard({ user, data, loading, onRefresh }) {
  const shellData = data?.overview ? { ...data, stats: { ...data.stats, totalIssuedExecutionPapers: data.overview.papers } } : data;
  return <FigmaShell user={user} data={shellData} loading={loading} mainClassName={styles.viewportMain} toolbar={<h1>داشبورد وصول و اجرا</h1>}>
    <DashboardOverview user={user} data={data} loading={loading} onRefresh={onRefresh} />
  </FigmaShell>;
}
