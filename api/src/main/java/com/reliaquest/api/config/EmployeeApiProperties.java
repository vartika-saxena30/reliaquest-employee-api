package com.reliaquest.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "employee.api")
public class EmployeeApiProperties {

    private String baseUrl = "http://localhost:8112/api/v1/employee";
    private int maxAttempts = 3;
}
