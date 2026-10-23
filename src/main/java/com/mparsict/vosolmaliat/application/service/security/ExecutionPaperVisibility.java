package com.mparsict.vosolmaliat.application.service.security;

import com.mparsict.vosolmaliat.application.security.CurrentUserContext;
import com.mparsict.vosolmaliat.domain.repository.ArrestOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import java.util.List;
import static com.mparsict.vosolmaliat.domain.enums.BusinessRole.*;

/** Shared read boundary for lists, links and paper subresources. */
@Service @RequiredArgsConstructor
public class ExecutionPaperVisibility {
    private final SecurityIdentityService identity;
    private final ArrestOrderRepository orders;

    public boolean assignedOnly(CurrentUserContext user) {
        return !user.isAdmin() && (user.businessRole() == DIRECTORATE_EXECUTION_TEAM_SUPERVISOR
                || user.businessRole() == EXECUTION_OFFICER);
    }

    public List<Long> paperIds(CurrentUserContext user) {
        return orders.findAssignedPaperIds(user.keycloakUserId(), user.businessRole() == DIRECTORATE_EXECUTION_TEAM_SUPERVISOR);
    }

    /** Null means an internal call, or a role which is not assignment restricted. */
    public List<Long> restrictedPaperIds() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) return null;
        var user = identity.currentUser();
        return assignedOnly(user) ? paperIds(user) : null;
    }

    public void requireVisible(Long paperId) {
        var ids = restrictedPaperIds();
        if (ids != null && !ids.contains(paperId)) throw new AccessDeniedException("این برگ اجرا به شما ارجاع نشده است.");
    }
}
