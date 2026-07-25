package com.mparsict.vosolmaliat.application.service.cartable;

import com.mparsict.vosolmaliat.application.security.CurrentUserContext;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.TreeSet;

@Service
public class CamundaIdentityMappingService {

    public String toCamundaAssignee(CurrentUserContext user) {
        return user.username();
    }

    public Set<String> toCamundaCandidateGroups(CurrentUserContext user) {
        Set<String> groups = new TreeSet<>(user.roles());
        groups.addAll(user.groups());
        String organizationGroup = OrganizationCandidateGroup.forUser(user.businessRole(), user.organization());
        if (organizationGroup != null) {
            groups.add(organizationGroup);
        }
        return groups;
    }
}
