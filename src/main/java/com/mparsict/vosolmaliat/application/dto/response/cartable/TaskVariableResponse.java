package com.mparsict.vosolmaliat.application.dto.response.cartable;

/**
 * مقدار یک متغیر وظیفه فرایندی.
 *
 * @param name نام متغیر فرایند
 * @param value مقدار متغیر
 * @param truncated آیا مقدار برای نمایش کوتاه شده است
 */
public record TaskVariableResponse(
        String name,
        Object value,
        Boolean truncated
) {}
