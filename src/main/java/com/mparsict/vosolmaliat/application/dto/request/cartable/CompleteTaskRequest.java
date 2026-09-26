package com.mparsict.vosolmaliat.application.dto.request.cartable;

import java.util.Map;

/**
 * متغیرهای ارسالی هنگام تکمیل وظیفه کارتابل.
 *
 * @param variables متغیرهای خروجی وظیفه برای ادامه فرایند
 */
public record CompleteTaskRequest(Map<String, Object> variables) {}
