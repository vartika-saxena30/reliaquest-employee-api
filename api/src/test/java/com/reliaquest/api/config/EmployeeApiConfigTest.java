package com.reliaquest.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

class EmployeeApiConfigTest {

    @Test
    void employeeRestTemplateBuildsWithRootUri() {
        EmployeeApiProperties properties = new EmployeeApiProperties();
        properties.setBaseUrl("http://localhost:8112/api/v1/employee");

        EmployeeApiConfig config = new EmployeeApiConfig();
        RestTemplate restTemplate = config.employeeRestTemplate(new RestTemplateBuilder(), properties);

        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate.getUriTemplateHandler()).isNotNull();
    }

    @Test
    void propertiesHaveDefaultBaseUrl() {
        EmployeeApiProperties properties = new EmployeeApiProperties();

        assertThat(properties.getBaseUrl()).isEqualTo("http://localhost:8112/api/v1/employee");
        assertThat(properties.getMaxAttempts()).isEqualTo(3);
    }
}
