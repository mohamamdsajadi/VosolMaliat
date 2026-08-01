package com.mparsict.vosolmaliat.application.dto.request.cartable;

/**
 * اطلاعات واگذاری وظیفه کارتابل.
 *
 * @param assignee نام کاربری دریافت‌کننده واگذاری
 * @param candidateGroup گروه سازمانی مجاز برای واگذاری
 */
public record DelegateTaskRequest(
        String assignee,
        String candidateGroup
) {
}
