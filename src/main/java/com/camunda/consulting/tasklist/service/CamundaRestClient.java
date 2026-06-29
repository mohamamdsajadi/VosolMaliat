package com.camunda.consulting.tasklist.service;

import com.camunda.consulting.tasklist.CamundaRestProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class CamundaRestClient {
  private final CamundaRestProperties properties;
  private final RestTemplate restTemplate;
  private String accessToken;
  private Instant accessTokenExpiresAt = Instant.EPOCH;

  public CamundaRestClient(CamundaRestProperties properties) {
    this.properties = properties;
    this.restTemplate = new RestTemplate();
  }

  public JsonNode searchProcessDefinitions() {
    return post("/v2/process-definitions/search", Map.of("page", page()));
  }

  public JsonNode createProcessInstance(String processDefinitionKey, Map<String, Object> variables) {
    return post(
        "/v2/process-instances",
        Map.of(
            "processDefinitionKey", processDefinitionKey,
            "variables", variables == null ? Map.of() : variables));
  }

  public JsonNode searchUserTasks(String processDefinitionKey) {
    return post(
        "/v2/user-tasks/search",
        Map.of(
            "sort", List.of(Map.of("field", "creationDate", "order", "desc")),
            "page", Map.of("from", 0, "limit", 50),
            "filter",
                Map.of(
                    "processDefinitionKey", processDefinitionKey,
                    "state",
                        Map.of(
                            "$in",
                            List.of(
                                "CREATED", "ASSIGNING", "UPDATING", "COMPLETING", "CANCELING")))));
  }

  public JsonNode searchUserTasks(JsonNode body) {
    return post("/v2/user-tasks/search", body);
  }

  public JsonNode getUserTask(String userTaskKey) {
    return get("/v2/user-tasks/" + userTaskKey);
  }

  public JsonNode getUserTaskForm(String userTaskKey) {
    return get("/v2/user-tasks/" + userTaskKey + "/form");
  }

  public JsonNode searchUserTaskVariables(String userTaskKey) {
    return post("/v2/user-tasks/" + userTaskKey + "/variables/search", Map.of("page", page()));
  }

  public void completeUserTask(String userTaskKey, Map<String, Object> completionRequest) {
    postForStatus(
        "/v2/user-tasks/" + userTaskKey + "/completion",
        normalizeCompletionRequest(completionRequest));
  }

  private Map<String, Object> normalizeCompletionRequest(Map<String, Object> completionRequest) {
    if (completionRequest == null) {
      return Map.of("variables", Map.of());
    }
    if (completionRequest.containsKey("variables")) {
      return completionRequest;
    }
    return Map.of("variables", completionRequest);
  }

  private JsonNode get(String path) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getAccessToken());
    ResponseEntity<JsonNode> response =
        restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    return response.getBody();
  }

  private JsonNode post(String path, Object body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(getAccessToken());
    return restTemplate.postForObject(url(path), new HttpEntity<>(body, headers), JsonNode.class);
  }

  private void postForStatus(String path, Object body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(getAccessToken());
    restTemplate.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers), Void.class);
  }

  private Map<String, Integer> page() {
    return Map.of("from", 0, "limit", 100);
  }

  private String url(String path) {
    String baseUrl = properties.baseUrl();
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalStateException("camunda.api.base-url is required.");
    }
    return baseUrl.replaceAll("/+$", "") + path;
  }

  private synchronized String getAccessToken() {
    if (accessToken != null && Instant.now().isBefore(accessTokenExpiresAt.minusSeconds(30))) {
      return accessToken;
    }

    CamundaRestProperties.Auth auth = properties.auth();
    if (auth == null || auth.tokenUrl() == null || auth.tokenUrl().isBlank()) {
      throw new IllegalStateException("camunda.api.auth.token-url is required.");
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");
    form.add("client_id", auth.clientId());
    form.add("client_secret", auth.clientSecret());
    addIfPresent(form, "audience", auth.audience());
    addIfPresent(form, "scope", auth.scope());

    JsonNode response =
        restTemplate.postForObject(auth.tokenUrl(), new HttpEntity<>(form, headers), JsonNode.class);
    if (response == null || response.path("access_token").asText("").isBlank()) {
      throw new IllegalStateException("Camunda token response did not contain access_token.");
    }

    accessToken = response.path("access_token").asText();
    accessTokenExpiresAt = Instant.now().plusSeconds(response.path("expires_in").asLong(300));
    return accessToken;
  }

  private void addIfPresent(MultiValueMap<String, String> form, String key, String value) {
    if (value != null && !value.isBlank()) {
      form.add(key, value);
    }
  }
}
