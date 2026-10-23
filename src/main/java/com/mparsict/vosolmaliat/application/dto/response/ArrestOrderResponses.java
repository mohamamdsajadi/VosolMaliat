package com.mparsict.vosolmaliat.application.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ArrestOrderResponses {
    private ArrestOrderResponses() {}
    public record User(Long id, String username, String fullName, String role, String roleTitle, Long generalDirectorateId) {}
    public record Row(Long id, Long version, Long executionPaperId, String executionPaperCode,
                      String taxpayerName, String taxpayerNationalId, String debtAmount,
                      String stage, String issuanceStatus, Instant referredAt, String referralDestination,
                      User assignee, String candidateRole, Instant startedAt, Instant issuedAt, String letterNumber,
                      String engine, List<String> allowedActions, String route) {}
    public record Event(Long id, String action, String actorName, String destination, Instant occurredAt, String note) {}
    public record Detail(Row order, Map<String, Object> letter, List<Event> history) {}
    public record Candidate(ExecutionPaperDto paper, String generalDirectorate, Instant paymentDeadline) {}
    public record Page<T>(List<T> content, long totalElements, int totalPages, int number, int size) {}
    public record StartResult(Long executionPaperId, boolean success, Row order, String message) {}
    public record Batch(List<StartResult> results, long succeeded, long failed) {}
}
