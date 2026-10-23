// Node 22+, Chrome, and running mock-local backend/frontend. No API interception.
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const assert = require('node:assert/strict');
const base = process.env.FRONTEND_URL || 'http://localhost:3100';
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'vosol-mock-browser-'));
const browser = spawn(process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe', [
  '--headless=new', '--no-sandbox', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
  '--remote-debugging-port=9318', `--user-data-dir=${profile}`, 'about:blank',
], { windowsHide: true, stdio: 'ignore' });
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
let ws, nextId = 0;
const pending = new Map();
const exceptions = [], externalRequests = [];
function send(method, params = {}) {
  const id = ++nextId;
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { pending.delete(id); reject(Error(`Timeout: ${method}`)); }, 15000);
    pending.set(id, { resolve, reject, timer });
    ws.send(JSON.stringify({ id, method, params }));
  });
}
async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
  if (result.exceptionDetails) throw Error(JSON.stringify(result.exceptionDetails));
  return result.result.value;
}
async function waitFor(expression) {
  for (let attempt = 0; attempt < 120; attempt++) {
    if (await evaluate(`Boolean(${expression})`)) return;
    await delay(100);
  }
  const state = await evaluate("({pathname:location.pathname,text:document.body.innerText.slice(0,1500)})");
  throw Error(`Condition did not become true: ${expression}\n${JSON.stringify(state)}`);
}
async function navigate(route) { await send('Page.navigate', { url: base + route }); }
async function login(username) {
  await navigate('/login');
  await waitFor("document.querySelector('#mock-user') && !document.querySelector('.login-action').disabled");
  if (username) {
    await evaluate(`(() => { const el=document.querySelector('#mock-user'); Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype,'value').set.call(el,${JSON.stringify(username)}); el.dispatchEvent(new Event('change',{bubbles:true})); })()`);
    await delay(100);
  }
  await evaluate("document.querySelector('.login-action').click()");
}
async function logout() {
  await evaluate("document.querySelector('[aria-controls=dashboard-profile]')?.click()");
  await evaluate("(document.querySelector('#dashboard-profile button') || document.querySelector('.topbar-logout')).click()");
  await waitFor("location.pathname==='/login' && !localStorage.getItem('access_token') && document.querySelector('#mock-user')");
}
(async () => {
  let target;
  for (let attempt = 0; attempt < 100; attempt++) {
    try { target = (await (await fetch('http://localhost:9318/json')).json()).find(item => item.type === 'page'); if (target) break; } catch {}
    await delay(150);
  }
  if (!target) throw Error('Chrome did not start');
  ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => { ws.addEventListener('open', resolve); ws.addEventListener('error', reject); });
  ws.addEventListener('message', event => {
    const message = JSON.parse(event.data);
    if (message.id) {
      const item = pending.get(message.id);
      if (item) { clearTimeout(item.timer); pending.delete(message.id); message.error ? item.reject(Error(JSON.stringify(message.error))) : item.resolve(message.result); }
    } else if (message.method === 'Runtime.exceptionThrown') exceptions.push(message.params.exceptionDetails.text);
    else if (message.method === 'Network.requestWillBeSent') {
      const url = message.params.request.url;
      if (/^https?:/.test(url) && new URL(url).origin !== new URL(base).origin) externalRequests.push(url);
    }
  });
  await send('Page.enable'); await send('Runtime.enable'); await send('Network.enable');
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1024, deviceScaleFactor: 1, mobile: false });
  await navigate('/');
  await waitFor("location.pathname==='/login' && document.querySelector('#mock-user')");
  assert.equal(await evaluate("document.querySelector('#mock-user').value"), 'admin');
  await login('demo_tax_directorate_director_general');
  await waitFor("document.getElementById('pending-heading') && document.querySelector('.mock-mode-banner') && document.body.innerText.includes('مدیرکل امور مالیاتی')");
  const token = await evaluate("localStorage.getItem('access_token')");
  assert.ok(token.startsWith('mock_'));
  assert.equal(await evaluate("localStorage.getItem('refresh_token')"), null);
  await send('Page.reload');
  await waitFor("document.getElementById('pending-heading') && document.body.innerText.includes('مدیرکل امور مالیاتی')");
  const screenshot = await send('Page.captureScreenshot', { format: 'png' });
  fs.mkdirSync(path.resolve(__dirname, '../temp'), { recursive: true });
  fs.writeFileSync(path.resolve(__dirname, '../temp/mock-local-dashboard.png'), Buffer.from(screenshot.data, 'base64'));
  await logout();
  assert.equal(await evaluate(`fetch('/api/v1/auth/me',{headers:{Authorization:${JSON.stringify(`Bearer ${token}`)}}}).then(r=>r.status)`), 401);
  await login('demo_execution_officer');
  await waitFor("location.pathname==='/' && document.getElementById('pending-heading') && document.querySelector('[aria-label^=\"حساب کاربری\"]')");
  await logout();
  await login('admin');
  await waitFor("location.pathname==='/' && document.getElementById('pending-heading') && document.querySelector('[aria-label^=\"حساب کاربری\"]')");
  await logout();
  await navigate('/login?next=https%3A%2F%2Fexample.com');
  await waitFor("document.querySelector('#mock-user') && !document.querySelector('.login-action').disabled");
  await evaluate("(()=>{const el=document.querySelector('#mock-user');Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype,'value').set.call(el,'demo_tax_directorate_director_general');el.dispatchEvent(new Event('change',{bubbles:true}));})()");
  await evaluate("document.querySelector('.login-action').click()");
  await waitFor("document.getElementById('pending-heading')");
  assert.equal(await evaluate('location.pathname'), '/');
  await logout();
  await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: false });
  await waitFor("document.querySelector('#mock-user') && !document.querySelector('.login-action').disabled");
  assert.ok(await evaluate('document.documentElement.scrollWidth <= innerWidth'), 'Mobile login must not overflow');
  assert.deepEqual(exceptions, []);
  assert.deepEqual(externalRequests, []);
  console.log('PASS: live mock login, dashboard, reload, revoked logout, expert/admin routing, safe redirects, mobile login, no external requests or runtime exceptions.');
})().catch(error => { console.error(error); process.exitCode = 1; }).finally(async () => {
  if (ws?.readyState === WebSocket.OPEN) { try { await send('Browser.close'); } catch {} ws.close(); }
  browser.kill();
  // The OS temp profile can be removed after Chrome releases its files.
});
