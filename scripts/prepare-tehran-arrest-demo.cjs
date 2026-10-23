// Prepare existing LOCAL demo papers, never start/approve an arrest order.
// Preview: node --no-warnings scripts/prepare-tehran-arrest-demo.cjs
// Apply:   node --no-warnings scripts/prepare-tehran-arrest-demo.cjs --apply
// Upgrade an existing natural-only demo: add --legal-only (two additional legal papers).
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const { DatabaseSync } = require('node:sqlite');
const { seedLegalRestrictions } = require('./lib/tehran-legal-restrictions.cjs');
const root = path.resolve(__dirname, '..');
const dbArg = process.argv.find(value => value.startsWith('--db='));
const database = path.resolve(root, dbArg ? dbArg.slice(5) : 'ef03.mock.sqlite');
assert.ok(database.startsWith(root + path.sep), 'The demo database must be inside this workspace.');
assert.ok(fs.existsSync(database), 'Database does not exist.');
const apply = process.argv.includes('--apply');
const legalOnly = process.argv.includes('--legal-only');
const count = legalOnly ? 2 : 10;
const key = legalOnly ? 'tehran-arrest-legal-primary-flow-v1' : 'tehran-arrest-primary-flow-v1';
const usernames = ['demo_directorate_execution_unit_head', 'demo_directorate_execution_team_supervisor', 'demo_execution_officer'];
const db = new DatabaseSync(database, { readOnly: !apply });
db.exec('PRAGMA busy_timeout=30000; PRAGMA foreign_keys=ON');
const hasTable = table => !!db.prepare("SELECT name FROM sqlite_master WHERE type='table' AND name=?").get(table);
const existing = hasTable('local_demo_preparation') ? db.prepare('SELECT payload FROM local_demo_preparation WHERE demo_key=?').get(key) : null;
if (existing) {
  const report = JSON.parse(existing.payload);
  console.log('This demo is already prepared; existing orders and user work are preserved.');
  console.log(JSON.stringify(report, null, 2)); db.close(); process.exit(0);
}
const general = db.prepare("SELECT id,general_directorate_of_tax_affairs_name AS name FROM general_directorate_of_tax_affairs WHERE general_directorate_of_tax_affairs_name='اداره کل امورمالیاتی مرکز تهران'").get();
assert.ok(general, 'The imported Central Tehran directorate was not found.');
const office = db.prepare('SELECT id,office_name AS name FROM tax_affairs_office WHERE general_directorate_of_tax_affairs_id=? ORDER BY id LIMIT 1').get(general.id);
const users = usernames.map(username => db.prepare('SELECT * FROM app_user_profile WHERE username=?').get(username));
assert.ok(users.every(Boolean), 'All three existing demo identities are required.');
const head = users[0];
const orderExclusion = hasTable('arrest_order') ? 'AND NOT EXISTS (SELECT 1 FROM arrest_order ao WHERE ao.execution_paper_id=ep.id)' : '';
const available = db.prepare(`SELECT ep.id,ep.execution_paper_unique_code AS code,ep.issuance_date AS originalIssuanceDate,
  ep.final_tax_debt_paper_id AS debtPaperId,dp.final_tax_debt_paper_notification_number AS noticeId,
  tp.taxpayer_id AS tin,COALESCE(np.national_code,np.fida,lp.national_id) AS nationalId,d.outstanding_tax_debt AS amount,
  CASE WHEN lp.id IS NOT NULL THEN 'LEGAL' ELSE 'NATURAL' END AS taxpayerType,
  lp.id AS ownerLegalPersonId,dp.tax_year AS taxYear,
  COALESCE(dp.tax_return_date,dp.final_tax_debt_notice_issue_date) AS debtPaperDate
  FROM execution_paper ep JOIN final_tax_debt_paper dp ON dp.id=ep.final_tax_debt_paper_id
  JOIN tax_affairs_office o ON o.id=dp.tax_affairs_office_id JOIN taxpayer tp ON tp.id=dp.taxpayer_id
  LEFT JOIN natural_person np ON np.id=tp.natural_person_id LEFT JOIN legal_person lp ON lp.id=tp.legal_person_id
  JOIN debt_information d ON d.debt_paper_unique_id=dp.id
  JOIN execution_workflow_state w ON w.execution_paper_id=ep.id
  WHERE o.general_directorate_of_tax_affairs_id=? AND cast(d.outstanding_tax_debt AS NUMERIC)>0
  AND (SELECT COUNT(*) FROM debt_information currentDebt WHERE currentDebt.debt_paper_unique_id=dp.id)=1
  AND w.current_state NOT IN ('COMPLETED','CANCELLED','ARREST_ORDER_ISSUED')
  AND NOT EXISTS (SELECT 1 FROM execution_paper_notification n WHERE n.execution_paper_id=ep.id)
  ${orderExclusion} ORDER BY ep.id DESC`).all(general.id);
const legal = available.filter(paper => paper.taxpayerType === 'LEGAL' && paper.debtPaperDate != null);
const natural = available.filter(paper => paper.taxpayerType === 'NATURAL');
// Fresh demos include both taxpayer types. The additive upgrade leaves the original demo untouched.
const papers = legalOnly ? legal.slice(0, count) : [...legal.slice(0, 2), ...natural, ...legal.slice(2)].slice(0, count);
assert.equal(papers.length, count, `Need ${count} existing, open, unnotified papers with positive debt and valid source dates.`);
const report = { scenario: key, database: path.relative(root, database), directorate: general, office,
  users: users.map(user => ({ id: user.id, username: user.username, role: user.business_role,
    originalDirectorateId: user.general_directorate_id, originalOfficeId: user.tax_affairs_office_id })),
  papers, note: 'MOCK primary-flow prerequisites and, for legal taxpayers, Tabsare 3 Article 186 restriction data. Arrest orders remain unstarted for manual testing.' };
if (!apply) { console.log(JSON.stringify(report, null, 2)); console.log('Preview only. Add --apply to back up and prepare these records.'); db.close(); process.exit(0); }
const stamp = new Date().toISOString().replace(/[:.]/g, '-');
const backup = path.join(root, 'temp', `before-tehran-arrest-demo-${stamp}.sqlite`);
fs.mkdirSync(path.dirname(backup), { recursive: true });
db.exec(`VACUUM INTO '${backup.replaceAll("'", "''")}'`);
report.backup = path.relative(root, backup); report.preparedAt = new Date().toISOString();
const now = Date.now(), day = 86400000;
function insert(table, row) {
  const names = Object.keys(row);
  return db.prepare(`INSERT INTO ${table} (${names.join(',')}) VALUES (${names.map(() => '?').join(',')})`).run(...names.map(name => row[name]));
}
function meta(time, creationColumn = 'created_at', updateColumn = 'updated_at') {
  return { [creationColumn]: time, created_by: 'MOCK_TEHRAN_PRIMARY_FLOW', creator_user_id: head.id,
    [updateColumn]: time, updated_by: 'MOCK_TEHRAN_PRIMARY_FLOW', updater_user_id: head.id };
}
function audit(paper, action, time, value = {}) {
  insert('audit_log', { action, actor_keycloak_user_id: head.keycloak_user_id, actor_username: head.username,
    created_at: time, target_type: 'EXECUTION_PAPER', target_id: String(paper.id),
    new_value: JSON.stringify({ mock: true, scenario: key, ...value }) });
}
function history(paper, state, time) { insert('execution_paper_state_history', { ...meta(time, 'creation_date'), state, user_id: head.id, execution_paper_id: paper.id }); }
db.exec('BEGIN IMMEDIATE');
try {
  for (const user of legalOnly ? [] : users) {
    db.prepare('UPDATE app_user_profile SET general_directorate_id=?,tax_affairs_office_id=?,enabled_in_business_system=1,locked_in_business_system=0,updated_at=?,updated_by=? WHERE id=?')
      .run(general.id, office.id, now, 'MOCK_TEHRAN_PRIMARY_FLOW', user.id);
  }
  for (const [index, paper] of papers.entries()) {
    const issued = now - (65 + index) * day;
    const firstInquiry = issued + day;
    const assigned = firstInquiry + 3600000;
    const notified = issued + 2 * day;
    const viewed = notified + 3600000;
    const deadline = viewed + 30 * day;
    const finalInquiry = now - day + index * 1000;
    const tracking = `MOCK-TEHRAN-NOTIFY-${paper.id}`;
    db.prepare('UPDATE execution_paper SET issuance_date=? WHERE id=?').run(issued, paper.id);
    insert('execution_paper_notification', { ...meta(notified, 'created_at', 'update_date'), electronic_notification_id: tracking,
      notification_date: notified, taxpayer_notification_view_date: viewed, version: 0, execution_paper_id: paper.id,
      // This table overrides the normal update timestamp column.
    });
    const debt = db.prepare('SELECT * FROM debt_information WHERE debt_paper_unique_id=?').get(paper.debtPaperId);
    const amountFields = { total_liability: debt.total_liability, total_penalties: debt.total_penalties,
      total_credits: debt.total_credits, total_payments: debt.total_payments, total_remissions: debt.total_remissions,
      outstanding_tax_debt: debt.outstanding_tax_debt, tax_liability_details: debt.tax_liability_details,
      credits_detail: debt.credits_detail, penalties_detail: debt.penalties_detail, payments_detail: debt.payments_detail,
      remissions_detail: debt.remissions_detail, outstanding_debt_details: debt.outstanding_debt_details };
    const previous = db.prepare('SELECT min(creation_date) AS first FROM debt_information_history WHERE debt_paper_unique_id=?').get(paper.debtPaperId).first;
    insert('debt_information_history', { ...meta(firstInquiry, 'creation_date'), ...amountFields, debt_paper_unique_id: paper.debtPaperId, next_inquiry_date: previous && previous > firstInquiry ? previous : finalInquiry });
    db.prepare('UPDATE debt_information_history SET next_inquiry_date=? WHERE debt_paper_unique_id=? AND next_inquiry_date IS NULL AND creation_date<=?')
      .run(finalInquiry, paper.debtPaperId, finalInquiry);
    insert('debt_information_history', { ...meta(finalInquiry, 'creation_date'), ...amountFields, debt_paper_unique_id: paper.debtPaperId });
    db.prepare('UPDATE debt_information SET updated_at=?,updated_by=?,updater_user_id=?,version=COALESCE(version,0)+1 WHERE debt_paper_unique_id=?')
      .run(finalInquiry, 'MOCK_TEHRAN_PRIMARY_FLOW', head.id, paper.debtPaperId);
    for (const [phase, time] of [['INITIAL', firstInquiry], ['AFTER_DEADLINE', finalInquiry]]) {
      const response = JSON.stringify({ mock: true, scenario: key, phase, executionPaperId: paper.id,
        taxpayerType: paper.taxpayerType, taxpayerIdentifier: paper.nationalId, outstandingDebtAmount: paper.amount,
        amounts: { totalTaxAmount: debt.total_liability, totalDutyAmount: '0', totalPenaltyAmount: debt.total_penalties,
          totalCreditAmount: debt.total_credits, totalPaidAmount: debt.total_payments, totalRemissionAmount: debt.total_remissions,
          outstandingDebtAmount: paper.amount }, asOf: new Date(time).toISOString() });
      insert('debt_inquiry_result', { ...meta(time), finalized_debt_paper_notification_id: paper.noticeId,
        xml: response, hash: crypto.createHash('sha256').update(response).digest('hex') });
      insert('fake_sanim_debt_inquiry_request', { ...meta(time), execution_paper_id: paper.id,
        request_id: `MOCK-TEHRAN-${paper.id}-${phase}`, taxpayer_type: paper.taxpayerType, taxpayer_identifier: paper.nationalId,
        status: 'RESPONDED', sent_at: time - 1000, responded_at: time, response_payload: response });
      audit(paper, 'SANIM_DEBT_INQUIRY_SENT', time - 1000, { phase });
      audit(paper, 'SANIM_DEBT_STATUS_RESPONDED', time, { phase, outstandingDebtAmount: paper.amount });
    }
    history(paper, 'UNASSIGNED', issued); audit(paper, 'EXECUTION_PAPER_CREATED', issued);
    history(paper, 'ASSIGNED', assigned); audit(paper, 'TASK_ASSIGNED', assigned, { assignee: head.username, assigneeId: head.id, role: head.business_role });
    history(paper, 'INFORM_SENT', notified - 1000); audit(paper, 'INFORM_SENT_TO_ELECTRONIC_NOTIFICATION', notified - 1000, { tracking });
    history(paper, 'NOTIFIED', notified); audit(paper, 'ELECTRONIC_NOTIFICATION_CONFIRMED', notified, { tracking });
    audit(paper, 'INFORM_DATE_SENT_TO_SANIM', notified + 1000); audit(paper, 'SANIM_INFORM_DATE_CONFIRMED', notified + 2000);
    audit(paper, 'NEXT_DEBT_INQUIRY_DATE_SET', viewed, { paymentDeadline: new Date(deadline).toISOString() });
    audit(paper, 'PAYMENT_DEADLINE_EXPIRED', deadline);
    audit(paper, 'TAXPAYER_DEBT_STATUS_UPDATED_FROM_SANIM', finalInquiry, { outstandingDebtAmount: paper.amount });
    if (paper.taxpayerType === 'LEGAL') {
      paper.restrictions = seedLegalRestrictions({ db, paper, head, issued, now, insert, meta, audit });
    }
    history(paper, 'AWAITING_ARREST_ORDER', finalInquiry + 1000);
    audit(paper, 'EXECUTION_PAPER_REQUIRES_COLLECTION', finalInquiry + 1000, { assignedTo: head.username, status: 'نیازمند وصول' });
    db.prepare("UPDATE execution_workflow_state SET current_state='AWAITING_ARREST_ORDER',inform_status='NOTIFIED',notification_tracking_id=?,next_debt_inquiry_date=?,view_date_message_pending=0,updated_at=?,updated_by=?,updater_user_id=?,version=COALESCE(version,0)+1 WHERE execution_paper_id=?")
      .run(tracking, new Date(finalInquiry + 30 * day).toISOString().replace('Z', ''), finalInquiry + 1000, 'MOCK_TEHRAN_PRIMARY_FLOW', head.id, paper.id);
    paper.notificationAt = new Date(notified).toISOString(); paper.paymentDeadline = new Date(deadline).toISOString();
    paper.assignedTo = head.username;
  }
  db.exec('CREATE TABLE IF NOT EXISTS local_demo_preparation (demo_key TEXT PRIMARY KEY,payload TEXT NOT NULL)');
  insert('local_demo_preparation', { demo_key: key, payload: JSON.stringify(report) });
  assert.deepEqual(db.prepare('PRAGMA foreign_key_check').all(), [], 'Foreign-key validation failed.');
  db.exec('COMMIT');
} catch (error) { db.exec('ROLLBACK'); throw error; }
const reportPath = path.join(root, 'temp', `tehran-arrest-demo${legalOnly ? '-legal' : ''}-${path.basename(database)}.json`);
fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
console.log(JSON.stringify(report, null, 2)); console.log(`Prepared ${count} unstarted arrest-order candidates. Report:`, path.relative(root, reportPath));
db.close();
