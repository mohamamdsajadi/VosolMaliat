package com.mparsict.vosolmaliat.common.config;

import io.camunda.client.CamundaClient;
import com.mparsict.vosolmaliat.common.config.security.DebtPaperSecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Proxy;

@Configuration
@org.springframework.boot.context.properties.EnableConfigurationProperties(DebtPaperSecurityProperties.class)
@ConditionalOnProperty(name = "app.ef03.import-samples", havingValue = "true")
public class Ef03ImportRuntimeConfig {
    @Bean
    @ConditionalOnMissingBean(CamundaClient.class)
    CamundaClient ef03ImportCamundaClient() {
        return (CamundaClient) Proxy.newProxyInstance(
                CamundaClient.class.getClassLoader(),
                new Class<?>[]{CamundaClient.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("close")) return null;
                    if (method.getName().equals("toString")) return "EF03 import no-op Camunda client";
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 0L;
                    return null;
                });
    }
}
