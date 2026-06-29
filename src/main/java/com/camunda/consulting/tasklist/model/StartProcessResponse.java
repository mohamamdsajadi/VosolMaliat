package com.camunda.consulting.tasklist.model;

public record StartProcessResponse(
    String processDefinitionKey,
    String processDefinitionId,
    int version,
    String processInstanceKey,
    String tenantId) {}
