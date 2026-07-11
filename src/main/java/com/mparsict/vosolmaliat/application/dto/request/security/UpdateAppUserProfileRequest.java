package com.mparsict.vosolmaliat.application.dto.request.security;

public record UpdateAppUserProfileRequest(
        String email,
        String firstName,
        String lastName,
        String phoneNumber
) {}
