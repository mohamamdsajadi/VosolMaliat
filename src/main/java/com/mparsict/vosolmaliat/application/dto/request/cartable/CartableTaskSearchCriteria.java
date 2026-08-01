package com.mparsict.vosolmaliat.application.dto.request.cartable;

/**
 * معیارهای جست‌وجوی وظایف کارتابل.
 *
 * @param executionPaper کد یا شناسه برگ اجرا
 * @param taskName عنوان وظیفه
 * @param assignee نام کاربری مسئول وظیفه
 * @param candidateGroup گروه نامزد انجام وظیفه
 * @param createdFrom ابتدای بازه ایجاد وظیفه
 * @param createdTo انتهای بازه ایجاد وظیفه
 */
public record CartableTaskSearchCriteria(
        String executionPaper,
        String taskName,
        String assignee,
        String candidateGroup,
        String createdFrom,
        String createdTo
) {
    /** @return معیار جست‌وجوی بدون فیلتر */
    public static CartableTaskSearchCriteria empty() {
        return new CartableTaskSearchCriteria(null, null, null, null, null, null);
    }
}
