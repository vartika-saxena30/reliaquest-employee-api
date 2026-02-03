package com.reliaquest.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.config.EmployeeApiProperties;
import com.reliaquest.api.exception.EmployeeApiException;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.api.model.Employee;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmployeeServiceTest {

    private static final String BASE_URL = "http://localhost:8112/api/v1/employee";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockRestServiceServer server;
    private EmployeeService service;

    @BeforeEach
    void setUp() {
        EmployeeApiProperties properties = new EmployeeApiProperties();
        properties.setBaseUrl(BASE_URL);

        RestTemplate restTemplate =
                new RestTemplateBuilder().rootUri(properties.getBaseUrl()).build();
        server = MockRestServiceServer.createServer(restTemplate);
        service = new EmployeeService(restTemplate, properties);
    }

    @Test
    void getAllEmployeesReturnsData() throws Exception {
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(listResponse(List.of(employeeMap("1", "Alpha", 100))), MediaType.APPLICATION_JSON));

        List<Employee> employees = service.getAllEmployees();

        assertThat(employees).hasSize(1);
        assertThat(employees.get(0).getEmployeeName()).isEqualTo("Alpha");
    }

    @Test
    void getAllEmployeesReturnsEmptyListWhenNoData() throws Exception {
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseWithoutData(), MediaType.APPLICATION_JSON));

        List<Employee> employees = service.getAllEmployees();

        assertThat(employees).isEmpty();
    }

    @Test
    void getEmployeesByNameSearchReturnsMatches() throws Exception {
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        listResponse(List.of(employeeMap("1", "Alpha", 100), employeeMap("2", "Bravo", 200))),
                        MediaType.APPLICATION_JSON));

        List<Employee> employees = service.getEmployeesByNameSearch("alp");

        assertThat(employees).hasSize(1);
        assertThat(employees.get(0).getEmployeeName()).isEqualTo("Alpha");
    }

    @Test
    void getEmployeesByNameSearchEmptyInputSkipsApiCall() {
        List<Employee> employees = service.getEmployeesByNameSearch(" ");

        assertThat(employees).isEmpty();
    }

    @Test
    void getEmployeeByIdThrowsNotFoundOn404() {
        server.expect(requestTo(BASE_URL + "/missing"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> service.getEmployeeById("missing"))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void getHighestSalaryOfEmployeesReturnsMax() throws Exception {
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        listResponse(List.of(employeeMap("1", "Alpha", 100), employeeMap("2", "Bravo", 450))),
                        MediaType.APPLICATION_JSON));

        Integer maxSalary = service.getHighestSalaryOfEmployees();

        assertThat(maxSalary).isEqualTo(450);
    }

    @Test
    void getTop10HighestEarningEmployeeNamesReturnsOrderedNames() throws Exception {
        List<Map<String, Object>> employees = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            employees.add(employeeMap(String.valueOf(i), "Emp" + i, i * 100));
        }
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(listResponse(employees), MediaType.APPLICATION_JSON));

        List<String> names = service.getTop10HighestEarningEmployeeNames();

        assertThat(names).hasSize(10);
        assertThat(names.get(0)).isEqualTo("Emp11");
        assertThat(names.get(9)).isEqualTo("Emp2");
    }

    @Test
    void createEmployeeReturnsCreatedEmployee() throws Exception {
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(singleResponse(employeeMap("9", "New Hire", 500)), MediaType.APPLICATION_JSON));

        CreateEmployeeInput input = new CreateEmployeeInput();
        input.setName("New Hire");
        input.setSalary(500);
        input.setAge(30);
        input.setTitle("Engineer");

        Employee created = service.createEmployee(input);

        assertThat(created.getId()).isEqualTo("9");
        assertThat(created.getEmployeeName()).isEqualTo("New Hire");
    }

    @Test
    void createEmployeeFailsWhenResponseMissingData() throws Exception {
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseWithoutData(), MediaType.APPLICATION_JSON));

        CreateEmployeeInput input = new CreateEmployeeInput();
        input.setName("New Hire");
        input.setSalary(500);
        input.setAge(30);
        input.setTitle("Engineer");

        assertThatThrownBy(() -> service.createEmployee(input))
                .isInstanceOf(EmployeeApiException.class)
                .hasMessageContaining("Failed to create employee");
    }

    @Test
    void deleteEmployeeByIdDeletesAndReturnsName() throws Exception {
        server.expect(requestTo(BASE_URL + "/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(singleResponse(employeeMap("1", "Alpha", 100)), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess(booleanResponse(true), MediaType.APPLICATION_JSON));

        String name = service.deleteEmployeeById("1");

        assertThat(name).isEqualTo("Alpha");
    }

    @Test
    void deleteEmployeeByIdFailsWhenDeleteReturnsFalse() throws Exception {
        server.expect(requestTo(BASE_URL + "/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(singleResponse(employeeMap("1", "Alpha", 100)), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess(booleanResponse(false), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.deleteEmployeeById("1"))
                .isInstanceOf(EmployeeApiException.class)
                .hasMessageContaining("Failed to delete employee");
    }

    @Test
    void rateLimitRetriesThenSucceeds() throws Exception {
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(listResponse(List.of(employeeMap("1", "Alpha", 100))), MediaType.APPLICATION_JSON));

        List<Employee> employees = service.getAllEmployees();

        assertThat(employees).hasSize(1);
    }

    @Test
    void rateLimitWithoutRetriesThrows() {
        EmployeeApiProperties limited = new EmployeeApiProperties();
        limited.setBaseUrl(BASE_URL);
        limited.setMaxAttempts(1);
        RestTemplate restTemplate = new RestTemplateBuilder().rootUri(limited.getBaseUrl()).build();
        MockRestServiceServer limitedServer = MockRestServiceServer.createServer(restTemplate);
        EmployeeService limitedService = new EmployeeService(restTemplate, limited);

        limitedServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(limitedService::getAllEmployees)
                .isInstanceOf(EmployeeApiException.class)
                .hasMessageContaining("status=429");
    }

    @Test
    void upstreamErrorsThrowEmployeeApiException() {
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> service.getAllEmployees())
                .isInstanceOf(EmployeeApiException.class)
                .hasMessageContaining("status=500");
    }

    @Test
    void resourceAccessExceptionThrowsEmployeeApiException() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        EmployeeApiProperties properties = new EmployeeApiProperties();
        properties.setBaseUrl(BASE_URL);
        EmployeeService failureService = new EmployeeService(restTemplate, properties);

        when(restTemplate.exchange(eq(BASE_URL), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        assertThatThrownBy(failureService::getAllEmployees)
                .isInstanceOf(EmployeeApiException.class)
                .hasMessageContaining(BASE_URL);
    }

    @Test
    void buildUrlAddsLeadingSlashWhenMissing() throws Exception {
        EmployeeApiProperties properties = new EmployeeApiProperties();
        properties.setBaseUrl(BASE_URL);
        RestTemplate restTemplate = new RestTemplateBuilder().rootUri(properties.getBaseUrl()).build();
        EmployeeService localService = new EmployeeService(restTemplate, properties);

        Method method = EmployeeService.class.getDeclaredMethod("buildUrl", String.class);
        method.setAccessible(true);

        String url = (String) method.invoke(localService, "segment");

        assertThat(url).isEqualTo(BASE_URL + "/segment");
    }

    private String listResponse(List<Map<String, Object>> employees) throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", employees);
        response.put("status", "ok");
        return objectMapper.writeValueAsString(response);
    }

    private String singleResponse(Map<String, Object> employee) throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", employee);
        response.put("status", "ok");
        return objectMapper.writeValueAsString(response);
    }

    private String booleanResponse(boolean value) throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", value);
        response.put("status", "ok");
        return objectMapper.writeValueAsString(response);
    }

    private String responseWithoutData() throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ok");
        return objectMapper.writeValueAsString(response);
    }

    private Map<String, Object> employeeMap(String id, String name, Integer salary) {
        Map<String, Object> employee = new LinkedHashMap<>();
        employee.put("id", id);
        employee.put("employee_name", name);
        employee.put("employee_salary", salary);
        return employee;
    }
}
