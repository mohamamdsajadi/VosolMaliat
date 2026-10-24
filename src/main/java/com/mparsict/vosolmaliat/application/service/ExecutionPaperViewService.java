package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperPageResponse;
import com.mparsict.vosolmaliat.application.service.security.ExecutionPaperVisibility;
import com.mparsict.vosolmaliat.application.service.security.SecurityIdentityService;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service @RequiredArgsConstructor
public class ExecutionPaperViewService {
    private final ExecutionPaperViewRepository views;
    private final ExecutionPaperRepository papers;
    private final SecurityIdentityService identity;
    private final ExecutionPaperVisibility visibility;

    /** Enrich only rows already authorized and paged by the paper list. */
    @Transactional(readOnly = true)
    public ExecutionPaperPageResponse includeViews(ExecutionPaperPageResponse page) {
        var user = identity.currentUser();
        var ids = page.content().stream().map(paper -> paper.id()).toList();
        Map<Long, Instant> viewed = new LinkedHashMap<>();
        if (!ids.isEmpty()) views.findByUserKeyAndExecutionPaper_IdIn(user.keycloakUserId(), ids)
                .forEach(view -> viewed.put(view.getExecutionPaper().getId(), Instant.ofEpochMilli(view.getLastViewedAtMillis())));
        return new ExecutionPaperPageResponse(page.content(), page.pageNumber(), page.pageSize(),
                page.totalElements(), page.totalPages(), viewed);
    }

    @Transactional
    public Instant markViewed(Long paperId) {
        var user = identity.currentUser();
        if (!user.hasPermission("REPORT_VIEW") && !user.hasPermission("EXECUTION_PAPER_DETAIL_VIEW"))
            throw new AccessDeniedException("مجوز مشاهده جزئیات برگ اجرا فعال نیست.");
        visibility.requireVisible(paperId);
        if (!papers.existsById(paperId)) throw new ResponseStatusException(NOT_FOUND, "برگ اجرا یافت نشد.");
        views.recordView(user.keycloakUserId(), paperId, Instant.now().toEpochMilli());
        return Instant.ofEpochMilli(views.findByUserKeyAndExecutionPaper_Id(user.keycloakUserId(), paperId)
                .orElseThrow().getLastViewedAtMillis());
    }
}
