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
    void taxExpertCanClaimCandidateTaskInsideOfficeScope() {
        CurrentUserContext user = user(Set.of("EXECUTION_OFFICER"), Set.of(), Set.of("TASK_CLAIM"), organization(10L));
        UserTask task = task(null, List.of("EXECUTION_OFFICER"), List.of());

        assertThat(service.canClaim(user, task, Map.of("generalDirectorateId", 10L))).isTrue();
    }

    @Test
    void userCanCompleteOnlyOwnAssignedTaskUnlessAdmin() {
        CurrentUserContext user = user(Set.of("EXECUTION_OFFICER"), Set.of(), Set.of("TASK_COMPLETE"), organization(10L));

        assertThat(service.canComplete(user, task("ali", List.of(), List.of()), Map.of("generalDirectorateId", 10L))).isTrue();
        assertThat(service.canComplete(user, task("sara", List.of(), List.of()), Map.of("generalDirectorateId", 10L))).isFalse();
    }

    @Test
    void adminCanAssignAndViewAnyTask() {
        CurrentUserContext admin = user(Set.of("ADMIN"), Set.of(), Set.of(), null);
        UserTask task = task("someone", List.of("OTHER"), List.of());

        assertThat(service.canAssign(admin)).isTrue();
        assertThat(service.canView(admin, task, Map.of("taxUnitId", 99L))).isTrue();
    }

    @Test
    void generalOfficeHeadOnlySeesTasksForOwnGeneralDirectorate() {
        CurrentUserContext tehranHead = user(
                BusinessRole.TAX_DIRECTORATE_DIRECTOR_GENERAL,
                Set.of("TAX_DIRECTORATE_DIRECTOR_GENERAL"),
                Set.of("TASK_CLAIM"),
                new MasterdataOrganizationResponse(700L, "Tehran", null, null));
        UserTask task = task(null, List.of("TAX_DIRECTORATE_DIRECTOR_GENERAL:700"), List.of());

        assertThat(service.canClaim(tehranHead, task, Map.of("generalDirectorateId", 700L))).isTrue();
        assertThat(service.canClaim(tehranHead, task, Map.of("generalDirectorateId", 701L))).isFalse();
    }

    @Test
    void executionUnitHeadCannotAccessAnotherDirectorate() {
        CurrentUserContext officeHead = user(
                BusinessRole.DIRECTORATE_EXECUTION_UNIT_HEAD,
                Set.of("DIRECTORATE_EXECUTION_UNIT_HEAD"),
                Set.of("TASK_CLAIM"),
                new MasterdataOrganizationResponse(700L, "Tehran", 70L, "Office 70"));
        UserTask task = task(null, List.of("DIRECTORATE_EXECUTION_UNIT_HEAD:700"), List.of());

        assertThat(service.canClaim(officeHead, task,
                Map.of("generalDirectorateId", 700L, "taxAffairsOfficeId", 70L))).isTrue();
        assertThat(service.canClaim(officeHead, task,
                Map.of("generalDirectorateId", 701L, "taxAffairsOfficeId", 70L))).isFalse();
    }

    @Test
    void identityMappingAddsUsersOrganizationScopedCandidateGroup() {
        CurrentUserContext user = user(
                BusinessRole.TAX_DIRECTORATE_DIRECTOR_GENERAL,
                Set.of("TAX_DIRECTORATE_DIRECTOR_GENERAL"),
                Set.of(),
                new MasterdataOrganizationResponse(700L, "Tehran", null, null));

        assertThat(new CamundaIdentityMappingService().toCamundaCandidateGroups(user))
                .contains("TAX_DIRECTORATE_DIRECTOR_GENERAL", "TAX_DIRECTORATE_DIRECTOR_GENERAL:700");
    }

    private CurrentUserContext user(Set<String> roles, Set<String> groups, Set<String> permissions, MasterdataOrganizationResponse organization) {
        return user(roles.contains("ADMIN") ? BusinessRole.ADMIN : BusinessRole.EXECUTION_OFFICER,
                roles, permissions, organization, groups);
    }

    private CurrentUserContext user(
            BusinessRole businessRole,
            Set<String> roles,
            Set<String> permissions,
            MasterdataOrganizationResponse organization) {
        return user(businessRole, roles, permissions, organization, Set.of());
    }

    private CurrentUserContext user(
            BusinessRole businessRole,
            Set<String> roles,
            Set<String> permissions,
            MasterdataOrganizationResponse organization,
            Set<String> groups) {
        return new CurrentUserContext(
                "kc-1",
                "ali",
                "ali@example.com",
                "Ali",
                "Ahmadi",
                businessRole,
                roles,
                groups,
                permissions,
                organization,
                true,
                false
        );
    }

    private MasterdataOrganizationResponse organization(Long id) {
        return new MasterdataOrganizationResponse(id, "Directorate " + id, null, null);
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
