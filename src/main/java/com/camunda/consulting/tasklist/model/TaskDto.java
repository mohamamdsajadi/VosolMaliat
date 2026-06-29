package com.camunda.consulting.tasklist.model;

import java.util.Map;

public record TaskDto(String id, String name, Map<String, Object> schema, Map<String, Object> data, String formKey) {}
