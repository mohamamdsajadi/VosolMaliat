// Live browser check. Run only against a disposable database containing expired, notified papers.
// Node 22+, Chrome, a running Next app, and the mock-local backend are required.
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const assert = require('node:assert/strict');
const base = process.env.FRONTEND_URL || 'http://localhost:3200';
const backend = process.env.BACKEND_URL || 'http://localhost:8231';
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'vosol-arrest-browser-'));
const browser = spawn(process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe', [
  '--headless=new', '--no-sandbox', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
  '--remote-debugging-port=9327', `--user-data-dir=${profile}`, 'about:blank',
], { windowsHide: true, stdio: 'ignore' });
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
let ws, nextId = 0;
const pending = new Map();
const exceptions = [];
function send(method, params = {}) {
  const id = ++nextId;
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { pending.delete(id); reject(Error('Timeout: ' + method)); }, 20000);
    pending.set(id, { resolve, reject, timer }); ws.send(JSON.stringify({ id, method, params }));
  });
}
async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
  if (result.exceptionDetails) throw Error(JSON.stringify(result.exceptionDetails));
  return result.result.value;
}
async function waitFor(expression) {
  for (let i=0;i<160;i++) { if (await evaluate(`Boolean(${expression})`)) return; await delay(150); }
  throw Error(`Condition failed: ${expression}\n` + JSON.stringify(await evaluate('({url:location.href,text:document.body.innerText.slice(0,2500)})')));
}
async function navigate(route) { await send('Page.navigate', { url: base + route }); }
async function screenshot(name) {
  await evaluate('document.fonts.ready'); await delay(120);
  const {data}=await send('Page.captureScreenshot',{format:'png'});
  fs.writeFileSync(path.resolve(__dirname,'../temp/'+name),Buffer.from(data,'base64'));
}
async function clickText(text, selector='button') {
  await evaluate(`Array.from(document.querySelectorAll(${JSON.stringify(selector)})).find(el=>el.textContent.includes(${JSON.stringify(text)})).click()`);
}
async function choose(selector,value) {
  await evaluate(`(()=>{const el=document.querySelector(${JSON.stringify(selector)});el.value=${JSON.stringify(String(value))};el.dispatchEvent(new Event('change',{bubbles:true}));})()`);
}
async function api(route, token, method='GET', body) {
  const response=await fetch(backend+route,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},...(body?{body:JSON.stringify(body)}:{})});
  const data=await response.json(); assert.ok(response.ok,route+': '+response.status+' '+JSON.stringify(data)); return data;
}
async function login(username) {
  const session=await api('/api/v1/auth/mock/login',null,'POST',{username});
  await evaluate(`localStorage.setItem('access_token',${JSON.stringify(session.accessToken)})`);return session.accessToken;
}
async function forward(event) {
  const url=new URL(event.request.url);
  const headers={...event.request.headers};delete headers.Host;delete headers['Content-Length'];
  try {
    const response=await fetch(backend+url.pathname+url.search,{method:event.request.method,headers,
      ...(['GET','HEAD'].includes(event.request.method)?{}:{body:event.request.postData})});
    await send('Fetch.fulfillRequest',{requestId:event.requestId,responseCode:response.status,
      responseHeaders:[{name:'Content-Type',value:response.headers.get('content-type')||'application/json'}],
      body:Buffer.from(await response.arrayBuffer()).toString('base64')});
  } catch(error) {
    // React cancels in-flight reads when changing tabs or leaving a page.
    if (error.message.includes('Invalid InterceptionId')) return;
    exceptions.push('API forwarding: '+error.message);
    try { await send('Fetch.failRequest',{requestId:event.requestId,errorReason:'Failed'}); }
    catch (cancelled) { if (!cancelled.message.includes('Invalid InterceptionId')) throw cancelled; }
  }
}
async function checks() {
  for (let attempt=0;attempt<120;attempt++) { try { if ((await fetch(backend+'/api/v1/auth/config')).ok) break; } catch {} await delay(250); }
  await navigate('/login');await waitFor("document.querySelector('#mock-user')");
  const token=await login('demo_directorate_execution_unit_head');
  const candidates=await api('/api/v1/arrest-orders/candidates?size=20',token);
  assert.ok(candidates.content.length>=3,'Need at least three eligible, expired test papers');
  const selectedPapers=candidates.content.slice(0,2).map(item=>item.paper);
  await navigate('/arrest-orders');
  await waitFor("document.querySelectorAll('tbody input[type=checkbox]').length>=2");
  assert.equal(await evaluate("document.querySelector('a[href=\"/arrest-orders\"][aria-current=page]').textContent"),'دستور بازداشت اموال');
  await screenshot('arrest-candidates-desktop.png');
  await evaluate("document.querySelectorAll('tbody input[type=checkbox]')[0].click();document.querySelectorAll('tbody input[type=checkbox]')[1].click()");
  await clickText('صدور گروهی');await waitFor("document.querySelector('dialog[open] #arrest-supervisor option[value]:not([value=\"\"])')");
  const supervisor=await evaluate("document.querySelector('#arrest-supervisor option:not([value=\"\"])').value");
  await choose('#arrest-supervisor',supervisor);await screenshot('arrest-bulk-dialog.png');
  await evaluate("document.querySelector('dialog form').requestSubmit()");
  await waitFor("document.querySelector('dialog [role=status]')?.textContent.includes('۲ مورد موفق')");
  await clickText('بستن','dialog button');
  const order=(await api('/api/v1/arrest-orders?executionPaperId='+selectedPapers[0].id,token)).content[0];
  assert.equal(order.stage,'AWAITING_APPROVAL');
  await navigate('/arrest-orders?order='+order.id);
  await waitFor("document.querySelector('#arrest-letter') && document.querySelector('input[type=checkbox]')");
  await screenshot('arrest-draft-desktop.png');
  await evaluate("document.querySelector('input[type=checkbox]').click()");
  await evaluate("document.querySelector('[class*=actionCard] form').requestSubmit()");
  await waitFor("document.body.innerText.includes('دستور صادر شده')");
  await screenshot('arrest-issued-desktop.png');
  assert.equal((await api('/api/v1/arrest-orders/'+order.id,token)).order.issuanceStatus,'ISSUED');
  // Check the isolated, printable letter document without opening an OS dialog.
  await evaluate(`window.__arrestPrintCalled=false;window.__arrestPrintObserver=new MutationObserver(()=>{const f=document.querySelector('iframe[title="چاپ دستور بازداشت اموال"]');if(f)f.contentWindow.print=()=>{window.__arrestPrintCalled=true;};});window.__arrestPrintObserver.observe(document.body,{childList:true});`);
  await clickText('چاپ / ذخیره PDF');await waitFor('window.__arrestPrintCalled');
  assert.equal(await evaluate(`getComputedStyle(document.querySelector('iframe[title="چاپ دستور بازداشت اموال"]').contentDocument.querySelector('table')).borderCollapse`),'collapse','Print document must load the letter stylesheet');
  const printable=await evaluate(`(()=>{const doc=document.querySelector('iframe[title="چاپ دستور بازداشت اموال"]').contentDocument;const base=doc.createElement('base');base.href=location.origin+'/';doc.head.prepend(base);return '<!doctype html>'+doc.documentElement.outerHTML;})()`);
  fs.writeFileSync(path.resolve(__dirname,'../temp/arrest-letter-verified.html'),printable);
  const supervisorToken=await login('demo_directorate_execution_team_supervisor');
  await navigate('/arrest-orders?order='+order.id);
  await waitFor("document.querySelector('[aria-label=\"کاربر مقصد مرحله بعد\"] option:not([value=\"\"])')");
  const officer=await evaluate("document.querySelector('[aria-label=\"کاربر مقصد مرحله بعد\"] option:not([value=\"\"])').value");
  await choose('[aria-label="کاربر مقصد مرحله بعد"]',officer);
  await evaluate("document.querySelector('[class*=actionCard] form').requestSubmit()");
  await waitFor("document.body.innerText.includes('تحویل به مأمور اجرا')");
  await clickText('سوابق گردش');await waitFor("document.querySelectorAll('[class*=timeline] li').length>=7");
  await screenshot('arrest-history-desktop.png');
  // The unit head may also select an officer directly, still approving the draft first.
  const headToken=await login('demo_directorate_execution_unit_head');
  const directPaper=(await api('/api/v1/arrest-orders/candidates?size=20',headToken)).content[0].paper;
  await navigate('/arrest-orders');await waitFor("document.querySelector('tbody button')");
  await evaluate("document.querySelector('tbody button').click()");
  await waitFor("document.querySelector('dialog input[value=OFFICER]')");
  await evaluate("document.querySelector('dialog input[value=OFFICER]').click()");
  await waitFor("document.querySelector('#arrest-officer option:not([value=\"\"])')");
  const directOfficer=await evaluate("document.querySelector('#arrest-officer option:not([value=\"\"])').value");
  await choose('#arrest-officer',directOfficer);await screenshot('arrest-direct-officer-dialog.png');
  await evaluate("document.querySelector('dialog form').requestSubmit()");
  await waitFor("document.querySelector('dialog [role=status]')?.textContent.includes('۱ مورد موفق')");
  await clickText('بستن','dialog button');
  const directOrder=(await api('/api/v1/arrest-orders?executionPaperId='+directPaper.id,headToken)).content[0];
  assert.equal(directOrder.stage,'AWAITING_APPROVAL');
  await navigate('/arrest-orders?order='+directOrder.id);
  await waitFor("document.querySelector('#arrest-letter') && document.querySelector('input[type=checkbox]')");
  assert.equal((await api('/api/v1/arrest-orders/'+directOrder.id,headToken)).letter.recipientRole,'مأمور اجرا');
  await evaluate("document.querySelector('input[type=checkbox]').click();document.querySelector('[class*=actionCard] form').requestSubmit()");
  await waitFor("document.body.innerText.includes('تحویل به مأمور اجرا')");
  assert.equal((await api('/api/v1/arrest-orders/'+directOrder.id,headToken)).order.assignee.id,Number(directOfficer));
  await screenshot('arrest-direct-officer-issued.png');
  await navigate('/arrest-orders?order='+order.id);await waitFor("document.body.innerText.includes('تحویل به مأمور اجرا')");
  await clickText('سوابق گردش');await waitFor("document.querySelectorAll('[class*=timeline] li').length>=7");
  await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:false});
  await send('Page.reload');await waitFor("document.body.innerText.includes('تحویل به مأمور اجرا')");
  await clickText('سوابق گردش');await waitFor("document.querySelectorAll('[class*=timeline] li').length>=7");
  await screenshot('arrest-history-mobile.png');
  assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'),'Mobile detail overflow');
  await login('demo_directorate_execution_unit_head');await navigate('/arrest-orders');
  await waitFor("document.querySelector('tbody input[type=checkbox]')");
  assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'),'Mobile list overflow');
  await screenshot('arrest-candidates-mobile.png');
  await evaluate("document.querySelector('tbody input[type=checkbox]').click()");
  await waitFor("!document.querySelector('[class*=bulkBar] button').disabled");
  await clickText('صدور گروهی');
  await waitFor("document.querySelector('dialog[open]')");
  await evaluate("document.querySelector('input[value=SPECIAL_DEPARTMENT]').click()");
  await screenshot('arrest-special-mobile.png');
  await evaluate("document.querySelector('dialog form').requestSubmit()");
  await waitFor("document.querySelector('dialog [role=status]')?.textContent.includes('۱ مورد موفق')");
  await clickText('بستن','dialog button');
  const specialToken=await login('demo_special_execution_department_head');
  const special=(await api('/api/v1/arrest-orders?mine=true',specialToken)).content.find(item=>item.stage==='SPECIAL_DEPARTMENT_REVIEW');
  assert.ok(special,'Special department head must receive the referral');
  await navigate('/arrest-orders?order='+special.id);
  await waitFor("document.querySelector('[aria-label=\"کاربر مقصد مرحله بعد\"] option:not([value=\"\"])')");
  await choose('[aria-label="کاربر مقصد مرحله بعد"]',supervisor);
  await evaluate("document.querySelector('[class*=actionCard] form').requestSubmit()");
  await waitFor("document.querySelector('#arrest-letter') && document.querySelector('input[type=checkbox]')");
  await evaluate("document.querySelector('input[type=checkbox]').click();document.querySelector('[class*=actionCard] form').requestSubmit()");
  await waitFor("document.body.innerText.includes('دستور صادر شده')");
  await login('demo_execution_officer');await navigate('/arrest-orders?order='+order.id);
  await waitFor("document.body.innerText.includes('تحویل به مأمور اجرا')");
  assert.equal(await evaluate("document.querySelector('[class*=actionCard] form')===null"),true);
  await navigate('/execution-papers/'+order.executionPaperId);
  await waitFor("document.querySelector('[aria-label=\"دستور بازداشت پیوست برگ اجرا\"] a[href*=\"order=\"]')");
  await screenshot('arrest-paper-attachment-mobile.png');
  assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'),'Attachment page overflow');
  assert.deepEqual(exceptions,[],'Browser errors');
  await exportLetter(printable);
  fs.writeFileSync(path.resolve(__dirname,'../temp/arrest-browser-result.json'),JSON.stringify({orderId:order.id,specialOrderId:special.id,pages:1,exceptions},null,2));
  console.log('PASS: live bulk start, approval, supervisor/officer handoff, special route, paper attachment, history, desktop/mobile, one-page A4 letter.');
}
async function exportLetter(printable) {
  // Retain the app origin so the same stylesheet, font and logo resources load.
  await send('Emulation.setDeviceMetricsOverride',{width:1000,height:1300,deviceScaleFactor:1,mobile:false});
  await navigate('/arrest-orders');
  await waitFor("document.querySelector('section[class*=workspace]')");
  const frameTree=await send('Page.getFrameTree');
  await send('Page.setDocumentContent',{frameId:frameTree.frameTree.frame.id,html:printable});
  await waitFor("document.querySelector('#arrest-letter') && document.styleSheets.length>1");
  await evaluate('document.fonts.ready');await delay(500);
  assert.equal(await evaluate("getComputedStyle(document.querySelector('#arrest-letter table')).borderCollapse"),'collapse');
  assert.equal(await evaluate("document.querySelector('#arrest-letter img').naturalWidth>0"),true,'Printed letter logo must load');
  await screenshot('arrest-letter-print-preview.png');
  const pdf=await send('Page.printToPDF',{preferCSSPageSize:true,printBackground:true});
  const bytes=Buffer.from(pdf.data,'base64');fs.writeFileSync(path.resolve(__dirname,'../temp/arrest-letter-verified.pdf'),bytes);
  const pages=(bytes.toString('latin1').match(/\/Type\s*\/Page\b/g)||[]).length;
  if(pages!==1) { await send('Emulation.setEmulatedMedia',{media:'print'});console.log('Print layout:',await evaluate(`Object.fromEntries(['html','body','article'].map(selector=>{const e=document.querySelector(selector),s=getComputedStyle(e);return [selector,{height:e.getBoundingClientRect().height,scroll:e.scrollHeight,minHeight:s.minHeight,padding:s.padding,margin:s.margin}]}))`)); }
  assert.equal(pages,1,'The letter must fit one A4 page');
}
async function viewChecks() {
  await navigate('/login');await waitFor("document.querySelector('#mock-user')");
  const token=await login('demo_directorate_execution_unit_head');
  const mine=await api('/api/v1/arrest-orders?mine=true',token);
  const pendingOrder=mine.content.find(order=>order.stage==='AWAITING_APPROVAL');
  assert.ok(pendingOrder,'Need a persisted pending approval for dashboard and inbox checks');
  await navigate('/');
  await waitFor("document.querySelector('a[href^=\"/arrest-orders?order=\"]')");
  assert.equal(await evaluate("document.querySelector('[role=alert]')===null"),true);
  await screenshot('arrest-dashboard-inbox.png');
  await navigate('/arrest-orders');await waitFor("document.querySelector('#arrest-tab-mine')");
  await clickText('ارجاع‌های من','[role=tab]');
  await waitFor(`document.querySelector('tbody')?.textContent.includes(${JSON.stringify(String(pendingOrder.executionPaperCode).replace(/\d/g,n=>'۰۱۲۳۴۵۶۷۸۹'[n]))})`);
  for(const title of ['تاریخ ارجاع','مقصد ارجاع','وضعیت صدور']) assert.ok(await evaluate(`document.querySelector('thead').textContent.includes(${JSON.stringify(title)})`));
  await screenshot('arrest-my-referrals-desktop.png');
  await clickText('پیگیری دستورها','[role=tab]');
  await waitFor("document.querySelector('[aria-label=\"فیلتر مرحله فرایند\"]')");
  await choose('[aria-label="فیلتر مرحله فرایند"]','AWAITING_APPROVAL');
  await waitFor("document.querySelector('tbody') && !document.querySelector('[aria-busy=true]')");
  await evaluate("(()=>{const input=document.querySelector('input[aria-label=\"جستجوی شماره برگ اجرا\"]');Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(input,'NO-MATCH-ARREST-000000');input.dispatchEvent(new Event('input',{bubbles:true}));})()");
  await waitFor("document.querySelector('h2')?.textContent.includes('سابقه دستوری برای شما یافت نشد')");
  assert.deepEqual(exceptions,[]);
  console.log('PASS: dashboard local tasks, my referrals, tracking columns, stage filter, empty search, no browser errors.');
}
async function tehranReadyChecks() {
  // Uses the real frontend rewrite; only opens/cancels the assignment dialog.
  await navigate('/login');await waitFor("document.querySelector('#mock-user')");
  await login('demo_directorate_execution_unit_head');await navigate('/arrest-orders');
  await waitFor("document.querySelectorAll('tbody input[type=checkbox]').length===10");
  assert.ok(await evaluate("document.body.innerText.includes('مرکز تهران')"));
  assert.equal(await evaluate("/\\bnull\\b/.test(document.querySelector('tbody').textContent)"),false);
  await screenshot('tehran-arrest-ready-desktop.png');
  await evaluate("document.querySelector('thead input[type=checkbox]').click()");
  await clickText('صدور گروهی');await waitFor("document.querySelector('#arrest-supervisor option:not([value=\"\"])')");
  await evaluate("document.querySelector('input[value=OFFICER]').click()");
  await waitFor("document.querySelector('#arrest-officer option:not([value=\"\"])')");
  await choose('#arrest-officer',await evaluate("document.querySelector('#arrest-officer option:not([value=\"\"])').value"));
  assert.ok(await evaluate("(()=>{const r=document.querySelector('dialog').getBoundingClientRect();return Math.abs(r.left+r.width/2-innerWidth/2)<8;})()"),'Assignment dialog must be centered');
  await screenshot('tehran-arrest-destinations.png');
  await evaluate("document.querySelector('dialog button[aria-label=\"بستن پنجره صدور\"]').click()");
  await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:false});
  await send('Page.reload');await waitFor("document.querySelectorAll('tbody input[type=checkbox]').length===10");
  assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'));
  await screenshot('tehran-arrest-ready-mobile.png');
  assert.deepEqual(exceptions,[]);
  console.log('PASS: real frontend proxy shows all ten Tehran papers, both assignment destinations, and mobile layout; no arrest process was started.');
}

async function privacyChecks() {
  await checks(); // All mutations use the disposable backend selected by BACKEND_URL.
  const fa = value => String(value).replace(/\d/g, n => '۰۱۲۳۴۵۶۷۸۹'[n]);
  const head = await login('demo_directorate_execution_unit_head');
  const stats = await api('/api/v1/dashboard/stats', head);
  await navigate('/');
  await waitFor("document.querySelector('[class*=performanceDetails]')");
  for (const [label, value] of [['اقدام شده', stats.myActionsCompleted], ['در انتظار اقدام', stats.myActionsAwaitingCompletion]]) {
    assert.equal(await evaluate(`Array.from(document.querySelectorAll('article')).find(el=>el.querySelector('h2')?.textContent===${JSON.stringify(label)})?.querySelector('strong')?.textContent`), fa(value));
  }
  assert.ok(await evaluate(`document.querySelector('[class*=performanceDetails]').textContent.includes(${JSON.stringify(fa(stats.arrestOrderPerformance.completed))})`));
  await screenshot('arrest-personal-dashboard.png');
  await navigate('/arrest-orders');
  await waitFor("document.querySelector('tbody')");
  assert.equal(await evaluate("document.body.innerText.includes('از انتخاب برگ اجرا تا ارجاع به مأمور')"), false);
  await evaluate("document.querySelector('button[aria-label=\"راهنمای دستور بازداشت\"]').click()");
  await waitFor("document.querySelector('dialog[open]')?.textContent.includes('ارجاع‌های من')");
  await screenshot('arrest-info-dialog.png');
  await evaluate("document.querySelector('dialog[open] button[aria-label=\"بستن راهنما\"]').click()");
  const mine = await api('/api/v1/arrest-orders?mine=true', head);
  const history = await api('/api/v1/arrest-orders?history=true', head);
  assert.ok(mine.content.length && history.content.length);
  assert.ok(mine.content.every(order => order.allowedActions.length));
  assert.ok(history.content.every(order => !mine.content.some(current => current.id === order.id)));
  await clickText('ارجاع‌های من', '[role=tab]');
  await waitFor(`document.querySelector('tbody')?.textContent.includes(${JSON.stringify(fa(mine.content[0].executionPaperCode))})`);
  await clickText('پیگیری دستورها', '[role=tab]');
  await waitFor(`document.querySelector('tbody')?.textContent.includes(${JSON.stringify(fa(history.content[0].executionPaperCode))})`);
  assert.equal(await evaluate(`document.querySelector('tbody').textContent.includes(${JSON.stringify(fa(mine.content[0].executionPaperCode))})`), false);
  await screenshot('arrest-personal-history.png');
  for (const username of ['demo_directorate_execution_team_supervisor', 'demo_execution_officer']) {
    const token = await login(username);
    const own = await api('/api/v1/arrest-orders', token);
    const papers = await api('/api/v1/execution-papers?size=100', token);
    assert.equal(papers.totalElements, own.totalElements);
    assert.ok(own.content.every(order => /^\d+$/.test(order.letterNumber)));
    assert.equal((await api('/api/v1/arrest-orders/candidates', token)).totalElements, 0);
    await navigate('/arrest-orders');
    await waitFor("document.querySelector('#arrest-tab-mine[aria-selected=true]') && !document.querySelector('[aria-busy=true]')");
    assert.equal(await evaluate("!!document.querySelector('#arrest-tab-candidates')"), false);
    await clickText('پیگیری دستورها', '[role=tab]');
    await waitFor("document.querySelector('tbody')");
    await screenshot(username.includes('supervisor') ? 'arrest-supervisor-own-history.png' : 'arrest-officer-own-history.png');
  }
  await send('Emulation.setDeviceMetricsOverride', { width:390, height:844, deviceScaleFactor:1, mobile:false });
  await send('Page.reload');
  await waitFor("document.querySelector('#arrest-tab-mine')");
  await evaluate("document.querySelector('button[aria-label=\"راهنمای دستور بازداشت\"]').click()");
  await waitFor("document.querySelector('dialog[open]')");
  assert.ok(await evaluate("document.querySelector('dialog[open]').scrollWidth <= document.querySelector('dialog[open]').clientWidth"));
  await screenshot('arrest-info-mobile.png');
  assert.deepEqual(exceptions, []);
  console.log('PASS: personal metrics, numeric letters, info dialogs, actionable inbox, separate history, assignment-only lists, desktop/mobile.');
}
async function paperViewChecks() {
  for(let n=0;n<120;n++){try{if((await fetch(backend+'/api/v1/auth/config')).ok)break;}catch{}await delay(250);}
  await navigate('/login'); await waitFor("document.querySelector('#mock-user')");
  const head = 'demo_directorate_execution_unit_head';
  const token = await login(head);
  const list = await api('/api/v1/execution-papers?size=50', token);
  const unread = list.content.filter(row => !list.lastViewedAt?.[row.id]);
  assert.ok(unread.length >= 2, 'Use a disposable database with two unread papers');
  const [first, second] = unread;
  const listRoute = paper => '/execution-papers?search=' + encodeURIComponent(paper.uniqueCode);
  const status = (paper, viewed) => `document.querySelector('[data-paper-id="${paper.id}"][data-paper-viewed="${viewed}"]')`;
  const hasView = async paper => Boolean((await api('/api/v1/execution-papers?search=' + encodeURIComponent(paper.uniqueCode), token)).lastViewedAt?.[paper.id]);
  const waitView = async paper => { for(let n=0;n<80;n++){if(await hasView(paper))return;await delay(100);}throw Error('Viewing was not persisted'); };
  await navigate(listRoute(first)); await waitFor(status(first, false));
  await evaluate("document.querySelector('tbody input[type=checkbox]').click()"); await waitFor("document.querySelector('#paper-summary-tab-1')");
  assert.equal(await hasView(first), false, 'An inline preview must leave the envelope closed');
  await evaluate("document.querySelector('a[data-paper-details]').click()"); await waitFor("document.querySelector('#paper-tab-0')");
  await waitView(first);
  await evaluate('history.back()'); await waitFor(status(first, true));
  assert.ok(await evaluate(`document.querySelector('[data-paper-id="${first.id}"]').title.includes('آخرین مشاهده')`));
  await send('Page.reload'); await waitFor(status(first, true));
  await login('admin'); await navigate(listRoute(first)); await waitFor(status(first, false));
  await login(head);
  const originalForward = forward;
  let blockDetails = true, failReceipt = true, receiptPosts = 0;
  forward = async event => {
    const url = new URL(event.request.url);
    let message;
    if (url.pathname === '/api/v1/execution-papers/' + second.id && blockDetails) message = 'جزئیات آزمایشی در دسترس نیست';
    if (url.pathname === '/api/v1/execution-papers/' + second.id + '/viewed') {
      receiptPosts++;
      if (failReceipt) { failReceipt = false; message = 'خطای آزمایشی ثبت مشاهده'; }
    }
    if (message) await send('Fetch.fulfillRequest', { requestId:event.requestId, responseCode:500, responseHeaders:[{name:'Content-Type',value:'application/json'}], body:Buffer.from(JSON.stringify({message})).toString('base64') });
    else await originalForward(event);
  };
  await navigate('/execution-papers/' + second.id); await waitFor("document.querySelector('[role=alert]')?.textContent.includes('جزئیات آزمایشی')");
  assert.equal(receiptPosts, 0); assert.equal(await hasView(second), false);
  blockDetails = false;
  await send('Page.reload'); await waitFor("document.querySelector('[role=alert]')?.textContent.includes('خطای آزمایشی ثبت مشاهده')");
  assert.equal(await hasView(second), false);
  await clickText('تلاش مجدد', '[role=alert] button'); await waitView(second);
  assert.equal(receiptPosts, 2);
  await navigate('/execution-papers'); await waitFor(status(first, true) + ' && ' + status(second, true));
  assert.ok(await evaluate("document.querySelector('[data-paper-viewed=false]')!==null"));
  await screenshot('paper-view-icons-desktop.png');
  for (const paper of [first,second]) {
    assert.ok(await evaluate(`document.querySelector('[data-paper-id="${paper.id}"]').parentElement.querySelector('a[data-paper-details]')!==null`));
  }
  await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:false});
  await send('Page.reload'); await waitFor(status(first,true));
  await evaluate(`document.querySelector('[data-paper-id="${first.id}"]').scrollIntoView({block:'nearest',inline:'nearest'})`);
  assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'));
  await screenshot('paper-view-icons-mobile.png');
  fs.writeFileSync(path.resolve(__dirname,'../temp/paper-view-checkpoint.json'), JSON.stringify({paperId:first.id, uniqueCode:first.uniqueCode, username:head}));
  assert.deepEqual(exceptions, []);
  console.log('PASS: closed/open envelopes beside the eye, personal server storage, reload/back navigation, preview exclusion, failed details, receipt retry, account isolation, desktop/mobile. Only disposable personal view metadata changed.');
}

async function gridSummaryChecks() {
  await navigate('/login'); await waitFor("document.querySelector('#mock-user')");
  const token = await login('demo_directorate_execution_unit_head');
  const candidates = (await api('/api/v1/arrest-orders/candidates?size=20', token)).content;
  assert.ok(candidates.length >= 2, 'Need at least two eligible papers');
  const legal = candidates.find(row => ['LEGAL', 'حقوقی'].includes(row.paper.taxpayerType));
  assert.ok(legal, 'Need a legal candidate to check the legal restriction stage');
  const route = '/execution-papers?search=' + encodeURIComponent(legal.paper.uniqueCode);
  const preview = "document.querySelector('#paper-summary-heading')";
  const loaded = "document.querySelector('#paper-summary-tab-1')";
  const checkPreview = async count => {
    await waitFor(loaded);
    assert.equal(await evaluate("document.querySelectorAll('[class*=workflow] [class*=stepName]').length"), count);
    assert.ok(await evaluate("document.querySelector('[aria-label=\"خلاصه اطلاعات بدهی\"]').textContent.includes('ریال')"));
  };
  await navigate(route); await waitFor("document.querySelectorAll('tbody input[type=checkbox]').length===1");
  await evaluate("document.querySelector('tbody input[type=checkbox]').click()");
  await checkPreview(7); await screenshot('grid-paper-summary-desktop.png');
  await evaluate("document.querySelector('#paper-summary-tab-2').click()");
  await waitFor("document.querySelector('[role=tabpanel]').textContent.includes('مؤدی حقوقی')");
  await evaluate("document.querySelector('#paper-summary-tab-0').click()");
  await waitFor("document.querySelector('[aria-label=\"جدول تاریخچه بدهی\"]')");
  await evaluate("document.querySelector('#paper-summary-tab-4').click()");
  await waitFor("document.querySelector('[role=tabpanel]').textContent.includes('RESTRICTION') || document.querySelector('[role=tabpanel]').textContent.includes('محدودیت')");
  await evaluate("document.querySelector('button[aria-label=\"بستن خلاصه برگ اجرا\"]').click()");
  assert.equal(await evaluate(`Boolean(${preview})`), false);
  assert.equal(await evaluate("document.querySelector('tbody input[type=checkbox]').checked"), false);
  const originalForward = forward;
  let failDetail = true;
  forward = async event => {
    if (failDetail && new URL(event.request.url).pathname === '/api/v1/execution-papers/' + legal.paper.id) {
      failDetail = false;
      await send('Fetch.fulfillRequest', { requestId: event.requestId, responseCode: 500, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }], body: Buffer.from(JSON.stringify({ message: 'خطای آزمایشی دریافت خلاصه' })).toString('base64') });
    } else await originalForward(event);
  };
  await evaluate("document.querySelector('tbody input[type=checkbox]').click()");
  await waitFor("document.querySelector('#paper-summary-heading')?.closest('section').textContent.includes('خطای آزمایشی دریافت خلاصه')");
  await clickText('تلاش مجدد', 'section[aria-labelledby=paper-summary-heading] button');
  await checkPreview(7);
  await evaluate("document.querySelector('button[aria-label=\"بستن خلاصه برگ اجرا\"]').click()");
  await evaluate("document.querySelector('button[aria-label=\"تنظیم نمایش فهرست\"]').click()");
  await waitFor("document.querySelector('dialog[open]')");
  await evaluate("Array.from(document.querySelectorAll('dialog[open] fieldset label')).find(el=>el.textContent==='سال').querySelector('input').click()");
  await screenshot('grid-columns-desktop.png');
  await evaluate("document.querySelector('button[aria-label=\"بستن تنظیم نمایش\"]').click()");
  assert.equal(await evaluate("Array.from(document.querySelectorAll('thead th')).some(el=>el.textContent==='سال')"), false);
  await send('Page.reload'); await waitFor("document.querySelector('tbody input[type=checkbox]')");
  assert.equal(await evaluate("Array.from(document.querySelectorAll('thead th')).some(el=>el.textContent==='سال')"), false);
  await login('admin'); await navigate(route); await waitFor("document.querySelector('tbody input[type=checkbox]')");
  assert.equal(await evaluate("Array.from(document.querySelectorAll('thead th')).some(el=>el.textContent==='سال')"), true);
  await login('demo_directorate_execution_unit_head'); await navigate('/execution-papers'); await waitFor("document.querySelectorAll('tbody input[type=checkbox]').length>=2");
  await evaluate("document.querySelectorAll('tbody input[type=checkbox]')[0].click();document.querySelectorAll('tbody input[type=checkbox]')[1].click()");
  assert.equal(await evaluate(`Boolean(${preview})`), false);
  await evaluate("document.querySelectorAll('tbody input[type=checkbox]')[1].click()"); await waitFor(loaded);
  await evaluate("document.querySelector('button[aria-label=\"صفحه بعد\"]').click()");
  await waitFor("!document.querySelector('#paper-summary-heading') && !document.querySelector('tbody input:checked')");
  await evaluate("document.querySelector('button[aria-label=\"صفحه قبل\"]').click()");
  await waitFor("document.querySelector('tbody input[type=checkbox]') && !document.querySelector('tbody input:checked')");
  await evaluate("document.querySelector('button[aria-label=\"تنظیم نمایش فهرست\"]').click()");
  await evaluate("Array.from(document.querySelectorAll('dialog[open] fieldset input:checked')).forEach(el=>{if(!el.disabled)el.click();})");
  assert.equal(await evaluate("document.querySelectorAll('dialog[open] fieldset input:checked').length"), 1);
  await clickText('بازگردانی نمایش پیش‌فرض', 'dialog button');
  await evaluate("document.querySelector('button[aria-label=\"بستن تنظیم نمایش\"]').click()");
  await navigate('/arrest-orders'); await waitFor("document.querySelectorAll('tbody input[type=checkbox]').length>=2");
  await evaluate('Array.from(document.querySelectorAll("tbody tr")).find(row=>row.textContent.includes(' + JSON.stringify(legal.paper.uniqueCode.replace(/\d/g, n => '۰۱۲۳۴۵۶۷۸۹'[n])) + ')).querySelector("input").click()');
  await checkPreview(7); await screenshot('grid-arrest-summary-desktop.png');
  const summary = await api('/api/v1/execution-papers/' + legal.paper.id, token);
  assert.ok(summary.notification.paymentDeadline, 'Summary uses the same payment deadline as the workflow');
  await evaluate("document.querySelector('thead input[type=checkbox]').click()");
  assert.equal(await evaluate(`Boolean(${preview})`), false);
  await clickText('صدور گروهی'); await waitFor("document.querySelector('dialog[open] #arrest-supervisor')");
  await evaluate("document.querySelector('button[aria-label=\"بستن پنجره صدور\"]').click()");
  await evaluate("document.querySelector('thead input[type=checkbox]').click()");
  await evaluate("document.querySelector('tbody input[type=checkbox]').click()"); await waitFor(loaded);
  await evaluate("document.querySelector('#arrest-tab-mine').click()");
  await waitFor("!document.querySelector('#paper-summary-heading')");
  const inbox = await api('/api/v1/arrest-orders?mine=true&size=20', token);
  if (inbox.content.length) {
    await waitFor("document.querySelector('tbody input[type=checkbox]')");
    await evaluate("document.querySelector('tbody input[type=checkbox]').click()"); await waitFor(loaded);
    const code = inbox.content[0].executionPaperCode.replace(/\d/g, n => '۰۱۲۳۴۵۶۷۸۹'[n]);
    assert.ok(await evaluate('document.querySelector("#paper-summary-heading").textContent.includes(' + JSON.stringify(code) + ')'));
    await evaluate("document.querySelector('button[aria-label=\"بستن خلاصه برگ اجرا\"]').click()");
  }
  await evaluate("document.querySelector('#arrest-tab-candidates').click()"); await waitFor("document.querySelector('tbody input[type=checkbox]')");
  assert.equal(await evaluate("document.querySelectorAll('tbody input:checked').length"), 0);
  await evaluate("document.querySelector('button[aria-label=\"تنظیم نمایش فهرست\"]').click()");
  await evaluate("Array.from(document.querySelectorAll('dialog[open] fieldset label')).find(el=>el.textContent==='پایان مهلت پرداخت').querySelector('input').click()");
  await evaluate("document.querySelector('button[aria-label=\"بستن تنظیم نمایش\"]').click()");
  await send('Page.reload'); await waitFor("document.querySelector('tbody input[type=checkbox]')");
  assert.equal(await evaluate("Array.from(document.querySelectorAll('thead th')).some(el=>el.textContent==='پایان مهلت پرداخت')"), false);
  await evaluate("document.querySelector('button[aria-label=\"تنظیم نمایش فهرست\"]').click()"); await clickText('بازگردانی نمایش پیش‌فرض', 'dialog button'); await evaluate("document.querySelector('button[aria-label=\"بستن تنظیم نمایش\"]').click()");
  const natural = candidates.find(row => ['NATURAL', 'حقیقی'].includes(row.paper.taxpayerType));
  assert.ok(natural, 'Need a natural candidate to check the legal-only stage');
  await navigate('/execution-papers?search=' + encodeURIComponent(natural.paper.uniqueCode));
  await waitFor("document.querySelector('tbody input[type=checkbox]')");
  await evaluate("document.querySelector('tbody input[type=checkbox]').click()"); await checkPreview(6);
  assert.equal(await evaluate("Array.from(document.querySelectorAll('[class*=stepName]')).some(el=>el.textContent.includes('محدودیت'))"), false);
  await navigate('/execution-papers/' + legal.paper.id); await waitFor("document.querySelector('#paper-tab-1')");
  await evaluate("document.querySelector('#paper-tab-1').click()");
  assert.equal(await evaluate("document.querySelectorAll('[class*=stepName]').length"), 7);
  await send('Emulation.setDeviceMetricsOverride', { width:390, height:844, deviceScaleFactor:1, mobile:false });
  await navigate(route); await waitFor("document.querySelector('tbody input[type=checkbox]')");
  await evaluate("document.querySelector('tbody input[type=checkbox]').click()"); await checkPreview(7);
  assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'));
  await evaluate("document.querySelector('#paper-summary-heading').scrollIntoView()"); await screenshot('grid-paper-summary-mobile.png');
  await navigate('/arrest-orders'); await waitFor("document.querySelector('tbody input[type=checkbox]')");
  await evaluate("document.querySelector('tbody input[type=checkbox]').click()"); await waitFor(loaded);
  assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'));
  await evaluate("document.querySelector('#paper-summary-heading').scrollIntoView()"); await screenshot('grid-arrest-summary-mobile.png');
  await evaluate("window.gridTestStorageSet=Storage.prototype.setItem; Storage.prototype.setItem=function(key,value){if(key.startsWith('vosol:grid:'))throw new DOMException('Test storage quota','QuotaExceededError'); return window.gridTestStorageSet.call(this,key,value);}");
  await evaluate("document.querySelector('button[aria-label=\"تنظیم نمایش فهرست\"]').click()");
  await evaluate("Array.from(document.querySelectorAll('dialog[open] fieldset label')).find(el=>el.textContent==='پایان مهلت پرداخت').querySelector('input').click()");
  assert.ok(await evaluate("document.querySelector('dialog[open]').scrollWidth<=innerWidth"));
  await evaluate("document.querySelector('button[aria-label=\"بستن تنظیم نمایش\"]').click()");
  assert.equal(await evaluate("Array.from(document.querySelectorAll('#arrest-list-panel thead th')).some(el=>el.textContent==='پایان مهلت پرداخت')"), false);
  await evaluate("Storage.prototype.setItem=window.gridTestStorageSet; delete window.gridTestStorageSet;");
  await evaluate("document.querySelector('button[aria-label=\"تنظیم نمایش فهرست\"]').click()"); await clickText('بازگردانی نمایش پیش‌فرض', 'dialog button'); await evaluate("document.querySelector('button[aria-label=\"بستن تنظیم نمایش\"]').click()");
  assert.deepEqual(exceptions, []);
  console.log('PASS: single and multiple selection, shared real summary and tabs, legal-only stage, full detail page, paging/tab resets, bulk dialog, column persistence/account isolation/reset/storage-quota fallback, desktop/mobile. No process started.');
}

(async()=>{
  try {
    let tabs;
    for(let i=0;i<80;i++){try{tabs=await(await fetch('http://localhost:9327/json/list')).json();break;}catch{await delay(150);}}
    assert.ok(tabs,'Chrome did not start');
    ws=new WebSocket(tabs.find(tab=>tab.type==='page').webSocketDebuggerUrl);
    await new Promise((resolve,reject)=>{ws.onopen=resolve;ws.onerror=reject;});
    ws.onmessage=event=>{const data=JSON.parse(event.data);if(data.id){const item=pending.get(data.id);if(item){clearTimeout(item.timer);pending.delete(data.id);data.error?item.reject(Error(JSON.stringify(data.error))):item.resolve(data.result);}}else if(data.method==='Fetch.requestPaused'){forward(data.params).catch(error=>exceptions.push(error.message));}else if(data.method==='Runtime.exceptionThrown'){exceptions.push(JSON.stringify(data.params.exceptionDetails));}};
    await send('Page.enable');await send('Runtime.enable');
    if(!process.argv.includes('--tehran-ready')) await send('Fetch.enable',{patterns:[{urlPattern:'*/api/*',requestStage:'Request'}]});
    await send('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false});
    if(process.argv.includes('--print-only')) { await navigate('/login');await waitFor('document.body');await exportLetter(fs.readFileSync(path.resolve(__dirname,'../temp/arrest-letter-verified.html'),'utf8')); console.log('PASS: styled one-page A4 letter.'); }
    else if(process.argv.includes('--tehran-ready')) await tehranReadyChecks();
    else if(process.argv.includes('--views-only')) await viewChecks();
    else if(process.argv.includes('--privacy')) await privacyChecks();
    else if(process.argv.includes('--grid-summary')) await gridSummaryChecks();
    else if(process.argv.includes('--paper-views')) await paperViewChecks();
    else await checks();
  } catch(error){console.error(error.stack);process.exitCode=1;if(ws?.readyState===1){try{await screenshot('arrest-browser-failure.png');}catch{}}}
  finally{ws?.close();browser.kill();}
})();
