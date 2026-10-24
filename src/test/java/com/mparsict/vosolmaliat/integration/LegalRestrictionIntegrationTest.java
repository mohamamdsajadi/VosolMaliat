package com.mparsict.vosolmaliat.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.common.config.security.MockIdentityProvider;
import com.mparsict.vosolmaliat.domain.enums.*;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.execution.*;
import com.mparsict.vosolmaliat.domain.model.integration.*;
import com.mparsict.vosolmaliat.domain.model.inquiry.RestrictionsAppliedToCompanies;
import com.mparsict.vosolmaliat.domain.model.masterdata.*;
import com.mparsict.vosolmaliat.domain.model.person.*;
import com.mparsict.vosolmaliat.domain.model.security.AuditLog;
import com.mparsict.vosolmaliat.domain.repository.TaxAffairsOfficeRepository;
import com.mparsict.vosolmaliat.domain.repository.security.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:file:legal-restrictions-integration?mode=memory&cache=shared")
@ActiveProfiles("mock-local") @AutoConfigureMockMvc @Transactional
class LegalRestrictionIntegrationTest {
    private static final String HEAD = "demo_directorate_execution_unit_head";
    private static final String SUPERVISOR = "demo_directorate_execution_team_supervisor";
    private static final String OFFICER = "demo_execution_officer";
    private static final String API = "/api/v1/legal-restrictions/article-186";
    @Autowired EntityManager em;
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @Autowired MockIdentityProvider identities;
    @Autowired AppUserProfileRepository profiles;
    @Autowired GroupPermissionRepository grants;
    @Autowired TaxAffairsOfficeRepository offices;
    @Autowired com.mparsict.vosolmaliat.common.config.security.SecurityDataInitializer securityData;
    private FakeRestrictionRequest confirmed, waiting, outside;

    @BeforeEach void fixtures() {
        var ownOffice = offices.findAll().stream().filter(office -> Objects.equals(office.getGeneralDirectorateOfTaxAffairsId(),
                profiles.findByUsername(HEAD).orElseThrow().getGeneralDirectorateId())).findFirst().orElseThrow();
        confirmed = request(paper("LEGAL-100", true, ownOffice), "CONFIRMED", "2026-10-10T09:01:00");
        waiting = request(paper("LEGAL-200", true, ownOffice), "SENT", "2026-10-11T09:01:00");
        request(paper("NATURAL-300", false, ownOffice), "CONFIRMED", "2026-10-11T10:01:00");
        var directorate = GeneralDirectorateOfTaxAffairs.builder().generalDirectorateName("اداره دیگر").postalCode("0000000000").address("نشانی").phone("021").build(); em.persist(directorate);
        var otherOffice = TaxAffairsOffice.builder().officeName("اداره دیگر").generalDirectorateOfTaxAffairs(directorate).postalCode("0000000000").address("نشانی").phone("021").build(); em.persist(otherOffice);
        outside = request(paper("OTHER-400", true, otherOffice), "FAILED", "2026-10-12T09:01:00");
        em.flush();
    }

    @Test void scopesCountsSearchDatesAndPaginationToVisibleLegalPapers() throws Exception {
        var result = read(HEAD, "?size=1", 200);
        assertThat(result.path("totalElements").asInt()).isEqualTo(2);
        assertThat(result.path("totalPages").asInt()).isEqualTo(2);
        assertThat(result.path("content").get(0).path("id").asLong()).isEqualTo(waiting.getId());
        assertThat(result.path("counts").path("total").asInt()).isEqualTo(2);
        assertThat(result.path("counts").path("pending").asInt()).isEqualTo(1);
        assertThat(result.path("counts").path("confirmed").asInt()).isEqualTo(1);
        assertThat(result.path("counts").path("failed").asInt()).isZero();
        assertThat(read(HEAD, "?page=1&size=1", 200).path("content").get(0).path("id").asLong()).isEqualTo(confirmed.getId());
        assertThat(read(HEAD, "?search=LEGAL-۱۰۰", 200).path("totalElements").asInt()).isEqualTo(1);
        assertThat(read(HEAD, "?search=شرکت LEGAL-100", 200).path("totalElements").asInt()).isEqualTo(1);
        assertThat(read(HEAD, "?status=SENT", 200).path("totalElements").asInt()).isEqualTo(1);
        assertThat(read(HEAD, "?from=2026-10-10&to=2026-10-10", 200).path("totalElements").asInt()).isEqualTo(1);
        assertThat(read(HEAD, "?search=%25", 200).path("totalElements").asInt()).isZero();
        assertThat(read("admin", "", 200).path("totalElements").asInt()).isEqualTo(3);
        read(HEAD, "/" + outside.getId(), 404);
    }

    @Test void supervisorAndOfficerSeeOnlyAssignedPapersIncludingDirectDetailLinks() throws Exception {
        assertThat(read(SUPERVISOR, "", 200).path("totalElements").asInt()).isZero();
        read(SUPERVISOR, "/" + confirmed.getId(), 404);
        var order = new ArrestOrder(); order.setExecutionPaper(em.find(ExecutionPaper.class, confirmed.getExecutionPaperId()));
        order.setStage(ArrestOrderStage.AWAITING_OFFICER_ASSIGNMENT); order.setEngine("local"); order.setRoute("USER");
        order.setStartedAt(Instant.now()); order.setIssuedAt(Instant.now());
        order.setReferredAt(Instant.now()); order.setReferralDestination("سرپرست هیئت اجرا");
        order.setSupervisor(profiles.findByUsername(SUPERVISOR).orElseThrow()); order.setAssignee(order.getSupervisor());
        em.persist(order); em.flush();
        assertThat(read(SUPERVISOR, "", 200).path("totalElements").asInt()).isEqualTo(1);
        assertThat(read(SUPERVISOR, "", 200).path("counts").path("total").asInt()).isEqualTo(1);
        read(SUPERVISOR, "/" + confirmed.getId(), 200);
        read(OFFICER, "/" + confirmed.getId(), 404);
        order.setOfficer(profiles.findByUsername(OFFICER).orElseThrow()); order.setAssignee(order.getOfficer());
        order.setStage(ArrestOrderStage.ASSIGNED_TO_OFFICER); em.flush();
        assertThat(read(OFFICER, "", 200).path("totalElements").asInt()).isEqualTo(1);
        read(OFFICER, "/" + confirmed.getId(), 200);
        read(OFFICER, "/" + waiting.getId(), 404);
    }

    @Test void detailsSeparateRequestSubjectsFromPaperRestrictionsAndKeepHistoryWithinTheRequest() throws Exception {
        long paperId = confirmed.getExecutionPaperId();
        var source = FakeOfficialGazetteRequest.builder().requestId(confirmed.getOfficialGazetteRequestId()).executionPaperId(paperId)
                .legalPersonNationalId("11111111111").debtPaperDate("1404-01-01").status("RESPONDED").sentAt("2026-10-10T09:00:00").build(); em.persist(source);
        event(paperId, "RESTRICTION_REQUEST_SENT", "2026-10-10T09:01:00");
        event(paperId, "RESTRICTION_REQUEST_CONFIRMED", "2026-10-10T09:02:00");
        event(paperId, "RESTRICTION_REQUEST_SENT", "2026-10-09T09:01:00");
        event(paperId, "RESTRICTION_REQUEST_CONFIRMED", "2026-10-11T09:02:00");
        event(paperId, "UNRELATED_ACTION", "2026-10-10T09:01:30");
        var person = NaturalPerson.builder().nationalCode("1234567890").firstName("علی").lastName("آزمایشی").build(); em.persist(person);
        var link = RestrictionsAppliedToCompanies.builder().executionPaper(em.find(ExecutionPaper.class, paperId)).naturalPerson(person)
                .restrictionState(RestrictionState.RELEASED).restrictionDate(LocalDateTime.parse("2026-10-10T09:02:00"))
                .restrictionTrackingCode("TRACK-1").releaseDate(LocalDateTime.parse("2026-10-12T10:00:00")).releaseTrackingCode("RELEASE-1").build(); em.persist(link); em.flush();
        var result = read(HEAD, "/" + confirmed.getId(), 200);
        assertThat(result.path("debtPaperDate").asText()).isEqualTo("1404-01-01");
        assertThat(result.path("request").path("status").asText()).isEqualTo("CONFIRMED");
        assertThat(result.path("subjects").size()).isEqualTo(1);
        assertThat(result.path("subjects").get(0).path("postTitle").asText()).isEqualTo("مدیرعامل");
        assertThat(result.path("paperRestrictions").get(0).path("state").asText()).isEqualTo("RELEASED");
        assertThat(result.path("history").size()).isEqualTo(2);
        assertThat(result.path("history").get(0).path("actor").asText()).doesNotContain("demo_");
        // The imported SQL demo uses ISO instants plus UTC epoch audit columns.
        confirmed.setCreatedBy("MOCK_TEHRAN_TABSARE_3_186"); confirmed.setSentAt("2026-10-10T09:01:00Z"); confirmed.setConfirmedAt("2026-10-10T09:02:00Z");
        source.setSentAt("2026-10-10T09:00:00Z"); em.flush();
        // Match the SQL seed's epoch storage, rather than persisting another local-time JPA value.
        em.createNativeQuery("update audit_log set created_at = :time where target_id = :paper and action = 'RESTRICTION_REQUEST_SENT' and created_at = :previous")
                .setParameter("time", Instant.parse("2026-10-10T09:01:00Z").toEpochMilli()).setParameter("paper", String.valueOf(paperId))
                .setParameter("previous", LocalDateTime.parse("2026-10-10T09:01:00").atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).executeUpdate();
        em.createNativeQuery("update audit_log set created_at = :time where target_id = :paper and action = 'RESTRICTION_REQUEST_CONFIRMED' and created_at = :previous")
                .setParameter("time", Instant.parse("2026-10-10T09:02:00Z").toEpochMilli()).setParameter("paper", String.valueOf(paperId))
                .setParameter("previous", LocalDateTime.parse("2026-10-10T09:02:00").atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).executeUpdate();
        em.clear();
        var seeded = read(HEAD, "/" + confirmed.getId(), 200);
        assertThat(seeded.path("history").size()).isEqualTo(2);
        assertThat(seeded.path("history").get(0).path("occurredAt").asText()).isEqualTo("2026-10-10T09:01:00Z");
    }

    @Test void requiresPermissionPreservesRevocationsAndRejectsInvalidFilters() throws Exception {
        http.perform(get(API)).andExpect(status().isUnauthorized());
        for (String path : List.of("?size=0", "?status=INVALID", "?from=2026-10-11&to=2026-10-01")) read(HEAD, path, 400);
        var grant = grants.findByGroupNameAndPermissionCode("DIRECTORATE_EXECUTION_UNIT_HEAD", "LEGAL_RESTRICTION_VIEW").orElseThrow();
        grants.delete(grant); em.flush(); securityData.initializeSecurityData();
        assertThat(grants.findByGroupNameAndPermissionCode("DIRECTORATE_EXECUTION_UNIT_HEAD", "LEGAL_RESTRICTION_VIEW")).isEmpty();
        read(HEAD, "", 403); read(HEAD, "/" + confirmed.getId(), 403);
    }

    private ExecutionPaper paper(String code, boolean legal, TaxAffairsOffice office) {
        var taxpayer = Taxpayer.builder().tin("TIN-" + code).registrationTraceCode("T-" + code).build();
        if (legal) { var person = LegalPerson.builder().nationalId(code).name("شرکت " + code).build(); em.persist(person); taxpayer.setLegalPerson(person); }
        else { var person = NaturalPerson.builder().nationalCode(code).firstName("علی").lastName(code).build(); em.persist(person); taxpayer.setNaturalPerson(person); }
        em.persist(taxpayer);
        var date = LocalDateTime.parse("2026-01-01T00:00:00");
        var debtPaper = FinalTaxDebtPaper.builder().taxpayer(taxpayer).paperType(PaperType.EF03).taxAffairsOffice(office)
                .finalizationReason(FinalizationReason.FINAL_NOTICE).uniqueFinalTaxDebtNoticeId("N-" + code).finalTaxDebtNoticeIssueDate(date)
                .finalTaxDebtPaperNotificationNumber(code).taxYear(1404).taxPeriod(TaxPeriod.of(1)).periodStartDate(date).periodEndDate(date).build(); em.persist(debtPaper);
        var paper = ExecutionPaper.builder().executionPaperUniqueCode(code).issuanceDate(date).finalTaxDebtPaper(debtPaper).currentState("NOTIFIED").build(); em.persist(paper);
        em.persist(DebtInformation.builder().finalTaxDebtPaper(debtPaper).totalLiability(new BigDecimal("100.25")).totalPenalties(BigDecimal.ZERO)
                .totalCredits(BigDecimal.ZERO).totalPayments(BigDecimal.ZERO).totalRemissions(BigDecimal.ZERO).outstandingTaxDebt(new BigDecimal("100.25")).build());
        return paper;
    }
    private FakeRestrictionRequest request(ExecutionPaper paper, String status, String sent) {
        var request = FakeRestrictionRequest.builder().executionPaperId(paper.getId()).officialGazetteRequestId("REQ-" + paper.getId())
                .status(status).relatedPersonCount(1).sentAt(sent).confirmedAt("CONFIRMED".equals(status) ? LocalDateTime.parse(sent).plusMinutes(1).toString() : null)
                .payload("[{\"personType\":\"NATURAL\",\"nationalCode\":\"1234567890\",\"firstName\":\"علی\",\"lastName\":\"آزمایشی\",\"postCode\":\"CEO\",\"startDate\":\"1400-01-01\"}]").build();
        em.persist(request); return request;
    }
    private void event(long paperId, String action, String date) { em.persist(AuditLog.builder().actorUsername(HEAD).action(action)
            .targetType("EXECUTION_PAPER").targetId(String.valueOf(paperId)).createdAt(LocalDateTime.parse(date)).newValue("{\"relatedPersonCount\":1}").build()); }
    private JsonNode read(String username, String path, int code) throws Exception {
        var response = http.perform(get(API + path).header("Authorization", "Bearer " + identities.login(username).accessToken()))
                .andExpect(status().is(code)).andReturn().getResponse();
        return response.getContentAsString().isBlank() ? json.createObjectNode() : json.readTree(response.getContentAsString());
    }
}
