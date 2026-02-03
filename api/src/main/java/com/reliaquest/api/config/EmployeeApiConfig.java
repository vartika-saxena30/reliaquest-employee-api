package com.reliaquest.api.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(EmployeeApiProperties.class)
public class EmployeeApiConfig {

    @Bean
    public RestTemplate employeeRestTemplate(RestTemplateBuilder builder, EmployeeApiProperties properties) {
        return builder.rootUri(properties.getBaseUrl())
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(4))
                .build();
    }
}
