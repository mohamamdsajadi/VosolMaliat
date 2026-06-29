package com.camunda.consulting.tasklist;

import com.camunda.consulting.tasklist.model.ProcessDefinitionDto;
import com.camunda.consulting.tasklist.model.StartProcessRequest;
import com.camunda.consulting.tasklist.model.StartProcessResponse;
import com.camunda.consulting.tasklist.service.ProcessDefinitionService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;

@RestController
@RequestMapping("/api")
public class ProcessDefinitionController {
  private static final Logger LOG = LoggerFactory.getLogger(ProcessDefinitionController.class);
  private final ProcessDefinitionService processDefinitionService;

  public ProcessDefinitionController(ProcessDefinitionService processDefinitionService) {
    this.processDefinitionService = processDefinitionService;
  }

  @GetMapping("/process-definitions")
  public ResponseEntity<List<ProcessDefinitionDto>> getProcessDefinitions() {
    return searchProcessDefinitions();
  }

  @PostMapping("/process-definitions/search")
  public ResponseEntity<List<ProcessDefinitionDto>> searchProcessDefinitions() {
    try {
      return ResponseEntity.ok(processDefinitionService.getStartableProcessDefinitions());
    } catch (Exception e) {
      LOG.error("Error while fetching process definitions", e);
      throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/process-instances")
  public ResponseEntity<StartProcessResponse> startProcess(@RequestBody StartProcessRequest request) {
    try {
      return ResponseEntity
          .status(HttpStatus.CREATED)
          .body(processDefinitionService.startProcess(request.processDefinitionKey(), request.variables()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      LOG.error("Error while starting process", e);
      throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }
}
