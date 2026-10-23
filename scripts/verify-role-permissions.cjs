// Node 22+ and Chrome. API fixtures exercise UI contracts without touching a database.
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const assert = require('node:assert/strict');
const base = process.env.FRONTEND_URL || 'http://localhost:3100';
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'vosol-paper-browser-'));
const browser = spawn(process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe', [
  '--headless=new', '--no-sandbox', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
  '--remote-debugging-port=9319', `--user-data-dir=${profile}`, 'about:blank',
], { windowsHide: true, stdio: 'ignore' });
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
let ws, nextId = 0;
const pending = new Map();
const exceptions = [], externalRequests = [];
const requests = [];
let failList=false, failDetail=false, empty=false, allowReport=true;
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
async function escape() {
 await send('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});
 await send('Input.dispatchKeyEvent',{type:'keyUp',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});
}

// API requests are forwarded to the real local backend, not fulfilled with fixtures.
const backend = process.env.BACKEND_URL || 'http://localhost:8081';
async function fulfill(event) {
  const url = new URL(event.request.url);
  const headers = { ...event.request.headers }; delete headers.Host; delete headers['Content-Length'];
  const response = await fetch(backend + url.pathname + url.search, { method: event.request.method, headers,
    body: ['GET','HEAD'].includes(event.request.method) ? undefined : event.request.postData });
  await send('Fetch.fulfillRequest', { requestId: event.requestId, responseCode: response.status,
    responseHeaders: [{ name: 'Content-Type', value: response.headers.get('content-type') || 'application/json' }],
    body: Buffer.from(await response.arrayBuffer()).toString('base64') });
}
async function screenshot(name) { await evaluate('document.fonts.ready'); await delay(150);const data=await send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(path.resolve(__dirname,'../temp/'+name),Buffer.from(data.data,'base64')); }
async function api(route, options={}) { const r=await fetch(backend+route,options);assert.ok(r.ok,route+': '+r.status);return r.status===204?null:r.json(); }
async function runChecks() {
  const users = await api('/api/v1/auth/mock/users');assert.equal(users.length,11);
  for(const user of users) {
    const token=await api('/api/v1/auth/mock/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:user.username})});
    const me=await api('/api/v1/auth/me',{headers:{Authorization:'Bearer '+token.accessToken}});assert.equal(me.businessRole,user.businessRole);
    await api('/api/v1/auth/mock/logout',{method:'POST',headers:{Authorization:'Bearer '+token.accessToken}});
  }
  const adminSession=await api('/api/v1/auth/mock/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:'admin'})});
  await api('/api/v1/group-permissions',{method:'POST',headers:{'Content-Type':'application/json',Authorization:'Bearer '+adminSession.accessToken},body:JSON.stringify({groupName:'EXECUTION_OFFICER',permissionCode:'EXECUTION_PAPER_LIST_VIEW'})});
  await navigate('/login?next=/admin/permissions');
  await waitFor("document.querySelector('#mock-user') && !document.querySelector('.login-action').disabled");
  assert.equal(await evaluate("document.querySelectorAll('#mock-user option').length"),11);
  assert.equal(await evaluate("document.querySelector('#mock-user').value"),'admin');
  await evaluate("document.querySelector('.login-action').click()");
  await waitFor("location.pathname==='/admin/permissions' && document.querySelectorAll('[aria-label=\"نقش‌های سامانه\"] button').length===11");
  assert.equal(await evaluate("document.querySelector('a[href=\"/admin/permissions\"][aria-current=page]').textContent"),'نقش‌ها و دسترسی‌ها');
  await screenshot('role-permissions-desktop.png');
  await evaluate("Array.from(document.querySelectorAll('[aria-label=\"نقش‌های سامانه\"] button')).find(b=>b.textContent==='مأمور اجرا').click()");
  const checkbox="Array.from(document.querySelectorAll('fieldset label')).find(l=>l.textContent.startsWith('مشاهده فهرست برگ‌های اجرا')).querySelector('input')";
  assert.equal(await evaluate(`${checkbox}.checked`),true);
  await evaluate(`${checkbox}.click()`);await waitFor(`${checkbox}.checked===false && document.querySelector('main [role=status]').textContent.includes('ذخیره')`);
  await send('Page.reload');await waitFor("document.querySelectorAll('[aria-label=\"نقش‌های سامانه\"] button').length===11");
  await evaluate("Array.from(document.querySelectorAll('[aria-label=\"نقش‌های سامانه\"] button')).find(b=>b.textContent==='مأمور اجرا').click()");
  assert.equal(await evaluate(`${checkbox}.checked`),false);
  await evaluate(`${checkbox}.click()`);await waitFor(`${checkbox}.checked===true && !${checkbox}.disabled`);
  await evaluate("Array.from(document.querySelectorAll('[aria-label=\"نقش‌های سامانه\"] button')).find(b=>b.textContent.includes('مدیر سامانه')).click()");
  assert.ok(await evaluate("Array.from(document.querySelectorAll('fieldset input')).every(i=>i.disabled&&i.checked)"));
  await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:false});
  await send('Page.reload');await waitFor("document.querySelectorAll('[aria-label=\"نقش‌های سامانه\"] button').length===11");
  assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'));await screenshot('role-permissions-mobile.png');
  const officer=await api('/api/v1/auth/mock/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:'demo_execution_officer'})});
  await evaluate(`localStorage.setItem('access_token',${JSON.stringify(officer.accessToken)})`);await send('Page.reload');
  await waitFor("document.body.innerText.includes('شما مجوز مشاهده این پنل را ندارید')");
  assert.equal(await evaluate("document.querySelector('a[href=\"/admin/permissions\"]')===null"),true);
  assert.deepEqual(exceptions,[]); assert.deepEqual(externalRequests,[]);
  console.log('PASS: eleven live demo logins, role panel and sidebar, grant/revoke persistence, admin protection, denied access, desktop/mobile.');
}
(async () => {
  let target;
  for (let attempt = 0; attempt < 100; attempt++) {
    try { target = (await (await fetch('http://localhost:9319/json')).json()).find(item => item.type === 'page'); if (target) break; } catch {}
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
    } else if (message.method === 'Fetch.requestPaused') fulfill(message.params).catch(e=>exceptions.push(String(e)));
    else if (message.method === 'Runtime.exceptionThrown') exceptions.push(message.params.exceptionDetails.text);
    else if (message.method === 'Network.requestWillBeSent') {
      const url = message.params.request.url;
      if (/^https?:/.test(url) && new URL(url).origin !== new URL(base).origin) externalRequests.push(url);
    }
  });
  await send('Page.enable'); await send('Runtime.enable'); await send('Network.enable');
  await send('Fetch.enable', {patterns:[{urlPattern:'*api/v1/*'}]});
  await send('Emulation.setDeviceMetricsOverride', { width:1440,height:1024,deviceScaleFactor:1,mobile:false });
  await runChecks();
})().catch(error => { console.error(error); process.exitCode = 1; }).finally(async () => {
  if (ws?.readyState === WebSocket.OPEN) { try { await send('Browser.close'); } catch {} ws.close(); }
  browser.kill();
  // The OS temp profile can be removed after Chrome releases its files.
});
