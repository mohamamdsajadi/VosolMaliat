const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const day = 86400000;
const calendar = new Intl.DateTimeFormat('en-US-u-ca-persian', {
  timeZone: 'UTC', year: 'numeric', month: '2-digit', day: '2-digit',
});
function jalali(time) {
  const parts = Object.fromEntries(calendar.formatToParts(new Date(time)).map(item => [item.type, item.value]));
  return `${parts.year}-${parts.month}-${parts.day}`;
}
function yearStart(year) {
  const start = Date.UTC(year + 621, 2, 18);
  for (let time = start; time < start + 8 * day; time += day) if (jalali(time) === `${year}-01-01`) return time;
  throw Error('Cannot resolve Persian year ' + year);
}
function nationalCode(seed) {
  const digits = String(100000000 + crypto.createHash('sha256').update(seed).digest().readUInt32BE(0) % 900000000);
  const remainder = [...digits].reduce((sum, digit, index) => sum + Number(digit) * (10 - index), 0) % 11;
  return digits + (remainder < 2 ? remainder : 11 - remainder);
}

/** Local data only: follows Visio page 4 and the existing Tabsare3-186 persistence contracts. */
function seedLegalRestrictions({ db, paper, head, issued, now, insert, meta, audit }) {
  assert.equal(paper.taxpayerType, 'LEGAL');
  assert.ok(paper.ownerLegalPersonId && paper.debtPaperDate);
  const actor = 'MOCK_TEHRAN_TABSARE_3_186';
  const requestId = `MOCK-TEHRAN-186-${paper.id}`;
  assert.ok(!db.prepare('SELECT id FROM fake_official_gazette_request WHERE execution_paper_id=?').get(paper.id),
    'Do not overwrite an existing registry workflow.');
  assert.ok(!db.prepare('SELECT id FROM fake_restriction_request WHERE execution_paper_id=?').get(paper.id),
    'Do not overwrite an existing restriction workflow.');
  const sourceDate = paper.debtPaperDate;
  const debtDate = jalali(sourceDate);
  const start = Math.min(sourceDate, yearStart(paper.taxYear)) - 365 * day;
  // Explicit historical tenures satisfy the schema and demonstrate liability at the debt date.
  const end = Math.max(sourceDate, now - day);
  const sent = issued + day + 7200000;
  const received = sent + 1000, filtered = sent + 2000, stored = sent + 3000;
  const restrictionSent = sent + 4000, confirmed = sent + 5000;
  const branchMeta = (time, created = 'created_at', updated = 'updated_at') => ({
    ...meta(time, created, updated), created_by: actor, updated_by: actor,
  });
  const person = (seed, firstName, lastName, postCode, endDate) => ({
    sourceRecordId: seed, personType: 'NATURAL', postCode,
    startDate: jalali(start), endDate, hasSignatoryAuthority: false,
    nationalCode: nationalCode(seed), fida: null, firstName, lastName, fatherName: 'حسن', birthDate: '1365-02-15',
    nationalId: null, registrationNumber: null, registrationDate: null, companyName: null, legalRepresentative: null,
  });
  const response = [
    person('MOCK-TEHRAN-186-SHARED-CHAIR', 'رضا', 'رئیس هیئت‌مدیره آزمایشی', 'BOARD_CHAIR', jalali(end)),
    person(`${requestId}-CEO`, 'مهدی', `مدیرعامل آزمایشی ${paper.id}`, 'CEO', jalali(end)),
    person(`${requestId}-FORMER`, 'علی', `عضو سابق آزمایشی ${paper.id}`, 'BOARD_MEMBER', jalali(sourceDate - day)),
  ];
  const selected = response.filter(person => person.startDate <= debtDate && person.endDate >= debtDate);
  assert.equal(selected.length, 2, 'Exactly the two members active on the source debt-paper date must be included.');
  insert('fake_official_gazette_request', { ...branchMeta(sent), request_id: requestId, execution_paper_id: paper.id,
    legal_person_national_id: paper.nationalId, debt_paper_date: debtDate, status: 'RESPONDED',
    sent_at: new Date(sent).toISOString(), responded_at: new Date(received).toISOString(), response_payload: JSON.stringify(response) });
  audit(paper, 'OFFICIAL_GAZETTE_REQUEST_SENT', sent, { requestId, debtPaperDate: debtDate });
  audit(paper, 'OFFICIAL_GAZETTE_RESPONSE_SENT', received, { requestId, receivedCount: response.length });
  audit(paper, 'RELATED_PERSONS_FILTERED_BY_DEBT_PAPER_DATE', filtered, {
    debtPaperDate: debtDate, receivedCount: response.length, selectedCount: selected.length,
    excludedSourceRecordIds: response.filter(person => !selected.includes(person)).map(person => person.sourceRecordId),
  });
  const snapshot = [];
  const newlyRestricted = [];
  const postCodes = { BOARD_CHAIR: '002', CEO: '001' };
  const registered = db.prepare("SELECT id FROM restriction_state WHERE code='REGISTERED'").get();
  assert.ok(registered);
  for (const [index, member] of selected.entries()) {
    let identity = db.prepare('SELECT id,created_by FROM natural_person WHERE national_code=?').get(member.nationalCode);
    const reused = !!identity;
    if (identity) assert.equal(identity.created_by, actor, 'A mock identity must not overwrite a real identity.');
    else {
      const id = Number(insert('natural_person', { ...branchMeta(stored, 'creation_date'), national_code: member.nationalCode }).lastInsertRowid);
      identity = { id };
      insert('natural_person_extra_info', { ...branchMeta(stored, 'creation_date', 'update_date'), natural_person_id: id,
        first_name: member.firstName, last_name: member.lastName, full_name: `${member.firstName} ${member.lastName}`,
        father_name: member.fatherName, birth_date: Date.UTC(1986, 4, 5), is_foreign: 0, version: 0 });
    }
    const post = db.prepare('SELECT id,title FROM post_in_company WHERE code_in_company_registration_system=?').get(postCodes[member.postCode]);
    assert.ok(post, 'The company post lookup is required.');
    // Reuse an identical owner/identity/role/tenure link if several papers share the taxpayer.
    let relationship = db.prepare(`SELECT id FROM related_persons WHERE owner_legal_person_id=?
      AND related_natural_person_id=? AND post_id=? AND start_date=?`).get(paper.ownerLegalPersonId, identity.id, post.id, start);
    if (!relationship) relationship = { id: Number(insert('related_persons', { ...branchMeta(stored, 'creation_date'),
      owner_legal_person_id: paper.ownerLegalPersonId, related_natural_person_id: identity.id,
      post_id: post.id, start_date: start, end_date: end, position_expiry_date: end }).lastInsertRowid) };
    const priorRestrictions = db.prepare(`SELECT r.execution_paper_id,r.restriction_date,r.restriction_tracking_code
      FROM restrictions_applied_to_companies r JOIN execution_workflow_state w ON w.execution_paper_id=r.execution_paper_id
      WHERE r.natural_person_id=? AND r.execution_paper_id<>? AND r.restriction_state_id IN (0,1)
      AND w.current_state NOT IN ('COMPLETED','CANCELLED') ORDER BY r.id`).all(identity.id, paper.id);
    // Visio's final gateway: other linked papers keep the existing restriction; do not send it again.
    const trackingCode = priorRestrictions.length ? null : `MOCK-186-${paper.id}-${index + 1}`;
    if (!priorRestrictions.length) newlyRestricted.push(member);
    insert('restrictions_applied_to_companies', { ...branchMeta(confirmed, 'creation_date'),
      execution_paper_id: paper.id, natural_person_id: identity.id, restriction_state_id: registered.id,
      restriction_date: priorRestrictions[0]?.restriction_date ?? confirmed, restriction_tracking_code: trackingCode });
    snapshot.push({ ...member, relationshipId: relationship.id, naturalPersonId: identity.id,
      postId: post.id, postCode: postCodes[member.postCode], postLabel: post.title,
      reusedIdentity: reused, restrictionTrackingCode: trackingCode,
      priorRestrictedPaperIds: priorRestrictions.map(restriction => restriction.execution_paper_id) });
  }
  const relatedSnapshot = { mock: true, scenario: 'tehran-tabsare3-186-v1', sourceSystem: 'FAKE_OFFICIAL_GAZETTE',
    executionPaperId: paper.id, ownerLegalPersonId: paper.ownerLegalPersonId, ownerNationalId: paper.nationalId,
    debtYear: String(paper.taxYear), debtPaperDate: debtDate, lastSyncedAt: new Date(stored).toISOString(), relatedPersons: snapshot };
  // Keep the usual API snapshot shape so the existing history tables can display the people.
  insert('audit_log', { action: 'RELATED_PERSONS_STORED_FROM_OFFICIAL_GAZETTE', actor_keycloak_user_id: head.keycloak_user_id,
    actor_username: head.username, created_at: stored, target_type: 'EXECUTION_PAPER', target_id: String(paper.id),
    new_value: JSON.stringify(relatedSnapshot) });
  audit(paper, 'RELATED_PERSONS_LINKED_TO_EXECUTION_PAPER', stored, { naturalPersonIds: snapshot.map(person => person.naturalPersonId) });
  insert('fake_restriction_request', { ...branchMeta(restrictionSent), official_gazette_request_id: requestId,
    execution_paper_id: paper.id, status: 'CONFIRMED', related_person_count: newlyRestricted.length, payload: JSON.stringify(newlyRestricted),
    sent_at: new Date(restrictionSent).toISOString(), confirmed_at: new Date(confirmed).toISOString() });
  audit(paper, 'RESTRICTION_REQUEST_SENT', restrictionSent, { officialGazetteRequestId: requestId, relatedPersonCount: newlyRestricted.length });
  audit(paper, 'RESTRICTION_REQUEST_CONFIRMED', confirmed, { officialGazetteRequestId: requestId,
    relatedPersonCount: newlyRestricted.length, trackingCodes: snapshot.map(person => person.restrictionTrackingCode).filter(Boolean) });
  return { requestId, debtPaperDate: debtDate, receivedCount: response.length, selectedCount: selected.length,
    newlyRestrictedCount: newlyRestricted.length, alreadyRestrictedCount: selected.length - newlyRestricted.length,
    confirmedAt: new Date(confirmed).toISOString(), relatedPersons: snapshot };
}
module.exports = { seedLegalRestrictions };
