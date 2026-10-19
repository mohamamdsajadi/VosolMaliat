package com.mparsict.vosolmaliat.common.config;

import io.camunda.client.CamundaClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.lang.reflect.Proxy;

@Configuration
@ConditionalOnProperty(name = "camunda.client.enabled", havingValue = "false")
public class OfflineCamundaConfiguration {
    /** Keeps database-backed services available while rejecting engine commands immediately. */
    @Bean
    @ConditionalOnMissingBean(CamundaClient.class)
    CamundaClient offlineCamundaClient() {
        return (CamundaClient) Proxy.newProxyInstance(CamundaClient.class.getClassLoader(),
                new Class<?>[]{CamundaClient.class}, (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "close" -> null;
                        case "toString" -> "Offline Camunda client (no engine connections)";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Camunda is disabled in offline mode; workflow execution is unavailable.");
                    };
                });
    }
}
