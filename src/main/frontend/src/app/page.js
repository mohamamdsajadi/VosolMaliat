"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import dynamic from "next/dynamic";
import AppShell from "./components/app-shell";

const PieChartComponent = dynamic(() => import("./components/PieChartWidget"), {
  ssr: false,
  loading: () => (
    <div
      style={{
        height: 260,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        color: "#9ca3af"
      }}
    >
      در حال بارگذاری نمودار...
    </div>
  )
});

function toPersianDigits(str) {
  const persianDigits = "۰۱۲۳۴۵۶۷۸۹";
  return String(str).replace(/\d/g, (d) => persianDigits[d]);
}

function formatRial(amount) {
  if (amount == null || amount === "") return "۰ ریال";
  return toPersianDigits(Number(amount).toLocaleString("en-US")) + " ریال";
}

function Card({ children, style }) {
  return (
    <div
      style={{
        background: "white",
        borderRadius: 12,
        boxShadow: "0 1px 4px rgba(0,0,0,0.08)",
        border: "1px solid #e5e7eb",
        padding: "24px",
        ...style
      }}
    >
      {children}
    </div>
  );
}

function CardTitle({ children }) {
  return (
    <h2
      style={{
        fontSize: 16,
        fontWeight: 700,
        color: "#1e3a5f",
        marginBottom: 20,
        textAlign: "right",
        paddingBottom: 12,
        borderBottom: "2px solid #eff6ff"
      }}
    >
      {children}
    </h2>
  );
}

function StatRow({ badgeBg, badgeColor, value, label }) {
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        gap: "12px",
        direction: "rtl",
        padding: "8px 0",
        borderBottom: "1px solid #f9fafb"
      }}
    >
      <span
        style={{
          display: "inline-flex",
          alignItems: "center",
          justifyContent: "center",
          minWidth: 48,
          padding: "3px 12px",
          borderRadius: 9999,
          background: badgeBg,
          color: badgeColor,
          fontWeight: 700,
          fontSize: 15,
          flexShrink: 0
        }}
      >
        {toPersianDigits(value ?? 0)}
      </span>
      <span style={{ color: "#374151", fontSize: 14 }}>{label}</span>
    </div>
  );
}

function FinancialRow({ label, value }) {
  return (
    <div
      style={{
        paddingBottom: 14,
        marginBottom: 14,
        borderBottom: "1px solid #f3f4f6"
      }}
    >
      <div
        style={{
          fontSize: 12,
          color: "#6b7280",
          marginBottom: 4,
          textAlign: "right"
        }}
      >
        {label}
      </div>
      <div
        style={{
          fontSize: 17,
          fontWeight: 700,
          color: "#1f2937",
          textAlign: "right"
        }}
      >
        {value}
      </div>
    </div>
  );
}

function ActionButton({ onClick, children, dark }) {
  return (
    <button
      onClick={onClick}
      style={{
        marginTop: 20,
        padding: "11px 20px",
        background: dark ? "#1e293b" : "#2563eb",
        color: "white",
        borderRadius: 8,
        border: "none",
        cursor: "pointer",
        width: "100%",
        fontSize: 14,
        fontWeight: 600,
        transition: "opacity 0.15s"
      }}
      onMouseEnter={(event) => (event.currentTarget.style.opacity = "0.85")}
      onMouseLeave={(event) => (event.currentTarget.style.opacity = "1")}
    >
      {children}
    </button>
  );
}

export default function DashboardPage() {
  const router = useRouter();
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchStats = () => {
    setLoading(true);
    setError(null);
    fetch("/api/v1/dashboard/stats")
      .then((response) => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
      })
      .then((data) => {
        setStats(data);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchStats();
  }, []);

  return (
    <AppShell
      active="processes"
      eyebrow="میزکار"
      title="دستیار هوشمند حسابرسی"
    >
      <div style={{ maxWidth: 1400, margin: "0 auto" }}>
        <h1
          style={{
            fontSize: 24,
            fontWeight: 800,
            color: "#1e3a5f",
            marginBottom: 24,
            textAlign: "right"
          }}
        >
          میزکار
        </h1>

        {loading && (
          <div
            style={{
              display: "flex",
              justifyContent: "center",
              alignItems: "center",
              height: 240
            }}
          >
            <div className="spinner" />
          </div>
        )}

        {!loading && error && (
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: 16,
              padding: "60px 40px"
            }}
          >
            <div style={{ fontSize: 36 }}>!</div>
            <p style={{ color: "#dc2626", fontSize: 15, textAlign: "center" }}>
              خطا در دریافت اطلاعات: {error}
            </p>
            <button
              onClick={fetchStats}
              style={{
                padding: "10px 28px",
                background: "#2563eb",
                color: "white",
                borderRadius: 8,
                border: "none",
                cursor: "pointer",
                fontSize: 14,
                fontWeight: 600
              }}
            >
              تلاش مجدد
            </button>
          </div>
        )}

        {!loading && !error && stats && (
          <>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "2fr 3fr",
                gap: 24,
                marginBottom: 24
              }}
            >
              <Card>
                <CardTitle>به میزکار مامورین مالیاتی خوش آمدید.</CardTitle>
                <div style={{ display: "flex", flexDirection: "column", gap: 0 }}>
                  <StatRow
                    badgeBg="#ffe4e6"
                    badgeColor="#be123c"
                    value={stats.unassignedExecutionPapers}
                    label="برگ اجرا تخصیص نیافته"
                  />
                  <StatRow
                    badgeBg="#dcfce7"
                    badgeColor="#15803d"
                    value={stats.arrestOrderIssued}
                    label="دستور بازداشت صادر شده"
                  />
                  <StatRow
                    badgeBg="#f3f4f6"
                    badgeColor="#374151"
                    value={stats.arrestOrderNotIssued}
                    label="دستور بازداشت صادر نشده"
                  />
                  <StatRow
                    badgeBg="#ffedd5"
                    badgeColor="#c2410c"
                    value={stats.awaitingArrestOrder}
                    label="در انتظار صدور دستور بازداشت"
                  />
                </div>
                <ActionButton onClick={() => router.push("/execution-papers")}>
                  مشاهده برگ‌های اجرا
                </ActionButton>
              </Card>

              <Card>
                <CardTitle>گزارش تخصیص</CardTitle>
                <PieChartComponent stats={stats} />
              </Card>
            </div>

            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: 24
              }}
            >
              <Card>
                <CardTitle>وضعیت وصول مطالبات</CardTitle>
                <div style={{ direction: "rtl" }}>
                  <FinancialRow
                    label="جمع بدهی در انتظار دستور بازداشت اموال"
                    value={formatRial(stats.totalDebtAwaitingSeizure)}
                  />
                  <FinancialRow
                    label="جمع بدهی صدور دستور بازداشت اموال"
                    value={formatRial(stats.totalDebtWithSeizureOrder)}
                  />
                  <FinancialRow
                    label="مبالغ وصول شده"
                    value={formatRial(stats.collectedAmounts)}
                  />
                  <FinancialRow
                    label="مبالغ وصول نشده"
                    value={formatRial(stats.uncollectedAmounts)}
                  />
                </div>
              </Card>

              <Card>
                <CardTitle>اقدامات من</CardTitle>
                <div style={{ display: "flex", flexDirection: "column", gap: 0 }}>
                  <StatRow
                    badgeBg="#dbeafe"
                    badgeColor="#1d4ed8"
                    value={stats.myActionsNew}
                    label="جدید"
                  />
                  <StatRow
                    badgeBg="#ffedd5"
                    badgeColor="#c2410c"
                    value={stats.myActionsAwaitingCompletion}
                    label="در انتظار تکمیل"
                  />
                  <StatRow
                    badgeBg="#dcfce7"
                    badgeColor="#15803d"
                    value={stats.myActionsCompleted}
                    label="تکمیل شده"
                  />
                  <StatRow
                    badgeBg="#ffe4e6"
                    badgeColor="#be123c"
                    value={stats.myActionsCancelled}
                    label="لغو شده"
                  />
                </div>
                <ActionButton dark onClick={() => router.push("/execution-papers")}>
                  مشاهده اقدامات من
                </ActionButton>
              </Card>
            </div>
          </>
        )}
      </div>
    </AppShell>
  );
}
