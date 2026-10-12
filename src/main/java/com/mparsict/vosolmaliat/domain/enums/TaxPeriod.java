package com.mparsict.vosolmaliat.domain.enums;

public enum TaxPeriod implements LookupValue {
    SPRING(1, "بهار"), SUMMER(2, "تابستان"), AUTUMN(3, "پاییز"), WINTER(4, "زمستان");
    private final int number;
    private final String label;
    TaxPeriod(int number, String label) { this.number = number; this.label = label; }
    @Override public int id() { return number; }
    @Override public String label() { return label; }
    public int number() { return number; }
    public static TaxPeriod of(int number) {
        for (var period : values()) if (period.number == number) return period;
        throw new IllegalArgumentException("دوره مالیاتی باید بین ۱ تا ۴ باشد.");
    }
}
