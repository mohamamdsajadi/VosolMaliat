package com.mparsict.vosolmaliat.common.config;

import com.mparsict.vosolmaliat.domain.enums.ExecutionPaperState;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.execution.DebtInformation;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.execution.FinalizedDebtPaper;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxUnit;
import com.mparsict.vosolmaliat.domain.model.person.LegalPerson;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;
import com.mparsict.vosolmaliat.domain.repository.DebtInformationRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.LegalPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.NaturalPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxpayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DemoDataInitializer {

    private final NaturalPersonRepository naturalPersonRepository;
    private final LegalPersonRepository legalPersonRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final ExecutionPaperRepository executionPaperRepository;
    private final DebtInformationRepository debtInformationRepository;
    private final TransactionTemplate transactionTemplate;

    @Bean
    CommandLineRunner seedDemoDashboardData() {
        return args -> transactionTemplate.executeWithoutResult(status -> seedIfNeeded());
    }

    private void seedIfNeeded() {
        if (executionPaperRepository.count() > 0) {
            return;
        }

        NaturalPerson naturalPerson = NaturalPerson.builder()
                .nationalCode("0012345678")
                .firstName("علی")
                .lastName("رضایی")
                .fatherName("حسین")
                .birthDate("1368/02/18")
                .build();
        naturalPerson.setCreatedBy("demo");
        naturalPerson.setUpdatedBy("demo");
        naturalPerson = naturalPersonRepository.save(naturalPerson);

        LegalPerson legalPerson = LegalPerson.builder()
                .nationalId("14001234567")
                .registrationNumber("54321")
                .registrationDate("1399/07/12")
                .name("شرکت نمونه پارس")
                .build();
        legalPerson.setCreatedBy("demo");
        legalPerson.setUpdatedBy("demo");
        legalPerson = legalPersonRepository.save(legalPerson);

        Taxpayer firstTaxpayer = taxpayerRepository.save(Taxpayer.builder()
                .debtPaperUniqueCode("DEMO-DEBT-001")
                .debtPaperIssueDate("1403/01/20")
                .naturalPerson(naturalPerson)
                .build());

        Taxpayer secondTaxpayer = taxpayerRepository.save(Taxpayer.builder()
                .debtPaperUniqueCode("DEMO-DEBT-002")
                .debtPaperIssueDate("1403/02/10")
                .legalPerson(legalPerson)
                .build());

        createExecutionPaper("DEMO-EP-001", firstTaxpayer.getId(), ExecutionPaperState.UNASSIGNED, 1_250_000_000.0);
        createExecutionPaper("DEMO-EP-002", firstTaxpayer.getId(), ExecutionPaperState.AWAITING_ARREST_ORDER, 860_000_000.0);
        createExecutionPaper("DEMO-EP-003", secondTaxpayer.getId(), ExecutionPaperState.ARREST_ORDER_ISSUED, 2_430_000_000.0);
        createExecutionPaper("DEMO-EP-004", secondTaxpayer.getId(), ExecutionPaperState.ARREST_ORDER_NOT_ISSUED, 415_000_000.0);
        createExecutionPaper("DEMO-EP-005", firstTaxpayer.getId(), ExecutionPaperState.COMPLETED, 710_000_000.0);
        createExecutionPaper("DEMO-EP-006", secondTaxpayer.getId(), ExecutionPaperState.CANCELLED, 325_000_000.0);
        createExecutionPaper("DEMO-EP-007", firstTaxpayer.getId(), ExecutionPaperState.ASSIGNED, 540_000_000.0);

        log.info("Seeded demo dashboard data for an empty database");
    }

    private void createExecutionPaper(String code, Long taxpayerId, ExecutionPaperState state, Double remainingTax) {
        ExecutionPaper executionPaper = executionPaperRepository.save(ExecutionPaper.builder()
                .executionPaperUniqueCode(code)
                .finalizedDebtPaper(FinalizedDebtPaper.builder().id(1000L + Math.abs(code.hashCode() % 1000)).build())
                .taxpayer(Taxpayer.builder().id(taxpayerId).build())
                .taxUnit(TaxUnit.builder().id(111L).build())
                .finalizationReason("داده نمونه برای نمایش داشبورد")
                .taxSource("مالیات بر ارزش افزوده")
                .currentState(state.name())
                .build());

        debtInformationRepository.save(DebtInformation.builder()
                .executionPaper(executionPaper)
                .taxAmount(remainingTax)
                .dutyAmount(remainingTax * 0.09)
                .penaltyArticle22(remainingTax * 0.02)
                .penaltyArticle23(remainingTax * 0.015)
                .taxPaidAmount(remainingTax * 0.1)
                .penaltyPaidAmount(remainingTax * 0.01)
                .remainingTax(remainingTax)
                .build());
    }
}
