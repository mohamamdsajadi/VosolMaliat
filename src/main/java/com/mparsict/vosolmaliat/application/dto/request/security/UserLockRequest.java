package com.mparsict.vosolmaliat.application.dto.request.security;

import jakarta.validation.constraints.NotNull;

/**
 * وضعیت قفل یا رفع قفل حساب کاربر.
 *
 * @param locked مقدار {@code true} برای قفل کردن حساب
 */
public record UserLockRequest(@NotNull Boolean locked) {}
