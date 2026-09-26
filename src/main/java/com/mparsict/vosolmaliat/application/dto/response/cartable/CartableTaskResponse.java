package com.mparsict.vosolmaliat.application.dto.response.cartable;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * اطلاعات نمایشی یک وظیفه در کارتابل کاربر.
 *
 * @param taskKey کلید وظیفه در Camunda
 * @param name عنوان وظیفه
 * @param processInstanceKey کلید نمونه فرایند
 * @param processDefinitionKey کلید نسخه تعریف فرایند
 * @param processDefinitionId شناسه تعریف فرایند
 * @param taskDefinitionId شناسه وظیفه در BPMN
 * @param assignee نام کاربری مسئول وظیفه
 * @param assigneeFullName نام کامل مسئول وظیفه
 * @param candidateUsers کاربران نامزد انجام وظیفه
 * @param candidateGroups گروه‌های نامزد انجام وظیفه
 * @param createdAt زمان ایجاد وظیفه
 * @param dueDate مهلت انجام وظیفه
 * @param followUpDate زمان پیگیری وظیفه
 * @param formKey کلید فرم
 * @param formId شناسه فرم
 * @param variables متغیرهای قابل نمایش وظیفه
 * @param allowedActions عملیات مجاز کاربر
 */
public record CartableTaskResponse(
        Long taskKey,
        String name,
        Long processInstanceKey,
        Long processDefinitionKey,
        String processDefinitionId,
        String taskDefinitionId,
        String assignee,
        String assigneeFullName,
        List<String> candidateUsers,
        List<String> candidateGroups,
        OffsetDateTime createdAt,
        OffsetDateTime dueDate,
        OffsetDateTime followUpDate,
        Long formKey,
        String formId,
        List<TaskVariableResponse> variables,
        AllowedTaskActionsResponse allowedActions
) {}
