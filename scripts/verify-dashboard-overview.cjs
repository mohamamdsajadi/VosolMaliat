// UI contract check with intercepted API responses; backend aggregates are tested in Java.
// Run with Node 22+ against a running frontend: FRONTEND_URL=http://localhost:3210.
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const assert = require('node:assert/strict');
const base = process.env.FRONTEND_URL || 'http://localhost:3210';
const output = path.resolve(__dirname, '../temp');
const browser = spawn(process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe', [
  '--headless=new', '--no-sandbox', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
  '--remote-debugging-port=9338', '--user-data-dir=' + fs.mkdtempSync(path.join(os.tmpdir(), 'vosol-overview-')), 'about:blank',
], { windowsHide: true, stdio: 'ignore' });
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
let ws, sequence = 0, failOverview = false, failDetails = false, empty = false, unknown = false, allowed = true;
const pending = new Map(), errors = [], requests = [];
const labels = ['تخصیص نیافته', 'تخصیص یافته', 'ارسال برای ابلاغ', 'ابلاغ شده', 'رد ابلاغ', 'ابلاغ شده', 'عدم ابلاغ', 'در انتظار دستور بازداشت', 'دستور بازداشت صادر شده', 'دستور بازداشت صادر نشده', 'تکمیل شده', 'لغو شده'];
const stateKeys = ['UNASSIGNED', 'ASSIGNED', 'INFORM_SENT', 'INFORMED', 'INFORM_REJECTED', 'NOTIFIED', 'NOTIFICATION_FAILED', 'AWAITING_ARREST_ORDER', 'ARREST_ORDER_ISSUED', 'ARREST_ORDER_NOT_ISSUED', 'COMPLETED', 'CANCELLED'];
const rows = Array.from({ length: 24 }, (_, index) => ({
  id: index + 1, uniqueCode: `1405-${index + 1}`, issuanceDate: '2026-10-01T10:00:00', taxpayerCode: `TIN-${Math.floor(index / 2)}`,
  taxpayerName: `شرکت نمونه ${index + 1}`, nationalId: `10000000${String(index).padStart(3, '0')}`,
  stateLabel: labels[index % labels.length], officeName: ['تهران', 'اصفهان', 'فارس'][index % 3],
  remaining: index ? '100' : '9007199254740993.02', contribution: index ? '100' : '9007199254740993.02',
}));
const total = '9007199254743293.02';
const bucket = (key, label, count, amount = total) => ({ key, label, count, amount, paid: '2400', available: true });
function overview() {
  return { scopeLabel: 'همه اداره‌های کل قابل مشاهده', taxpayers: empty ? 0 : 12, papers: empty ? 0 : 24, canViewDetails: allowed,
    balances: { liability: empty ? '0' : total, penalties: empty ? '0' : '1800000000000000', credits: empty ? '0' : '125000000000', remissions: empty ? '0' : '3700000000000.25', paid: empty ? '0' : '3382219720528724.65', remaining: unknown ? null : empty ? '0' : total },
    states: stateKeys.map((key, i) => bucket(key, labels[i], empty ? 0 : 2)),
    notifications: [bucket('NOTIFIED', 'ابلاغ‌شده', empty ? 0 : 16), bucket('NOT_NOTIFIED', 'ابلاغ‌نشده', empty ? 0 : 8)],
    offices: empty ? [] : ['تهران', 'اصفهان', 'فارس', 'خراسان رضوی', 'آذربایجان شرقی', 'مازندران', 'البرز', 'گیلان', 'قم', 'کرمان'].map((label, i) => bucket(String(i + 1), 'اداره کل امور مالیاتی ' + label, i < 4 ? 3 : 2)),
    enforcement: [bucket('PAPERS', 'برگ‌های اجرا', empty ? 0 : 24), bucket('ISSUED', 'دستور بازداشت صادر شده', empty ? 0 : 8),
      { key: 'SEIZURE', label: 'توقیف شده', count: null, available: false }, { key: 'AUCTION', label: 'فروش رفته', count: null, available: false }],
  };
}
function send(method, params = {}) {
  const id = ++sequence;
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { pending.delete(id); reject(Error('Timeout: ' + method)); }, 15000);
    pending.set(id, { resolve, reject, timer }); ws.send(JSON.stringify({ id, method, params }));
  });
}
async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
  if (result.exceptionDetails) throw Error(JSON.stringify(result.exceptionDetails));
  return result.result.value;
}
async function waitFor(expression) {
  for (let attempt = 0; attempt < 120; attempt++) { if (await evaluate(`Boolean(${expression})`)) return; await delay(100); }
  throw Error('Condition failed: ' + expression + '\n' + await evaluate('document.body.innerText.slice(0,2200)'));
}
async function navigate() { await send('Page.navigate', { url: base + '/' }); }
async function clickText(text, selector = 'button') {
  await evaluate(`Array.from(document.querySelectorAll(${JSON.stringify(selector)})).find(el=>el.textContent.includes(${JSON.stringify(text)})).click()`);
}
async function screenshot(name) {
  await evaluate('document.fonts.ready'); await delay(200);
  // Capture the actual viewport: page-origin clips can shift RTL screenshots.
  const { data } = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false });
  fs.writeFileSync(path.join(output, name), Buffer.from(data, 'base64'));
}
async function assertDialogCentered() {
  const bounds = await evaluate('(()=>{const r=document.querySelector("dialog[open]").getBoundingClientRect();return {x:r.left+r.width/2,y:r.top+r.height/2,viewportX:innerWidth/2,viewportY:innerHeight/2,top:r.top,bottom:r.bottom,left:r.left,right:r.right,width:innerWidth,height:innerHeight}})()');
  assert.ok(Math.abs(bounds.x-bounds.viewportX)<=1 && Math.abs(bounds.y-bounds.viewportY)<=1, JSON.stringify(bounds));
  assert.ok(bounds.top>=0&&bounds.left>=0&&bounds.right<=bounds.width+1&&bounds.bottom<=bounds.height+1, JSON.stringify(bounds));
}
async function respond(event) {
  const url = new URL(event.request.url); requests.push(url.pathname + url.search);
  let data, status = 200;
  switch (url.pathname) {
    case '/api/v1/auth/me': data = { username: 'dashboard-test', fullName: 'کاربر بررسی داشبورد', businessRole: 'ADMIN', roles: ['ADMIN'], groups: ['ADMIN'], permissions: allowed ? ['EXECUTION_PAPER_LIST_VIEW', 'EXECUTION_PAPER_DETAIL_VIEW'] : [], organization: { generalDirectorateName: 'تهران' } }; break;
    case '/api/v1/auth/config': data = { provider: 'mock', workflow: 'mock' }; break;
    case '/api/v1/dashboard/stats': data = { totalIssuedExecutionPapers: 24, myActionsAwaitingCompletion: 4, myActionsCompleted: 8, arrestOrderPerformance: { percent: 66, completed: 8, total: 12 } }; break;
    case '/api/v1/cartable/my-tasks': data = []; break;
    case '/api/v1/dashboard/pending-arrest-papers': {
      const page = Number(url.searchParams.get('page') || 0), size = Number(url.searchParams.get('size') || 5);
      data = { content: rows.slice(page * size, (page + 1) * size).map(row => ({ paper: { id: row.id, uniqueCode: row.uniqueCode, taxpayerName: row.taxpayerName, taxpayerNationalId: row.nationalId, totalDebt: row.remaining }, status: 'READY_FOR_ISSUANCE' })), totalPages: Math.ceil(rows.length / size), totalElements: rows.length }; break;
    }
    case '/api/v1/dashboard/overview': data = failOverview ? { message: 'unavailable' } : overview(); status = failOverview ? 503 : 200; break;
    case '/api/v1/dashboard/details': {
      if (failDetails) { status = 503; data = { message: 'unavailable' }; break; }
      const key = url.searchParams.get('key'), group = url.searchParams.get('group');
      const selected = empty ? [] : group === 'state' ? rows.filter((_, i) => stateKeys[i % stateKeys.length] === key)
        : group === 'enforcement' && key === 'ISSUED' ? rows.slice(0, 8)
        : group === 'enforcement' && key === 'NOT_ISSUED' ? rows.slice(8)
        : group === 'notification' ? (key === 'NOTIFIED' ? rows.slice(0, 16) : rows.slice(16)) : rows;
      const page = Number(url.searchParams.get('page') || 0), size = Number(url.searchParams.get('size') || 10);
      data = { content: selected.slice(page * size, (page + 1) * size), totalElements: selected.length, totalPages: Math.ceil(selected.length / size), page, size, amount: unknown ? null : total }; break;
    }
    default: data = { message: 'Unexpected API request: ' + url.pathname }; status = 404; errors.push(data.message);
  }
  await send('Fetch.fulfillRequest', { requestId: event.requestId, responseCode: status,
    responseHeaders: [{ name: 'Content-Type', value: 'application/json; charset=utf-8' }], body: Buffer.from(JSON.stringify(data)).toString('base64') });
}
async function checks() {
  await navigate();
  await waitFor('document.querySelectorAll(".recharts-pie-sector").length>=32');
  assert.ok(await evaluate('Array.from(document.querySelectorAll("[title]")).some(el=>el.title.includes("۹٬۰۰۷٬۱۹۹٬۲۵۴٬۷۴۳٬۲۹۳٫۰۲"))'));
  assert.equal(await evaluate(`document.querySelector('[aria-label="آمار کلی"] [data-money=true] strong').textContent`), '۹٬۰۰۷٬۱۹۹٬۲۵۴٬۷۴۳٬۲۹۳٫۰۲');
  assert.ok(await evaluate('!/(میلیارد|میلیون)/.test(document.querySelector("[data-dashboard-overview]").innerText)'));
  assert.equal(await evaluate('Array.from(document.querySelectorAll("button")).filter(el=>el.textContent.includes("اطلاعات در دسترس نیست")).length'), 2);
  assert.ok(await evaluate('Array.from(document.querySelectorAll("button")).filter(el=>el.textContent.includes("اطلاعات در دسترس نیست")).every(el=>el.disabled)'));
  for (const [width, height] of [[1366,768], [1280,720], [1440,900], [1920,1080]]) {
    await send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: false }); await delay(250);
    const geometry = await evaluate(`({ width: innerWidth, height: innerHeight, pageWidth: document.documentElement.scrollWidth, pageHeight: document.documentElement.scrollHeight,
      panels: Array.from(document.querySelectorAll('[data-report-panel]')).map(el=>({id:el.dataset.reportPanel, height:el.clientHeight, content:el.scrollHeight, bottom:el.getBoundingClientRect().bottom})),
      root: (()=>{const el=document.querySelector('main').parentElement,r=el.getBoundingClientRect();return {left:r.left,right:r.right,scrollX:window.scrollX}})(),
      officeCount: document.querySelectorAll('[data-report-panel=offices] [data-pie-layer=primary]').length, stateCount: document.querySelectorAll('[class*=stateLegend] button').length })`);
    assert.ok(geometry.pageWidth <= width && geometry.pageHeight <= height + 1, JSON.stringify(geometry));
    assert.ok(geometry.root.left >= -1 && geometry.root.right <= width + 1, JSON.stringify(geometry));
    assert.equal(geometry.officeCount, 10); assert.equal(geometry.stateCount, 12);
    const moneyGeometry = await evaluate(`Array.from(document.querySelectorAll('[data-money=true] strong bdi')).map(el=>({text:el.textContent,width:el.getBoundingClientRect().width,available:el.parentElement.clientWidth}))`);
    assert.ok(moneyGeometry.every(item=>item.width<=item.available+1),JSON.stringify(moneyGeometry));
    const typography = await evaluate(`({heading:parseFloat(getComputedStyle(document.querySelector('[data-report-panel=composition] h2')).fontSize),weight:Number(getComputedStyle(document.querySelector('[data-report-panel=composition] h2')).fontWeight),label:parseFloat(getComputedStyle(document.querySelector('[data-report-panel=adjustments] [class*=legendItem]')).fontSize)})`);
    assert.ok(typography.heading >= 16 && typography.weight >= 800 && typography.label >= 12, JSON.stringify(typography));
    assert.equal(await evaluate('document.querySelectorAll("[data-report-panel]>header p").length'), 0);
    assert.ok(await evaluate('Array.from(document.querySelectorAll("[data-report-panel]")).every(panel=>!/[٪%]/.test(panel.innerText))'));
    const piePanels = await evaluate(`['composition','adjustments','payment','status','offices','enforcement'].map(id=>({id,pies:document.querySelectorAll('[data-report-panel='+id+'] [data-chart-kind=pie]').length,slices:document.querySelectorAll('[data-report-panel='+id+'] .recharts-pie-sector').length}))`);
    assert.ok(piePanels.every(panel => panel.pies === 1 && panel.slices > 0), JSON.stringify(piePanels));
    assert.equal(await evaluate('document.querySelectorAll("[data-chart-style=doughnut]").length'), 6);
    const doughnutGeometry = await evaluate(`Array.from(document.querySelectorAll('[data-chart-style=doughnut]')).map(chart=>{const sector=chart.querySelector('[data-pie-layer=primary]'),summary=chart.querySelector('[data-chart-summary]')?.getBoundingClientRect(),canvas=chart.querySelector('[class*=pieCanvas]').getBoundingClientRect(),figure=chart.getBoundingClientRect(),inner=Number(sector.dataset.innerRadius),outer=Number(sector.dataset.outerRadius);return {ratio:inner/outer,captionOutside:!summary||(summary.top>=canvas.bottom-1&&summary.bottom<=figure.bottom+1&&summary.width<=figure.width+1)}})`);
    assert.ok(doughnutGeometry.every(chart=>chart.ratio>.58&&chart.ratio<.62&&chart.captionOutside), JSON.stringify(doughnutGeometry));
    assert.equal(await evaluate('document.querySelectorAll("[class*=doughnutCenter]").length'), 0);
    assert.equal(await evaluate('document.querySelectorAll("[data-chart-style=doughnut] radialGradient").length'), 0);
    assert.equal(await evaluate('document.querySelectorAll("[data-pie-layer=notification]").length'), 2);
    assert.equal(await evaluate('document.querySelectorAll("[class*=officeLegend],[class*=officeRow]").length'), 0);
    assert.ok(geometry.panels.every(panel => panel.content <= panel.height + 1 && panel.bottom <= height), JSON.stringify(geometry));
    await screenshot(`dashboard-single-view-${width}.png`);
  }
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 900, deviceScaleFactor: 1, mobile: false });
  await screenshot('dashboard-overview-desktop.png');
  const hoverPoint = await evaluate('(()=>{const r=document.querySelector("[data-report-panel=composition] [class*=pieCanvas]").getBoundingClientRect();return {x:r.x+r.width/2+Math.min(r.width,r.height)*.37,y:r.y+r.height/2-3}})()');
  await send('Input.dispatchMouseEvent', { type: 'mouseMoved', x: hoverPoint.x, y: hoverPoint.y });
  await waitFor('document.querySelector("[class*=pieTooltip]")?.textContent.includes("٪")');
  assert.ok(await evaluate('document.querySelector("[class*=pieTooltip]").textContent.includes("ریال")'));
  await send('Input.dispatchMouseEvent', { type: 'mouseMoved', x: 0, y: 0 });
  await waitFor('!document.querySelector("[class*=pieTooltip]")');
  assert.ok(await evaluate('Array.from(document.querySelectorAll("[data-report-panel]")).every(panel=>!/[٪%]/.test(panel.innerText))'));
  await evaluate('document.querySelectorAll(".recharts-pie-sector")[0].dispatchEvent(new MouseEvent("click",{bubbles:true}))');
  await waitFor('document.querySelector("[aria-labelledby=dashboard-details-heading][aria-busy=false] tbody tr a")');
  assert.ok(requests.some(url => url.includes('group=finance') && url.includes('key=liability')));
  assert.ok(await evaluate('document.querySelector("dialog[open]") && document.documentElement.scrollHeight<=innerHeight'));
  await clickText('بستن جزئیات');
  assert.equal(await evaluate('document.querySelectorAll("[class*=adjustmentBar],[class*=statusStrip],[class*=officeTrack]").length'), 0);
  for (const [text, key] of [['بخشودگی‌ها', 'remissions'], ['اعتبارات', 'credits']]) {
    await clickText(text, '[data-report-panel=adjustments] button');
    await waitFor('document.querySelector("[aria-labelledby=dashboard-details-heading][aria-busy=false] tbody tr a")');
    assert.ok(requests.some(url => url.includes('group=finance') && url.includes('key=' + key)));
    await clickText('بستن جزئیات');
  }
  for (const [panel, group, key] of [['payment', 'finance', 'paid'], ['status', 'state', 'UNASSIGNED'], ['offices', 'office', '1'], ['enforcement', 'enforcement', 'ISSUED']]) {
    await evaluate(`document.querySelector('[data-report-panel=${panel}] .recharts-pie-sector').dispatchEvent(new MouseEvent('click',{bubbles:true}))`);
    await waitFor('document.querySelector("[aria-labelledby=dashboard-details-heading][aria-busy=false] tbody tr a")');
    assert.ok(requests.some(url => url.includes('group=' + group) && url.includes('key=' + key)));
    await clickText('بستن جزئیات');
  }
  for (const key of ['NOTIFIED', 'NOT_NOTIFIED']) {
    await evaluate(`document.querySelector('[data-pie-layer=notification][data-pie-key=${key}]').dispatchEvent(new MouseEvent('click',{bubbles:true}))`);
    await waitFor('document.querySelector("[aria-labelledby=dashboard-details-heading][aria-busy=false] tbody tr a")');
    assert.ok(requests.some(url => url.includes('group=notification') && url.includes('key=' + key)));
    await clickText('بستن جزئیات');
  }
  await evaluate(`document.querySelector('[data-report-panel=enforcement] [data-pie-key=NOT_ISSUED]').dispatchEvent(new MouseEvent('click',{bubbles:true}))`);
  await waitFor('document.querySelector("[aria-labelledby=dashboard-details-heading][aria-busy=false] tbody tr a")');
  assert.equal(await evaluate('document.querySelector("[aria-labelledby=dashboard-details-heading] tbody a").getAttribute("href")'), '/execution-papers/9');
  await clickText('بستن جزئیات');
  await evaluate(`document.querySelector('[data-report-panel=offices] [data-pie-key="1"]').focus()`);
  await send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Enter', code: 'Enter', windowsVirtualKeyCode: 13 });
  await send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Enter', code: 'Enter', windowsVirtualKeyCode: 13 });
  await waitFor('document.querySelector("[aria-labelledby=dashboard-details-heading][aria-busy=false] tbody tr a")');
  await clickText('بستن جزئیات');
  await clickText('تعداد برگ‌های اجرا', '[aria-label="آمار کلی"] button');
  await waitFor('document.querySelector("[aria-labelledby=dashboard-details-heading][aria-busy=false] tbody tr a")');
  assert.equal(await evaluate('document.querySelectorAll("[aria-labelledby=dashboard-details-heading] tbody tr").length'), 5);
  await clickText('بعدی', '[aria-labelledby=dashboard-details-heading] button');
  await waitFor('document.querySelector("[aria-labelledby=dashboard-details-heading] tbody a")?.getAttribute("href")==="/execution-papers/6"');
  await screenshot('dashboard-overview-details.png');
  await assertDialogCentered();
  const dialogGeometry = await evaluate('(()=>{const d=document.querySelector("dialog[open]");return {height:d.clientHeight, content:d.scrollHeight, top:d.getBoundingClientRect().top, bottom:d.getBoundingClientRect().bottom}})()');
  assert.ok(dialogGeometry.content <= dialogGeometry.height + 1 && dialogGeometry.top >= 0 && dialogGeometry.bottom <= 900, JSON.stringify(dialogGeometry));
  await send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
  await send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
  await waitFor('!document.querySelector("dialog[open]")');
  assert.ok(await evaluate('document.activeElement.textContent.includes("تعداد برگ‌های اجرا")'));
  await clickText('میزکار شخصی'); await waitFor('document.querySelector("dialog[open] #dashboard-work-heading")');
  await waitFor('document.querySelector("dialog[open] [aria-labelledby=pending-heading][aria-busy=false] tbody tr a")');
  const workGeometry = await evaluate('(()=>{const d=document.querySelector("dialog[open]");return {height:d.clientHeight, content:d.scrollHeight, top:d.getBoundingClientRect().top, bottom:d.getBoundingClientRect().bottom}})()');
  assert.ok(workGeometry.content <= workGeometry.height + 1 && workGeometry.top >= 0 && workGeometry.bottom <= 900, JSON.stringify(workGeometry));
  await assertDialogCentered();
  await clickText('بستن میزکار');
  await clickText('تعداد برگ‌های اجرا', '[aria-label="آمار کلی"] button');
  await waitFor('document.querySelector("[aria-labelledby=dashboard-details-heading][aria-busy=false] tbody tr a")');
  failDetails = true; await clickText('بستن جزئیات'); await clickText('تعداد برگ‌های اجرا', '[aria-label="آمار کلی"] button');
  await waitFor('document.querySelector("[aria-labelledby=dashboard-details-heading] [role=alert]")');
  await assertDialogCentered();
  failDetails = false; await clickText('تلاش مجدد', '[aria-labelledby=dashboard-details-heading] button');
  await waitFor('document.querySelector("[aria-labelledby=dashboard-details-heading][aria-busy=false] tbody tr a")');
  await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
  await evaluate('Array.from(document.querySelectorAll("button")).find(el=>el.getAttribute("aria-label")==="بستن منوی اصلی")?.click();window.scrollTo(0,0)');
  await assertDialogCentered();
  assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'));
  await screenshot('dashboard-overview-mobile.png');
  await clickText('بستن جزئیات');
  for (const width of [320, 360, 390, 768, 1000, 1100, 1200]) {
    await send('Emulation.setDeviceMetricsOverride', { width, height: 900, deviceScaleFactor: 1, mobile: width < 700 });
    await evaluate('window.scrollTo(0,0)'); await delay(250);
    const responsive = await evaluate(`({width:innerWidth,pageWidth:document.documentElement.scrollWidth,
      money:Array.from(document.querySelectorAll('[data-money=true] strong bdi')).map(el=>({text:el.textContent,width:el.getBoundingClientRect().width,available:el.parentElement.clientWidth})),
      panels:Array.from(document.querySelectorAll('[data-report-panel]')).map(el=>({id:el.dataset.reportPanel,width:el.clientWidth,content:el.scrollWidth,height:el.clientHeight,contentHeight:el.scrollHeight}))})`);
    assert.ok(responsive.pageWidth <= width, JSON.stringify(responsive));
    assert.ok(responsive.money.every(item=>item.width<=item.available+1), JSON.stringify(responsive));
    assert.ok(responsive.panels.every(panel=>panel.content<=panel.width+1&&panel.contentHeight<=panel.height+1), JSON.stringify(responsive));
    await screenshot('dashboard-overview-responsive-' + width + '.png');
  }
  failOverview = true; await navigate(); await waitFor('document.querySelector("[role=alert]")');
  failOverview = false; await clickText('تلاش مجدد'); await waitFor('document.querySelectorAll(".recharts-pie-sector").length>0');
  empty = true; await navigate(); await waitFor('document.body.innerText.includes("برگ اجرایی در این محدوده وجود ندارد")');
  assert.equal(await evaluate('document.querySelectorAll(".recharts-pie-sector").length'), 0);
  empty = false; unknown = true; await navigate(); await waitFor('document.body.innerText.includes("تفکیک نمودار در دسترس نیست")');
  allowed = false; unknown = false; await navigate(); await waitFor('document.body.innerText.includes("نمایش جزئیات به مجوز")');
  assert.ok(await evaluate(`Array.from(document.querySelectorAll('[aria-label="آمار کلی"] button')).every(el=>el.disabled)`));
  assert.ok(requests.every(url => !url.includes('group=enforcement&key=SEIZURE') && !url.includes('group=enforcement&key=AUCTION')));
  assert.deepEqual(errors, []);
  console.log('PASS: six report pies, independent notification ring, enforcement filters, office keyboard access; four desktop sizes without scrolling, seven responsive widths (320–1200px) without clipped amounts or overflowing panels, paged dialogs, hover, retries, unknown/empty states and permissions. API responses intercepted.');
}
(async () => {
  try {
    let tabs;
    for (let attempt = 0; attempt < 80; attempt++) { try { tabs = await (await fetch('http://localhost:9338/json/list')).json(); break; } catch {} await delay(150); }
    assert.ok(tabs, 'Chrome did not start');
    ws = new WebSocket(tabs.find(tab => tab.type === 'page').webSocketDebuggerUrl);
    await new Promise((resolve, reject) => { ws.onopen = resolve; ws.onerror = reject; });
    ws.onmessage = event => { const data = JSON.parse(event.data); if (data.id) { const item = pending.get(data.id); if (item) { clearTimeout(item.timer); pending.delete(data.id); data.error ? item.reject(Error(JSON.stringify(data.error))) : item.resolve(data.result); } }
      else if (data.method === 'Fetch.requestPaused') respond(data.params).catch(error => { if (!error.message.includes('Invalid InterceptionId')) errors.push(error.message); });
      else if (data.method === 'Runtime.exceptionThrown') errors.push(JSON.stringify(data.params.exceptionDetails)); };
    await send('Page.enable'); await send('Runtime.enable');
    await send('Page.addScriptToEvaluateOnNewDocument', { source: "try{localStorage.setItem('access_token','dashboard-ui-contract-test')}catch{}" });
    await send('Fetch.enable', { patterns: [{ urlPattern: '*/api/*', requestStage: 'Request' }] });
    await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false });
    await checks();
  } catch (error) { console.error(error.stack); process.exitCode = 1; if (ws?.readyState === 1) try { await screenshot('dashboard-overview-failure.png'); } catch {} }
  finally { ws?.close(); browser.kill(); }
})();
