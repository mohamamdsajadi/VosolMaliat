"use client";

import { useEffect, useState } from "react";
import Image from "next/image";

// ── Jalali date helpers ────────────────────────────────────────────────────────

const PERSIAN_MONTHS = [
  "فروردین",
  "اردیبهشت",
  "خرداد",
  "تیر",
  "مرداد",
  "شهریور",
  "مهر",
  "آبان",
  "آذر",
  "دی",
  "بهمن",
  "اسفند",
];

const PERSIAN_WEEKDAYS = [
  "یکشنبه",
  "دوشنبه",
  "سه‌شنبه",
  "چهارشنبه",
  "پنجشنبه",
  "جمعه",
  "شنبه",
];

function toPersianDigits(str) {
  const d = "۰۱۲۳۴۵۶۷۸۹";
  return String(str).replace(/\d/g, (ch) => d[ch]);
}

function toJalali(gy, gm, gd) {
  const g_d_m = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334];
  let jy = gy <= 1600 ? 0 : 979;
  gy -= gy <= 1600 ? 621 : 1600;
  const gy2 = gm > 2 ? gy + 1 : gy;
  let days =
    365 * gy +
    Math.floor((gy2 + 3) / 4) -
    Math.floor((gy2 + 99) / 100) +
    Math.floor((gy2 + 399) / 400) -
    80 +
    gd +
    g_d_m[gm - 1];
  jy += 33 * Math.floor(days / 12053);
  days %= 12053;
  jy += 4 * Math.floor(days / 1461);
  days %= 1461;
  if (days > 365) {
    jy += Math.floor((days - 1) / 365);
    days = (days - 1) % 365;
  }
  const jm =
    days < 186 ? 1 + Math.floor(days / 31) : 7 + Math.floor((days - 186) / 30);
  const jd = 1 + (days < 186 ? days % 31 : (days - 186) % 30);
  return { jy, jm, jd };
}

function formatJalaliDate(date) {
  const { jy, jm, jd } = toJalali(
    date.getFullYear(),
    date.getMonth() + 1,
    date.getDate(),
  );
  const weekday = PERSIAN_WEEKDAYS[date.getDay()];
  return `${weekday} ${toPersianDigits(jd)} ${PERSIAN_MONTHS[jm - 1]} ${toPersianDigits(jy)}`;
}

function formatTime(date) {
  return toPersianDigits(
    date.toLocaleTimeString("fa-IR", {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    }),
  );
}

// ── Component ──────────────────────────────────────────────────────────────────

/**
 * AppHeader — formal top bar for every page.
 *
 * Props:
 *   title    {string}  — role/page title shown in centre
 *   subtitle {string}  — smaller subtitle below title (optional)
 */
export default function AppHeader({ title, subtitle }) {
  const [dateStr, setDateStr] = useState("");
  const [timeStr, setTimeStr] = useState("");

  useEffect(() => {
    const updateDateTime = () => {
      const now = new Date();
      setDateStr(formatJalaliDate(now));
      setTimeStr(formatTime(now));
    };

    updateDateTime();
    const intervalId = window.setInterval(updateDateTime, 1000);
    return () => window.clearInterval(intervalId);
  }, []);

  return (
    <header
      style={{
        background: "white",
        borderBottom: "2px solid #e5e7eb",
        width: "100%",
        position: "sticky",
        top: 0,
        zIndex: 50,
        boxShadow: "0 1px 8px rgba(0,0,0,0.07)",
      }}
    >
      {/* ── Main identity row ── */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: "10px 28px",
          direction: "rtl",
          gap: 16,
        }}
      >
        {/* RIGHT block: organisation logo + text */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 14,
            flexShrink: 0,
          }}
        >
          <div
            style={{
              width: 56,
              height: 56,
              position: "relative",
              flexShrink: 0,
            }}
          >
            <Image
              src="/images/iran-tax-logo.png"
              alt="سازمان امور مالیاتی کشور"
              fill
              style={{ objectFit: "contain" }}
              priority
            />
          </div>

          <div style={{ lineHeight: 1.7 }}>
            <div
              style={{
                fontSize: 10,
                color: "#6b7280",
                fontWeight: 400,
                letterSpacing: "0.02em",
              }}
            >
              وزارت امور اقتصاد و دارایی
            </div>
            <div
              style={{
                fontSize: 13,
                color: "#15803d",
                fontWeight: 700,
                letterSpacing: "0.01em",
              }}
            >
              سازمان امور مالیاتی کشور
            </div>
            <div
              style={{
                fontSize: 10,
                color: "#9ca3af",
                fontWeight: 400,
              }}
            >
              معاونت فناوری‌های مالیاتی
            </div>
          </div>
        </div>

        {/* CENTRE: page title */}
        <div
          style={{
            textAlign: "center",
            flex: 1,
            padding: "0 16px",
            minWidth: 0,
          }}
        >
          <div
            style={{
              fontSize: 17,
              fontWeight: 700,
              color: "#1e3a5f",
              letterSpacing: "0.02em",
              whiteSpace: "nowrap",
              overflow: "hidden",
              textOverflow: "ellipsis",
            }}
          >
            {title}
          </div>
          {subtitle && (
            <div
              style={{
                fontSize: 12,
                color: "#6b7280",
                fontWeight: 400,
                marginTop: 2,
              }}
            >
              {subtitle}
            </div>
          )}
        </div>

        {/* LEFT corner: Iran flag with date/time below */}
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            minWidth: 170,
            flexShrink: 0,
            direction: "rtl",
          }}
        >
          <div
            style={{
              width: 44,
              height: 44,
              position: "relative",
              flexShrink: 0,
            }}
          >
            <Image
              src="/images/iran-flag.png"
              alt="پرچم ایران"
              fill
              style={{ objectFit: "contain", borderRadius: "50%" }}
              priority
            />
          </div>
          <div
            style={{
              fontSize: 12,
              color: "#374151",
              fontWeight: 500,
              textAlign: "center",
              lineHeight: 1.6,
              letterSpacing: "0.01em",
              marginTop: 3,
            }}
          >
            <div>{dateStr}</div>
            <div>{timeStr}</div>
          </div>
        </div>
      </div>

      {/* ── Action row: bell + logout ── */}
      <div
        style={{
          display: "flex",
          justifyContent: "flex-end",
          alignItems: "center",
          padding: "5px 28px",
          gap: 12,
          borderTop: "1px solid #f0f0f0",
          background: "#fafbfc",
          direction: "rtl",
        }}
      >
        <button
          aria-label="اعلان‌ها"
          style={{
            background: "none",
            border: "none",
            cursor: "pointer",
            fontSize: 17,
            padding: "4px 6px",
            borderRadius: 6,
            color: "#374151",
            display: "flex",
            alignItems: "center",
            transition: "background 0.15s",
          }}
          onMouseEnter={(e) => (e.currentTarget.style.background = "#f3f4f6")}
          onMouseLeave={(e) => (e.currentTarget.style.background = "none")}
        >
          🔔
        </button>

        <button
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: 6,
            padding: "4px 14px",
            background: "none",
            color: "#dc2626",
            borderRadius: 6,
            border: "1px solid #fecaca",
            cursor: "pointer",
            fontSize: 13,
            fontWeight: 600,
            fontFamily: "inherit",
            transition: "background 0.15s, border-color 0.15s",
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.background = "#fee2e2";
            e.currentTarget.style.borderColor = "#fca5a5";
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.background = "none";
            e.currentTarget.style.borderColor = "#fecaca";
          }}
        >
          <span style={{ fontSize: 14, lineHeight: 1 }}>&#x2190;</span>
          خروج
        </button>
      </div>
    </header>
  );
}
