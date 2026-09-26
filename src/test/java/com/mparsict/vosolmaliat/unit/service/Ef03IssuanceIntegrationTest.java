package com.mparsict.vosolmaliat.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.*;
import com.mparsict.vosolmaliat.application.service.*;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.common.util.UniqueCodeGenerator;
import com.mparsict.vosolmaliat.domain.enums.DebtPaperProcessingStatus;
import com.mparsict.vosolmaliat.domain.repository.*;
import com.mparsict.vosolmaliat.domain.repository.security.AuditLogRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class Ef03IssuanceIntegrationTest {
    @TempDir Path directory;
    static <T> T transactional(T target, JpaTransactionManager tx) {
        var proxy=new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(tx,new AnnotationTransactionAttributeSource()));
        return (T)proxy.getProxy();
    }
    static DebtPaperPushRequest source(String reference,String identifier,String tin,String basis) {
        String xml="<content><MAIN><B>مالیات عملکرد</B><C>1403</C><D>1403/01/01</D><E>1403/12/30</E>"
            +"<F>letter-"+reference+"</F><G>1405/06/03</G><H>اداره کل</H><I>اداره امور</I><M>خود اظهاری</M>"
            +"<N>"+basis+"</N><O>1404/09/07</O><Q>"+tin+"</Q><R>"+identifier+"</R><S>نام سنیم</S>"
            +"<CB>100.75</CB><GB>20.5</GB><NB>20.50</NB><EB>10</EB><HB>111.25</HB></MAIN>"
            +"<penalty><KB>الف</KB><MB>10.25</MB><KB>الف</KB><MB>10.25</MB></penalty></content>";
        return new DebtPaperPushRequest(identifier,xml,"hash",OffsetDateTime.now(),reference,"follow-"+tin,null,
                1390,4,"001",null,"TAXB","LTR_EF03");
    }
    @Test void receiveWaitVerifyIssueAndPersistIndependentAmountsAndDetailsWithTinCardinality() throws Exception {
        var cfg=new Configuration().setProperty("hibernate.connection.url","jdbc:sqlite:"+directory.resolve("ef03.db")+"?foreign_keys=on&busy_timeout=30000")
            .setProperty("hibernate.connection.driver_class","org.sqlite.JDBC")
            .setProperty("hibernate.dialect","com.mparsict.vosolmaliat.common.config.RelationalSqliteDialect")
            .setProperty("hibernate.hbm2ddl.auto","create-only")
            .setProperty("hibernate.physical_naming_strategy","org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        var scanner=new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(jakarta.persistence.Entity.class));
        for(var bean:scanner.findCandidateComponents("com.mparsict.vosolmaliat.domain.model")) cfg.addAnnotatedClass(Class.forName(bean.getBeanClassName()));
        try(var factory=cfg.buildSessionFactory()) {
            var em=SharedEntityManagerCreator.createSharedEntityManager(factory);
            var tx=new JpaTransactionManager(factory); var repos=new JpaRepositoryFactory(em);
            var receipts=repos.getRepository(DebtPaperPushRecordRepository.class);
            var papers=repos.getRepository(ExecutionPaperRepository.class);
            var requests=repos.getRepository(ExecutionPaperIssuanceRequestRepository.class);
            var persons=repos.getRepository(NaturalPersonRepository.class);
            var legal=repos.getRepository(LegalPersonRepository.class);
            var taxpayers=repos.getRepository(TaxpayerRepository.class);
            var debts=repos.getRepository(DebtInformationRepository.class);
            var finalized=repos.getRepository(FinalizedDebtPaperRepository.class);
            var codeTarget=new UniqueCodeGenerator(); ReflectionTestUtils.setField(codeTarget,"entityManager",em);
            var codes=transactional(codeTarget,tx);
            var identities=transactional(new FakeIdentityVerificationService(repos.getRepository(FakeIdentityVerificationRepository.class),codes),tx);
            var issuer=transactional(new ExecutionPaperService(papers,finalized,taxpayers,
                repos.getRepository(ContactInfoRepository.class),persons,legal,debts,
                repos.getRepository(ExecutionPaperStateHistoryRepository.class),repos.getRepository(ExecutionPaperNotificationRepository.class),
                repos.getRepository(GeneralDirectorateOfTaxAffairsRepository.class),repos.getRepository(TaxAffairsOfficeRepository.class),
                repos.getRepository(TaxUnitRepository.class),repos.getRepository(AuditLogRepository.class),mock(AuditLogService.class),codes,requests,identities),tx);
            var issuance=transactional(new IssuanceRequestService(requests,new ObjectMapper().findAndRegisterModules()),tx);
            var rules=mock(RuleEngineService.class);
            when(rules.evaluate(any())).thenReturn(new RuleEngineService.RuleEvaluationResult(true,null,null,null));
            var persistence=transactional(new DebtPaperPushPersistenceService(receipts,papers,issuance,rules,issuer,identities,codes,requests),tx);
            var workflow=mock(CamundaProcessService.class);
            var service=new DebtPaperPushService(new DebtPaperPushMapper(new DebtPaperXmlReader(),persons,legal),persistence,workflow, mock(org.springframework.core.env.Environment.class));
            var input=source("r1","1234567890","10001","basis-1");
            assertThat(service.push(input,"trace-1")).isNull();
            var receipt=receipts.findByReferenceId("r1").orElseThrow();
            assertThat(receipt.getProcessingStatus()).isEqualTo(DebtPaperProcessingStatus.WAITING_IDENTITY);
            assertThat(papers.count()).isZero(); assertThat(finalized.count()).isZero();
            assertThat(requests.findById(receipt.getIssuanceRequestId()).orElseThrow().getXml()).isEqualTo(input.xml());
            identities.decide("1234567890",new FakeIdentityVerificationService.Decision(true,"نام صحیح","نام خانوادگی",null,null,null,null,null,null));
            Long id=service.resume(receipt.getId());
            var paper=papers.findById(id).orElseThrow();
            assertThat(paper.getExecutionPaperUniqueCode()).matches("[0-9]{11}").endsWith("0000001");
            assertThat(paper.getIssuanceDate()).isNotNull();
            assertThat(paper.getFinalizationReason()).isEqualTo("SELF_DECLARATION");
            var basis=finalized.findByDebtPaperUniqueCode("basis-1").orElseThrow();
            assertThat(basis.getBasisIssueDate()).isEqualTo("1404/09/07");
            assertThat(basis.getSourceDocument().getDocumentNumber()).isEqualTo("letter-r1");
            assertThat(basis.getSourceDocument().getTaxYear()).isEqualTo("1403");
            assertThat(basis.getSourceDocument().getTaxPeriod()).isEqualTo("1");
            assertThat(basis.getSourceDocument().getTaxPeriodEndRaw()).isEqualTo("1403/12/30");
            assertThat(basis.getSourceDocument().getXml()).isEqualTo(input.xml());
            var debt=debts.findByExecutionPaperId(id).orElseThrow();
            assertThat(debt.getCurrentAmounts().getTotalTaxAmount()).isEqualByComparingTo("100.75");
            assertThat(debt.getCurrentAmounts().getTotalPenaltyAmount()).isEqualByComparingTo("20.5");
            assertThat(debt.getCurrentAmounts().getTotalRemissionAmount()).isZero();
            assertThat(debt.getCurrentAmounts().getTotalPaidAmount()).isEqualByComparingTo("10");
            assertThat(debt.getCurrentAmounts().getOutstandingDebtAmount()).isEqualByComparingTo("111.25");
            assertThat(new ObjectMapper().readTree(debt.getFinancialDetails().getTotalPenaltiesDetails())).hasSize(2);
            assertThat(new ObjectMapper().readTree(debt.getFinancialDetails().getTotalRemissionsDetails()).get(0).get("reported").asText()).isEqualTo("false");
            assertThat(persons.findByNationalCode("1234567890").orElseThrow().getFirstName()).isEqualTo("نام صحیح");
            assertThat(service.push(source("r2","1234567890","10002","basis-2"),"trace-2")).isNotNull();
            assertThat(persons.count()).isEqualTo(1); assertThat(taxpayers.count()).isEqualTo(2);
            assertThat(service.push(source("r3","1234567890","10001","basis-3"),"trace-3")).isNotNull();
            assertThat(taxpayers.count()).isEqualTo(2);
            assertThatThrownBy(()->service.resume(receipt.getId())).hasMessageContaining("در انتظار هویت نیست");
            assertThat(receipts.findById(receipt.getId()).orElseThrow().getProcessingStatus()).isEqualTo(DebtPaperProcessingStatus.WAITING_ORGANIZATION);
            var rejected=source("rejected","1111111111","10003","basis-reject"); service.push(rejected,"trace-r");
            var rejectReceipt=receipts.findByReferenceId("rejected").orElseThrow();
            identities.decide("1111111111",new FakeIdentityVerificationService.Decision(false,null,null,null,null,null,null,null,"عدم تطابق"));
            assertThatThrownBy(()->service.resume(rejectReceipt.getId())).hasMessageContaining("رد شد");
            assertThat(receipts.findById(rejectReceipt.getId()).orElseThrow().getProcessingStatus()).isEqualTo(DebtPaperProcessingStatus.REJECTED_IDENTITY);
            assertThat(finalized.findByDebtPaperUniqueCode("basis-reject")).isEmpty();
            service.push(source("legal-1","12345678901","20001","legal-basis-1"),"trace-l");
            identities.decide("12345678901",new FakeIdentityVerificationService.Decision(true,null,null,null,null,"شرکت تأییدشده",null,null,null));
            service.resume(receipts.findByReferenceId("legal-1").orElseThrow().getId());
            assertThatThrownBy(()->service.push(source("legal-2","12345678901","20002","legal-basis-2"),"trace-l2")).hasMessageContaining("فقط یک TIN");
            assertThat(finalized.findByDebtPaperUniqueCode("legal-basis-2")).isEmpty();
            assertThat(legal.count()).isEqualTo(1);
            // Concurrent replies for one pending receipt must never issue two papers.
            service.push(source("concurrent","2222222222","30001","concurrent-basis"),"trace-c");
            identities.decide("2222222222",new FakeIdentityVerificationService.Decision(true,"نام","همزمان",null,null,null,null,null,null));
            Long pendingId=receipts.findByReferenceId("concurrent").orElseThrow().getId();
            long before=papers.count();
            try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
                var jobs=java.util.List.<java.util.concurrent.Callable<Boolean>>of(
                    ()->{try{service.resume(pendingId);return true;}catch(RuntimeException ex){return false;}},
                    ()->{try{service.resume(pendingId);return true;}catch(RuntimeException ex){return false;}});
                var outcomes=pool.invokeAll(jobs);
                assertThat(outcomes.get(0).get() || outcomes.get(1).get()).isTrue();
                assertThat(outcomes.get(0).get() && outcomes.get(1).get()).isFalse();
            }
            assertThat(papers.count()).isEqualTo(before+1);
            assertThat(receipts.findById(pendingId).orElseThrow().getProcessingStatus()).isEqualTo(DebtPaperProcessingStatus.WAITING_ORGANIZATION);
            var directInput=source("direct","3333333333","40001","direct-basis");
            assertThat(service.push(directInput,"direct-pending")).isNull();
            Long directId=service.pushImmediately(directInput,"admin-direct");
            assertThat(directId).isNotNull();
            assertThat(papers.findById(directId)).isPresent();
            assertThat(receipts.findByReferenceId("direct").orElseThrow().getProcessingStatus())
                    .isEqualTo(DebtPaperProcessingStatus.WAITING_ORGANIZATION);
            assertThat(requests.findById(receipts.findByReferenceId("direct").orElseThrow().getIssuanceRequestId())
                    .orElseThrow().getIdentityStatus()).isEqualTo("NOT_REQUIRED");

            // Startup migrations must preserve new finalization fields and exact source facts.
            com.mparsict.vosolmaliat.common.config.SqliteLegacySchemaMigrator.migrate("jdbc:sqlite:"+directory.resolve("ef03.db"));
            assertThat(finalized.findByDebtPaperUniqueCode("basis-1").orElseThrow().getFinalizationReason()).isEqualTo("SELF_DECLARATION");
            assertThat(papers.findById(id).orElseThrow().getFinalizationReason()).isEqualTo("SELF_DECLARATION");
            try(var connection=java.sql.DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("ef03.db")+"?foreign_keys=on"); var sql=connection.createStatement()) {
                try(var keys=sql.executeQuery("PRAGMA foreign_key_list(execution_paper)")) { assertThat(keys.next()).isTrue(); }
                assertThatThrownBy(()->sql.executeUpdate("UPDATE execution_paper SET taxpayer_id=999999 WHERE id="+id))
                    .isInstanceOf(java.sql.SQLException.class);
            }
            verifyNoInteractions(workflow);
        }
    }
}
