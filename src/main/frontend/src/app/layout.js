import '@bpmn-io/form-js-viewer/dist/assets/form-js.css'
import './globals.css'
import './styles/embedded-form-override.css'

export const metadata = {
  title: 'میز کار درخواست‌ها',
  description: 'دموی مدیریت و تکمیل درخواست‌ها',
}

export default function RootLayout({ children }) {
  return (
    <html lang="fa" dir="rtl">
      <body>{children}</body>
    </html>
  )
}
