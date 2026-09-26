package com.mparsict.vosolmaliat.common.util;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
@Component
public class UniqueCodeGenerator {
    @PersistenceContext private EntityManager entityManager;
    /** Reserve the database writer before reading issuance state, across processes. */
    @Transactional
    public void lockIssuance() {
        entityManager.createNativeQuery("INSERT INTO execution_number_sequence (year, last_number) VALUES (0, 0) ON CONFLICT(year) DO UPDATE SET last_number = 0").executeUpdate();
    }
    @Transactional
    public String generateExecutionPaperCode() {
        return generateForYear(persianYear(LocalDate.now(ZoneId.of("Asia/Tehran"))));
    }
    @Transactional
    public String generateForYear(int year) {
        if (year < 1 || year > 9999) throw new IllegalArgumentException("Invalid issuance year");
        lockIssuance();
        entityManager.createNativeQuery("INSERT INTO execution_number_sequence (year, last_number) VALUES (:year, 0) ON CONFLICT(year) DO NOTHING")
                .setParameter("year", year).executeUpdate();
        int changed = entityManager.createNativeQuery("UPDATE execution_number_sequence SET last_number = last_number + 1 WHERE year = :year AND last_number < 9999999")
                .setParameter("year", year).executeUpdate();
        if (changed != 1) throw new IllegalStateException("Execution paper number capacity exhausted for year " + year);
        Number number = (Number) entityManager.createNativeQuery("SELECT last_number FROM execution_number_sequence WHERE year = :year")
                .setParameter("year", year).getSingleResult();
        return String.format(Locale.ROOT, "%04d%07d", year, number.intValue());
    }
    public static int persianYear(LocalDate date) {
        int gy = date.getYear(), gm = date.getMonthValue(), gd = date.getDayOfMonth();
        int[] elapsed = {0,31,59,90,120,151,181,212,243,273,304,334};
        int gy2 = gm > 2 ? gy + 1 : gy;
        int days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100
                + (gy2 + 399) / 400 + gd + elapsed[gm - 1];
        int jy = -1595 + 33 * (days / 12053);
        days %= 12053; jy += 4 * (days / 1461); days %= 1461;
        if (days > 365) jy += (days - 1) / 365;
        return jy;
    }
    public String generateHash(String input) { return HashUtil.sha256(input); }
}
