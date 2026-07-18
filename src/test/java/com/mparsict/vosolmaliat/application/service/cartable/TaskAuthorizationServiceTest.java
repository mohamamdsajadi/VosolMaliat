package com.mparsict.vosolmaliat.application.service.cartable;

import com.mparsict.vosolmaliat.application.dto.response.security.MasterdataOrganizationResponse;
import com.mparsict.vosolmaliat.application.security.CurrentUserContext;
import com.mparsict.vosolmaliat.application.service.security.OrganizationScopeService;
import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import io.camunda.client.api.search.enums.UserTaskState;
import io.camunda.client.api.search.response.UserTask;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TaskAuthorizationServiceTest {

    private final TaskAuthorizationService service = new TaskAuthorizationService(
            new CamundaIdentityMappingService(),
            new OrganizationScopeService()
    );

    @Test
    void taxUnitUserCanClaimCandidateTaskInsideScope() {
        CurrentUserContext user = user(Set.of("TAX_UNIT_USER"), Set.of(), Set.of("TASK_CLAIM"), organization(10L));
        UserTask task = task(null, List.of("TAX_UNIT_USER"), List.of());

        assertThat(service.canClaim(user, task, Map.of("taxUnitId", 10L))).isTrue();
    }

    @Test
    void userCanCompleteOnlyOwnAssignedTaskUnlessAdmin() {
        CurrentUserContext user = user(Set.of("TAX_UNIT_USER"), Set.of(), Set.of("TASK_COMPLETE"), organization(10L));

        assertThat(service.canComplete(user, task("ali", List.of(), List.of()), Map.of("taxUnitId", 10L))).isTrue();
        assertThat(service.canComplete(user, task("sara", List.of(), List.of()), Map.of("taxUnitId", 10L))).isFalse();
    }

    @Test
    void adminCanAssignAndViewAnyTask() {
        CurrentUserContext admin = user(Set.of("ADMIN"), Set.of(), Set.of(), null);
        UserTask task = task("someone", List.of("OTHER"), List.of());

        assertThat(service.canAssign(admin)).isTrue();
        assertThat(service.canView(admin, task, Map.of("taxUnitId", 99L))).isTrue();
    }

    private CurrentUserContext user(Set<String> roles, Set<String> groups, Set<String> permissions, MasterdataOrganizationResponse organization) {
        return new CurrentUserContext(
                "kc-1",
                "ali",
                "ali@example.com",
                "Ali",
                "Ahmadi",
                roles.contains("ADMIN") ? BusinessRole.ADMIN : BusinessRole.TAX_EXPERT,
                roles,
                groups,
                permissions,
                organization,
                true,
                false
        );
    }

    private MasterdataOrganizationResponse organization(Long id) {
        return new MasterdataOrganizationResponse(null, null, null, null, id, "Unit " + id);
    }

    private UserTask task(String assignee, List<String> candidateGroups, List<String> candidateUsers) {
        return new TestUserTask(assignee, candidateGroups, candidateUsers);
    }

    private record TestUserTask(String assignee, List<String> candidateGroups, List<String> candidateUsers) implements UserTask {
        @Override public Long getUserTaskKey() { return 1L; }
        @Override public String getName() { return "Task"; }
        @Override public UserTaskState getState() { return UserTaskState.CREATED; }
        @Override public String getAssignee() { return assignee; }
        @Override public String getElementId() { return "task"; }
        @Override public Long getElementInstanceKey() { return 2L; }
        @Override public List<String> getCandidateGroups() { return candidateGroups; }
        @Override public List<String> getCandidateUsers() { return candidateUsers; }
        @Override public String getBpmnProcessId() { return "process"; }
        @Override public Long getProcessDefinitionKey() { return 3L; }
        @Override public Long getProcessInstanceKey() { return 4L; }
        @Override public Long getFormKey() { return null; }
        @Override public OffsetDateTime getCreationDate() { return null; }
        @Override public OffsetDateTime getCompletionDate() { return null; }
        @Override public OffsetDateTime getFollowUpDate() { return null; }
        @Override public OffsetDateTime getDueDate() { return null; }
        @Override public String getTenantId() { return null; }
        @Override public String getExternalFormReference() { return null; }
        @Override public Integer getProcessDefinitionVersion() { return 1; }
        @Override public Map<String, String> getCustomHeaders() { return Map.of(); }
        @Override public Integer getPriority() { return 50; }
    }
}
