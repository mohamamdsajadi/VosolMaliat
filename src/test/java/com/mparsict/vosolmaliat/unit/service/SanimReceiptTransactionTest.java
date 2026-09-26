package com.mparsict.vosolmaliat.unit.service;

import com.mparsict.vosolmaliat.application.dto.request.*;
import com.mparsict.vosolmaliat.application.service.*;
import com.mparsict.vosolmaliat.domain.enums.DebtPaperProcessingStatus;
import com.mparsict.vosolmaliat.domain.repository.*;
import com.mparsict.vosolmaliat.infrastructure.camunda.service.CamundaProcessService;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SanimReceiptTransactionTest {
    @TempDir Path directory;
    @Test void rejectedIssuanceRollsBackCaseButRetainsExactReceiptAndReviewReason() throws Exception {
        var configuration = new Configuration()
                .setProperty("hibernate.connection.url", "jdbc:sqlite:" + directory.resolve("receipt.db"))
                .setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC")
                .setProperty("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(jakarta.persistence.Entity.class));
        for (var bean : scanner.findCandidateComponents("com.mparsict.vosolmaliat.domain.model"))
            configuration.addAnnotatedClass(Class.forName(bean.getBeanClassName()));
        try (var factory = configuration.buildSessionFactory()) {
            var entityManager = SharedEntityManagerCreator.createSharedEntityManager(factory);
            var repositories = new JpaRepositoryFactory(entityManager);
            var records = repositories.getRepository(DebtPaperPushRecordRepository.class);
            // Also validate changed HQL against the actual Hibernate metamodel.
            repositories.getRepository(DebtInformationRepository.class);
            var papers = repositories.getRepository(ExecutionPaperRepository.class);
            var rules = mock(RuleEngineService.class);
            when(rules.evaluate(any())).thenReturn(new RuleEngineService.RuleEvaluationResult(false, "R", "review rule", null));
            var issuance = mock(IssuanceRequestService.class);
            var execution = mock(ExecutionPaperService.class);
            var identities = mock(FakeIdentityVerificationService.class);
            when(identities.status(any())).thenReturn("VERIFIED");
            when(identities.verifiedPerson(any())).thenAnswer(c -> ((DebtPaperIssuanceRequest)c.getArgument(0)).taxpayer());
            var requestEntities = repositories.getRepository(ExecutionPaperIssuanceRequestRepository.class);
            var requestEntity = new com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperIssuanceRequest(); requestEntity.setId(1L);
            when(issuance.createAndSave(any())).thenReturn(requestEntity);
            var target = new DebtPaperPushPersistenceService(records, papers, issuance, rules, execution,
                    identities, mock(com.mparsict.vosolmaliat.common.util.UniqueCodeGenerator.class), requestEntities);
            var proxy = new ProxyFactory(target);
            proxy.setProxyTargetClass(true);
            proxy.addAdvice(new TransactionInterceptor(new JpaTransactionManager(factory), new AnnotationTransactionAttributeSource()));
            var persistence = (DebtPaperPushPersistenceService) proxy.getProxy();
            var mapper = mock(DebtPaperPushMapper.class);
            var rejected = mock(DebtPaperIssuanceRequest.class);
            when(rejected.sourceDocument()).thenReturn(com.mparsict.vosolmaliat.TestSanimDocuments.document("doc","date","1400","1","activity"));
            when(mapper.map(any())).thenReturn(rejected);
            var workflow = mock(CamundaProcessService.class);
            var service = new DebtPaperPushService(mapper, persistence, workflow, mock(org.springframework.core.env.Environment.class));
            String xml = "<content>\n  <MAIN><HB>-1.20</HB></MAIN>\n</content>";
            var request = new DebtPaperPushRequest("1234567890", xml, "hash", OffsetDateTime.now(), "ref-1", null, null, 1400, 1, "00123", null, "TAXB", "LTR_EF03");
            assertThatThrownBy(() -> service.push(request, "trace-1")).hasMessageContaining("review rule");
            var receipt = records.findByReferenceId("ref-1").orElseThrow();
            assertThat(receipt.getXmlPayload()).isEqualTo(xml);
            assertThat(receipt.getSourceTaxOfficeCode()).isEqualTo("00123");
            assertThat(receipt.getProcessingStatus()).isEqualTo(DebtPaperProcessingStatus.NEEDS_REVIEW);
            assertThat(receipt.getFailureReason()).contains("review rule");
            assertThat(receipt.getExecutionPaperId()).isNull();
            assertThatThrownBy(() -> service.push(request, "trace-2")).hasMessageContaining("already been processed");
            assertThat(records.count()).isEqualTo(1);
            verifyNoInteractions(workflow, execution);
            // Valid documents use the real entity creation path and await internal routing.
            var codes = mock(com.mparsict.vosolmaliat.common.util.UniqueCodeGenerator.class);
            when(codes.generateForYear(anyInt())).thenReturn("EP-SANIM-1");
            var actualExecution = new ExecutionPaperService(papers,
                    repositories.getRepository(FinalizedDebtPaperRepository.class),
                    repositories.getRepository(TaxpayerRepository.class),
                    repositories.getRepository(ContactInfoRepository.class),
                    repositories.getRepository(NaturalPersonRepository.class),
                    repositories.getRepository(LegalPersonRepository.class),
                    repositories.getRepository(DebtInformationRepository.class),
                    repositories.getRepository(ExecutionPaperStateHistoryRepository.class),
                    repositories.getRepository(ExecutionPaperNotificationRepository.class),
                    repositories.getRepository(GeneralDirectorateOfTaxAffairsRepository.class),
                    repositories.getRepository(TaxAffairsOfficeRepository.class),
                    repositories.getRepository(TaxUnitRepository.class),
                    repositories.getRepository(com.mparsict.vosolmaliat.domain.repository.security.AuditLogRepository.class),
                    mock(com.mparsict.vosolmaliat.application.service.security.AuditLogService.class), codes,
                    repositories.getRepository(ExecutionPaperIssuanceRequestRepository.class), identities);
            when(execution.createFromRequest(any())).thenAnswer(call -> actualExecution.createFromRequest(call.getArgument(0)));
            when(rules.evaluate(any())).thenReturn(new RuleEngineService.RuleEvaluationResult(true, null, null, null));
            var actualMapper = new DebtPaperPushMapper(new DebtPaperXmlReader(),
                    repositories.getRepository(NaturalPersonRepository.class), repositories.getRepository(LegalPersonRepository.class));
            when(mapper.map(any())).thenAnswer(call -> actualMapper.map(call.getArgument(0)));
            String validXml = "<content><MAIN><C>1400</C><F>doc</F><G>1400/01/01</G><P>حقیقی</P><Q>10001</Q><M>خود اظهاری</M><N>basis-1</N><O>1400/01/01</O><D>1400/01/01</D><E>1400/12/29</E><R>1234567890</R><S>نام کامل</S><HB>12345678901234567890.123</HB><CB>12345678901234567890.123</CB></MAIN></content>";
            var valid = new DebtPaperPushRequest("1234567890", validXml, "hash", OffsetDateTime.now(), "ref-valid", "follow-valid", null, 1400, 1, "00123", null, "TAXB", "LTR_EF03");
            Long paperId = service.push(valid, "trace-valid");
            var saved = records.findByReferenceId("ref-valid").orElseThrow();
            assertThat(saved.getProcessingStatus()).isEqualTo(DebtPaperProcessingStatus.WAITING_ORGANIZATION);
            assertThat(saved.getXmlPayload()).isEqualTo(validXml);
            assertThat(saved.getExecutionPaperId()).isEqualTo(paperId);
            var initial = repositories.getRepository(FinalizedDebtPaperRepository.class).findByDebtPaperUniqueCode("basis-1").orElseThrow();
            var current = repositories.getRepository(DebtInformationRepository.class).findByExecutionPaperId(paperId).orElseThrow();
            assertThat(initial.getSourceDocument().getAmounts().getOutstandingDebtAmount()).isEqualByComparingTo("12345678901234567890.123");
            assertThat(current.getCurrentAmounts().getOutstandingDebtAmount()).isEqualByComparingTo("12345678901234567890.123");
            assertThat(current.getCurrentAmounts().getTotalTaxAmount()).isEqualByComparingTo("12345678901234567890.123");
            assertThat(repositories.getRepository(DebtInformationRepository.class).sumTotalDebtForIssuedExecutionPapers())
                    .isEqualByComparingTo("12345678901234567890.123");
            verifyNoInteractions(workflow);
            var justAbove = new java.math.BigDecimal("12345678901234567890.124");
            var minCriteria = new ExecutionPaperSearchCriteria(null, null, null, null, null, null, justAbove, null);
            assertThat(actualExecution.searchAll(minCriteria, 0, 10).totalElements()).isZero();
            var exactCriteria = new ExecutionPaperSearchCriteria(null, null, null, null, null, null,
                    new java.math.BigDecimal("12345678901234567890.123"), justAbove);
            assertThat(actualExecution.searchAll(exactCriteria, 0, 10).content()).hasSize(1);


        }
    }
}
