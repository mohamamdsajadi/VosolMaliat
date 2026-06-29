package com.camunda.consulting.tasklist.model;

import java.util.Map;

public record StartProcessRequest(String processDefinitionKey, Map<String, Object> variables) {}
