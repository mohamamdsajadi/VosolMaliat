package com.mparsict.vosolmaliat.domain.enums;

public enum TaxPeriod {
    SPRING(1), SUMMER(2), AUTUMN(3), WINTER(4);
    private final int number;
    TaxPeriod(int number) { this.number = number; }
    public int number() { return number; }
    public static TaxPeriod of(int number) {
        for (var period : values()) if (period.number == number) return period;
        throw new IllegalArgumentException("دوره مالیاتی باید بین ۱ تا ۴ باشد.");
    }
}
