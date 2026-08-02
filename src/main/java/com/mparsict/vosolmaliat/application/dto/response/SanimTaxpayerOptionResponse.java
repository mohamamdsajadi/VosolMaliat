package com.mparsict.vosolmaliat.application.dto.response;

import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;

public record SanimTaxpayerOptionResponse(Long taxpayerId, TaxpayerType taxpayerType, String identifier, String displayName) {}
