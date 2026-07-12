package com.mparsict.vosolmaliat.application.dto.response.masterdata;

public record GeneralDirectorateResponse(
        Long id,
        String name,
        String postalCode,
        String address,
        String phone
) {}
