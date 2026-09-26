package com.mparsict.vosolmaliat.unit.config;

import com.mparsict.vosolmaliat.domain.model.sanim.*;
import jakarta.persistence.*;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;

class SanimExactPersistenceTest {
    @TempDir Path directory;
    @Entity(name="ExactDocument") @Table(name="exact_document")
    public static class ExactDocument {
        @Id public Long id;
        @Embedded public SanimDocumentDetails source;
        public ExactDocument() {}
    }
    @Test void hibernateStoresAndReloadsNegativeDecimalAsTextWithoutSqliteNumericCoercion() throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("exact.db");
        String exact = "-999999999999999999999999999999.12345678901234567890";
        try (var factory = new Configuration().addAnnotatedClass(ExactDocument.class)
                .setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC")
                .setProperty("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop").buildSessionFactory()) {
            try (var session = factory.openSession()) {
                var tx = session.beginTransaction();
                ExactDocument doc = new ExactDocument(); doc.id = 1L;
                doc.source = SanimDocumentDetails.builder().sourceTaxOfficeCode("00123")
                        .amounts(SanimAmounts.builder().outstandingDebtAmount(new BigDecimal(exact)).build()).build();
                session.persist(doc); tx.commit();
            }
            try (var session = factory.openSession()) {
                var doc = session.find(ExactDocument.class, 1L);
                assertThat(doc.source.getAmounts().getOutstandingDebtAmount().toPlainString()).isEqualTo(exact);
                assertThat(doc.source.getSourceTaxOfficeCode()).isEqualTo("00123");
            }
            try (var c = DriverManager.getConnection(url); var s = c.createStatement();
                 var rows = s.executeQuery("SELECT typeof(aggregate_outstanding_debt_amount), aggregate_outstanding_debt_amount FROM exact_document")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("text");
                assertThat(rows.getString(2)).isEqualTo(exact);
            }
        }
    }
}
