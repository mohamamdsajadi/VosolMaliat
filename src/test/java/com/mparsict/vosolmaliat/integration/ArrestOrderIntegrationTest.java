package com.mparsict.vosolmaliat.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.domain.enums.*;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.execution.*;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;
import com.mparsict.vosolmaliat.domain.model.masterdata.*;
import com.mparsict.vosolmaliat.domain.repository.*;
import com.mparsict.vosolmaliat.domain.repository.security.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:sqlite:file:arrest-order-integration?mode=memory&cache=shared",
        "logging.level.com.mparsict.vosolmaliat=INFO"
})
@ActiveProfiles("mock-local")
class ArrestOrderIntegrationTest {
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper json;
    @Autowired TransactionTemplate tx;
    @Autowired EntityManager em;
    @Autowired TaxAffairsOfficeRepository offices;
    @Autowired AppUserProfileRepository profiles;
    @Autowired ArrestOrderRepository orders;
    @Autowired DebtInformationRepository debts;
    @Autowired GroupPermissionRepository grants;
    @Autowired com.mparsict.vosolmaliat.common.config.security.SecurityDataInitializer securityData;
    @Autowired com.mparsict.vosolmaliat.application.service.arrest.ArrestOrderNumbers numbers;

    private static final String HEAD = "demo_directorate_execution_unit_head";
    private static final String SUPERVISOR = "demo_directorate_execution_team_supervisor";
    private static final String SPECIAL = "demo_special_execution_department_head";
    private static final String OFFICER = "demo_execution_officer";

    @Test
    void dashboardEnforcementCountsIssuedOrdersAndTheirActualVisiblePapers() {
        var before = request(HEAD, "/api/v1/dashboard/overview", HttpMethod.GET, null).getBody();
        long issued = before.path("enforcement").get(1).path("count").asLong();
        long paper = fixture("DASHBOARD-ISSUED-ORDER", 40, "100", false);
        var draft = start(HEAD, List.of(paper), "USER", profileId(SUPERVISOR)).getBody().path("results").get(0).path("order");
        var notIssuedBefore = request(HEAD, "/api/v1/dashboard/details?group=enforcement&key=NOT_ISSUED&size=100", HttpMethod.GET, null);
        assertThat(notIssuedBefore.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(notIssuedBefore.getBody().path("content")).anyMatch(row -> row.path("id").asLong() == paper);
        post(HEAD, "/" + draft.path("id").asLong() + "/approve", Map.of("version", draft.path("version").asLong()));
        var after = request(HEAD, "/api/v1/dashboard/overview", HttpMethod.GET, null);
        assertThat(after.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(after.getBody().path("enforcement").get(1).path("count").asLong()).isEqualTo(issued + 1);
        var details = request(HEAD, "/api/v1/dashboard/details?group=enforcement&key=ISSUED&size=100", HttpMethod.GET, null);
        assertThat(details.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(details.getBody().path("content")).anyMatch(row -> row.path("id").asLong() == paper);
        var notIssuedAfter = request(HEAD, "/api/v1/dashboard/details?group=enforcement&key=NOT_ISSUED&size=100", HttpMethod.GET, null);
        assertThat(notIssuedAfter.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(notIssuedAfter.getBody().path("content")).noneMatch(row -> row.path("id").asLong() == paper);
        assertThat(notIssuedAfter.getBody().path("totalElements").asLong() + details.getBody().path("totalElements").asLong())
                .isEqualTo(after.getBody().path("papers").asLong());
    }

    @Test
    void dashboardPaperRowsCombineOnlyEligibleCandidatesAndMyApprovalDraftsAcrossPageBoundaries() {
        long ready = fixture("DASHBOARD-READY", 40, "9007199254740993.02", false);
        long draft = fixture("DASHBOARD-DRAFT", 40, "100", false);
        long assigned = fixture("DASHBOARD-ASSIGNED", 40, "200", false);
        long special = fixture("DASHBOARD-SPECIAL", 40, "300", false);
        long foreign = fixture("DASHBOARD-FOREIGN", 40, "400", true);
        long unexpired = fixture("DASHBOARD-NOT-DUE", 10, "500", false);
        var draftOrder = start(HEAD, List.of(draft), "USER", profileId(SUPERVISOR)).getBody().path("results").get(0).path("order");
        var assignedOrder = start(HEAD, List.of(assigned), "USER", profileId(SUPERVISOR)).getBody().path("results").get(0).path("order");
        post(HEAD, "/" + assignedOrder.path("id").asLong() + "/approve", Map.of("version", assignedOrder.path("version").asLong()));
        var specialOrder = start(HEAD, List.of(special), "SPECIAL_DEPARTMENT", null).getBody().path("results").get(0).path("order");
        var rows = pendingDashboardPapers(HEAD);
        var ids = rows.stream().map(row -> row.path("paper").path("id").asLong()).toList();
        assertThat(ids).contains(ready, draft).doesNotContain(assigned, special, foreign, unexpired).doesNotHaveDuplicates();
        assertThat(rows.stream().filter(row -> row.path("paper").path("id").asLong() == ready).findFirst().orElseThrow()
                .path("paper").path("totalDebt").asText()).isEqualTo("9007199254740993.02");
        assertThat(rows.stream().filter(row -> row.path("paper").path("id").asLong() == draft).findFirst().orElseThrow()
                .path("arrestOrderId").asLong()).isEqualTo(draftOrder.path("id").asLong());
        boolean candidateSeen = false;
        for (var row : rows) {
            if (row.path("status").asText().equals("READY_FOR_ISSUANCE")) candidateSeen = true;
            else { assertThat(candidateSeen).isFalse(); assertThat(row.path("status").asText()).isEqualTo("AWAITING_APPROVAL"); }
        }
        assertThat(pendingDashboardPapers(SUPERVISOR)).isEmpty();
        assertThat(pendingDashboardPapers(OFFICER)).isEmpty();
        assertThat(pendingDashboardPapers(SPECIAL)).noneMatch(row -> row.path("paper").path("id").asLong() == special);
        var prepared = post(SPECIAL, "/" + specialOrder.path("id").asLong() + "/prepare",
                Map.of("version", specialOrder.path("version").asLong(), "userId", profileId(SUPERVISOR))).getBody();
        assertThat(prepared.path("stage").asText()).isEqualTo("AWAITING_APPROVAL");
        assertThat(pendingDashboardPapers(SPECIAL)).anyMatch(row -> row.path("paper").path("id").asLong() == special);
        assertThat(pendingDashboardPapers(HEAD)).noneMatch(row -> row.path("paper").path("id").asLong() == special);
        post(HEAD, "/" + draftOrder.path("id").asLong() + "/approve", Map.of("version", draftOrder.path("version").asLong()));
        assertThat(pendingDashboardPapers(HEAD)).noneMatch(row -> row.path("paper").path("id").asLong() == draft);
        assertThat(http.getForEntity("/api/v1/dashboard/pending-arrest-papers", JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(request(HEAD, "/api/v1/dashboard/pending-arrest-papers?size=0", HttpMethod.GET, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private List<JsonNode> pendingDashboardPapers(String user) {
        var rows = new ArrayList<JsonNode>();
        for (int page = 0; ; page++) {
            var response = request(user, "/api/v1/dashboard/pending-arrest-papers?page=" + page + "&size=3", HttpMethod.GET, null);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            var body = response.getBody();
            assertThat(body.path("content").size()).isLessThanOrEqualTo(3);
            body.path("content").forEach(rows::add);
            if (page + 1 >= body.path("totalPages").asInt()) {
                assertThat(rows).hasSize(body.path("totalElements").asInt());
                return rows;
            }
        }
    }

    @Test
    void headCanSelectOfficerDirectlyButMustApproveBeforeHandoff() {
        long paper = fixture("DIRECT-OFFICER", 40, "1234.56", false);
        var response = post(HEAD, "", Map.of("executionPaperIds", List.of(paper), "destination", "OFFICER", "officerId", profileId(OFFICER)));
        assertThat(response.getBody().path("succeeded").asInt()).isEqualTo(1);
        var row = response.getBody().path("results").get(0).path("order");
        assertThat(row.path("taxpayerName").asText()).isEqualTo("علی");
        long id = row.path("id").asLong();
        assertThat(row.path("stage").asText()).isEqualTo("AWAITING_APPROVAL");
        assertThat(row.path("assignee").path("id").asLong()).isEqualTo(profileId(HEAD));
        assertThat(get(HEAD, "/" + id).getBody().path("letter").path("recipientRole").asText()).isEqualTo("مأمور اجرا");
        assertThat(post(OFFICER, "/" + id + "/approve", Map.of("version", row.path("version").asLong())).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        var approved = post(HEAD, "/" + id + "/approve", Map.of("version", row.path("version").asLong()));
        assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approved.getBody().path("stage").asText()).isEqualTo("ASSIGNED_TO_OFFICER");
        assertThat(approved.getBody().path("assignee").path("id").asLong()).isEqualTo(profileId(OFFICER));
        assertThat(get(OFFICER, "/" + id).getBody().path("history").toString()).contains("ASSIGNED_TO_OFFICER").doesNotContain("REFERRED_TO_SUPERVISOR");
        long invalid = fixture("DIRECT-INVALID", 40, "100", false);
        assertThat(post(HEAD, "", Map.of("executionPaperIds", List.of(invalid), "destination", "OFFICER", "officerId", profileId(SUPERVISOR)))
                .getBody().path("failed").asInt()).isEqualTo(1);
    }

    @Test
    void persistsActualFlowProtectsStagesAndFreezesExactLetterAmounts() {
        long paper = fixture("FLOW", 40, "9007199254740993.02", false);
        long supervisor = profileId(SUPERVISOR), officer = profileId(OFFICER);
        var started = start(HEAD, List.of(paper), "USER", supervisor).getBody();
        assertThat(started.path("succeeded").asInt()).isEqualTo(1);
        var row = started.path("results").get(0).path("order"); long id = row.path("id").asLong();
        assertThat(row.path("stage").asText()).isEqualTo("AWAITING_APPROVAL");
        assertThat(row.path("issuanceStatus").asText()).isEqualTo("PENDING");
        assertThat(get(HEAD, "/candidates?search=FLOW").getBody().path("totalElements").asInt()).isZero();
        assertThat(start(HEAD, List.of(paper), "USER", supervisor).getBody().path("failed").asInt()).isEqualTo(1);
        assertThat(post(OFFICER, "/" + id + "/approve", Map.of("version", 0)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post(SUPERVISOR, "/" + id + "/assign-officer", Map.of("version", 0, "userId", officer)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post(HEAD, "/" + id + "/approve", Map.of("version", 99)).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var approved = post(HEAD, "/" + id + "/approve", Map.of("version", row.path("version").asLong()));
        assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approved.getBody().path("stage").asText()).isEqualTo("AWAITING_OFFICER_ASSIGNMENT");
        assertThat(approved.getBody().path("assignee").path("id").asLong()).isEqualTo(supervisor);
        assertThat(post(HEAD, "/" + id + "/approve", Map.of("version", 0)).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var before = get(HEAD, "/" + id).getBody();
        assertThat(before.path("letter").path("debtAmount").asText()).isEqualTo("9007199254740993.02");
        assertThat(new BigDecimal(before.path("letter").path("surchargeAmount").asText())).isEqualByComparingTo("900719925474099.302");
        assertThat(new BigDecimal(before.path("letter").path("totalAmount").asText())).isEqualByComparingTo("9907919180215092.322");
        tx.executeWithoutResult(status -> {
            var debt = debts.findByExecutionPaperId(paper).orElseThrow();
            debt.setOutstandingTaxDebt(new BigDecimal("200"));
            debt.getCurrentAmounts().setOutstandingDebtAmount(new BigDecimal("200"));
            em.flush(); em.clear();
        });
        var assigned = post(SUPERVISOR, "/" + id + "/assign-officer", Map.of("version", approved.getBody().path("version").asLong(), "userId", officer));
        assertThat(assigned.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(assigned.getBody().path("stage").asText()).isEqualTo("ASSIGNED_TO_OFFICER");
        // Reload through a new request / persistence context, not a browser fixture.
        var persisted = get(OFFICER, "/" + id).getBody();
        assertThat(persisted.path("letter")).isEqualTo(before.path("letter"));
        assertThat(persisted.path("order").path("assignee").path("id").asLong()).isEqualTo(officer);
        assertThat(persisted.path("history").toString()).contains("DEBT_INQUIRY_MOCKED", "LETTER_ATTACHED", "ASSIGNED_TO_OFFICER");
        assertThat(get(OFFICER, "?mine=true").getBody().path("totalElements").asInt()).isZero();
        assertThat(get(OFFICER, "?history=true").getBody().path("content").toString()).contains(row.path("executionPaperCode").asText());
        assertThat(persisted.path("order").path("letterNumber").asText()).matches("[0-9]+");
        tx.executeWithoutResult(status -> assertThat(em.find(ExecutionPaper.class, paper).getCurrentState()).isEqualTo("ARREST_ORDER_ISSUED"));
    }

    @Test
    void filtersEligibilityAndOrganizationAndSupportsPartialBulkAndSpecialQueue() {
        long eligible = fixture("BULK-YES", 40, "123.45", false);
        long future = fixture("BULK-FUTURE", 3, "123.45", false);
        long paid = fixture("BULK-PAID", 40, "0", false);
        long unnotified = fixture("BULK-NONE", -1, "123.45", false);
        long other = fixture("BULK-OTHER", 40, "123.45", true);
        var candidates = get(HEAD, "/candidates?search=BULK").getBody();
        assertThat(candidates.path("content").toString()).contains("BULK-YES").doesNotContain("BULK-FUTURE", "BULK-PAID", "BULK-NONE", "BULK-OTHER");
        var batch = start(HEAD, List.of(eligible, future, paid, unnotified, other), "SPECIAL_DEPARTMENT", null).getBody();
        assertThat(batch.path("succeeded").asInt()).isEqualTo(1);
        assertThat(batch.path("failed").asInt()).isEqualTo(4);
        var row = batch.path("results").get(0).path("order"); long id = row.path("id").asLong();
        assertThat(row.path("candidateRole").asText()).isEqualTo("SPECIAL_EXECUTION_DEPARTMENT_HEAD");
        assertThat(get(SPECIAL, "?mine=true").getBody().path("content").toString()).contains("BULK-YES");
        assertThat(get(HEAD, "/targets?orderId=" + id + "&role=DIRECTORATE_EXECUTION_TEAM_SUPERVISOR").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        var prepared = post(SPECIAL, "/" + id + "/prepare", Map.of("version", row.path("version").asLong(), "userId", profileId(SUPERVISOR)));
        assertThat(prepared.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(prepared.getBody().path("stage").asText()).isEqualTo("AWAITING_APPROVAL");
        assertThat(post(SPECIAL, "/" + id + "/approve", Map.of("version", prepared.getBody().path("version").asLong())).getStatusCode()).isEqualTo(HttpStatus.OK);
        long otherId = start("admin", List.of(other), "SPECIAL_DEPARTMENT", null).getBody().path("results").get(0).path("order").path("id").asLong();
        assertThat(get(HEAD, "/" + otherId).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void requiresAnotherReviewWhenDebtChangesBeforeIssuance() {
        long paper = fixture("CHANGED-DEBT", 40, "100", false);
        var row = start(HEAD, List.of(paper), "USER", profileId(SUPERVISOR)).getBody().path("results").get(0).path("order");
        long id = row.path("id").asLong();
        tx.executeWithoutResult(status -> {
            var debt = debts.findByExecutionPaperId(paper).orElseThrow();
            debt.setOutstandingTaxDebt(new BigDecimal("80.55"));
            debt.getCurrentAmounts().setOutstandingDebtAmount(new BigDecimal("80.55"));
        });
        assertThat(post(HEAD, "/" + id + "/approve", Map.of("version", row.path("version").asLong())).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var refreshed = post(HEAD, "/" + id + "/refresh-draft", Map.of("version", row.path("version").asLong()));
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get(HEAD, "/" + id).getBody().path("letter").path("debtAmount").asText()).isEqualTo("80.55");
        assertThat(post(HEAD, "/" + id + "/approve", Map.of("version", row.path("version").asLong())).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(post(HEAD, "/" + id + "/approve", Map.of("version", refreshed.getBody().path("version").asLong())).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void concurrentStartsCreateExactlyOnePersistedProcess() {
        long paper = fixture("CONCURRENT", 40, "500", false);
        long supervisor = profileId(SUPERVISOR);
        var first = java.util.concurrent.CompletableFuture.supplyAsync(() -> start(HEAD, List.of(paper), "USER", supervisor).getBody());
        var second = java.util.concurrent.CompletableFuture.supplyAsync(() -> start(HEAD, List.of(paper), "USER", supervisor).getBody());
        var results = List.of(first.join(), second.join());
        assertThat(results.stream().mapToInt(item -> item.path("succeeded").asInt()).sum()).isEqualTo(1);
        assertThat(results.stream().mapToInt(item -> item.path("failed").asInt()).sum()).isEqualTo(1);
        assertThat(get(HEAD, "?executionPaperId=" + paper).getBody().path("totalElements").asInt()).isEqualTo(1);
    }

    @Test
    void rejectsWrongOrDisabledDestinationAndPreservesRevokedPermissions() {
        long paper = fixture("INVALID-TARGET", 40, "50", false);
        assertThat(start(HEAD, List.of(paper), "USER", profileId(OFFICER)).getBody().path("failed").asInt()).isEqualTo(1);
        tx.executeWithoutResult(status -> profiles.findByUsername(SUPERVISOR).orElseThrow().setLockedInBusinessSystem(true));
        try { assertThat(start(HEAD, List.of(paper), "USER", profileId(SUPERVISOR)).getBody().path("failed").asInt()).isEqualTo(1); }
        finally { tx.executeWithoutResult(status -> profiles.findByUsername(SUPERVISOR).orElseThrow().setLockedInBusinessSystem(false)); }
        assertThat(post(OFFICER, "", Map.of("executionPaperIds", List.of(paper), "destination", "SPECIAL_DEPARTMENT")).getBody().path("failed").asInt()).isEqualTo(1);
        long grantId = grants.findByGroupNameAndPermissionCode("DIRECTORATE_EXECUTION_UNIT_HEAD", "ARREST_ORDER_START").orElseThrow().getId();
        var deleted = request("admin", "/api/v1/group-permissions/" + grantId, HttpMethod.DELETE, null);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        try {
            securityData.initializeSecurityData();
            assertThat(grants.findByGroupNameAndPermissionCode("DIRECTORATE_EXECUTION_UNIT_HEAD", "ARREST_ORDER_START")).isEmpty();
            assertThat(start(HEAD, List.of(paper), "SPECIAL_DEPARTMENT", null).getBody().path("failed").asInt()).isEqualTo(1);
        } finally {
            request("admin", "/api/v1/group-permissions", HttpMethod.POST,
                    Map.of("groupName", "DIRECTORATE_EXECUTION_UNIT_HEAD", "permissionCode", "ARREST_ORDER_START"));
        }
    }

    @Test
    void enforcesAssignmentOnListsDirectLinksAndDashboardAndSeparatesPendingFromHistory() {
        long paper = fixture("PRIVATE", 40, "750", false);
        long hidden = fixture("PRIVATE-HIDDEN", 40, "750", false);
        String colleague = "supervisor_" + UUID.randomUUID().toString().substring(0, 8);
        tx.executeWithoutResult(status -> {
            var original = profiles.findByUsername(SUPERVISOR).orElseThrow();
            em.persist(com.mparsict.vosolmaliat.domain.model.security.AppUserProfile.builder()
                    .keycloakUserId(colleague).username(colleague).firstName("همکار")
                    .businessRole(original.getBusinessRole()).generalDirectorate(original.getGeneralDirectorate())
                    .taxAffairsOffice(original.getTaxAffairsOffice()).build());
        });
        var headBefore = dashboard(HEAD);
        var supervisorBefore = dashboard(SUPERVISOR);
        var row = start(HEAD, List.of(paper), "USER", profileId(SUPERVISOR)).getBody().path("results").get(0).path("order");
        long id = row.path("id").asLong();
        assertThat(get(SUPERVISOR, "/" + id).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get(SUPERVISOR, "/candidates").getBody().path("totalElements").asLong()).isZero();
        assertThat(get(HEAD, "?mine=true&executionPaperId=" + paper).getBody().path("totalElements").asInt()).isEqualTo(1);
        assertThat(get(HEAD, "?history=true&executionPaperId=" + paper).getBody().path("totalElements").asInt()).isZero();
        var approved = post(HEAD, "/" + id + "/approve", Map.of("version", row.path("version").asLong())).getBody();
        assertThat(approved.path("letterNumber").asText()).matches("[0-9]+");
        assertThat(get(HEAD, "?mine=true&executionPaperId=" + paper).getBody().path("totalElements").asInt()).isZero();
        assertThat(get(HEAD, "?history=true&executionPaperId=" + paper).getBody().path("totalElements").asInt()).isEqualTo(1);
        assertThat(get(SUPERVISOR, "?mine=true&executionPaperId=" + paper).getBody().path("totalElements").asInt()).isEqualTo(1);
        assertThat(get(SUPERVISOR, "?history=true&executionPaperId=" + paper).getBody().path("totalElements").asInt()).isZero();
        assertThat(get(colleague, "/" + id).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get(colleague, "").getBody().path("totalElements").asInt()).isZero();
        assertThat(request(colleague, "/api/v1/execution-papers?size=1", HttpMethod.GET, null).getBody().path("totalElements").asInt()).isZero();
        for (String suffix : List.of("", "/trace", "/external-requests", "/related-persons")) {
            assertThat(request(colleague, "/api/v1/execution-papers/" + paper + suffix, HttpMethod.GET, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(request(SUPERVISOR, "/api/v1/execution-papers/" + hidden + suffix, HttpMethod.GET, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
        assertThat(request(SUPERVISOR, "/api/v1/execution-papers/" + paper, HttpMethod.GET, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dashboard(colleague).path("totalIssuedExecutionPapers").asInt()).isZero();
        assertThat(dashboard(SUPERVISOR).path("myActionsAwaitingCompletion").asLong()).isEqualTo(supervisorBefore.path("myActionsAwaitingCompletion").asLong() + 1);
        post(SUPERVISOR, "/" + id + "/assign-officer", Map.of("version", approved.path("version").asLong(), "userId", profileId(OFFICER)));
        assertThat(get(SUPERVISOR, "?mine=true&executionPaperId=" + paper).getBody().path("totalElements").asInt()).isZero();
        assertThat(get(SUPERVISOR, "?history=true&executionPaperId=" + paper).getBody().path("totalElements").asInt()).isEqualTo(1);
        assertThat(get(OFFICER, "?mine=true&executionPaperId=" + paper).getBody().path("totalElements").asInt()).isZero();
        assertThat(get(OFFICER, "?history=true&executionPaperId=" + paper).getBody().path("totalElements").asInt()).isEqualTo(1);
        var supervisorAfter = dashboard(SUPERVISOR);
        assertThat(supervisorAfter.path("myActionsCompleted").asLong()).isEqualTo(supervisorBefore.path("myActionsCompleted").asLong() + 1);
        assertThat(supervisorAfter.path("myActionsAwaitingCompletion").asLong()).isEqualTo(supervisorBefore.path("myActionsAwaitingCompletion").asLong());
        var headAfter = dashboard(HEAD);
        assertThat(headAfter.path("myActionsCompleted").asLong()).isEqualTo(headBefore.path("myActionsCompleted").asLong() + 1);
        assertThat(headAfter.path("arrestOrderPerformance").path("total").asLong()).isEqualTo(headBefore.path("arrestOrderPerformance").path("total").asLong());
        var performance = headAfter.path("arrestOrderPerformance");
        assertThat(performance.path("percent").asInt()).isEqualTo((int)Math.round(performance.path("completed").asDouble() * 100 / performance.path("total").asDouble()));
    }

    @Test
    void upgradesLegacyNumbersAndPrintSnapshotsWithoutChangingTheLetterData() {
        long paper = fixture("NUMBER-MIGRATION", 40, "125.50", false);
        var row = start(HEAD, List.of(paper), "USER", profileId(SUPERVISOR)).getBody().path("results").get(0).path("order");
        long id = row.path("id").asLong();
        post(HEAD, "/" + id + "/approve", Map.of("version", row.path("version").asLong()));
        var letter = get(HEAD, "/" + id).getBody().path("letter").deepCopy();
        tx.executeWithoutResult(status -> {
            var order = orders.findById(id).orElseThrow();
            String oldNumber = order.getLetterNumber();
            order.setLetterNumber("AO-1405-" + id);
            order.setLetterSnapshot(order.getLetterSnapshot().replace(oldNumber, "AO-1405-" + id));
        });
        numbers.migrateLegacyNumbers();
        var migrated = get(HEAD, "/" + id).getBody();
        assertThat(migrated.path("order").path("letterNumber").asText()).matches("[0-9]+");
        assertThat(migrated.path("letter")).isEqualTo(letter);
        numbers.migrateLegacyNumbers();
        assertThat(get(HEAD, "/" + id).getBody()).isEqualTo(migrated);
    }

    private JsonNode dashboard(String user) { return request(user, "/api/v1/dashboard/stats", HttpMethod.GET, null).getBody(); }

    private long fixture(String label, int days, String outstanding, boolean otherOrganization) {
        return tx.execute(status -> {
            String code = "ARREST-" + label + "-" + UUID.randomUUID().toString().substring(0, 6);
            var office = offices.findAll().stream().filter(item -> Objects.equals(item.getGeneralDirectorateOfTaxAffairsId(),
                    profiles.findByUsername(HEAD).orElseThrow().getGeneralDirectorateId())).findFirst().orElseThrow();
            if (otherOrganization) {
                var general = GeneralDirectorateOfTaxAffairs.builder().generalDirectorateName(code).postalCode("0000000000").address("نشانی").phone("021").build(); em.persist(general);
                office = TaxAffairsOffice.builder().officeName(code).generalDirectorateOfTaxAffairs(general).postalCode("0000000000").address("نشانی").phone("021").build(); em.persist(office);
            }
            var person = NaturalPerson.builder().nationalCode(code).firstName("علی").lastName(label).build();
            if (label.equals("DIRECT-OFFICER")) person.setLastName(null);
            person.getExtraInfo().setIsForeign(false); em.persist(person);
            var taxpayer = Taxpayer.builder().tin("TIN-" + code).registrationTraceCode(code).naturalPerson(person).build(); em.persist(taxpayer);
            var date = LocalDateTime.now(ZoneId.of("Asia/Tehran")).minusDays(90);
            var finalPaper = FinalTaxDebtPaper.builder().taxpayer(taxpayer).paperType(PaperType.EF03).taxAffairsOffice(office)
                    .finalizationReason(FinalizationReason.FINAL_NOTICE).uniqueFinalTaxDebtNoticeId("N-" + code).finalTaxDebtNoticeIssueDate(date)
                    .finalTaxDebtPaperNotificationNumber(code).taxYear(1404).taxPeriod(TaxPeriod.of(1)).periodStartDate(date).periodEndDate(date).build(); em.persist(finalPaper);
            var paper = ExecutionPaper.builder().executionPaperUniqueCode(code).issuanceDate(date).finalTaxDebtPaper(finalPaper).currentState("NOTIFIED").build(); em.persist(paper);
            var amount = new BigDecimal(outstanding);
            em.persist(DebtInformation.builder().finalTaxDebtPaper(finalPaper).totalLiability(amount).totalPenalties(BigDecimal.ZERO)
                    .totalCredits(BigDecimal.ZERO).totalPayments(BigDecimal.ZERO).totalRemissions(BigDecimal.ZERO).outstandingTaxDebt(amount).build());
            if (days >= 0) em.persist(ExecutionPaperNotification.builder().executionPaper(paper).electronicNotificationId(code)
                    .notificationDate(date.plusDays(1)).taxpayerNotificationViewDate(LocalDateTime.now(ZoneId.of("Asia/Tehran")).minusDays(days)).build());
            em.flush(); return paper.getId();
        });
    }
    private long profileId(String username) { return profiles.findByUsername(username).orElseThrow().getId(); }
    private ResponseEntity<JsonNode> start(String username, List<Long> papers, String destination, Long supervisor) {
        var body = new HashMap<String, Object>(); body.put("executionPaperIds", papers); body.put("destination", destination);
        if (supervisor != null) body.put("supervisorId", supervisor);
        return post(username, "", body);
    }
    private ResponseEntity<JsonNode> get(String user, String path) { return request(user, "/api/v1/arrest-orders" + path, HttpMethod.GET, null); }
    private ResponseEntity<JsonNode> post(String user, String path, Object body) { return request(user, "/api/v1/arrest-orders" + path, HttpMethod.POST, body); }
    private ResponseEntity<JsonNode> request(String username, String path, HttpMethod method, Object body) {
        var login = http.postForEntity("/api/v1/auth/mock/login", Map.of("username", username), JsonNode.class);
        var headers = new HttpHeaders(); headers.setBearerAuth(login.getBody().path("accessToken").asText());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
}
