package com.mparsict.vosolmaliat.application.dto.request.cartable;

import jakarta.validation.constraints.NotBlank;

/**
 * کاربر مقصد برای تخصیص مستقیم وظیفه کارتابل.
 *
 * @param assignee نام کاربری مسئول جدید وظیفه
 */
public record AssignTaskRequest(@NotBlank String assignee) {}
