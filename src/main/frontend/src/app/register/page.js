'use client';

import { useMemo, useState } from 'react';
import styles from './page.module.css';

const EMPTY_FORM = {
  firstName: '',
  lastName: '',
  nationalCode: '',
  phoneNumber: '',
  password: '',
  confirmationPassword: '',
};

const COMMON_PASSWORDS = new Set([
  '123456', '1234567', '12345678', '123456789', '1234567890',
  'password', 'password1', 'qwerty', 'qwerty123', 'abcdef',
  '111111', '000000', 'instagram', 'admin123',
]);

const PERSIAN_DIGITS = '۰۱۲۳۴۵۶۷۸۹';
const ARABIC_DIGITS = '٠١٢٣٤٥٦٧٨٩';

function toEnglishDigits(value) {
  return value.replace(/[۰-۹٠-٩]/g, (digit) => {
    const persianIndex = PERSIAN_DIGITS.indexOf(digit);
    return String(persianIndex >= 0 ? persianIndex : ARABIC_DIGITS.indexOf(digit));
  });
}

function passwordChecks(password) {
  const normalized = password.toLowerCase();
  const isSimple = COMMON_PASSWORDS.has(normalized)
    || /(123456|654321|abcdef|qwerty)/.test(normalized)
    || /^(.)\1{5,}$/.test(normalized);

  return {
    length: password.length > 6 && password.length <= 128,
    notSimple: password.length > 0 && !isSimple,
  };
}

function FieldIcon({ type }) {
  const paths = {
    user: <><circle cx="12" cy="8" r="3.4"/><path d="M5.5 19c.6-3.3 3-5.1 6.5-5.1s5.9 1.8 6.5 5.1"/></>,
    id: <><rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="8" cy="11" r="2"/><path d="M5.5 16c.4-1.6 1.3-2.4 2.5-2.4s2.1.8 2.5 2.4M13.5 10h4.5M13.5 14h4.5"/></>,
    phone: <path d="M7.2 3.8 10 7.3 8.4 9.4c1.2 2.5 3 4.3 5.5 5.5l2.1-1.6 3.5 2.8-.7 3c-.3 1.1-1.3 1.8-2.4 1.6C9.5 19.5 4.3 14.3 3.2 7.6 3 6.5 3.7 5.5 4.8 5.2l2.4-.7Z"/>,
    lock: <><rect x="4.5" y="10" width="15" height="10" rx="2"/><path d="M8 10V7.5a4 4 0 0 1 8 0V10M12 14v2.5"/></>,
  };
  return <svg viewBox="0 0 24 24" aria-hidden="true">{paths[type]}</svg>;
}

function FormField({ label, name, icon, error, children }) {
  return (
    <label className={`${styles.field} ${error ? styles.fieldError : ''}`}>
      <span className={styles.fieldLabel}>{label}</span>
      <span className={styles.inputShell}>
        <span className={styles.fieldIcon}><FieldIcon type={icon} /></span>
        {children}
      </span>
      {error && <span className={styles.errorText}>{error}</span>}
    </label>
  );
}

export default function RegisterPage() {
  const [form, setForm] = useState(EMPTY_FORM);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const checks = useMemo(() => passwordChecks(form.password), [form.password]);

  const update = (name, value) => {
    const nextValue = ['nationalCode', 'phoneNumber'].includes(name)
      ? toEnglishDigits(value).replace(/\D/g, '')
      : value;
    setForm((current) => ({ ...current, [name]: nextValue }));
    setErrors((current) => ({ ...current, [name]: '' }));
    setMessage('');
  };

  const validate = () => {
    const nextErrors = {};
    if (!form.firstName.trim()) nextErrors.firstName = 'نام را وارد کنید.';
    if (!form.lastName.trim()) nextErrors.lastName = 'نام خانوادگی را وارد کنید.';
    if (!/^\d{10}$/.test(form.nationalCode)) nextErrors.nationalCode = 'کد ملی باید دقیقاً ۱۰ رقم باشد.';
    if (!/^09\d{9}$/.test(form.phoneNumber)) nextErrors.phoneNumber = 'شماره موبایل باید ۱۱ رقم و با ۰۹ شروع شود.';
    if (!checks.length) nextErrors.password = 'رمز عبور باید بین ۷ تا ۱۲۸ کاراکتر باشد.';
    else if (!checks.notSimple) nextErrors.password = 'این رمز عبور بیش از حد ساده است.';
    if (form.password !== form.confirmationPassword) nextErrors.confirmationPassword = 'تکرار رمز عبور یکسان نیست.';
    if (!form.confirmationPassword) nextErrors.confirmationPassword = 'تکرار رمز عبور را وارد کنید.';
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const submit = async (event) => {
    event.preventDefault();
    if (!validate()) return;
    setSubmitting(true);
    setMessage('');
    try {
      const response = await fetch('/api/v1/test/registrations', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
      });
      const body = await response.json().catch(() => ({}));
      if (!response.ok) {
        if (body.fieldErrors) setErrors(body.fieldErrors);
        throw new Error(body.message || 'ثبت اطلاعات انجام نشد. دوباره تلاش کنید.');
      }
      setMessage(body.message || 'ثبت‌نام آزمایشی با موفقیت انجام شد.');
      setForm(EMPTY_FORM);
      setErrors({});
    } catch (error) {
      setMessage(error.message || 'ارتباط با سرور برقرار نشد.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className={styles.page}>
      <div className={styles.glowOne} />
      <div className={styles.glowTwo} />
      <section className={styles.shell}>
        <aside className={styles.intro}>
          <div className={styles.brandMark}>و</div>
          <div>
            <span className={styles.kicker}>سامانه وصول مالیات</span>
            <h1>حساب شما، شروع یک مسیر ساده‌تر</h1>
            <p>اطلاعات پایه را وارد کنید تا حساب آزمایشی شما در چند ثانیه ساخته شود.</p>
          </div>

          <div className={styles.featureList}>
            <div><span>۰۱</span><p><strong>سریع و روشن</strong><small>فرم کوتاه، بازخورد فوری و بدون مرحله اضافه</small></p></div>
            <div><span>۰۲</span><p><strong>کاملاً فارسی</strong><small>تجربه‌ای راست‌چین و سازگار با موبایل</small></p></div>
          </div>
        </aside>

        <div className={styles.formPanel}>
          <header className={styles.formHeader}>
            <span className={styles.step}>ثبت‌نام / ۰۱</span>
            <h2>ساخت حساب جدید</h2>
            <p>همه فیلدها الزامی هستند.</p>
          </header>

          <form onSubmit={submit} noValidate className={styles.form}>
            <div className={styles.twoColumns}>
              <FormField label="نام" name="firstName" icon="user" error={errors.firstName}>
                <input value={form.firstName} onChange={(e) => update('firstName', e.target.value)} maxLength={80} autoComplete="given-name" placeholder="مثلاً محمد" />
              </FormField>
              <FormField label="نام خانوادگی" name="lastName" icon="user" error={errors.lastName}>
                <input value={form.lastName} onChange={(e) => update('lastName', e.target.value)} maxLength={100} autoComplete="family-name" placeholder="مثلاً رضایی" />
              </FormField>
            </div>

            <div className={styles.twoColumns}>
              <FormField label="کد ملی" name="nationalCode" icon="id" error={errors.nationalCode}>
                <input dir="ltr" value={form.nationalCode} onChange={(e) => update('nationalCode', e.target.value)} maxLength={10} inputMode="numeric" autoComplete="off" placeholder="۰۰۱۲۳۴۵۶۷۸" />
              </FormField>
              <FormField label="شماره موبایل" name="phoneNumber" icon="phone" error={errors.phoneNumber}>
                <input dir="ltr" value={form.phoneNumber} onChange={(e) => update('phoneNumber', e.target.value)} maxLength={11} inputMode="tel" autoComplete="tel" placeholder="۰۹۱۲۱۲۳۴۵۶۷" />
              </FormField>
            </div>

            <FormField label="رمز عبور" name="password" icon="lock" error={errors.password}>
              <input dir="ltr" type={showPassword ? 'text' : 'password'} value={form.password} onChange={(e) => update('password', e.target.value)} maxLength={128} autoComplete="new-password" placeholder="حداقل ۷ کاراکتر" />
              <button type="button" className={styles.showButton} onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? 'پنهان کردن رمز' : 'نمایش رمز'}>
                {showPassword ? 'پنهان' : 'نمایش'}
              </button>
            </FormField>

            <FormField label="تکرار رمز عبور" name="confirmationPassword" icon="lock" error={errors.confirmationPassword}>
              <input dir="ltr" type={showPassword ? 'text' : 'password'} value={form.confirmationPassword} onChange={(e) => update('confirmationPassword', e.target.value)} maxLength={128} autoComplete="new-password" placeholder="رمز را دوباره وارد کنید" />
            </FormField>

            {message && <div className={message.includes('موفقیت') ? styles.success : styles.formMessage} role="status">{message}</div>}

            <button type="submit" className={styles.submitButton} disabled={submitting}>
              <span>{submitting ? 'در حال ثبت اطلاعات...' : 'ساخت حساب آزمایشی'}</span>
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14M13 6l6 6-6 6" /></svg>
            </button>
          </form>
        </div>
      </section>
      <footer className={styles.footer}>محیط تست داخلی <span /> نسخه ۱.۰</footer>
    </main>
  );
}
