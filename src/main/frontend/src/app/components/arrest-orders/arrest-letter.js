'use client';
import { arrestDate, digits } from '../../lib/arrest-orders';
import { formatDashboardNumber as money } from '../../lib/manager-dashboard';
import styles from './arrest-orders.module.css';

const show = value => value == null || value === '' ? '—' : value;

export default function ArrestLetter({ detail }) {
  const { letter, order } = detail;
  if (!letter?.paper) return <div className={styles.empty}>پیش‌نویس پس از انتخاب سرپرست هیئت اجرا آماده می‌شود.</div>;
  const { paper, taxpayer = {}, executionPaper: execution = {}, finalTaxDebtPaper: debtPaper = {}, notification = {} } = letter;
  const issued = order.issuanceStatus === 'ISSUED';
  return <article className={styles.letter} id="arrest-letter" aria-label={issued ? 'نامه دستور بازداشت اموال' : 'پیش‌نویس دستور بازداشت اموال'} dir="rtl">
    <header className={styles.letterHeader}><img src="/images/iran-tax-logo.png" alt="سازمان امور مالیاتی کشور" /><div><small>بسمه تعالی</small><h2>دستور بازداشت اموال</h2><span>سازمان امور مالیاتی کشور</span></div><dl><div><dt>شماره</dt><dd>{digits(order.letterNumber || 'پس از تأیید')}</dd></div><div><dt>تاریخ</dt><dd>{arrestDate(order.issuedAt, false)}</dd></div><div><dt>صفحه</dt><dd>۱ از ۱</dd></div></dl></header>
    {!issued && <p className={styles.draftMark}>پیش‌نویس — در انتظار تأیید صدور</p>}
    <div className={styles.letterOffice}><div><b>اداره کل امور مالیاتی: </b>{show(execution.mainTaxBureauName)}</div><div><b>اداره امور مالیاتی: </b>{show(execution.taxBureauName)}</div></div>
    <div className={styles.letterOffice}><div><b>نشانی اداره: </b>{show(execution.taxAffairsOfficeAddress || execution.generalDirectorateAddress)}</div><div><b>کد پستی: </b>{digits(execution.taxAffairsOfficePostalCode || execution.generalDirectoratePostalCode)}<br /><b>تلفن: </b>{digits(execution.taxAffairsOfficePhone || execution.generalDirectoratePhone)}</div></div>
    <p className={styles.recipient}>{letter.recipientRole || 'مسئول هیئت اجرایی اداره'}<br /><strong>{show(letter.recipientName || letter.supervisorName)}</strong></p>
    <table className={styles.letterTable}><tbody>
      <tr><th colSpan="2">مشخصات مؤدی</th><th>نوع شخصیت: {show(paper.taxpayerType)}</th></tr>
      <tr><td>شناسه مؤدی: {digits(taxpayer.tin || paper.taxpayerCode)}</td><td colSpan="2">شماره شناسایی ملی / شماره ملی: {digits(paper.taxpayerNationalId)}</td></tr>
      <tr><td colSpan="3">نام شخص حقوقی / نام و نام خانوادگی: <b>{show(paper.taxpayerName)}</b></td></tr>
      <tr><td colSpan="3">نام تجاری / شهرت کسبی: {show(taxpayer.tradeName)}</td></tr>
      <tr><td colSpan="3">نشانی: {show(taxpayer.address)}</td></tr>
      <tr><td>شماره پستی: {digits(taxpayer.postalCode)}</td><td colSpan="2">تلفن: {digits(taxpayer.phone)}</td></tr>
    </tbody></table>
    <p className={styles.letterText}>نظر به اینکه مؤدی به مشخصات فوق مبلغ <b>{money(letter.debtAmount)}</b> ریال بدهی به شرح جدول زیر دارد و با توجه به اینکه برگ اجرای شماره <b>{digits(paper.uniqueCode)}</b> مورخ <b>{arrestDate(paper.issuanceDate, false)}</b> در تاریخ <b>{arrestDate(notification.electronicNotificationDate, false)}</b> ابلاغ گردیده و در موعد مقرر نسبت به پرداخت یا ترتیب پرداخت آن اقدام ننموده است، لذا به استناد ماده ۲۱۱ قانون مالیات‌های مستقیم مقرر می‌شود با توجه به مقررات و آیین‌نامه مربوط، تا سقف مبلغ بدهی به شرح جدول زیر از اموال مؤدی، اعم از منقول و غیرمنقول، بازداشت نمایید.</p>
    <table className={styles.letterTable}><thead><tr><th>ردیف</th><th>منبع</th><th>دوره</th><th>سال</th><th>کد شعبه</th><th>نوع بدهی</th><th>مبلغ مانده (ریال)</th></tr></thead><tbody>
      <tr><td>۱</td><td>{show(execution.taxSourceLabel || paper.taxType)}</td><td>{digits(paper.taxPeriod ?? debtPaper.taxPeriod)}</td><td>{digits(paper.taxYear ?? debtPaper.taxYear)}</td><td>{digits(execution.taxBureauId)}</td><td>مالیات</td><td>{money(letter.debtAmount)}</td></tr>
      <tr><td colSpan="6">جمع کل بدهی‌ها</td><td>{money(letter.debtAmount)}</td></tr>
      <tr><td colSpan="6">اضافه می‌شود {digits(letter.surchargeRate)}٪ کل بدهی موضوع ماده ۲۱۱ ق.م.م</td><td>{money(letter.surchargeAmount)}</td></tr>
      <tr className={styles.letterTotal}><th colSpan="6">جمع کل بدهی</th><th>{money(letter.totalAmount)}</th></tr>
    </tbody></table>
    <div className={styles.letterSignatures}><div><b>صادرکننده</b><span>نام و نام خانوادگی: {show(letter.preparedBy)}</span><span>سمت: {show(letter.issuerRole)}</span><small>مهر و امضاء</small></div><div><b>تأییدکننده</b><span>نام و نام خانوادگی: {show(letter.approvedBy)}</span><span>سمت: {show(letter.approverRole)}</span><small>مهر و امضاء</small></div></div>
    <footer className={styles.letterFooter}>مبالغ از آخرین بدهی ذخیره‌شده در زمان تهیه پیش‌نویس • {arrestDate(letter.debtInquiredAt)}</footer>
  </article>;
}

export async function printArrestLetter() {
  const source = document.getElementById('arrest-letter');
  if (!source) return;
  const frame = document.createElement('iframe');
  frame.title = 'چاپ دستور بازداشت اموال';
  frame.style.cssText = 'position:fixed;inset:0;width:0;height:0;border:0';
  document.body.appendChild(frame);
  const doc = frame.contentDocument;
  doc.documentElement.lang = 'fa'; doc.documentElement.dir = 'rtl';
  for (const style of document.querySelectorAll('style,link[rel="stylesheet"]')) doc.head.appendChild(style.cloneNode(true));
  const printStyle = doc.createElement('style');
  printStyle.textContent = '@page{size:A4;margin:12mm}html,body{margin:0!important;padding:0!important;min-height:0!important;height:auto!important;background:white;font-family:IranYekan,IRANSans,Tahoma,sans-serif}article{box-shadow:none!important;border:0!important;margin:0!important;width:100%!important;min-height:260mm!important;box-sizing:border-box}';
  doc.head.appendChild(printStyle); doc.body.appendChild(source.cloneNode(true));
  // Wait for stylesheet/font/image loading before opening the print dialog.
  await Promise.all([...doc.querySelectorAll('link[rel="stylesheet"]')].map(link => link.sheet ? Promise.resolve() : new Promise(resolve => { link.onload = resolve; link.onerror = resolve; setTimeout(resolve, 3000); })));
  await Promise.all([...doc.images].map(img => img.decode?.().catch(() => {}) || Promise.resolve()));
  await doc.fonts.ready;
  frame.contentWindow.addEventListener('afterprint', () => frame.remove(), { once: true });
  frame.contentWindow.focus(); frame.contentWindow.print();
  setTimeout(() => frame.remove(), 60000);
}
