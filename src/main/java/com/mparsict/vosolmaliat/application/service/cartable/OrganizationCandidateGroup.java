package com.mparsict.vosolmaliat.application.service.cartable;

import com.mparsict.vosolmaliat.application.dto.response.security.MasterdataOrganizationResponse;
import com.mparsict.vosolmaliat.domain.enums.BusinessRole;

public final class OrganizationCandidateGroup {

    private OrganizationCandidateGroup() {
    }

    public static String forUser(BusinessRole role, MasterdataOrganizationResponse organization) {
        if (role == null || role == BusinessRole.ADMIN) return null;
        if (role.isNationalScope()) return role.name();
        return organization == null ? null : forOrganization(role, organization.generalDirectorateId());
    }

    public static String forOrganization(BusinessRole role, Long organizationId) {
        return role == null || organizationId == null ? null : role.name() + ":" + organizationId;
    }
}
