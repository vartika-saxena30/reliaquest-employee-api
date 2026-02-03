package com.reliaquest.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EmployeeModelTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void employeeJsonDeserializesWithApiFieldNames() throws Exception {
        String json =
                """
                {
                  "id": "1",
                  "employee_name": "Alpha",
                  "employee_salary": 100,
                  "employee_age": 30,
                  "employee_title": "Engineer",
                  "employee_email": "alpha@company.com"
                }
                """;

        Employee employee = objectMapper.readValue(json, Employee.class);

        assertThat(employee.getId()).isEqualTo("1");
        assertThat(employee.getEmployeeName()).isEqualTo("Alpha");
        assertThat(employee.getEmployeeSalary()).isEqualTo(100);
        assertThat(employee.getEmployeeAge()).isEqualTo(30);
        assertThat(employee.getEmployeeTitle()).isEqualTo("Engineer");
        assertThat(employee.getEmployeeEmail()).isEqualTo("alpha@company.com");
    }

    @Test
    void apiResponseWrapsData() {
        ApiResponse<Employee> response = new ApiResponse<>();
        Employee employee = new Employee();
        employee.setId("99");
        response.setData(employee);
        response.setStatus("ok");
        response.setError("none");

        assertThat(response.getData().getId()).isEqualTo("99");
        assertThat(response.getStatus()).isEqualTo("ok");
        assertThat(response.getError()).isEqualTo("none");
    }

    @Test
    void createAndDeleteInputsStoreValues() {
        CreateEmployeeInput createInput = new CreateEmployeeInput();
        createInput.setName("New");
        createInput.setSalary(200);
        createInput.setAge(25);
        createInput.setTitle("Engineer");

        DeleteEmployeeInput deleteInput = new DeleteEmployeeInput();
        deleteInput.setName("Old");

        assertThat(createInput.getName()).isEqualTo("New");
        assertThat(createInput.getSalary()).isEqualTo(200);
        assertThat(createInput.getAge()).isEqualTo(25);
        assertThat(createInput.getTitle()).isEqualTo("Engineer");
        assertThat(deleteInput.getName()).isEqualTo("Old");
    }
}
