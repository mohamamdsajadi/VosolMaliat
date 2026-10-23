// Read-only validation; already-started orders and user progress are preserved.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const base = process.env.BACKEND_URL || 'http://localhost:8231';
const report = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../temp/tehran-arrest-demo-ef03.mock.sqlite.json')));
const legalReportPath = path.resolve(__dirname, '../temp/tehran-arrest-demo-legal-ef03.mock.sqlite.json');
const legalReport = fs.existsSync(legalReportPath) ? JSON.parse(fs.readFileSync(legalReportPath)) : null;
const papers = [...new Map([...report.papers, ...(legalReport?.papers || [])].map(paper => [paper.id, paper])).values()];
async function login(username) {
  const response = await fetch(base + '/api/v1/auth/mock/login', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({username}) });
  assert.ok(response.ok); return (await response.json()).accessToken;
}
async function api(url, token) {
  const response = await fetch(base + url, {headers:{Authorization:'Bearer ' + token}});
  const data = await response.json(); assert.ok(response.ok, url + ': ' + JSON.stringify(data)); return data;
}
(async () => {
  for (let attempt = 0; attempt < 100; attempt++) {
    try { if ((await fetch(base + '/api/v1/auth/config')).ok) break; } catch {}
    await new Promise(resolve => setTimeout(resolve, 300));
  }
  for (const user of report.users) {
    const me = await api('/api/v1/auth/me', await login(user.username));
    assert.equal(me.organization.generalDirectorateId, report.directorate.id);
    assert.equal(me.organization.taxAffairsOfficeId, report.office.id);
  }
  const head = await login(report.users[0].username);
  const candidates = await api('/api/v1/arrest-orders/candidates?size=100', head);
  let ready = 0, started = 0, legal = 0;
  for (const paper of papers) {
    const detail = await api('/api/v1/execution-papers/' + paper.id, head);
    const orders = await api('/api/v1/arrest-orders?executionPaperId=' + paper.id, head);
    const trace = await api('/api/v1/execution-papers/' + paper.id + '/trace', head);
    const actions = trace.items.map(event => event.action);
    for (const action of ['EXECUTION_PAPER_CREATED','TASK_ASSIGNED','ELECTRONIC_NOTIFICATION_CONFIRMED',
      'PAYMENT_DEADLINE_EXPIRED','TAXPAYER_DEBT_STATUS_UPDATED_FROM_SANIM','EXECUTION_PAPER_REQUIRES_COLLECTION']) assert.ok(actions.includes(action), action);
    assert.equal(actions.filter(action => action === 'SANIM_DEBT_STATUS_RESPONDED').length, 2);
    if (orders.totalElements) {
      started++;
      assert.ok(!candidates.content.some(row => row.paper.id === paper.id));
    } else if (!['COMPLETED','CANCELLED'].includes(detail.summary.currentState) && Number(detail.summary.totalDebt) > 0) {
      const candidate = candidates.content.find(row => row.paper.id === paper.id);
      assert.ok(candidate, 'Prepared unstarted paper must remain eligible: ' + paper.code);
      assert.equal(candidate.generalDirectorate, report.directorate.name);
      assert.equal(candidate.paper.notificationStatus, 'NOTIFIED');
      assert.ok(Date.parse(candidate.paymentDeadline) < Date.now()); ready++;
      for (const role of ['DIRECTORATE_EXECUTION_TEAM_SUPERVISOR','EXECUTION_OFFICER']) {
        const targets = await api('/api/v1/arrest-orders/targets?executionPaperId=' + paper.id + '&role=' + role, head);
        assert.ok(targets.some(target => target.role === role && target.generalDirectorateId === report.directorate.id));
      }
    }
    if (detail.taxpayer.type !== 'LEGAL') {
      assert.ok(!actions.includes('RESTRICTION_REQUEST_SENT'), 'Natural taxpayers must not acquire legal restriction data.');
      continue;
    }
    legal++;
    const related = await api('/api/v1/execution-papers/' + paper.id + '/related-persons', head);
    assert.equal(related.relatedPersons.length, paper.restrictions.selectedCount);
    const stored = trace.items.find(event => event.action === 'RELATED_PERSONS_STORED_FROM_OFFICIAL_GAZETTE');
    assert.equal(JSON.parse(stored.newValue).relatedPersons.length, related.relatedPersons.length);
    for (const action of ['OFFICIAL_GAZETTE_REQUEST_SENT','OFFICIAL_GAZETTE_RESPONSE_SENT',
      'RELATED_PERSONS_FILTERED_BY_DEBT_PAPER_DATE','RELATED_PERSONS_STORED_FROM_OFFICIAL_GAZETTE',
      'RELATED_PERSONS_LINKED_TO_EXECUTION_PAPER','RESTRICTION_REQUEST_SENT','RESTRICTION_REQUEST_CONFIRMED']) assert.ok(actions.includes(action), action);
    const external = await api('/api/v1/execution-papers/' + paper.id + '/external-requests', head);
    const restriction = external.find(request => request.systemCode === 'COMPANY_REGISTRY');
    assert.equal(restriction.status, 'CONFIRMED');
    assert.equal(restriction.requestBody.relatedPersons.length, paper.restrictions.newlyRestrictedCount);
    assert.equal(external.find(request => request.systemCode === 'OFFICIAL_GAZETTE').status, 'RESPONDED');
    assert.ok(Date.parse(restriction.completedAt) < Date.parse(trace.items.find(event => event.action === 'EXECUTION_PAPER_REQUIRES_COLLECTION').occurredAt));
  }
  console.log(`PASS: ${papers.length} prepared papers (${ready} ready, ${started} already started); ${legal} legal papers have completed restriction history, related persons and mock external requests. No data changed.`);
})().catch(error => { console.error(error); process.exitCode = 1; });
