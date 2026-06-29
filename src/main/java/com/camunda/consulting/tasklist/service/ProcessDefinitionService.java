package com.camunda.consulting.tasklist.service;

import com.camunda.consulting.tasklist.model.ProcessDefinitionDto;
import com.camunda.consulting.tasklist.model.StartProcessResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ProcessDefinitionService {
  private final CamundaRestClient camundaRestClient;

  public ProcessDefinitionService(CamundaRestClient camundaRestClient) {
    this.camundaRestClient = camundaRestClient;
  }

  public List<ProcessDefinitionDto> getStartableProcessDefinitions() {
    JsonNode response = camundaRestClient.searchProcessDefinitions();
    List<ProcessDefinitionDto> processDefinitions = new ArrayList<>();
    for (JsonNode item : items(response)) {
      processDefinitions.add(mapProcessDefinition(item));
    }
    return processDefinitions;
  }

  public StartProcessResponse startProcess(String processDefinitionKey, Map<String, Object> variables) {
    parseProcessDefinitionKey(processDefinitionKey);
    JsonNode response = camundaRestClient.createProcessInstance(processDefinitionKey, variables);

    return new StartProcessResponse(
        text(response, "processDefinitionKey"),
        text(response, "processDefinitionId"),
        response.path("processDefinitionVersion").asInt(),
        text(response, "processInstanceKey"),
        text(response, "tenantId"));
  }

  private ProcessDefinitionDto mapProcessDefinition(JsonNode processDefinition) {
    return new ProcessDefinitionDto(
        text(processDefinition, "processDefinitionKey"),
        text(processDefinition, "processDefinitionId"),
        text(processDefinition, "name"),
        text(processDefinition, "resourceName"),
        processDefinition.path("version").asInt(),
        text(processDefinition, "tenantId"),
        processDefinition.path("hasStartForm").isMissingNode()
            ? null
            : processDefinition.path("hasStartForm").asBoolean());
  }

  private Iterable<JsonNode> items(JsonNode response) {
    if (response == null) {
      return List.of();
    }
    if (response.isArray()) {
      return response;
    }
    JsonNode items = response.path("items");
    return items.isArray() ? items : List.of();
  }

  private String text(JsonNode node, String fieldName) {
    JsonNode value = node.path(fieldName);
    return value.isMissingNode() || value.isNull() ? null : value.asText();
  }

  private long parseProcessDefinitionKey(String processDefinitionKey) {
    if (processDefinitionKey == null || processDefinitionKey.isBlank()) {
      throw new IllegalArgumentException("processDefinitionKey is required.");
    }

    try {
      return Long.parseLong(processDefinitionKey);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("processDefinitionKey must be a numeric Camunda key.", e);
    }
  }
}
