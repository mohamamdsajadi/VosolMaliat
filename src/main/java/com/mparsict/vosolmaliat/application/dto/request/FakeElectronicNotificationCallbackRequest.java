package com.mparsict.vosolmaliat.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * نتیجه آزمایشی ابلاغ الکترونیک برای ادامه فرایند ابلاغ.
 *
 * @param executionPaperId شناسه داخلی برگ اجرا
 * @param InformDate تاریخ ثبت ابلاغ
 */
public record FakeElectronicNotificationCallbackRequest(
        @NotNull Long executionPaperId,
        @NotBlank String InformDate
) {}
