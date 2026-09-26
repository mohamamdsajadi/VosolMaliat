package com.mparsict.vosolmaliat.unit.config;
import com.mparsict.vosolmaliat.common.util.UniqueCodeGenerator;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionNumberSequence;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.orm.jpa.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
class ExecutionNumberConcurrencyTest {
    @TempDir Path directory;
    private org.hibernate.SessionFactory factory(String ddl) {
        return new Configuration().addAnnotatedClass(ExecutionNumberSequence.class)
            .setProperty("hibernate.connection.url","jdbc:sqlite:"+directory.resolve("numbers.db")+"?busy_timeout=30000")
            .setProperty("hibernate.connection.driver_class","org.sqlite.JDBC")
            .setProperty("hibernate.dialect","org.hibernate.community.dialect.SQLiteDialect")
            .setProperty("hibernate.hbm2ddl.auto",ddl).buildSessionFactory();
    }
    private UniqueCodeGenerator allocator(org.hibernate.SessionFactory factory) {
        var target=new UniqueCodeGenerator(); ReflectionTestUtils.setField(target,"entityManager",SharedEntityManagerCreator.createSharedEntityManager(factory));
        var proxy=new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(new JpaTransactionManager(factory),new AnnotationTransactionAttributeSource()));
        return (UniqueCodeGenerator)proxy.getProxy();
    }
    @Test void twoApplicationInstancesAllocateUniqueNumbersAndKeepSequenceAcrossRestart() throws Exception {
        try(var first=factory("create"); var second=factory("validate"); var pool=Executors.newFixedThreadPool(8)) {
            var a=allocator(first); var b=allocator(second);
            List<Callable<String>> jobs=new ArrayList<>();
            for(int i=0;i<40;i++) {var source=i%2==0?a:b;jobs.add(()->source.generateForYear(1405));}
            Set<String> numbers=new HashSet<>();
            for(var result:pool.invokeAll(jobs)) numbers.add(result.get(30,TimeUnit.SECONDS));
            assertThat(numbers).hasSize(40).contains("14050000001","14050000040");
            assertThat(a.generateForYear(1406)).isEqualTo("14060000001");
        }
        try(var restarted=factory("validate")) {assertThat(allocator(restarted).generateForYear(1405)).isEqualTo("14050000041");}
    }
    @Test void rolledBackIssuanceDoesNotConsumeNumberAndExhaustionDoesNotWrap() {
        try(var factory=factory("create")) {
            var codes=allocator(factory);
            var tx=new org.springframework.transaction.support.TransactionTemplate(new JpaTransactionManager(factory));
            assertThatThrownBy(()->tx.executeWithoutResult(status->{
                assertThat(codes.generateForYear(1405)).isEqualTo("14050000001");
                throw new IllegalStateException("issuance failed");
            })).hasMessage("issuance failed");
            assertThat(codes.generateForYear(1405)).isEqualTo("14050000001");
            tx.executeWithoutResult(status->{
                var em=SharedEntityManagerCreator.createSharedEntityManager(factory);
                em.createNativeQuery("update execution_number_sequence set last_number=9999999 where year=1405").executeUpdate();
            });
            assertThatThrownBy(()->codes.generateForYear(1405)).hasMessageContaining("capacity exhausted");
            assertThat(codes.generateForYear(1406)).isEqualTo("14060000001");
        }
    }
    @Test void solarHijriYearChangesAtNowruz() {
        assertThat(UniqueCodeGenerator.persianYear(LocalDate.of(2026,3,20))).isEqualTo(1404);
        assertThat(UniqueCodeGenerator.persianYear(LocalDate.of(2026,3,21))).isEqualTo(1405);
        assertThat(UniqueCodeGenerator.persianYear(LocalDate.of(2025,3,20))).isEqualTo(1403);
        assertThat(UniqueCodeGenerator.persianYear(LocalDate.of(2025,3,21))).isEqualTo(1404);
    }
    @Test void generatedInternalIdIsJalaliYearFollowedBySevenDigits() {
        try(var factory=factory("create")) {
            var number = allocator(factory).generateNumberForYear(1405);
            assertThat(number.id()).isEqualTo(14050000001L);
            assertThat(number.code()).isEqualTo("14050000001");
        }
    }
}
