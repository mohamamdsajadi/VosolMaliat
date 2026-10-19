"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import dynamic from "next/dynamic";
import AppShell from "./components/app-shell";
import RefreshButton from "./components/refresh-button";
import { apiFetch } from "./lib/api";
import { fetchCurrentUser } from "./lib/security";
import { isAdminUser, isGeneralOfficeHead } from "./lib/navigation";
import ManagerDashboard from "./components/manager-dashboard/manager-dashboard";
import { fetchManagerDashboard } from "./lib/manager-dashboard";

const PieChartComponent = dynamic(() => import("./components/PieChartWidget"), {
  ssr: false,
  loading: () => <div className="dashboard-chart-loading">در حال بارگذاری نمودار...</div>
});

function toPersianDigits(str) {
  const persianDigits = "۰۱۲۳۴۵۶۷۸۹";
  return String(str).replace(/\d/g, (d) => persianDigits[d]);
}

function formatRial(amount) {
  if (amount == null || amount === "") return "نیازمند تکمیل اطلاعات";
  const [integer, fraction] = String(amount).split('.')
  return `${toPersianDigits(integer.replace(/\B(?=(\d{3})+(?!\d))/g, ',') + (fraction == null ? '' : '.' + fraction))} ریال`;
}

const EXECUTION_STATS = [
  {
    key: "notInformedExecutionPapers",
    label: "برگ اجرا ابلاغ نشده",
    tone: "warning"
  },
  {
    key: "informedExecutionPapers",
    label: "برگ اجرا ابلاغ شده",
    tone: "success"
  },
  {
    key: "rejectedDebtPapers",
    label: "برگ بدهی رد شده",
    tone: "danger"
  },
  {
    key: "totalIssuedExecutionPapers",
    label: "کل برگ اجرا صادر شده",
    tone: "neutral"
  }
];

const DEBT_SUMMARY = [
  {
    key: "debtSumNotInformedExecutionPapers",
    label: "جمع بدهی برگ اجرا ابلاغ نشده"
  },
  {
    key: "debtSumInformedExecutionPapers",
    label: "جمع بدهی برگ اجرا ابلاغ شده"
  },
  {
    key: "debtSumRejectedDebtPapers",
    label: "جمع بدهی برگ بدهی رد شده"
  },
  {
    key: "debtSumTotalIssuedExecutionPapers",
    label: "جمع بدهی کل برگ اجرا صادر شده"
  }
];

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState(null);
  const [accessError, setAccessError] = useState('');
  const [accessAttempt, setAccessAttempt] = useState(0);

  useEffect(() => {
    let active = true;
    fetchCurrentUser().then(current => {
      if (!active) return;
      if (isGeneralOfficeHead(current) || isAdminUser(current)) setUser(current);
      else router.replace('/tasks');
    }).catch(() => { if (active) setAccessError('دریافت اطلاعات کاربر ناموفق بود.'); });
    return () => { active = false; };
  }, [router, accessAttempt]);

  if (accessError) return <div className="dashboard-error" role="alert"><strong>{accessError}</strong><button className="primary-action" onClick={() => { setAccessError(''); setAccessAttempt(value => value + 1); }}>تلاش مجدد</button></div>;
  if (!user) return <div className="dashboard-loading" role="status">در حال دریافت میزکار...</div>;
  return isGeneralOfficeHead(user) ? <GeneralOfficeDashboard user={user} /> : <AdminDashboardPage />;
}

function GeneralOfficeDashboard({ user }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const refresh = useCallback(async () => {
    setLoading(true);
    try { setData(await fetchManagerDashboard()); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { refresh(); }, [refresh]);
  return <ManagerDashboard user={user} data={data} loading={loading} onRefresh={refresh} />;
}

function AdminDashboardPage() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadStats = useCallback(async () => {
    try {
      const response = await apiFetch('/api/v1/dashboard/stats');
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      setStats(await response.json());
    } catch (err) {
      setError(err.message || 'دریافت اطلاعات ناموفق بود.');
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchStats = () => {
    setLoading(true);
    setError(null);
    loadStats();
  };

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  const router = useRouter();

  return (
    <AppShell
      active="processes"
      eyebrow="میزکار"
      title="میزکار مدیریتی وصول"
      subtitle="نمای مدیریتی وضعیت برگ‌های اجرا، ابلاغ و مبالغ بدهی در فرایند وصول."
      actions={<RefreshButton onClick={fetchStats} loading={loading}>بروزرسانی گزارش</RefreshButton>}
    >
      <div className="dashboard-page">
        {loading && (
          <div className="dashboard-loading">
            <div className="spinner" />
            <span>در حال دریافت گزارش مدیریتی...</span>
          </div>
        )}

        {!loading && error && (
          <div className="dashboard-error">
            <strong>دریافت اطلاعات با خطا روبه‌رو شد.</strong>
            <span>{error}</span>
            <button className="primary-action" type="button" onClick={fetchStats}>تلاش مجدد</button>
          </div>
        )}

        {!loading && !error && stats && (
          <>
            <section className="dashboard-hero-panel">
              <div>
                <p className="eyebrow">گزارش لحظه‌ای</p>
                <h2>وضعیت جاری برگ‌های اجرا</h2>
                <span>شاخص‌های اصلی برای پایش ابلاغ، رد برگ بدهی و مجموع برگ‌های صادرشده.</span>
              </div>
              <button className="dashboard-hero-action" type="button" onClick={() => router.push("/execution-papers")}>
                مشاهده برگ‌های اجرا
              </button>
            </section>

            <section className="dashboard-stat-grid">
              {EXECUTION_STATS.map((item) => (
                <article className={`dashboard-stat-card dashboard-stat-card--${item.tone}`} key={item.key}>
                  <span>{item.label}</span>
                  <strong>{toPersianDigits((stats[item.key] || 0).toLocaleString("en-US"))}</strong>
                </article>
              ))}
            </section>

            <section className="dashboard-main-grid">
              <div className="dashboard-card dashboard-chart-card">
                <div className="dashboard-card__header">
                  <div>
                    <p className="eyebrow">نمودار</p>
                    <h2>گزارش برگ‌های اجرا</h2>
                  </div>
                </div>
                <PieChartComponent stats={stats} />
              </div>

              <div className="dashboard-card">
                <div className="dashboard-card__header">
                  <div>
                    <p className="eyebrow">مطالبات</p>
                    <h2>وضعیت وصول مطالبات</h2>
                  </div>
                </div>
                <div className="dashboard-financial-list">
                  {DEBT_SUMMARY.map((item) => (
                    <div className="dashboard-financial-row" key={item.key}>
                      <span>{item.label}</span>
                      <strong>{formatRial(stats[item.key])}</strong>
                    </div>
                  ))}
                </div>
              </div>
            </section>
          </>
        )}
      </div>
    </AppShell>
  );
}
