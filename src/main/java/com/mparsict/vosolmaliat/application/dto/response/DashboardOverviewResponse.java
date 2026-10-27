package com.mparsict.vosolmaliat.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** Monetary strings preserve exact decimals through the browser. */
public record DashboardOverviewResponse(
        String scopeLabel, long taxpayers, long papers, Balances balances,
        List<Bucket> states, List<Bucket> notifications, List<Bucket> offices,
        List<Bucket> enforcement, boolean canViewDetails
) {
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
    public record Balances(String liability, String penalties, String credits,
                           String paid, String remissions, String remaining) {}
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
    public record Bucket(String key, String label, Long count, String amount, String paid, boolean available) {}
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
    public record Detail(Long id, String uniqueCode, LocalDateTime issuanceDate,
                         String taxpayerCode, String taxpayerName, String nationalId,
                         String stateLabel, String officeName, String remaining, String contribution) {}
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
    public record Details(List<Detail> content, long totalElements, int totalPages,
                          int page, int size, String amount) {}
}
