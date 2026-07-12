package com.mparsict.vosolmaliat.application.dto.response.cartable;

public record DelegationTargetResponse(
        Long userProfileId,
        String username,
        String fullName,
        String businessRole
) {
}
