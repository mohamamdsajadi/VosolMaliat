package com.mparsict.vosolmaliat.application.dto.request.security;

import jakarta.validation.constraints.NotNull;

/**
 * واحد مالیاتی انتخاب‌شده برای انتساب به کاربر.
 *
 * @param taxUnitId شناسه واحد مالیاتی
 */
public record AssignTaxUnitRequest(@NotNull Long taxUnitId) {}
