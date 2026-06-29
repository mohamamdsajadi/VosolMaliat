package com.camunda.consulting.tasklist.model;

public record ProcessDefinitionDto(
    String processDefinitionKey,
    String processDefinitionId,
    String name,
    String resourceName,
    int version,
    String tenantId,
    Boolean hasStartForm) {}
