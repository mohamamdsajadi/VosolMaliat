package com.mparsict.vosolmaliat.unit.domain;

import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessRoleUnitTest {

    @Test
    void nextDelegationRoleFollowsOrganizationalHierarchy() {
        assertThat(BusinessRole.GENERAL_OFFICE_HEAD.nextDelegationRole()).isEqualTo(BusinessRole.TAX_OFFICE_HEAD);
        assertThat(BusinessRole.TAX_OFFICE_HEAD.nextDelegationRole()).isEqualTo(BusinessRole.TAX_UNIT_HEAD);
        assertThat(BusinessRole.TAX_UNIT_HEAD.nextDelegationRole()).isEqualTo(BusinessRole.TAX_EXPERT);
        assertThat(BusinessRole.TAX_EXPERT.nextDelegationRole()).isNull();
        assertThat(BusinessRole.ADMIN.nextDelegationRole()).isNull();
    }

    @Test
    void adminIsNotOperationalRole() {
        assertThat(BusinessRole.ADMIN.isOperational()).isFalse();
        assertThat(BusinessRole.TAX_EXPERT.isOperational()).isTrue();
    }
}
