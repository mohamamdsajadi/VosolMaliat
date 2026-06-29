package com.camunda.consulting.tasklist;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("camunda.api")
public record CamundaRestProperties(String baseUrl, Auth auth) {
  public record Auth(
      String tokenUrl,
      String clientId,
      String clientSecret,
      String audience,
      String scope) {}
}
