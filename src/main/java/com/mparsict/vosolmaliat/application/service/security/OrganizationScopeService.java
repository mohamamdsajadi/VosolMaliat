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
        if (variables == null || variables.isEmpty() || user.organization() == null || user.businessRole() == null) {
            return false;
        }
        BusinessRole role = user.businessRole();
        return switch (role) {
            case GENERAL_OFFICE_HEAD -> matches(
                    user.organization().generalDirectorateId(),
                    firstPresent(variables, "generalDirectorateId", "mainTaxBureauId", "general_directorate_id", "main_tax_bureau_id"));
            case TAX_OFFICE_HEAD -> matches(
                    user.organization().taxAffairsOfficeId(),
                    firstPresent(variables, "taxAffairsOfficeId", "taxBureauId", "tax_affairs_office_id", "tax_bureau_id"));
            case TAX_EXPERT -> matches(
                    user.organization().taxAffairsOfficeId(),
                    firstPresent(variables, "taxAffairsOfficeId", "taxBureauId", "tax_affairs_office_id", "tax_bureau_id"));
            case ADMIN -> true;
        };
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
