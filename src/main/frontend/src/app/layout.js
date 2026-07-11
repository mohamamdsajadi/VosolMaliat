import "./globals.css";
import AuthGuard from "./components/auth-guard";

export const metadata = {
  title: "دستیار هوشمند حسابرسی | سازمان امور مالیاتی کشور",
  description: "سامانه وصول و اجرای مالیاتی",
};

export default function RootLayout({ children }) {
  return (
    <html lang="fa" dir="rtl">
      <head>
        {/* Preload IranYekan font weights */}
        <link
          rel="preload"
          href="/fonts/iranyekan/iranyekanwebregular.woff"
          as="font"
          type="font/woff"
          crossOrigin="anonymous"
        />
        <link
          rel="preload"
          href="/fonts/iranyekan/iranyekanwebmedium.woff"
          as="font"
          type="font/woff"
          crossOrigin="anonymous"
        />
        <link
          rel="preload"
          href="/fonts/iranyekan/iranyekanwebbold.woff"
          as="font"
          type="font/woff"
          crossOrigin="anonymous"
        />
      </head>
      <body
        style={{
          fontFamily: "'IranYekan', 'IRANSans', Tahoma, Arial, sans-serif",
          direction: "rtl",
          textAlign: "right",
          margin: 0,
          padding: 0,
          backgroundColor: "#f4f6f9",
        }}
      >
        <AuthGuard>{children}</AuthGuard>
      </body>
    </html>
  );
}
