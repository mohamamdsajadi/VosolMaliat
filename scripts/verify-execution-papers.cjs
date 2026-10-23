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

const papers=Array.from({length:50},(_,i)=>({
 id:i+1,uniqueCode:['1-140456305','200000359979083','200000359312767','200000359661853'][i%4],
 taxpayerNationalId:['1743760930','1743459996','1743760930','1755678509'][i%4],
 taxpayerCode:['038627471740002','140100330126','140104422113','140109202015'][i%4],
 taxpayerName:['مهدی دهقانی سازی','انتقال نیروی آذرخش','یاسمن بلند','پیشگامان صنعت'][i%4],
 taxpayerType:i%2?'حقوقی':'حقیقی',taxType:i%3?'برگ قطعی مالیات و عوارض ارزش افزوده':'برگ اجرایی',taxYear:[1404,1402,1405,1404][i%4],taxPeriod:i%4+1,
 totalDebt:['516074617','12775885746','3347392540','10567585346'][i%4],
 issuanceDate:'2026-05-13T12:00:00',currentState:i<8?'UNASSIGNED':'ASSIGNED',
}));
const detail={
 summary:{...papers[0],taxpayerType:'حقوقی'},
 executionPaper:{id:1,uniqueCode:papers[0].uniqueCode,issuanceDate:'2026-08-30T12:00:00',currentState:'ARREST_ORDER_ISSUED',otherActivePapers:[{id:2,uniqueCode:papers[1].uniqueCode}]},
 taxpayer:{type:'LEGAL',companyName:papers[0].taxpayerName,nationalId:papers[0].taxpayerNationalId,tin:papers[0].taxpayerCode,tradeName:'نساجی هدیه البرز مشهد',postalCode:'1234567890',address:'تهران، خیابان نمونه'},
 finalTaxDebtPaper:{paperType:'EF23',finalizationReason:'FINAL_NOTICE',debtPaperUniqueCode:'200000304562689',finalTaxDebtNoticeIssueDate:'2026-08-21T12:00:00',generalDirectorateName:'اداره کل امور مالیاتی اصفهان',taxAffairsOfficeName:'اداره امور مالیاتی کاشان',taxAffairsOfficePostalCode:'8716731147',taxAffairsOfficeAddress:'کاشان، میدان جهاد'},
 notification:{},
 debtInformation:{currentAmounts:{totalTaxAmount:'1253069720',totalDutyAmount:'665534860',totalPenaltyAmount:'370019730',outstandingDebtAmount:'3126680867'},outstandingDebtAmount:'3126680867',history:[
  {id:1,receivedAt:'2026-08-30T12:00:00',principalAmount:'1918604580',creditAmount:'0',penaltyAmount:'350019730',remissionAmount:'103012443',paidAmount:'0',outstandingAmount:'2126680867',current:false},
  {id:2,receivedAt:'2026-09-12T12:00:00',principalAmount:'1918604580',creditAmount:'10.01',penaltyAmount:'370019730',remissionAmount:'103012443',paidAmount:'1000000000',outstandingAmount:'3126680867',current:true},
 ]},ruleRejection:{},
};
detail.debtInformation.history.forEach(row=>{row.financialDetails={
 taxLiabilityDetails:'<details category="LIABILITY" paperType="EF23"><DB>1253069720</DB><EB>665534860</EB><PD>-1.01</PD><NB>1253069720</NB><OB>665534860</OB></details>',
 taxCreditDetails:`<details paperType="EF23"><XB>${row.creditAmount}</XB></details>`,
 totalPenaltiesDetails:`<details paperType="EF23"><DC>${row.penaltyAmount}</DC></details>`,
 totalRemissionsDetails:'<details/>',totalPaymentsDetails:`<details paperType="EF23"><JC>${row.paidAmount}</JC></details>`,
 outstandingDebtDetails:`<details><item><value name="tag">LC</value><value name="amount">${row.outstandingAmount}</value><value name="reported">true</value></item></details>`
};});
async function fulfill(event){
 const url=new URL(event.request.url);requests.push(url.pathname+url.search); let body={},code=200;
 if(url.pathname==='/api/v1/auth/config') body={provider:'keycloak',workflow:'camunda'};
 else if(url.pathname==='/api/v1/auth/me') body={username:'manager',fullName:'مهدی احمدی',businessRole:'TAX_DIRECTORATE_DIRECTOR_GENERAL',organization:{generalDirectorateName:'اداره کل امور مالیاتی تهران'},allowedActions:['TASK_VIEW_OWN','EXECUTION_PAPER_DETAIL_VIEW','EXECUTION_PAPER_TRACE_VIEW',...(allowReport?['EXECUTION_PAPER_LIST_VIEW']:[])]};
 else if(url.pathname==='/api/v1/dashboard/stats') body={totalIssuedExecutionPapers:300};
 else if(url.pathname==='/api/v1/cartable/my-tasks') body=[];
 else if(url.pathname.match(/execution-papers(\/(assigned|unassigned))?$/)){code=failList?503:200;body={content:empty?[]:papers,totalElements:empty?0:300,totalPages:empty?0:6,pageNumber:Number(url.searchParams.get('page')||0),pageSize:50};}
 else if(url.pathname.match(/\/execution-papers\/[12]$/)){code=failDetail?503:200;const id=Number(url.pathname.split('/').at(-1));body={...detail,summary:{...papers[id-1],taxpayerType:'حقوقی'},executionPaper:{...detail.executionPaper,id,uniqueCode:papers[id-1].uniqueCode}};}
 else if(url.pathname.endsWith('/trace'))body={items:[{type:'STATE_CHANGE',action:'EXECUTION_PAPER_ISSUED',label:'صدور برگ اجرا',actorUsername:'ali',occurredAt:'2026-08-30T12:00:00'},{type:'STATE_CHANGE',action:'INFORMED',label:'ابلاغ برگ اجرا',occurredAt:'2026-08-31T12:00:00'},{type:'STATE_CHANGE',action:'ARREST_ORDER_ISSUED',label:'دستور بازداشت صادر شد',actorUsername:'ali',occurredAt:'2026-09-03T10:43:00'},{type:'AUDIT',action:'RESTRICTION_REQUEST_SENT',label:'ارسال درخواست محدودیت',occurredAt:'2026-09-04T12:00:00'}]};
 else if(url.pathname.endsWith('/related-persons')||url.pathname.endsWith('/refresh'))body={persons:[]};
 else if(url.pathname.endsWith('/external-requests'))body=[];
 await send('Fetch.fulfillRequest',{requestId:event.requestId,responseCode:code,responseHeaders:[{name:'Content-Type',value:'application/json; charset=utf-8'}],body:Buffer.from(JSON.stringify(body)).toString('base64')});
}
async function screenshot(name){await evaluate('document.fonts.ready');await delay(200);const r=await send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(path.resolve(__dirname,'../temp/'+name),Buffer.from(r.data,'base64'));}
async function assertAssets(){ const assets=await evaluate("Array.from(document.images).filter(i=>i.getBoundingClientRect().width>0&&i.getBoundingClientRect().height>0).map(i=>({src:new URL(i.src).pathname,w:i.getBoundingClientRect().width,h:i.getBoundingClientRect().height,loaded:i.complete&&i.naturalWidth>0}))");for(const asset of assets){assert.ok(asset.loaded,asset.src);if(!asset.src.endsWith('.svg'))continue;const filename=path.resolve(__dirname,'../src/main/frontend/public'+asset.src);const svg=fs.readFileSync(filename,'utf8');const root=svg.match(/<svg[^>]+>/)[0];assert.ok(fs.statSync(filename).size>0);assert.equal(asset.w,Number(root.match(/width="([^"]+)"/)[1]),asset.src+' width');assert.equal(asset.h,Number(root.match(/height="([^"]+)"/)[1]),asset.src+' height');}}
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
  await send('Page.addScriptToEvaluateOnNewDocument', {source:"localStorage.setItem('access_token','paper-browser-fixture');"});
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1024, deviceScaleFactor: 1, mobile: false });

 await navigate('/execution-papers');
 await waitFor("document.querySelectorAll('tbody tr').length===50");
 await screenshot('execution-papers-desktop.png');await assertAssets();
 const geometry=await evaluate("Array.from(document.querySelectorAll('main form,main section')).map(el=>{const r=el.getBoundingClientRect();return {label:el.getAttribute('aria-label'),x:r.x,y:r.y,w:r.width,h:r.height}})");console.log(JSON.stringify(geometry));
 const listGeometry=geometry.find(item=>item.label==='فهرست برگ‌های اجرا');
 assert.deepEqual(listGeometry,{label:'فهرست برگ‌های اجرا',x:28,y:176,w:1121,h:820});
 assert.equal(await evaluate("document.querySelectorAll('thead th').length"),10);
 assert.equal(await evaluate("document.querySelectorAll('[data-paper-details]').length"),50);
 assert.ok(await evaluate("!document.querySelector('[data-identity-status]')"));
 assert.ok(await evaluate("Array.from(document.querySelectorAll('thead th')).some(th=>th.textContent==='شناسه مودی')"));
 const firstRow=await evaluate("Array.from(document.querySelector('tbody tr').cells).map(cell=>cell.innerText)");
 assert.equal(firstRow[2],'برگ اجرایی');assert.equal(firstRow[3],'۰۳۸۶۲۷۴۷۱۷۴۰۰۰۲');assert.equal(firstRow[7],'۱۴۰۴');assert.equal(firstRow[8],'۱');
 assert.equal(await evaluate("document.querySelector('tbody tr:nth-child(2) td:nth-child(3)').textContent"),'برگ قطعی مالیات و عوارض ارزش افزوده');
 assert.ok(await evaluate("document.querySelector('main').innerText.includes('آخرین به‌روزرسانی اکنون')"));
 await evaluate("document.querySelector('[aria-label=\"راهنمای صفحه برگ‌های اجرا\"]').click()");await waitFor("document.querySelector('dialog[open]')");
 await escape();await waitFor("!document.querySelector('dialog[open]')");
 await evaluate("document.querySelector('[aria-label=\"تنظیم نمایش فهرست\"]').click()");await waitFor("document.querySelector('dialog[open]')");
 await evaluate("document.querySelector('dialog[open] fieldset input').click()");assert.equal(await evaluate("document.querySelectorAll('thead th').length"),9);
 await evaluate("document.querySelector('dialog[open] fieldset input').click()");assert.equal(await evaluate("document.querySelectorAll('thead th').length"),10);
 await escape();
 assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'));
 await evaluate("document.querySelector('[data-paper-details]').click()");
 await waitFor("document.getElementById('paper-detail-heading')&&document.querySelector('[aria-selected=true]')&&document.querySelector('[aria-label=\"جدول تاریخچه بدهی\"] tbody tr')");
 await screenshot('execution-paper-detail-desktop.png');await assertAssets();
 const detailGeometry=await evaluate("(()=>{const r=document.querySelector('section[aria-labelledby=paper-detail-heading]').getBoundingClientRect();return{x:r.x,y:r.y,w:r.width,h:r.height}})()");console.log('DETAIL',detailGeometry);
 assert.deepEqual(detailGeometry,{x:28,y:161,w:1121,h:835});
 assert.equal(await evaluate("document.querySelectorAll('[aria-label=\"خلاصه برگ اجرای انتخاب‌شده\"] tbody tr').length"),1);
 assert.equal(await evaluate("document.querySelectorAll('[aria-label=\"جدول تاریخچه بدهی\"] tbody tr').length"),2);
 assert.equal(await evaluate("document.querySelectorAll('[aria-label=\"جدول تاریخچه بدهی\"] tbody button').length"),12);
 for (const row of ['رکورد اولیه','وضعیت جاری']) for(const group of ['اصل مالیات و عوارض','اعتبار مالیاتی','جرائم','بخشودگی','پرداختی','مانده بدهی']) {
   await evaluate(`document.querySelector('button[aria-label="جزئیات ${group} — ${row}"]').click()`);await waitFor("document.querySelector('dialog[open] table')");
   if(group==='اصل مالیات و عوارض') {assert.ok(await evaluate("document.querySelector('dialog[open]').innerText.includes('کالا و خدمات')"));assert.equal(await evaluate("document.querySelector('dialog[open] tfoot td').innerText"),'۱٬۹۱۸٬۶۰۴٬۵۸۰');assert.equal(await evaluate("document.querySelectorAll('dialog[open] tbody tr').length"),5);}
   if(group==='بخشودگی') assert.ok(await evaluate("document.querySelector('dialog[open]').innerText.includes('جزئیاتی برای این گروه ثبت نشده')"));
   if(group==='پرداختی') assert.equal(await evaluate("document.querySelector('dialog[open] tbody td:last-child').innerText"),row==='رکورد اولیه'?'۰':'۱٬۰۰۰٬۰۰۰٬۰۰۰');
   if(group==='اصل مالیات و عوارض'&&row==='وضعیت جاری')await screenshot('execution-paper-debt-breakdown.png');
   await escape();
 }
 await evaluate("document.querySelector('[aria-label=\"برگ‌های اجرای فعال دیگر این مؤدی\"] button').click()");await waitFor("document.querySelector('dialog[open]')");
 assert.ok(await evaluate("document.querySelector('dialog[open] a').href.includes('/execution-papers/2')"));await escape();
 await evaluate("document.getElementById('paper-tab-1').click()");await waitFor("document.querySelector('[aria-label=\"مراحل فرآیند اجرایی\"] li')");
 assert.equal(await evaluate("document.querySelectorAll('[aria-label=\"مراحل فرآیند اجرایی\"] li').length"),7);
 await screenshot('execution-paper-actions-desktop.png');await assertAssets();
 await evaluate("document.querySelector('[aria-label=\"جزئیات مرحله انتخاب‌شده\"] button').click()");await waitFor("document.querySelector('dialog[open]')");
 await evaluate("window.print=()=>{window.paperPrintCalled=true};document.querySelector('dialog[open] button[class*=outlineAction]').click()");await waitFor('window.paperPrintCalled===true');
 await send('Emulation.setEmulatedMedia',{media:'print'});
 await evaluate("document.body.classList.add('execution-printing')");
 assert.ok(await evaluate("getComputedStyle(document.querySelector('main').parentElement).display==='none'"));
 assert.ok(await evaluate("getComputedStyle(document.querySelector('dialog[open]')).display==='none'"));
 assert.ok(await evaluate("getComputedStyle(document.querySelector('.execution-print-template')).display==='block'"));
 await send('Emulation.setEmulatedMedia',{media:''});await evaluate("window.dispatchEvent(new Event('afterprint'))");
 assert.ok(await evaluate("!document.body.classList.contains('execution-printing')"));await escape();
 await evaluate("Array.from(document.querySelectorAll('button')).find(b=>b.textContent==='سوابق و استعلام‌ها').click()");await waitFor("document.querySelector('dialog[open]')");await escape();
 await evaluate("document.getElementById('paper-tab-2').click()");await waitFor("document.body.innerText.includes('تهران، خیابان نمونه')");await screenshot('execution-paper-taxpayer-desktop.png');
 await evaluate("document.getElementById('paper-tab-3').click()");await waitFor("document.body.innerText.includes('اداره کل امور مالیاتی اصفهان')");await screenshot('execution-paper-file-desktop.png');
 await evaluate("document.querySelector('[aria-label=\"مسیر صفحه\"] a[href^=\"/execution-papers\"]').click()");await waitFor("!document.getElementById('paper-detail-heading')&&location.pathname==='/execution-papers'");
 await evaluate("(()=>{const el=document.getElementById('paper-search');Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(el,'۱۲۳');el.dispatchEvent(new Event('input',{bubbles:true}));})()");
 await waitFor("location.search.includes('search=123')");assert.ok(requests.some(x=>x.includes('search=123')));
 await evaluate("document.querySelector('[aria-label=\"مرتب‌سازی بر اساس مانده بدهی(ریال)\"]').click()");await waitFor("location.search.includes('sortBy=totalDebt')");
 await evaluate("document.querySelector('[aria-label=\"مرتب‌سازی بر اساس سال\"]').click()");await waitFor("location.search.includes('sortBy=taxYear')");
 await evaluate("document.querySelector('[aria-controls=paper-filters]').click()");await waitFor("document.getElementById('paper-filters')");
 await evaluate("document.querySelector('[aria-label=\"تاریخ صدور از\"]').click()");await waitFor("document.querySelector('[role=dialog]')");await assertAssets();
 await evaluate("document.querySelector('[role=dialog] [aria-pressed]').click()");await waitFor("location.search.includes('issuedFrom=')");
 await waitFor("!document.querySelector('[aria-label=\"صفحه بعد\"]').disabled");await evaluate("document.querySelector('[aria-label=\"صفحه بعد\"]').click()");await waitFor("location.search.includes('page=1')");
 await navigate('/execution-papers/1');await waitFor("document.getElementById('paper-detail-heading')");
 await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:false});await send('Page.reload');await waitFor("document.getElementById('paper-detail-heading')&&document.querySelector('[aria-label=\"باز کردن منوی اصلی\"]')");await delay(200);
 await screenshot('execution-paper-mobile.png');assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'));await assertAssets();
 await evaluate("document.querySelector('button[aria-label=\"جزئیات اصل مالیات و عوارض — وضعیت جاری\"]').click()");await waitFor("document.querySelector('dialog[open] table')");
 assert.ok(await evaluate("(()=>{const r=document.querySelector('dialog[open]').getBoundingClientRect();return r.x>=0&&r.right<=innerWidth})()"));
 await screenshot('execution-paper-debt-breakdown-mobile.png');await escape();
 await evaluate("document.querySelector('[aria-label=\"باز کردن منوی اصلی\"]').click()");await waitFor("document.querySelector('aside').inert===false");
 await escape();
 await send('Emulation.setDeviceMetricsOverride',{width:1440,height:1024,deviceScaleFactor:1,mobile:false});
 failList=true;await navigate('/execution-papers');await waitFor("document.querySelector('main [role=alert]')");
 failList=false;await evaluate("Array.from(document.querySelectorAll('main [role=alert] button')).at(-1).click()");await waitFor("document.querySelectorAll('tbody tr').length===50");
 empty=true;await navigate('/execution-papers');await waitFor("document.body.innerText.includes('یافت نشد')");empty=false;
 failDetail=true;await navigate('/execution-papers/1');await waitFor("document.querySelector('main [role=alert]')");failDetail=false;
 await evaluate("document.querySelector('main [role=alert] button').click()");await waitFor("document.getElementById('paper-tab-0')");
 allowReport=false;await navigate('/execution-papers');await waitFor("document.body.innerText.includes('شما مجوز مشاهده فهرست')");
 await navigate('/execution-papers/1');await waitFor("document.getElementById('paper-tab-0')");assert.ok(await evaluate("!document.body.innerText.includes('شما مجوز مشاهده فهرست')"));
 assert.deepEqual(exceptions,[]);assert.deepEqual(externalRequests,[]);console.log('PASS: list/detail, tabs, preserved filters, date calendar, pagination, retry, empty/permission states, desktop/mobile assets and layout.');
})().catch(error => { console.error(error); process.exitCode = 1; }).finally(async () => {
  if (ws?.readyState === WebSocket.OPEN) { try { await send('Browser.close'); } catch {} ws.close(); }
  browser.kill();
  // The OS temp profile can be removed after Chrome releases its files.
});
