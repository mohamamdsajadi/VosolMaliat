package com.mparsict.vosolmaliat.application.service.cartable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.cartable.AssignTaskRequest;
import com.mparsict.vosolmaliat.application.dto.request.cartable.ClaimTaskRequest;
import com.mparsict.vosolmaliat.application.dto.request.cartable.CompleteTaskRequest;
import com.mparsict.vosolmaliat.application.dto.request.cartable.DelegateTaskRequest;
import com.mparsict.vosolmaliat.application.dto.response.cartable.CartableTaskResponse;
import com.mparsict.vosolmaliat.application.dto.response.cartable.DelegationTargetResponse;
import com.mparsict.vosolmaliat.application.dto.response.cartable.TaskVariableResponse;
import com.mparsict.vosolmaliat.application.security.CurrentUserContext;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.application.service.security.SecurityIdentityService;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import com.mparsict.vosolmaliat.domain.model.security.AppUserProfile;
import com.mparsict.vosolmaliat.domain.repository.security.AppUserProfileRepository;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.enums.UserTaskState;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.client.api.search.response.Variable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartableService {

    private final CamundaClient camundaClient;
    private final SecurityIdentityService securityIdentityService;
    private final CamundaIdentityMappingService identityMappingService;
    private final TaskAuthorizationService taskAuthorizationService;
    private final AuditLogService auditLogService;
    private final AppUserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;

    public List<CartableTaskResponse> myTasks() {
        CurrentUserContext user = securityIdentityService.currentUser();
        if (user.isAdmin()) {
            List<UserTask> tasks = searchTasks(filter -> filter.state(UserTaskState.CREATED));
            return toAuthorizedResponses(user, tasks);
        }
        Map<Long, UserTask> tasks = new LinkedHashMap<>();
        searchTasks(filter -> filter
                .state(UserTaskState.CREATED)
                .assignee(identityMappingService.toCamundaAssignee(user)))
                .forEach(task -> tasks.put(task.getUserTaskKey(), task));
        candidateTasksFor(user).forEach((key, task) -> tasks.putIfAbsent(key, task));
        return toAuthorizedResponses(user, new ArrayList<>(tasks.values()));
    }

    public List<CartableTaskResponse> candidateTasks() {
        CurrentUserContext user = securityIdentityService.currentUser();
        return toAuthorizedResponses(user, new ArrayList<>(candidateTasksFor(user).values()));
    }

    private Map<Long, UserTask> candidateTasksFor(CurrentUserContext user) {
        Map<Long, UserTask> tasks = new LinkedHashMap<>();
        for (String group : identityMappingService.toCamundaCandidateGroups(user)) {
            searchTasks(filter -> filter.state(UserTaskState.CREATED).candidateGroup(group))
                    .forEach(task -> tasks.put(task.getUserTaskKey(), task));
        }
        searchTasks(filter -> filter.state(UserTaskState.CREATED).candidateUser(user.username()))
                .forEach(task -> tasks.put(task.getUserTaskKey(), task));
        return tasks;
    }

    public CartableTaskResponse getTask(Long taskKey) {
        CurrentUserContext user = securityIdentityService.currentUser();
        UserTask task = fetchTask(taskKey);
        Map<String, Object> variables = fetchVariablesMap(taskKey);
        if (!taskAuthorizationService.canView(user, task, variables)) {
            throw new BusinessException("UNAUTHORIZED_TASK_ACCESS", "دسترسی به این کار مجاز نیست");
        }
        return toResponse(user, task, variables);
    }

    public CartableTaskResponse claim(Long taskKey, ClaimTaskRequest request) {
        CurrentUserContext user = securityIdentityService.currentUser();
        UserTask task = fetchTask(taskKey);
        Map<String, Object> variables = fetchVariablesMap(taskKey);
        if (!taskAuthorizationService.canClaim(user, task, variables)) {
            throw new BusinessException("TASK_CLAIM_NOT_ALLOWED", "امکان دریافت این کار وجود ندارد");
        }
        String assignee = request.assignee() == null || request.assignee().isBlank()
                ? identityMappingService.toCamundaAssignee(user)
                : request.assignee();
        try {
            camundaClient.newAssignUserTaskCommand(taskKey)
                    .assignee(assignee)
                    .allowOverride(false)
                    .action("claim")
                    .requestTimeout(Duration.ofSeconds(20))
                    .send()
                    .join();
            auditLogService.record(user, "TASK_CLAIMED", "CAMUNDA_USER_TASK", taskKey.toString(), null, assignee);
            return getTask(taskKey);
        } catch (Exception ex) {
            throw new BusinessException("TASK_CLAIM_FAILED", "دریافت کار ناموفق بود", ex);
        }
    }

    public CartableTaskResponse unclaim(Long taskKey) {
        CurrentUserContext user = securityIdentityService.currentUser();
        UserTask task = fetchTask(taskKey);
        Map<String, Object> variables = fetchVariablesMap(taskKey);
        if (!taskAuthorizationService.canUnclaim(user, task, variables)) {
            throw new BusinessException("TASK_UNCLAIM_NOT_ALLOWED", "امکان آزادسازی این کار وجود ندارد");
        }
        try {
            camundaClient.newUnassignUserTaskCommand(taskKey)
                    .requestTimeout(Duration.ofSeconds(20))
                    .send()
                    .join();
            auditLogService.record(user, "TASK_UNCLAIMED", "CAMUNDA_USER_TASK", taskKey.toString(), task.getAssignee(), null);
            return getTask(taskKey);
        } catch (Exception ex) {
            throw new BusinessException("TASK_UNCLAIM_FAILED", "آزادسازی کار ناموفق بود", ex);
        }
    }

    public void complete(Long taskKey, CompleteTaskRequest request) {
        CurrentUserContext user = securityIdentityService.currentUser();
        UserTask task = fetchTask(taskKey);
        Map<String, Object> variables = fetchVariablesMap(taskKey);
        if (!taskAuthorizationService.canComplete(user, task, variables)) {
            throw new BusinessException("TASK_COMPLETE_NOT_ALLOWED", "امکان تکمیل این کار وجود ندارد");
        }
        try {
            camundaClient.newCompleteUserTaskCommand(taskKey)
                    .variables(request.variables() == null ? Map.of() : request.variables())
                    .action("complete")
                    .requestTimeout(Duration.ofSeconds(30))
                    .send()
                    .join();
            auditLogService.record(user, "TASK_COMPLETED", "CAMUNDA_USER_TASK", taskKey.toString(), null, "completed");
        } catch (Exception ex) {
            throw new BusinessException("TASK_COMPLETION_FAILED", "تکمیل کار ناموفق بود", ex);
        }
    }

    public CartableTaskResponse assign(Long taskKey, AssignTaskRequest request) {
        CurrentUserContext user = securityIdentityService.currentUser();
        UserTask task = fetchTask(taskKey);
        if (!taskAuthorizationService.canAssign(user)) {
            throw new BusinessException("TASK_ASSIGN_NOT_ALLOWED", "امکان ارجاع این کار وجود ندارد");
        }
        try {
            camundaClient.newAssignUserTaskCommand(taskKey)
                    .assignee(request.assignee())
                    .allowOverride(true)
                    .action("assign")
                    .requestTimeout(Duration.ofSeconds(20))
                    .send()
                    .join();
            auditLogService.record(user, "TASK_ASSIGNED", "CAMUNDA_USER_TASK", taskKey.toString(), task.getAssignee(), request.assignee());
            return getTask(taskKey);
        } catch (Exception ex) {
            throw new BusinessException("TASK_ASSIGN_FAILED", "ارجاع کار ناموفق بود", ex);
        }
    }

    public List<DelegationTargetResponse> delegationTargets(Long taskKey) {
        CurrentUserContext user = securityIdentityService.currentUser();
        UserTask task = fetchTask(taskKey);
        Map<String, Object> variables = fetchVariablesMap(taskKey);
        validateCanDelegate(user, task, variables);
        BusinessRole nextRole = user.businessRole().nextDelegationRole();
        return userProfileRepository.findByBusinessRole(nextRole).stream()
                .filter(profile -> Boolean.TRUE.equals(profile.getEnabledInBusinessSystem()))
                .filter(profile -> !Boolean.TRUE.equals(profile.getLockedInBusinessSystem()))
                .filter(profile -> isInDelegationScope(user, profile))
                .map(profile -> new DelegationTargetResponse(
                        profile.getId(),
                        profile.getUsername(),
                        fullName(profile),
                        profile.getBusinessRole().name()
                ))
                .toList();
    }

    public CartableTaskResponse delegate(Long taskKey, DelegateTaskRequest request) {
        CurrentUserContext user = securityIdentityService.currentUser();
        UserTask task = fetchTask(taskKey);
        Map<String, Object> variables = fetchVariablesMap(taskKey);
        validateCanDelegate(user, task, variables);
        BusinessRole nextRole = user.businessRole().nextDelegationRole();
        String targetAssignee = request == null ? null : blankToNull(request.assignee());
        String candidateGroup = request == null ? null : blankToNull(request.candidateGroup());
        try {
            if (targetAssignee != null) {
                AppUserProfile target = userProfileRepository.findByUsername(targetAssignee)
                        .orElseThrow(() -> new BusinessException("DELEGATION_TARGET_NOT_FOUND", "کاربر مقصد ارجاع یافت نشد"));
                if (target.getBusinessRole() != nextRole || !isInDelegationScope(user, target)) {
                    throw new BusinessException("DELEGATION_TARGET_NOT_ALLOWED", "کاربر مقصد در نقش یا محدوده سازمانی مجاز نیست");
                }
                camundaClient.newAssignUserTaskCommand(taskKey)
                        .assignee(target.getUsername())
                        .allowOverride(true)
                        .action("delegate")
                        .requestTimeout(Duration.ofSeconds(20))
                        .send()
                        .join();
                auditLogService.record(user, "TASK_DELEGATED_TO_USER", "CAMUNDA_USER_TASK", taskKey.toString(), task.getAssignee(), target.getUsername());
            } else {
                String group = candidateGroup == null ? nextRole.name() : candidateGroup;
                if (!group.equals(nextRole.name())) {
                    throw new BusinessException("DELEGATION_GROUP_NOT_ALLOWED", "گروه مقصد ارجاع باید نقش مستقیم بعدی باشد");
                }
                if (!isBlank(task.getAssignee())) {
                    camundaClient.newUnassignUserTaskCommand(taskKey)
                            .requestTimeout(Duration.ofSeconds(20))
                            .send()
                            .join();
                }
                camundaClient.newUpdateUserTaskCommand(taskKey)
                        .candidateGroups(group)
                        .clearCandidateUsers()
                        .action("delegate")
                        .requestTimeout(Duration.ofSeconds(20))
                        .send()
                        .join();
                auditLogService.record(user, "TASK_DELEGATED_TO_GROUP", "CAMUNDA_USER_TASK", taskKey.toString(), task.getAssignee(), group);
            }
            return getTask(taskKey);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("TASK_DELEGATION_FAILED", "ارجاع کار ناموفق بود", ex);
        }
    }

    private void validateCanDelegate(CurrentUserContext user, UserTask task, Map<String, Object> variables) {
        if (!taskAuthorizationService.canDelegate(user, task, variables)) {
            throw new BusinessException("TASK_DELEGATION_NOT_ALLOWED", "امکان ارجاع این کار برای شما وجود ندارد");
        }
    }

    private boolean isInDelegationScope(CurrentUserContext actor, AppUserProfile target) {
        if (actor.businessRole() == BusinessRole.GENERAL_OFFICE_HEAD) {
            return actor.organization() != null
                    && actor.organization().generalDirectorateId() != null
                    && Objects.equals(actor.organization().generalDirectorateId(), target.getGeneralDirectorateId());
        }
        if (actor.businessRole() == BusinessRole.TAX_OFFICE_HEAD) {
            return actor.organization() != null
                    && actor.organization().taxAffairsOfficeId() != null
                    && Objects.equals(actor.organization().taxAffairsOfficeId(), target.getTaxAffairsOfficeId());
        }
        if (actor.businessRole() == BusinessRole.TAX_UNIT_HEAD) {
            return actor.organization() != null
                    && actor.organization().taxUnitId() != null
                    && target.getTaxUnit() != null
                    && Objects.equals(actor.organization().taxUnitId(), target.getTaxUnit().getId());
        }
        return false;
    }

    private String fullName(AppUserProfile profile) {
        String value = ((profile.getFirstName() == null ? "" : profile.getFirstName()) + " "
                + (profile.getLastName() == null ? "" : profile.getLastName())).trim();
        return value.isBlank() ? profile.getUsername() : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<UserTask> searchTasks(Consumer<io.camunda.client.api.search.filter.UserTaskFilter> filter) {
        return camundaClient.newUserTaskSearchRequest()
                .filter(filter)
                .sort(sort -> sort.creationDate().desc())
                .page(page -> page.from(0).limit(100))
                .requestTimeout(Duration.ofSeconds(20))
                .send()
                .join()
                .items();
    }

    private UserTask fetchTask(Long taskKey) {
        try {
            return camundaClient.newUserTaskGetRequest(taskKey)
                    .requestTimeout(Duration.ofSeconds(20))
                    .send()
                    .join();
        } catch (Exception ex) {
            throw new BusinessException("CAMUNDA_TASK_NOT_FOUND", "کار فعال در Camunda یافت نشد", ex);
        }
    }

    private List<CartableTaskResponse> toAuthorizedResponses(CurrentUserContext user, List<UserTask> tasks) {
        return tasks.stream()
                .map(task -> Map.entry(task, fetchVariablesMap(task.getUserTaskKey())))
                .filter(entry -> taskAuthorizationService.canView(user, entry.getKey(), entry.getValue()))
                .map(entry -> toResponse(user, entry.getKey(), entry.getValue()))
                .toList();
    }

    private CartableTaskResponse toResponse(CurrentUserContext user, UserTask task, Map<String, Object> variables) {
        return new CartableTaskResponse(
                task.getUserTaskKey(),
                task.getName(),
                task.getProcessInstanceKey(),
                task.getProcessDefinitionKey(),
                task.getBpmnProcessId(),
                task.getElementId(),
                task.getAssignee(),
                safeList(task.getCandidateUsers()),
                safeList(task.getCandidateGroups()),
                task.getCreationDate(),
                task.getDueDate(),
                task.getFollowUpDate(),
                task.getFormKey(),
                task.getExternalFormReference(),
                variables.entrySet().stream()
                        .map(entry -> new TaskVariableResponse(entry.getKey(), entry.getValue(), false))
                        .toList(),
                taskAuthorizationService.allowedActions(user, task, variables)
        );
    }

    private Map<String, Object> fetchVariablesMap(Long taskKey) {
        try {
            List<Variable> variables = camundaClient.newUserTaskVariableSearchRequest(taskKey)
                    .withFullValues()
                    .page(page -> page.from(0).limit(100))
                    .requestTimeout(Duration.ofSeconds(20))
                    .send()
                    .join()
                    .items();
            return variables.stream().collect(Collectors.toMap(Variable::getName, this::parseVariableValue, (a, b) -> b, LinkedHashMap::new));
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private Object parseVariableValue(Variable variable) {
        String value = variable.getValue();
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (JsonProcessingException ex) {
            return value;
        }
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
