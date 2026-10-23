package com.mparsict.vosolmaliat.application.service.security;

import com.mparsict.vosolmaliat.application.security.CurrentUserContext;
import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OrganizationScopeService {

    public boolean canAccessTaskVariables(CurrentUserContext user, Map<String, Object> variables) {
        if (user.isAdmin()) {
            return true;
        }
        if (user.businessRole() == null) return false;
        BusinessRole role = user.businessRole();
        if (role.isNationalScope()) return true;
        return variables != null && user.organization() != null && matches(user.organization().generalDirectorateId(),
                firstPresent(variables, "generalDirectorateId", "mainTaxBureauId", "general_directorate_id", "main_tax_bureau_id"));
    }

    private boolean matches(Long userOrganizationId, Object taskOrganizationId) {
        return userOrganizationId != null
                && taskOrganizationId != null
                && userOrganizationId.toString().equals(taskOrganizationId.toString());
    }

    private Object firstPresent(Map<String, Object> variables, String... keys) {
        for (String key : keys) {
            if (variables.containsKey(key)) {
                return variables.get(key);
            }
        }
        return null;
    }
}
