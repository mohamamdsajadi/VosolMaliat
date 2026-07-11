package com.mparsict.vosolmaliat.application.dto.response.cartable;

import java.time.OffsetDateTime;
import java.util.List;

public record CartableTaskResponse(
        Long taskKey,
        String name,
        Long processInstanceKey,
        Long processDefinitionKey,
        String processDefinitionId,
        String taskDefinitionId,
        String assignee,
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
