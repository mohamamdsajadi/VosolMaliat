package com.mparsict.vosolmaliat.presentation.api.cartable;

import com.mparsict.vosolmaliat.application.dto.request.cartable.AssignTaskRequest;
import com.mparsict.vosolmaliat.application.dto.request.cartable.CartableTaskSearchCriteria;
import com.mparsict.vosolmaliat.application.dto.request.cartable.ClaimTaskRequest;
import com.mparsict.vosolmaliat.application.dto.request.cartable.CompleteTaskRequest;
import com.mparsict.vosolmaliat.application.dto.request.cartable.DelegateTaskRequest;
import com.mparsict.vosolmaliat.application.dto.response.cartable.CartableTaskResponse;
import com.mparsict.vosolmaliat.application.dto.response.cartable.DelegationTargetResponse;
import com.mparsict.vosolmaliat.application.service.cartable.CartableOperations;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/cartable")
@RequiredArgsConstructor
public class CartableController {

    private final CartableOperations cartableService;

    @GetMapping("/my-tasks")
    @PreAuthorize("hasAuthority('TASK_VIEW_OWN')")
    public List<CartableTaskResponse> myTasks(
            @RequestParam(required = false) String executionPaper,
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String candidateGroup,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo) {
        log.debug("GET /api/v1/cartable/my-tasks");
        return cartableService.myTasks(new CartableTaskSearchCriteria(
                executionPaper,
                taskName,
                assignee,
                candidateGroup,
                createdFrom,
                createdTo
        ));
    }

    @GetMapping("/candidate-tasks")
    @PreAuthorize("hasAuthority('TASK_VIEW_CANDIDATE')")
    public List<CartableTaskResponse> candidateTasks() {
        log.debug("GET /api/v1/cartable/candidate-tasks");
        return cartableService.candidateTasks();
    }

    @GetMapping("/tasks/{taskKey}")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW_OWN','TASK_VIEW_CANDIDATE','TASK_VIEW_ORG_SCOPE')")
    public CartableTaskResponse getTask(@PathVariable Long taskKey) {
        log.debug("GET /api/v1/cartable/tasks/{}", taskKey);
        return cartableService.getTask(taskKey);
    }

    @PostMapping("/tasks/{taskKey}/claim")
    @PreAuthorize("hasAuthority('TASK_CLAIM')")
    public CartableTaskResponse claim(@PathVariable Long taskKey, @RequestBody(required = false) ClaimTaskRequest request) {
        log.debug("POST /api/v1/cartable/tasks/{}/claim", taskKey);
        return cartableService.claim(taskKey, request == null ? new ClaimTaskRequest(null) : request);
    }

    @PostMapping("/tasks/{taskKey}/unclaim")
    @PreAuthorize("hasAuthority('TASK_UNCLAIM')")
    public CartableTaskResponse unclaim(@PathVariable Long taskKey) {
        log.debug("POST /api/v1/cartable/tasks/{}/unclaim", taskKey);
        return cartableService.unclaim(taskKey);
    }

    @PostMapping("/tasks/{taskKey}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TASK_COMPLETE')")
    public void complete(@PathVariable Long taskKey, @RequestBody(required = false) CompleteTaskRequest request) {
        log.debug("POST /api/v1/cartable/tasks/{}/complete", taskKey);
        cartableService.complete(taskKey, request == null ? new CompleteTaskRequest(null) : request);
    }

    @PostMapping("/tasks/{taskKey}/assign")
    @PreAuthorize("hasAuthority('TASK_ASSIGN')")
    public CartableTaskResponse assign(@PathVariable Long taskKey, @RequestBody @Valid AssignTaskRequest request) {
        log.debug("POST /api/v1/cartable/tasks/{}/assign", taskKey);
        return cartableService.assign(taskKey, request);
    }

    @GetMapping("/tasks/{taskKey}/delegation-targets")
    @PreAuthorize("hasAuthority('TASK_DELEGATE')")
    public List<DelegationTargetResponse> delegationTargets(@PathVariable Long taskKey) {
        log.debug("GET /api/v1/cartable/tasks/{}/delegation-targets", taskKey);
        return cartableService.delegationTargets(taskKey);
    }

    @PostMapping("/tasks/{taskKey}/delegate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TASK_DELEGATE')")
    public void delegate(@PathVariable Long taskKey, @RequestBody(required = false) DelegateTaskRequest request) {
        log.debug("POST /api/v1/cartable/tasks/{}/delegate", taskKey);
        cartableService.delegate(taskKey, request == null ? new DelegateTaskRequest(null, null) : request);
    }
}
