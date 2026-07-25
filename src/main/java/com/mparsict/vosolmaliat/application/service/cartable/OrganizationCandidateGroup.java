package com.mparsict.vosolmaliat.application.service.cartable;

import com.mparsict.vosolmaliat.application.dto.response.security.MasterdataOrganizationResponse;
import com.mparsict.vosolmaliat.domain.enums.BusinessRole;

public final class OrganizationCandidateGroup {

    private OrganizationCandidateGroup() {
    }

    public static String forUser(BusinessRole role, MasterdataOrganizationResponse organization) {
        if (role == null || organization == null) {
            return null;
        }
        return switch (role) {
            case GENERAL_OFFICE_HEAD -> forOrganization(role, organization.generalDirectorateId());
            case TAX_OFFICE_HEAD -> forOrganization(role, organization.taxAffairsOfficeId());
            case TAX_UNIT_HEAD, TAX_EXPERT -> forOrganization(role, organization.taxUnitId());
            case ADMIN -> null;
        };
    }

    public static String forOrganization(BusinessRole role, Long organizationId) {
        return role == null || organizationId == null ? null : role.name() + ":" + organizationId;
    }
}
