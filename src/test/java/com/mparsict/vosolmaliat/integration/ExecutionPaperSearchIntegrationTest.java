package com.mparsict.vosolmaliat.integration;

import com.mparsict.vosolmaliat.application.dto.request.ExecutionPaperSearchCriteria;
import com.mparsict.vosolmaliat.application.service.ExecutionPaperService;
import com.mparsict.vosolmaliat.domain.enums.*;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.execution.*;
import com.mparsict.vosolmaliat.domain.model.person.*;
import com.mparsict.vosolmaliat.domain.repository.TaxAffairsOfficeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import com.mparsict.vosolmaliat.common.config.security.MockIdentityProvider;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:sqlite:file:paper-search-integration?mode=memory&cache=shared"})
@ActiveProfiles("mock-local")
@AutoConfigureMockMvc
@Transactional
class ExecutionPaperSearchIntegrationTest {
    @Autowired EntityManager em;
    @Autowired ExecutionPaperService service;
    @Autowired TaxAffairsOfficeRepository offices;
    @Autowired MockMvc http;
    @Autowired MockIdentityProvider identities;

    @BeforeEach
    void papers() {
        addPaper(true, "2026-08-30T23:59:59", "9007199254740993.02", "PAPER-1", "11111111111");
        addPaper(false, "2026-08-31T00:00:00", "9007199254740993.01", "PAPER-2", "2222222222");
        addPaper(true, "2026-09-01T12:00:00", "9.1", "PAPER-3", "33333333333");
        em.flush(); em.clear();
    }

    @Test
    void combinedSearchMatchesNamesIdentifiersAndEconomicCodeBeforePagination() {
        for (String search : java.util.List.of("شرکت PAPER-1", "11111111111", "TIN-PAPER-1", "PAPER-1")) {
            var result = service.searchAll(criteria(search, null, null, null, null, null), 0, 1);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.content().getFirst().uniqueCode()).isEqualTo("PAPER-1");
        }
    }

    @Test
    void filtersTaxpayerTypeAndInclusiveIssueDateUsingActualIssuanceDate() {
        var day = LocalDate.parse("2026-08-30");
        var result = service.searchAll(criteria(null, "LEGAL", day, day, "issuanceDate", "asc"), 0, 50);
        assertThat(result.content()).extracting(item -> item.uniqueCode()).containsExactly("PAPER-1");
        assertThat(result.content().getFirst().issuanceDate()).isEqualTo(LocalDateTime.parse("2026-08-30T23:59:59"));
        assertThat(service.searchAll(criteria(null, "NATURAL", null, null, null, null), 0, 50).content())
                .extracting(item -> item.uniqueCode()).containsExactly("PAPER-2");
    }

    @Test
    void sortsDebtAsExactDecimalsAcrossPages() {
        var query = criteria(null, null, null, null, "totalDebt", "desc");
        assertThat(service.searchAll(query, 0, 1).content().getFirst().uniqueCode()).isEqualTo("PAPER-1");
        assertThat(service.searchAll(query, 1, 1).content().getFirst().uniqueCode()).isEqualTo("PAPER-2");
        assertThat(service.searchAll(criteria(null, null, null, null, "totalDebt", "asc"), 0, 1)
                .content().getFirst().uniqueCode()).isEqualTo("PAPER-3");
    }

    @Test
    void listExposesSourceFieldsAndSortsTaxYearAndPeriodAcrossPages() {
        var result = service.searchAll(criteria(null, null, null, null, "taxYear", "asc"), 0, 1);
        var first = result.content().getFirst();
        assertThat(first.uniqueCode()).isEqualTo("PAPER-1");
        assertThat(first.taxpayerCode()).isEqualTo("TIN-PAPER-1");
        assertThat(first.taxType()).isEqualTo(PaperType.EF03.getPersianTitle());
        assertThat(first.taxYear()).isEqualTo(1403);
        assertThat(first.taxPeriod()).isEqualTo(1);
        assertThat(service.searchAll(criteria("PAPER-2", null, null, null, null, null), 0, 1)
                .content().getFirst().taxType()).isEqualTo(PaperType.EF23.getPersianTitle());
        assertThat(service.searchAll(criteria("PAPER-3", null, null, null, null, null), 0, 1)
                .content().getFirst().taxType()).isEqualTo(PaperType.EF104.getPersianTitle());
        assertThat(service.searchAll(criteria(null, null, null, null, "taxYear", "desc"), 0, 1)
                .content().getFirst().uniqueCode()).isEqualTo("PAPER-3");
        assertThat(service.searchAll(criteria(null, null, null, null, "taxPeriod", "desc"), 1, 1)
                .content().getFirst().uniqueCode()).isEqualTo("PAPER-2");
    }

    @Test
    void rejectsInvalidRangesAndSortFields() {
        assertThatThrownBy(() -> service.searchAll(criteria(null, null,
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-08-30"), null, null), 0, 50))
                .isInstanceOf(com.mparsict.vosolmaliat.common.exception.BusinessException.class);
        assertThatThrownBy(() -> service.searchAll(criteria(null, null, null, null, "arbitraryField", "asc"), 0, 50))
                .isInstanceOf(com.mparsict.vosolmaliat.common.exception.BusinessException.class);
    }

    @Test
    void detailsExposeExactDebtHistoryAndOnlyOtherActivePapersForSameTaxpayer() throws Exception {
        Long id = service.searchAll(criteria("PAPER-1", null, null, null, null, null), 0, 1).content().getFirst().id();
        var paper = em.find(ExecutionPaper.class, id);
        var debt = em.createQuery("SELECT d FROM DebtInformation d WHERE d.finalTaxDebtPaper.id = :id", DebtInformation.class)
                .setParameter("id", paper.getFinalTaxDebtPaper().getId()).getSingleResult();
        var firstDate = paper.getIssuanceDate();
        debt.setFinancialDetails(com.mparsict.vosolmaliat.domain.model.sanim.DebtFinancialDetails.builder()
                .taxLiabilityDetails("<details paperType=\"EF03\"><CB>9007199254740993.02</CB></details>")
                .taxCreditDetails("<details/>").totalPenaltiesDetails("<details><GB>0</GB></details>")
                .totalPaymentsDetails("<details><EB>0</EB></details>").build());
        var first = DebtInformationHistory.current(debt, firstDate);
        first.close(firstDate.plusDays(1));
        em.persist(first);
        debt.getCurrentAmounts().setTotalPaidAmount(new BigDecimal("50"));
        debt.getCurrentAmounts().setOutstandingDebtAmount(new BigDecimal("9007199254740943.02"));
        em.persist(DebtInformationHistory.current(debt, firstDate.plusDays(1)));
        // The current table is authoritative even if the last history row differs.
        debt.getCurrentAmounts().setTotalPaidAmount(new BigDecimal("100"));
        debt.getCurrentAmounts().setTotalCreditAmount(new BigDecimal("0.01"));
        debt.getCurrentAmounts().setOutstandingDebtAmount(new BigDecimal("9007199254740893.02"));
        debt.getFinancialDetails().setTotalPaymentsDetails("<details paperType=\"EF03\"><EB>100</EB></details>");
        debt.setTotalPayments(new BigDecimal("100"));
        debt.setTotalCredits(new BigDecimal("0.01"));
        debt.setOutstandingTaxDebt(new BigDecimal("9007199254740893.02"));
        debt.setPaymentsDetail(debt.getFinancialDetails().getTotalPaymentsDetails());
        for (String state : java.util.List.of("ASSIGNED", "COMPLETED", "CANCELLED")) {
            em.persist(ExecutionPaper.builder().executionPaperUniqueCode("RELATED-" + state)
                    .issuanceDate(firstDate).finalTaxDebtPaper(paper.getFinalTaxDebtPaper()).currentState(state).build());
        }
        em.flush(); em.clear();
        String bearer = "Bearer " + identities.login("demo_tax_directorate_director_general").accessToken();
        http.perform(get("/api/v1/execution-papers/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debtInformation.history.length()").value(2))
                .andExpect(jsonPath("$.debtInformation.history[0].outstandingAmount").value("9007199254740993.02"))
                .andExpect(jsonPath("$.debtInformation.history[0].current").value(false))
                .andExpect(jsonPath("$.debtInformation.history[1].outstandingAmount").value("9007199254740893.02"))
                .andExpect(jsonPath("$.debtInformation.history[1].paidAmount").value("100"))
                .andExpect(jsonPath("$.debtInformation.history[1].creditAmount").value("0.01"))
                .andExpect(jsonPath("$.debtInformation.history[0].financialDetails.totalPaymentsDetails").value("<details><EB>0</EB></details>"))
                .andExpect(jsonPath("$.debtInformation.history[1].financialDetails.totalPaymentsDetails").value("<details paperType=\"EF03\"><EB>100</EB></details>"))
                .andExpect(jsonPath("$.debtInformation.history[1].current").value(true))
                .andExpect(jsonPath("$.executionPaper.otherActivePapers.length()").value(1))
                .andExpect(jsonPath("$.executionPaper.otherActivePapers[0].uniqueCode").value("RELATED-ASSIGNED"));
    }

    @Test
    void debtWithoutHistoryStillExposesCurrentColumnsWithoutInventingInitialAmounts() {
        Long id = service.searchAll(criteria("PAPER-2", null, null, null, null, null), 0, 1).content().getFirst().id();
        var debt = service.getDetails(id).debtInformation();
        var rows = (java.util.List<java.util.Map<String, Object>>) debt.get("history");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("unavailable", true).containsEntry("current", false)
                .containsEntry("principalAmount", null);
        assertThat(rows.get(1)).containsEntry("current", true).containsEntry("principalAmount", "9007199254740993.01")
                .containsEntry("creditAmount", "0").containsEntry("paidAmount", "0");
    }

    @Test
    void generalOfficeHeadCanReadPaperListDetailsAndHistoryWithoutBroaderPrivileges() throws Exception {
        String token = identities.login("demo_tax_directorate_director_general").accessToken();
        String bearer = "Bearer " + token;
        var permissions = http.perform(get("/api/v1/auth/me/permissions").header("Authorization", bearer))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(permissions).contains("EXECUTION_PAPER_LIST_VIEW", "EXECUTION_PAPER_DETAIL_VIEW", "EXECUTION_PAPER_TRACE_VIEW")
                .doesNotContain("REPORT_VIEW", "USER_PROFILE_CREATE", "PERMISSION_ASSIGN");
        Long id = service.searchAll(criteria("PAPER-1", null, null, null, null, null), 0, 50).content().getFirst().id();
        for (String path : java.util.List.of("/api/v1/execution-papers", "/api/v1/execution-papers/assigned", "/api/v1/execution-papers/unassigned")) {
            http.perform(get(path).header("Authorization", bearer)).andExpect(status().isOk());
        }
        for (String path : java.util.List.of("/api/v1/execution-papers", "/api/v1/execution-papers/unassigned")) {
            http.perform(get(path).param("search", "PAPER-2").header("Authorization", bearer))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].taxpayerCode").value("TIN-PAPER-2"))
                    .andExpect(jsonPath("$.content[0].taxType").value(PaperType.EF23.getPersianTitle()))
                    .andExpect(jsonPath("$.content[0].taxYear").value(1404))
                    .andExpect(jsonPath("$.content[0].taxPeriod").value(2));
        }
        http.perform(get("/api/v1/execution-papers/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.uniqueCode").value("PAPER-1"));
        http.perform(get("/api/v1/execution-papers/{id}/trace", id).header("Authorization", bearer)).andExpect(status().isOk());
        http.perform(get("/api/v1/security-bootstrap/keycloak-sync-status").header("Authorization", bearer)).andExpect(status().isForbidden());
    }

    @Test
    void demoOperationalRolesCanViewPapersButCannotManagePermissions() throws Exception {
        for (String username : java.util.List.of("demo_directorate_execution_unit_head", "demo_execution_officer")) {
            String bearer = "Bearer " + identities.login(username).accessToken();
            http.perform(get("/api/v1/execution-papers").header("Authorization", bearer)).andExpect(status().isOk());
            http.perform(get("/api/v1/group-permissions").header("Authorization", bearer)).andExpect(status().isForbidden());
        }
    }

    private ExecutionPaperSearchCriteria criteria(String search, String type, LocalDate from, LocalDate to, String sort, String direction) {
        return new ExecutionPaperSearchCriteria(null, null, null, null, null, null, null, search, type, from, to, sort, direction);
    }

    private void addPaper(boolean legal, String issued, String debt, String code, String nationalId) {
        var taxpayer = Taxpayer.builder().tin("TIN-" + code).registrationTraceCode("FOLLOW-" + code).build();
        if (legal) {
            var person = LegalPerson.builder().nationalId(nationalId).name("شرکت " + code).build();
            em.persist(person); taxpayer.setLegalPerson(person);
        } else {
            var person = NaturalPerson.builder().nationalCode(nationalId).firstName("علی").lastName(code).build();
            person.getExtraInfo().setIsForeign(false);
            em.persist(person); taxpayer.setNaturalPerson(person);
        }
        em.persist(taxpayer);
        var date = LocalDateTime.parse(issued);
        var type = switch (code) { case "PAPER-2" -> PaperType.EF23; case "PAPER-3" -> PaperType.EF104; default -> PaperType.EF03; };
        var finalPaper = FinalTaxDebtPaper.builder().taxpayer(taxpayer).paperType(type)
                .taxAffairsOffice(offices.findAll().getFirst()).finalizationReason(FinalizationReason.FINAL_NOTICE)
                .uniqueFinalTaxDebtNoticeId("NOTICE-" + code).finalTaxDebtNoticeIssueDate(date)
                .finalTaxDebtPaperNotificationNumber("N-" + code).taxYear(1402 + Integer.parseInt(code.substring(6)))
                .taxPeriod(TaxPeriod.of(Integer.parseInt(code.substring(6))))
                .periodStartDate(date).periodEndDate(date).build();
        em.persist(finalPaper);
        var paper = ExecutionPaper.builder().executionPaperUniqueCode(code).issuanceDate(date)
                .finalTaxDebtPaper(finalPaper).currentState("UNASSIGNED").build();
        em.persist(paper);
        var amount = new BigDecimal(debt);
        em.persist(DebtInformation.builder().finalTaxDebtPaper(finalPaper).totalLiability(amount)
                .totalPenalties(BigDecimal.ZERO).totalCredits(BigDecimal.ZERO).totalPayments(BigDecimal.ZERO)
                .totalRemissions(BigDecimal.ZERO).outstandingTaxDebt(amount).build());
    }
}
