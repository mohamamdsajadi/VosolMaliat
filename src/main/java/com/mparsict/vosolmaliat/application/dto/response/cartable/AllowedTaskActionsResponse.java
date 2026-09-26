package com.mparsict.vosolmaliat.application.dto.response.cartable;

/**
 * عملیات مجاز کاربر روی یک وظیفه کارتابل.
 *
 * @param claim امکان دریافت وظیفه
 * @param unclaim امکان رها کردن وظیفه
 * @param complete امکان تکمیل وظیفه
 * @param assign امکان تخصیص مستقیم
 * @param delegate امکان واگذاری
 * @param view امکان مشاهده
 */
public record AllowedTaskActionsResponse(
        boolean claim,
        boolean unclaim,
        boolean complete,
        boolean assign,
        boolean delegate,
        boolean view
) {}
