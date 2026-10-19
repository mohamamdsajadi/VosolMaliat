package com.mparsict.vosolmaliat.application.service.cartable;

import com.mparsict.vosolmaliat.application.dto.request.cartable.*;
import com.mparsict.vosolmaliat.application.dto.response.cartable.CartableTaskResponse;
import com.mparsict.vosolmaliat.application.dto.response.cartable.DelegationTargetResponse;
import com.mparsict.vosolmaliat.application.service.security.SecurityIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

/** Empty offline task store. No fake BPMN execution or successful workflow writes. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "camunda.client.enabled", havingValue = "false")
public class OfflineCartableService implements CartableOperations {
    private final SecurityIdentityService identity;

    @Override
    public List<CartableTaskResponse> myTasks(CartableTaskSearchCriteria criteria) {
        identity.currentUser();
        return List.of();
    }

    @Override
    public List<CartableTaskResponse> candidateTasks() {
        identity.currentUser();
        return List.of();
    }

    @Override
    public CartableTaskResponse getTask(Long taskKey) {
        throw unavailable();
    }

    @Override
    public CartableTaskResponse claim(Long taskKey, ClaimTaskRequest request) {
        throw unavailable();
    }

    @Override
    public CartableTaskResponse unclaim(Long taskKey) {
        throw unavailable();
    }

    @Override
    public void complete(Long taskKey, CompleteTaskRequest request) {
        throw unavailable();
    }

    @Override
    public CartableTaskResponse assign(Long taskKey, AssignTaskRequest request) {
        throw unavailable();
    }

    @Override
    public List<DelegationTargetResponse> delegationTargets(Long taskKey) {
        throw unavailable();
    }

    @Override
    public void delegate(Long taskKey, DelegateTaskRequest request) {
        throw unavailable();
    }

    private ResponseStatusException unavailable() {
        identity.currentUser();
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Camunda در حالت آزمایشی غیرفعال است؛ کارتابل خالی است و اجرای فرایند پشتیبانی نمی‌شود.");
    }
}
