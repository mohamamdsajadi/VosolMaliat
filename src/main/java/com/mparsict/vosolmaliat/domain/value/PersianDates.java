package com.mparsict.vosolmaliat.domain.value;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import com.mparsict.vosolmaliat.common.util.UniqueCodeGenerator;

/** Persian business dates are converted to ISO timestamps only at the persistence boundary. */
public final class PersianDates {
    private PersianDates() {}
    public static String digits(String value) {
        if (value == null) return null;
        StringBuilder result = new StringBuilder();
        for (char c : value.trim().toCharArray()) {
            if (c >= '۰' && c <= '۹') c = (char) ('0' + c - '۰');
            if (c >= '٠' && c <= '٩') c = (char) ('0' + c - '٠');
            result.append(c);
        }
        return result.toString();
    }
    public static LocalDate startOfYear(int year) {
        if (year < 1200 || year > 1600) throw new IllegalArgumentException("سال شمسی خارج از محدوده پشتیبانی‌شده است.");
        LocalDate date = LocalDate.of(year + 621, 3, 18);
        while (UniqueCodeGenerator.persianYear(date) < year) date = date.plusDays(1);
        return date;
    }
    public static int monthLength(int year, int month) {
        if (month < 1 || month > 12) throw new IllegalArgumentException("ماه شمسی نامعتبر است.");
        return month <= 6 ? 31 : month <= 11 ? 30
                : (int) java.time.temporal.ChronoUnit.DAYS.between(startOfYear(year), startOfYear(year + 1)) - 336;
    }
    public static LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String[] parts = digits(value).replace('/', '-').replace(' ', 'T').split("T", 2);
            String[] date = parts[0].split("-");
            int year = Integer.parseInt(date[0]), month = Integer.parseInt(date[1]), day = Integer.parseInt(date[2]);
            if (date.length != 3 || day < 1 || day > monthLength(year, month)) throw new IllegalArgumentException();
            int offset = month <= 6 ? (month - 1) * 31 : 186 + (month - 7) * 30;
            LocalTime time = parts.length == 1 ? LocalTime.MIDNIGHT : LocalTime.parse(parts[1]);
            return startOfYear(year).plusDays(offset + day - 1).atTime(time);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("تاریخ شمسی نامعتبر است: " + value, exception);
        }
    }
    public static String format(LocalDateTime value) {
        if (value == null) return null;
        int year = UniqueCodeGenerator.persianYear(value.toLocalDate());
        int day = (int) java.time.temporal.ChronoUnit.DAYS.between(startOfYear(year), value.toLocalDate());
        int month = day < 186 ? day / 31 + 1 : (day - 186) / 30 + 7;
        int monthDay = day < 186 ? day % 31 + 1 : (day - 186) % 30 + 1;
        String date = String.format(Locale.ROOT, "%04d/%02d/%02d", year, month, monthDay);
        return value.toLocalTime().equals(LocalTime.MIDNIGHT) ? date : date + "T" + value.toLocalTime();
    }
    public static int currentYear() { return UniqueCodeGenerator.persianYear(LocalDate.now(ZoneId.of("Asia/Tehran"))); }
    public static String periodStart(int year, Integer period) {
        int month = period == null ? 1 : (com.mparsict.vosolmaliat.domain.enums.TaxPeriod.of(period).number() - 1) * 3 + 1;
        return String.format(Locale.ROOT, "%04d/%02d/01", year, month);
    }
    public static String periodEnd(int year, Integer period) {
        int month = period == null ? 12 : com.mparsict.vosolmaliat.domain.enums.TaxPeriod.of(period).number() * 3;
        return String.format(Locale.ROOT, "%04d/%02d/%02d", year, month, monthLength(year, month));
    }
}
