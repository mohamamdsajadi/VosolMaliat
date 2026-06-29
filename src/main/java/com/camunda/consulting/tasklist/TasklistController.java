package com.camunda.consulting.tasklist;

import com.camunda.consulting.tasklist.model.TaskDto;
import com.camunda.consulting.tasklist.model.TaskOverviewDto;
import com.camunda.consulting.tasklist.service.TaskService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

@RestController
@RequestMapping("/api")
public class TasklistController {
  private static final Logger LOG = LoggerFactory.getLogger(TasklistController.class);
  private final TaskService taskService;

  @Autowired
  public TasklistController(TaskService taskService) {
    this.taskService = taskService;
  }

  @GetMapping("/tasks/{id}")
  public ResponseEntity<TaskDto> getTask(@PathVariable("id") String id) {
    return getUserTaskForm(id);
  }

  @GetMapping("/user-tasks/{id}/form")
  public ResponseEntity<TaskDto> getUserTaskForm(@PathVariable("id") String id) {
    try {
      return ResponseEntity.ok(taskService.getTask(id));
    } catch (NullPointerException e) {
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      LOG.error("Error while fetching task", e);
      throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/tasks")
  public ResponseEntity<List<TaskOverviewDto>> getTasks(
      @RequestParam(value = "assignedOnly", required = false, defaultValue = "true") boolean assignedOnly,
      @RequestParam(value = "processDefinitionKey", required = false) String processDefinitionKey) {

    return ResponseEntity.ok(taskService.getTasks(assignedOnly, processDefinitionKey));
  }

  @PostMapping("/user-tasks/search")
  public ResponseEntity<List<TaskOverviewDto>> searchTasks(@RequestBody JsonNode request) {
    return ResponseEntity.ok(taskService.searchTasks(request));
  }

  @PatchMapping("/tasks/{id}/complete")
  public ResponseEntity<String> completeTask(
      @PathVariable("id") String id, @RequestBody Map<String, Object> data) {
    return completeUserTask(id, data);
  }

  @PostMapping("/user-tasks/{id}/completion")
  public ResponseEntity<String> completeUserTask(
      @PathVariable("id") String id, @RequestBody Map<String, Object> data) {
    try {
      taskService.completeTask(id, data);
      return ResponseEntity.noContent().build();
    } catch (NullPointerException e) {
      return ResponseEntity.notFound().build();
    } catch (HttpStatusCodeException e) {
      LOG.error("Camunda rejected user task completion", e);
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    } catch (RestClientException e) {
      LOG.error("Camunda user task completion request failed", e);
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body("Camunda completion request failed: " + e.getMessage());
    } catch (Exception e){
      LOG.error("Error while fetching task", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("User task completion failed: " + e.getMessage());
    }
  }
}
