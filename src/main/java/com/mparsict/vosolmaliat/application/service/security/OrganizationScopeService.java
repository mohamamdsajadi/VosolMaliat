package com.mparsict.vosolmaliat.application.service.security;

import com.mparsict.vosolmaliat.application.security.CurrentUserContext;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OrganizationScopeService {

    public boolean canAccessTaskVariables(CurrentUserContext user, Map<String, Object> variables) {
        if (user.isAdmin() || variables == null || variables.isEmpty() || user.organizationUnit() == null) {
            return true;
        }
        Object organizationUnitId = firstPresent(variables, "organizationUnitId", "taxUnitId", "tax_unit_id");
        if (organizationUnitId == null) {
            return true;
        }
        return user.organizationUnit().id().toString().equals(organizationUnitId.toString());
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
