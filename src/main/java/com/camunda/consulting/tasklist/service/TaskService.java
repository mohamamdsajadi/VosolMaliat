package com.camunda.consulting.tasklist.service;

import com.camunda.consulting.tasklist.model.TaskDto;
import com.camunda.consulting.tasklist.model.TaskOverviewDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.tasklist.CamundaTaskListClient;
import io.camunda.tasklist.dto.Form;
import io.camunda.tasklist.dto.Task;
import io.camunda.tasklist.dto.Variable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
  private static final Logger LOG = LoggerFactory.getLogger(TaskService.class);
  private static final String EVALUATION_REVIEW_TASK_NAME = "بازبینی توسط کارشناس";
  private static final String INCOMPLETE_INFORMATION_TASK_NAME = "تکمیل مجدد اطلاعات ناقص توسط کاربر";
  private final ObjectMapper objectMapper;
  private final CamundaTaskListClient camundaTaskListClient;
  private final CamundaRestClient camundaRestClient;

  public TaskService(
      ObjectMapper objectMapper,
      CamundaTaskListClient camundaTaskListClient,
      CamundaRestClient camundaRestClient) {
    this.objectMapper = objectMapper;
    this.camundaTaskListClient = camundaTaskListClient;
    this.camundaRestClient = camundaRestClient;
  }

  public TaskDto getTask(String id) {
    JsonNode task = camundaRestClient.getUserTask(id);
    String taskName = firstText(task, "name", "elementId", "id");
    Map<String, Object> variables = getTaskVariables(id);

    if (EVALUATION_REVIEW_TASK_NAME.equals(taskName)
        || INCOMPLETE_INFORMATION_TASK_NAME.equals(taskName)) {
      return new TaskDto(id, taskName, null, variables, null);
    }

    JsonNode form = camundaRestClient.getUserTaskForm(id);
    if (form == null || form.isMissingNode() || form.isNull()) {
      throw new RuntimeException("Task form was not found.");
    }
    return new TaskDto(
        id,
        taskName,
        parseFormSchema(form.path("schema")),
        variables,
        text(form, "formKey"));
  }

  public List<TaskOverviewDto> getTasks(boolean assignedOnly, String processDefinitionKey) {
    if (processDefinitionKey == null || processDefinitionKey.isBlank()) {
      return List.of();
    }

    JsonNode response = camundaRestClient.searchUserTasks(processDefinitionKey);
    return mapUserTasks(response);
  }

  public List<TaskOverviewDto> searchTasks(JsonNode request) {
    JsonNode response = camundaRestClient.searchUserTasks(request);
    return mapUserTasks(response);
  }

  private List<TaskOverviewDto> mapUserTasks(JsonNode response) {
    List<TaskOverviewDto> tasks = new ArrayList<>();
    for (JsonNode item : items(response)) {
      tasks.add(mapUserTask(item));
    }
    return tasks;
  }

  private TaskOverviewDto map(Task task) {
    return new TaskOverviewDto(
        task.getId(),
        task.getName(),
        task.getProcessName(),
        task.getProcessDefinitionKey(),
        task.getProcessInstanceKey());
  }

  private TaskOverviewDto mapUserTask(JsonNode task) {
    return new TaskOverviewDto(
        firstText(task, "id", "userTaskKey"),
        text(task, "name"),
        text(task, "processName"),
        text(task, "processDefinitionKey"),
        text(task, "processInstanceKey"));
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

  private String firstText(JsonNode node, String... fieldNames) {
    for (String fieldName : fieldNames) {
      String value = text(node, fieldName);
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private String text(JsonNode node, String fieldName) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    JsonNode value = node.path(fieldName);
    return value.isMissingNode() || value.isNull() ? null : value.asText();
  }

  private Map<String, Object> getTaskVariables(String id) {
    JsonNode response = camundaRestClient.searchUserTaskVariables(id);
    return mapVariables(response);
  }

  private Map<String, Object> mapVariables(JsonNode response) {
    Map<String, Object> variables = new LinkedHashMap<>();
    for (JsonNode item : items(response)) {
      String name = text(item, "name");
      if (name != null && !name.isBlank()) {
        variables.put(name, variableValue(item));
      }
    }
    return variables;
  }

  private Object variableValue(JsonNode variable) {
    JsonNode value = variable.path("value");
    if (value.isMissingNode() || value.isNull()) {
      return null;
    }
    if (value.isTextual()) {
      String rawValue = value.asText();
      try {
        return objectMapper.readValue(rawValue, new TypeReference<>() {});
      } catch (JsonProcessingException ignored) {
        return rawValue;
      }
    }
    return objectMapper.convertValue(value, new TypeReference<>() {});
  }

  private Map<String, Object> parseFormSchema(JsonNode schema) {
    try {
      if (schema == null || schema.isMissingNode() || schema.isNull()) {
        return null;
      }
      if (schema.isTextual()) {
        return objectMapper.readValue(schema.asText(), new TypeReference<>() {});
      }
      return objectMapper.convertValue(schema, new TypeReference<>() {});
    } catch (IllegalArgumentException | JsonProcessingException e) {
      throw new RuntimeException("Error while parsing task form schema", e);
    }
  }

  private TaskDto map(Task task, Form form, List<Variable> variables) {
    try {
      Map<String, Object> schema = null;
      if (form != null) {
        schema = objectMapper.readValue(form.getSchema(), new TypeReference<>() {});
      }
      Map<String, Object> data =
          (variables == null ? List.<Variable>of() : variables).stream()
              .map(v -> Map.entry(v.getName(), v.getValue()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      return new TaskDto(task.getId(), task.getName(), schema, data, task.getFormKey());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public void completeTask(String id, Map<String, Object> data) {
    camundaRestClient.completeUserTask(id, data);
  }
}
