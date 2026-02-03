package com.reliaquest.api.service;

import com.reliaquest.api.config.EmployeeApiProperties;
import com.reliaquest.api.exception.EmployeeApiException;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.model.ApiResponse;
import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.api.model.DeleteEmployeeInput;
import com.reliaquest.api.model.Employee;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private static final long INITIAL_BACKOFF_MILLIS = 250L;

    private final RestTemplate employeeRestTemplate;
    private final EmployeeApiProperties properties;

    public List<Employee> getAllEmployees() {
        log.debug("Fetching all employees");
        ApiResponse<List<Employee>> response =
                exchangeWithRetry("", HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
        if (response == null || response.getData() == null) {
            log.info("Employee API returned empty response for getAllEmployees");
            return Collections.emptyList();
        }
        log.debug("Fetched {} employees", response.getData().size());
        return response.getData();
    }

    public List<Employee> getEmployeesByNameSearch(String searchString) {
        if (searchString == null || searchString.isBlank()) {
            log.debug("Empty search string provided; returning empty list");
            return Collections.emptyList();
        }
        String needle = searchString.toLowerCase(Locale.ROOT);
        List<Employee> matches = getAllEmployees().stream()
                .filter(employee -> {
                    String name = employee.getEmployeeName();
                    return name != null && name.toLowerCase(Locale.ROOT).contains(needle);
                })
                .toList();
        log.debug("Found {} employees matching searchString='{}'", matches.size(), searchString);
        return matches;
    }

    public Employee getEmployeeById(String id) {
        log.debug("Fetching employee by id={}", id);
        try {
            ApiResponse<Employee> response =
                    exchangeWithRetry("/" + id, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
            if (response == null || response.getData() == null) {
                log.info("Employee API returned empty response for id={}", id);
                throw new EmployeeNotFoundException("Employee not found for id=" + id);
            }
            return response.getData();
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Employee not found for id={}", id);
            throw new EmployeeNotFoundException("Employee not found for id=" + id);
        }
    }

    public Integer getHighestSalaryOfEmployees() {
        Integer highestSalary = getAllEmployees().stream()
                .map(Employee::getEmployeeSalary)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        log.debug("Highest employee salary resolved to {}", highestSalary);
        return highestSalary;
    }

    public List<String> getTop10HighestEarningEmployeeNames() {
        List<String> names = getAllEmployees().stream()
                .sorted(Comparator.comparing(Employee::getEmployeeSalary, Comparator.nullsFirst(Integer::compareTo))
                        .reversed())
                .limit(10)
                .map(Employee::getEmployeeName)
                .filter(Objects::nonNull)
                .toList();
        log.debug("Top 10 highest earning employee names resolved (count={})", names.size());
        return names;
    }

    public Employee createEmployee(CreateEmployeeInput input) {
        log.info("Creating employee name={}", input.getName());
        ApiResponse<Employee> response =
                exchangeWithRetry("", HttpMethod.POST, new HttpEntity<>(input), new ParameterizedTypeReference<>() {});
        if (response == null || response.getData() == null) {
            log.error("Employee API returned empty response for createEmployee");
            throw new EmployeeApiException("Failed to create employee");
        }
        log.info("Created employee id={}", response.getData().getId());
        return response.getData();
    }

    public String deleteEmployeeById(String id) {
        log.info("Deleting employee by id={}", id);
        Employee employee = getEmployeeById(id);
        DeleteEmployeeInput deleteInput = new DeleteEmployeeInput();
        deleteInput.setName(employee.getEmployeeName());

        ApiResponse<Boolean> response = exchangeWithRetry(
                "", HttpMethod.DELETE, new HttpEntity<>(deleteInput), new ParameterizedTypeReference<>() {});
        if (response == null || response.getData() == null || !response.getData()) {
            log.error("Employee API failed to delete employee id={} name={}", id, employee.getEmployeeName());
            throw new EmployeeApiException("Failed to delete employee with id=" + id);
        }
        log.info("Deleted employee id={} name={}", id, employee.getEmployeeName());
        return employee.getEmployeeName();
    }

    private <T> ApiResponse<T> exchangeWithRetry(
            String path,
            HttpMethod method,
            HttpEntity<?> entity,
            ParameterizedTypeReference<ApiResponse<T>> responseType) {
        String url = buildUrl(path);
        int maxAttempts = Math.max(1, properties.getMaxAttempts());
        long backoffMillis = INITIAL_BACKOFF_MILLIS;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int attemptNumber = attempt + 1;
            try {
                ResponseEntity<ApiResponse<T>> response =
                        employeeRestTemplate.exchange(url, method, entity, responseType);
                return response.getBody();
            } catch (HttpStatusCodeException ex) {
                if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                    throw ex;
                }
                if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS && attemptNumber < maxAttempts) {
                    log.info(
                            "Rate limited by employee API (attempt {}/{}), backing off {}ms",
                            attemptNumber,
                            maxAttempts,
                            backoffMillis);
                    sleep(backoffMillis);
                    backoffMillis *= 2;
                    continue;
                }
                throw new EmployeeApiException("Employee API request failed with status=" + ex.getStatusCode(), ex);
            } catch (ResourceAccessException ex) {
                throw new EmployeeApiException("Employee API request failed: " + properties.getBaseUrl(), ex);
            }
        }
        throw new EmployeeApiException("Employee API request failed after retries");
    }

    private String buildUrl(String path) {
        String baseUrl = properties.getBaseUrl();
        if (path == null || path.isBlank()) {
            return baseUrl;
        }
        if (path.startsWith("/")) {
            return baseUrl + path;
        }
        return baseUrl + "/" + path;
    }

    private void sleep(long backoffMillis) {
        try {
            TimeUnit.MILLISECONDS.sleep(backoffMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
