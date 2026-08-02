import Link from 'next/link';
import AppShell from '../components/app-shell';

const TEST_SERVICES = [
  {
    href: '/admin/fake-sanim-audit',
    eyebrow: 'SANIM Audit',
    title: 'زیرسامانه حسابرسی بدهی سنیم',
    description: 'ثبت رویدادهای بدهی، جریمه، پرداخت و اعتبار و استعلام آخرین مانده مؤدی.',
    action: 'ورود به حسابرسی سنیم'
  },
  {
    href: '/admin/fake-sanim',
    eyebrow: 'Fake SANIM',
    title: 'سامانه سنیم آزمایشی',
    description: 'مشاهده پیام‌های ارسالی به سنیم، تایید دریافت و ساخت برگ بدهی تصادفی.',
    action: 'ورود به سنیم آزمایشی'
  },
  {
    href: '/admin/fake-electronic-notification',
    eyebrow: 'Fake Eblagh',
    title: 'سامانه ابلاغ الکترونیک آزمایشی',
    description: 'مشاهده درخواست‌های ابلاغ و ارسال پاسخ غیرهمزمان تاریخ ابلاغ به فرایند.',
    action: 'ورود به ابلاغ آزمایشی'
  },
  {
    href: '/admin/fake-official-gazette',
    eyebrow: 'Fake Official Gazette',
    title: 'روزنامه رسمی آزمایشی',
    description: 'مشاهده درخواست استخراج اعضای هیئت‌مدیره و ارسال دستی پاسخ غیرهمزمان.',
    action: 'ورود به روزنامه رسمی'
  },
  {
    href: '/admin/fake-restrictions',
    eyebrow: 'Fake Restrictions',
    title: 'سامانه اعمال محدودیت آزمایشی',
    description: 'مشاهده فهرست اشخاص فیلترشده و تایید دریافت درخواست اعمال محدودیت.',
    action: 'ورود به سامانه محدودیت'
  }
];

export default function TestFrontPage() {
  return (
    <AppShell
      active="test-front"
      title="Test Front"
      eyebrow="سامانه‌ها و فرایندهای آزمایشی"
      contextItems={[
        `ابزارهای آزمایشی: ${TEST_SERVICES.length.toLocaleString('fa-IR')}`
      ]}
    >
      <section className="test-front-hero">
        <p className="eyebrow">محیط تست یکپارچه</p>
        <h2>ابزارهای تست و سامانه‌های Fake</h2>
        <span>همه ورودی‌های آزمایشی از این صفحه در دسترس هستند و در منوی اصلی فقط همین مرکز نمایش داده می‌شود.</span>
      </section>

      <section className="data-entry-choice-grid test-front-grid">
        {TEST_SERVICES.map((service) => (
          <Link className="data-entry-choice" href={service.href} key={service.href}>
            <p className="eyebrow">{service.eyebrow}</p>
            <h2>{service.title}</h2>
            <span>{service.description}</span>
            <strong className="data-entry-choice-button">{service.action}</strong>
          </Link>
        ))}
      </section>
    </AppShell>
  );
}
