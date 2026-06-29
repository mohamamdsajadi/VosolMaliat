package com.camunda.consulting.tasklist.model;

public record TaskOverviewDto(
    String id,
    String name,
    String processName,
    String processDefinitionKey,
    String processInstanceKey) {}
