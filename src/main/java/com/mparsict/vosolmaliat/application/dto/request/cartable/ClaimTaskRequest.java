package com.mparsict.vosolmaliat.application.dto.request.cartable;

/**
 * کاربر درخواست‌کننده دریافت وظیفه کارتابل.
 *
 * @param assignee نام کاربری دریافت‌کننده وظیفه
 */
public record ClaimTaskRequest(String assignee) {}
