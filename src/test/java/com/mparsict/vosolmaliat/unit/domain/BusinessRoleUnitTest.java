package com.mparsict.vosolmaliat.unit.domain;

import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessRoleUnitTest {

    @Test
    void newRoleCatalogHasTenOperationalRolesAndAwaitsApprovedDelegationHierarchy() {
        assertThat(BusinessRole.values()).hasSize(11);
        for (var role : BusinessRole.values()) assertThat(role.nextDelegationRole()).isNull();
    }

    @Test
    void adminIsNotOperationalRole() {
        assertThat(BusinessRole.ADMIN.isOperational()).isFalse();
        assertThat(BusinessRole.EXECUTION_OFFICER.isOperational()).isTrue();
    }
}
