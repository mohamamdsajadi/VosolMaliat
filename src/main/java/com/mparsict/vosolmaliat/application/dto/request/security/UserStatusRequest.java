package com.mparsict.vosolmaliat.application.dto.request.security;

import jakarta.validation.constraints.NotNull;

/**
 * وضعیت فعال یا غیرفعال بودن حساب کاربر.
 *
 * @param enabled مقدار {@code true} برای فعال کردن حساب
 */
public record UserStatusRequest(@NotNull Boolean enabled) {}
