package com.mparsict.vosolmaliat.application.dto.response;

import java.time.Instant;
import java.util.List;

public final class LegalRestrictionResponses {
    private LegalRestrictionResponses() {}
    public record Counts(long total, long pending, long confirmed, long failed) {}
    public record Row(Long id, String trackingCode, Long executionPaperId, String executionPaperCode,
                      String taxpayerName, String taxpayerNationalId, String directorateName,
                      String status, int subjectCount, Instant sentAt, Instant completedAt, boolean mock) {}
    public record Page(List<Row> content, long totalElements, int totalPages, int number, int size, Counts counts) {}
    public record Subject(String name, String identifier, String personType, String postTitle,
                          String startDate, String endDate, String representative) {}
    public record Restriction(String name, String identifier, String state, String stateTitle,
                              String trackingCode, Instant appliedAt, Instant releasedAt, String releaseTrackingCode) {}
    public record Event(Long id, String title, String actor, Instant occurredAt, String note) {}
    public record Detail(Row request, String debtPaperDate, String errorMessage,
                         List<Subject> subjects, List<Restriction> paperRestrictions, List<Event> history) {}
}
