package com.mparsict.vosolmaliat.application.service.cartable;

import com.mparsict.vosolmaliat.application.dto.response.cartable.AllowedTaskActionsResponse;
import com.mparsict.vosolmaliat.application.security.CurrentUserContext;
import com.mparsict.vosolmaliat.application.service.security.OrganizationScopeService;
import io.camunda.client.api.search.response.UserTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskAuthorizationService {

    private final CamundaIdentityMappingService identityMappingService;
    private final OrganizationScopeService organizationScopeService;

    public boolean canView(CurrentUserContext user, UserTask task, Map<String, Object> variables) {
        return user.isAdmin()
                || (organizationScopeService.canAccessTaskVariables(user, variables)
                && (isAssignedTo(user, task)
                || (isBlank(task.getAssignee()) && isCandidateFor(user, task))
                || user.hasPermission("TASK_VIEW_ORG_SCOPE")));
    }

    public boolean canClaim(CurrentUserContext user, UserTask task, Map<String, Object> variables) {
        return !user.isAdmin()
                && (user.hasPermission("TASK_CLAIM")
                && isBlank(task.getAssignee())
                && isCandidateFor(user, task)
                && organizationScopeService.canAccessTaskVariables(user, variables));
    }

    public boolean canUnclaim(CurrentUserContext user, UserTask task, Map<String, Object> variables) {
        return !user.isAdmin()
                && user.hasPermission("TASK_UNCLAIM")
                && isAssignedTo(user, task)
                && organizationScopeService.canAccessTaskVariables(user, variables);
    }

    public boolean canComplete(CurrentUserContext user, UserTask task, Map<String, Object> variables) {
        return !user.isAdmin()
                && user.hasPermission("TASK_COMPLETE")
                && isAssignedTo(user, task)
                && organizationScopeService.canAccessTaskVariables(user, variables);
    }

    public boolean canAssign(CurrentUserContext user) {
        return user.isAdmin() || user.hasPermission("TASK_ASSIGN");
    }

    public boolean canDelegate(CurrentUserContext user, UserTask task, Map<String, Object> variables) {
        return !user.isAdmin()
                && user.hasPermission("TASK_DELEGATE")
                && isAssignedTo(user, task)
                && user.businessRole() != null
                && user.businessRole().nextDelegationRole() != null
                && organizationScopeService.canAccessTaskVariables(user, variables);
    }

    public AllowedTaskActionsResponse allowedActions(CurrentUserContext user, UserTask task, Map<String, Object> variables) {
        return new AllowedTaskActionsResponse(
                canClaim(user, task, variables),
                canUnclaim(user, task, variables),
                canComplete(user, task, variables),
                canAssign(user),
                canDelegate(user, task, variables),
                canView(user, task, variables)
        );
    }

    private boolean isAssignedTo(CurrentUserContext user, UserTask task) {
        return identityMappingService.toCamundaAssignee(user).equals(task.getAssignee());
    }

    private boolean isCandidateFor(CurrentUserContext user, UserTask task) {
        return intersects(identityMappingService.toCamundaCandidateGroups(user), task.getCandidateGroups())
                || contains(task.getCandidateUsers(), user.username());
    }

    private boolean intersects(Collection<String> first, Collection<String> second) {
        if (first == null || second == null) {
            return false;
        }
        return first.stream().anyMatch(second::contains);
    }

    private boolean contains(Collection<String> values, String value) {
        return values != null && values.contains(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
