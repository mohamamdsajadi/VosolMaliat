package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.response.DashboardStatsResponse;
import com.mparsict.vosolmaliat.application.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import com.mparsict.vosolmaliat.application.service.arrest.ArrestOrderService;
import com.mparsict.vosolmaliat.application.dto.response.ArrestOrderResponses;

@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final com.mparsict.vosolmaliat.application.service.DashboardReportingService reporting;
    private final ArrestOrderService arrestOrders;

    @GetMapping("/overview")
    public com.mparsict.vosolmaliat.application.dto.response.DashboardOverviewResponse overview() {
        return reporting.overview();
    }

    @GetMapping("/details")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('REPORT_VIEW','EXECUTION_PAPER_LIST_VIEW')")
    public com.mparsict.vosolmaliat.application.dto.response.DashboardOverviewResponse.Details details(
            @RequestParam String group, @RequestParam String key,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return reporting.details(group, key, page, size);
    }

    @GetMapping("/pending-arrest-papers")
    public ArrestOrderResponses.Page<ArrestOrderResponses.PendingPaper> pendingArrestPapers(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
        return arrestOrders.pendingPapers(page, size);
    }

    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {
        log.debug("GET /api/v1/dashboard/stats");
        return dashboardService.getStats();
    }
}
