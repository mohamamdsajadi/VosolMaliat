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

// Explicit API fixtures test frontend behavior; they do not run a Camunda process.
let role = 'DIRECTORATE_EXECUTION_UNIT_HEAD', withSchema = false, failTask = false;
let failPaperTasks = false;
let paperIds = [101,102,103,104,105], failCompletionId = null, mismatchId = null;
const completionAttempts = [];
const claims = [], completions = [], legacyDelegations = [];
const makeTask = (id, complete, claim, name='انتخاب مأمور اجرا جهت ارجاع برگ اجرا') => ({
  taskKey:id, name, processDefinitionId:'55',processDefinitionKey:55,taskDefinitionId:'SelectExecutionOfficer',
  assignee:claim?null:'demo_current_user',candidateGroups:[role],createdAt:'2026-10-19T10:00:00Z',
  variables:[{name:'executionPaperId',value:id},{name:'executionPaperCode',value:'EXEC-'+id}],
  allowedActions:{view:true,complete,claim,delegate:false},
});
const tasks=[makeTask(101,true,false),makeTask(102,false,true),makeTask(103,false,false),makeTask(104,true,false,'بررسی بدهی')];
const schema={type:'default',id:'ReferralDemoForm',schemaVersion:19,components:[
  {type:'textfield',id:'Field_Assignee',key:'selectedAssignee',label:'نام کاربری مسئول',validate:{required:true}},
  {type:'button',id:'Submit_Referral',label:'ثبت ارجاع',action:'submit'},
]};
async function fulfill(event) {
  const url=new URL(event.request.url);requests.push(url.pathname);let body={},code=200;
  if(url.pathname==='/api/v1/auth/config')body={provider:'keycloak',workflow:'camunda'};
  else if(url.pathname==='/api/v1/auth/me')body={username:'demo_current_user',fullName:'کاربر نمونه',businessRole:role,allowedActions:['TASK_VIEW_OWN','TASK_COMPLETE','TASK_CLAIM','EXECUTION_PAPER_DETAIL_VIEW','EXECUTION_PAPER_LIST_VIEW']};
  else if(url.pathname==='/api/v1/cartable/my-tasks') {code=failPaperTasks?503:200;body=failPaperTasks?{message:'خطای آزمایشی کارتابل'}:tasks.filter(t=>!completions.some(c=>c.id===t.taskKey)).map(t=>({...t,candidateGroups:[role]}));}
  else if(url.pathname==='/api/v1/execution-papers')body={content:paperIds.map(id=>({id,uniqueCode:'EXEC-'+id,taxpayerName:'مودی آزمایشی',totalDebt:'100',taxYear:1404,taxPeriod:1})),totalElements:paperIds.length,totalPages:1};
  else if(/\/tasks\/\d+$/.test(url.pathname)) {code=failTask?503:200;const task=tasks.find(t=>t.taskKey===Number(url.pathname.split('/').at(-1)));body={...task,schema:withSchema?{...schema,id:task?.taskKey===mismatchId?'OtherForm':schema.id}:undefined};}
  else if(url.pathname.endsWith('/claim')) {const id=Number(url.pathname.split('/').at(-2));claims.push(id);const task=tasks.find(t=>t.taskKey===id);task.allowedActions={...task.allowedActions,claim:false,complete:true};task.assignee='demo_current_user';code=204;}
  else if(url.pathname.endsWith('/complete')) {const id=Number(url.pathname.split('/').at(-2));completionAttempts.push(id);if(id===failCompletionId){code=503;body={message:'خطای آزمایشی ثبت'};}else{completions.push({id,body:JSON.parse(event.request.postData)});code=204;}}
  else if(url.pathname.endsWith('/delegate')||url.pathname.endsWith('/delegation-targets')) {legacyDelegations.push(url.pathname);code=500;}
  await send('Fetch.fulfillRequest',{requestId:event.requestId,responseCode:code,responseHeaders:[{name:'Content-Type',value:'application/json; charset=utf-8'}],body:code===204?'':Buffer.from(JSON.stringify(body)).toString('base64')});
}
async function screenshot(name){await evaluate('document.fonts.ready');await delay(150);const r=await send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(path.resolve(__dirname,'../temp/'+name),Buffer.from(r.data,'base64'));}
async function runPaperChecks(){
  await navigate('/execution-papers');await waitFor("document.querySelectorAll('[data-paper-referral]').length===5");
  assert.ok(await evaluate("[...document.querySelectorAll('[data-paper-referral]')].every(b=>b.previousElementSibling?.hasAttribute('data-paper-details'))"));
  await screenshot('execution-paper-referral-desktop.png');
  assert.ok(await evaluate("[...document.querySelectorAll('[data-paper-referral]')].every(b=>b.textContent.trim()===''&&b.getBoundingClientRect().width===b.previousElementSibling.getBoundingClientRect().width)"));
  await evaluate("document.querySelector('[data-paper-referral]').click()");await waitFor("document.querySelector('dialog[open]').innerText.includes('فرم انتخاب مقصد این مرحله هنوز در دسترس نیست')");
  await evaluate("document.querySelector('dialog[open] p').click()");assert.equal(await evaluate('location.pathname'),'/execution-papers');await escape();
  await evaluate("document.querySelectorAll('[data-paper-referral]')[2].click()");await waitFor("document.querySelector('dialog[open]').innerText.includes('مجوز ارجاع در وضعیت فعلی وجود ندارد')");await escape();
  for(const index of [3,4]){await evaluate(`document.querySelectorAll('[data-paper-referral]')[${index}].click()`);await waitFor("document.querySelector('dialog[open]').innerText.includes('وظیفه ارجاعی برای این برگ')");await escape();}
  failPaperTasks=true;await navigate('/execution-papers');await waitFor("document.querySelectorAll('[data-paper-referral]').length===5");await evaluate("document.querySelector('[data-paper-referral]').click()");await waitFor("document.querySelector('dialog[open] [role=alert]')");
  failPaperTasks=false;await evaluate("[...document.querySelectorAll('dialog[open] button')].find(b=>b.textContent.includes('به‌روزرسانی')).click()");await waitFor("document.querySelector('dialog[open]').innerText.includes('فرم انتخاب مقصد این مرحله هنوز در دسترس نیست')");await escape();
  await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:false});assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'));
  await evaluate("document.querySelectorAll('[data-paper-referral]')[4].click()");await waitFor("document.querySelector('dialog[open]')");assert.ok(await evaluate("document.querySelector('dialog[open]').getBoundingClientRect().width<=innerWidth"));await screenshot('execution-paper-referral-mobile.png');await escape();
  await send('Emulation.setDeviceMetricsOverride',{width:1440,height:1024,deviceScaleFactor:1,mobile:false});
  role='EXECUTION_OFFICER';await navigate('/execution-papers');await waitFor("document.querySelectorAll('[data-paper-details]').length===5");assert.equal(await evaluate("document.querySelectorAll('[data-paper-referral]').length"),0);
  role='DIRECTORATE_EXECUTION_UNIT_HEAD';console.log('PASS: execution-paper row referral placement, matching task, unavailable/wrong-stage, denied actions, retry, role visibility and mobile dialog.');
}
async function runChecks(){
  const manualRoles=['DIRECTORATE_EXECUTION_UNIT_HEAD','DIRECTORATE_EXECUTION_TEAM_SUPERVISOR','SPECIAL_COLLECTION_DIRECTOR_GENERAL','SPECIAL_EXECUTION_DEPUTY','SPECIAL_EXECUTION_DEPARTMENT_HEAD'];
  for(const value of manualRoles){role=value;await navigate('/tasks');await waitFor("document.querySelectorAll('.task-grid-row').length===4");assert.equal(await evaluate("document.querySelectorAll('a[aria-label^=\"ارجاع برگ اجرا\"]').length"),3);assert.equal(await evaluate("document.querySelectorAll('button[aria-label^=\"ارجاع برگ اجرا\"]:disabled').length"),1);}
  for(const value of ['EXECUTION_OFFICER','TAX_COLLECTION_OFFICE','TAX_ORGANIZATION_HEAD','TAX_DIRECTORATE_DIRECTOR_GENERAL','EXECUTION_DEPUTY','ADMIN']){role=value;await navigate('/tasks');await waitFor("document.querySelectorAll('.task-grid-row').length===4");assert.equal(await evaluate("document.querySelectorAll('[aria-label^=\"ارجاع برگ اجرا\"]').length"),0);}
  role=manualRoles[0];await navigate('/tasks');await waitFor("document.querySelector('a[aria-label=\"ارجاع برگ اجرا EXEC-101\"]')");
  assert.ok(await evaluate("document.querySelector('a[aria-label=\"ارجاع برگ اجرا EXEC-101\"]').previousElementSibling.href.includes('/execution-papers/101')"));assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'));await screenshot('task-referral-cartable-desktop.png');
  await evaluate("document.querySelector('a[aria-label=\"ارجاع برگ اجرا EXEC-101\"]').click()");await waitFor("location.search.includes('intent=referral')&&document.body.innerText.includes('فرم ارجاع این مرحله هنوز در دسترس نیست')");assert.equal(completions.length,0);await screenshot('task-referral-unavailable.png');
  withSchema=true;await send('Page.reload');await waitFor("document.querySelector('.fjs-form input')");
  await delay(400);await evaluate("document.querySelector('.fjs-form input').focus()");await send('Input.insertText',{text:'demo_execution_officer'});
  await send('Input.dispatchKeyEvent',{type:'keyDown',key:'Tab',code:'Tab',windowsVirtualKeyCode:9});await send('Input.dispatchKeyEvent',{type:'keyUp',key:'Tab',code:'Tab',windowsVirtualKeyCode:9});await delay(700);
  await screenshot('task-referral-form-desktop.png');await evaluate("Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim()==='ثبت ارجاع').click()");await waitFor("location.pathname==='/tasks'");assert.equal(completions[0].id,101);assert.equal(completions[0].body.variables.selectedAssignee,'demo_execution_officer');assert.equal(completions[0].body.variables.finishExecutionPaperCheck,undefined);
  withSchema=false;await navigate('/processes/55/tasks');await waitFor("document.querySelector('a[aria-label=\"ارجاع برگ اجرا EXEC-102\"]')");
  assert.ok(await evaluate("document.querySelector('a[aria-label=\"ارجاع برگ اجرا EXEC-102\"]').href.includes('processDefinitionKey=55')"));await evaluate("document.querySelector('a[aria-label=\"ارجاع برگ اجرا EXEC-102\"]').click()");await waitFor("document.body.innerText.includes('برای تکمیل فرم ارجاع، ابتدا کار را دریافت کنید')");await evaluate("Array.from(document.querySelectorAll('button')).find(b=>b.textContent==='دریافت کار').click()");await waitFor("document.body.innerText.includes('فرم ارجاع این مرحله هنوز در دسترس نیست')");assert.deepEqual(claims,[102]);assert.ok(await evaluate("document.querySelector('a.task-page-back-action').href.endsWith('/processes/55/tasks')"));
  await navigate('/tasks/104?intent=referral');await waitFor("document.body.innerText.includes('مرحله جاری، مرحله انتخاب مسئول و ارجاع نیست')");
  role='EXECUTION_OFFICER';await navigate('/tasks/102?intent=referral');await waitFor("document.body.innerText.includes('شما مجوز ارجاع این برگ اجرا را ندارید')");
  role=manualRoles[0];failTask=true;await navigate('/tasks/102?intent=referral');await waitFor("document.querySelector('.notice.error')");failTask=false;await evaluate("Array.from(document.querySelectorAll('button')).find(b=>b.textContent==='بروزرسانی وضعیت کار').click()");await waitFor("document.body.innerText.includes('فرم ارجاع این مرحله هنوز در دسترس نیست')");
  await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:false});await send('Page.reload');await waitFor("document.body.innerText.includes('فرم ارجاع این مرحله هنوز در دسترس نیست')");assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'));assert.ok(await evaluate("document.querySelector('.formal-header').getBoundingClientRect().height<260"));await screenshot('task-referral-mobile.png');
  await waitFor("document.querySelector('[aria-controls=app-sidebar]').getAttribute('aria-expanded')==='false'");
  await evaluate("document.querySelector('[aria-controls=app-sidebar]').click()");await waitFor("document.activeElement.getAttribute('aria-label')==='بستن منوی کارتابل'");await escape();await waitFor("document.querySelector('#app-sidebar').inert===true");
  assert.deepEqual(legacyDelegations,[]);assert.deepEqual(exceptions,[]);assert.deepEqual(externalRequests,[]);console.log('PASS: referral roles, row placement, permissions, unavailable/wrong-stage states, claim, process form completion, retry, process return link and mobile.');
}
async function runBulkChecks(){
  role='DIRECTORATE_EXECUTION_UNIT_HEAD';withSchema=true;
  paperIds=[201,202,203];tasks.push(makeTask(201,true,false),makeTask(202,false,true),makeTask(203,true,false));
  await send('Emulation.setDeviceMetricsOverride',{width:1440,height:1024,deviceScaleFactor:1,mobile:false});
  await navigate('/execution-papers');await waitFor("document.querySelectorAll('[data-paper-referral]').length===3");
  await evaluate("document.querySelector('input[aria-label=\"انتخاب همه برگ‌های این صفحه\"]').click()");
  assert.equal(await evaluate("document.querySelectorAll('tbody input:checked').length"),3);
  await evaluate("document.querySelector('tbody input').click()");
  assert.equal(await evaluate("document.querySelector('thead input').indeterminate"),true);assert.equal(await evaluate('location.pathname'),'/execution-papers');
  await evaluate("document.querySelector('tbody input').click()");
  const sortBy=async label=>{await evaluate(`document.querySelector('button[aria-label="مرتب‌سازی بر اساس ${label}"]').click()`);await waitFor("document.querySelectorAll('tbody input[type=checkbox]').length===3");};
  await sortBy('تاریخ صدور');assert.equal(await evaluate("document.querySelectorAll('tbody input:checked').length"),0);
  await evaluate("document.querySelector('thead input').click()");await sortBy('سال');await sortBy('تاریخ صدور');assert.equal(await evaluate("document.querySelectorAll('tbody input:checked').length"),0);
  await evaluate("document.querySelector('thead input').click()");await screenshot('execution-paper-bulk-selection.png');
  const openBulk=()=>evaluate("[...document.querySelectorAll('button')].find(b=>b.textContent.trim()==='ارجاع گروهی').click()");
  const refresh=()=>evaluate("[...document.querySelectorAll('dialog[open] button')].find(b=>b.textContent.includes('به‌روزرسانی')).click()");
  mismatchId=203;await openBulk();await waitFor("document.querySelector('dialog[open] [role=alert]')");assert.equal(await evaluate("document.querySelectorAll('dialog[open] .fjs-form').length"),0);
  mismatchId=null;await refresh();await waitFor("document.querySelector('dialog[open] .fjs-form input')");
  assert.ok(await evaluate("document.querySelector('dialog[open] fieldset').disabled"));
  async function fillAndSubmit(){
    await evaluate("document.querySelector('dialog[open] label input[type=checkbox]').click()");await delay(400);
    await evaluate("document.querySelector('dialog[open] .fjs-form input').focus()");await send('Input.insertText',{text:'demo_execution_officer'});
    await send('Input.dispatchKeyEvent',{type:'keyDown',key:'Tab',code:'Tab',windowsVirtualKeyCode:9});await send('Input.dispatchKeyEvent',{type:'keyUp',key:'Tab',code:'Tab',windowsVirtualKeyCode:9});await delay(700);
    await screenshot('execution-paper-bulk-form.png');
    await evaluate("[...document.querySelectorAll('dialog[open] button')].find(b=>b.textContent.trim()==='ثبت ارجاع').click()");
  }
  failCompletionId=202;await fillAndSubmit();await waitFor("document.querySelector('dialog[open]').innerText.includes('عملیات متوقف شد')");
  assert.deepEqual(completionAttempts.filter(id=>id>=200),[201,202]);assert.equal(claims.filter(id=>id===202).length,1);
  assert.equal(completions.filter(c=>c.id===201).length,1);await screenshot('execution-paper-bulk-partial.png');
  failCompletionId=null;await refresh();await waitFor("document.querySelector('dialog[open] .fjs-form input')");await fillAndSubmit();
  await waitFor("document.querySelector('dialog[open]').innerText.includes('EXEC-۲۰۳: ارجاع شد')||document.querySelector('dialog[open]').innerText.includes('EXEC-203: ارجاع شد')");
  assert.deepEqual(completionAttempts.filter(id=>id>=200),[201,202,202,203]);
  for(const id of paperIds){const done=completions.filter(c=>c.id===id);assert.equal(done.length,1);assert.deepEqual(done[0].body.variables,{selectedAssignee:'demo_execution_officer'});}
  await escape();await waitFor("document.querySelectorAll('[data-paper-referral]').length===3");assert.equal(await evaluate("document.querySelectorAll('tbody input:checked').length"),0);
  await evaluate("document.querySelector('thead input').click()");await evaluate("[...document.querySelectorAll('button')].find(b=>b.textContent.trim()==='لغو انتخاب').click()");assert.equal(await evaluate("document.querySelectorAll('tbody input:checked').length"),0);
  await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:false});await evaluate("document.querySelector('thead input').click()");await openBulk();await waitFor("document.querySelector('dialog[open]').innerText.includes('وظیفه ارجاعی برای این برگ')");
  assert.ok(await evaluate("document.querySelector('dialog[open]').getBoundingClientRect().width<=innerWidth"));assert.ok(await evaluate('document.documentElement.scrollWidth<=innerWidth'));await screenshot('execution-paper-bulk-mobile.png');await escape();
  assert.deepEqual(exceptions,[]);assert.deepEqual(legacyDelegations,[]);console.log('PASS: icon-only row actions, page selection, indeterminate/select-all/clear, schema compatibility, common destination, candidate claim, partial failure, remaining-only retry, exact payload isolation and mobile.');
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
  await send('Page.addScriptToEvaluateOnNewDocument', {source:"localStorage.setItem('access_token','paper-browser-fixture');"});
  await send('Emulation.setDeviceMetricsOverride',{width:1440,height:1024,deviceScaleFactor:1,mobile:false});
  await runPaperChecks();await runChecks();await runBulkChecks();
})().catch(error => { console.error(error); process.exitCode = 1; }).finally(async () => {
  if (ws?.readyState === WebSocket.OPEN) { try { await send('Browser.close'); } catch {} ws.close(); }
  browser.kill();
  // The OS temp profile can be removed after Chrome releases its files.
});
