package com.reliaquest.api.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reliaquest.api.exception.EmployeeApiException;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.EmployeeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Test
    void getAllEmployeesReturnsList() throws Exception {
        given(employeeService.getAllEmployees()).willReturn(List.of(employee("1", "Alpha")));

        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isOk())
                .andExpect(content().json("[{\"id\":\"1\",\"employee_name\":\"Alpha\"}]"));
    }

    @Test
    void getEmployeesByNameSearchReturnsMatches() throws Exception {
        given(employeeService.getEmployeesByNameSearch("al"))
                .willReturn(List.of(employee("1", "Alpha"), employee("2", "Alina")));

        mockMvc.perform(get("/api/v1/employee/search/al"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        "[{\"id\":\"1\",\"employee_name\":\"Alpha\"},{\"id\":\"2\",\"employee_name\":\"Alina\"}]"));
    }

    @Test
    void getEmployeesByNameSearchRejectsBlank() throws Exception {
        mockMvc.perform(get("/api/v1/employee/search/ ")).andExpect(status().isBadRequest());
    }

    @Test
    void getEmployeeByIdReturnsEmployee() throws Exception {
        given(employeeService.getEmployeeById("1")).willReturn(employee("1", "Alpha"));

        mockMvc.perform(get("/api/v1/employee/1"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":\"1\",\"employee_name\":\"Alpha\"}"));
    }

    @Test
    void getEmployeeByIdRejectsBlank() throws Exception {
        mockMvc.perform(get("/api/v1/employee/ ")).andExpect(status().isBadRequest());
    }

    @Test
    void getHighestSalaryOfEmployeesReturnsValue() throws Exception {
        given(employeeService.getHighestSalaryOfEmployees()).willReturn(500);

        mockMvc.perform(get("/api/v1/employee/highestSalary"))
                .andExpect(status().isOk())
                .andExpect(content().string("500"));
    }

    @Test
    void getTopTenHighestEarningEmployeeNamesReturnsNames() throws Exception {
        given(employeeService.getTop10HighestEarningEmployeeNames()).willReturn(List.of("Alpha", "Bravo"));

        mockMvc.perform(get("/api/v1/employee/topTenHighestEarningEmployeeNames"))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"Alpha\",\"Bravo\"]"));
    }

    @Test
    void createEmployeeReturnsEmployee() throws Exception {
        given(employeeService.createEmployee(org.mockito.ArgumentMatchers.any()))
                .willReturn(employee("9", "New Hire"));

        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Hire\",\"salary\":500,\"age\":30,\"title\":\"Engineer\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":\"9\",\"employee_name\":\"New Hire\"}"));
    }

    @Test
    void createEmployeeValidationFailsWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"salary\":500,\"age\":30,\"title\":\"Engineer\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name must not be blank"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void deleteEmployeeByIdReturnsName() throws Exception {
        given(employeeService.deleteEmployeeById("1")).willReturn("Alpha");

        mockMvc.perform(delete("/api/v1/employee/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Alpha"));
    }

    @Test
    void deleteEmployeeByIdRejectsBlank() throws Exception {
        mockMvc.perform(delete("/api/v1/employee/ ")).andExpect(status().isBadRequest());
    }

    @Test
    void notFoundIsMappedTo404() throws Exception {
        doThrow(new EmployeeNotFoundException("Employee not found for id=missing"))
                .when(employeeService)
                .getEmployeeById("missing");

        mockMvc.perform(get("/api/v1/employee/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found for id=missing"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void apiErrorsAreMappedToBadGateway() throws Exception {
        doThrow(new EmployeeApiException("Upstream error"))
                .when(employeeService)
                .getAllEmployees();

        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Upstream error"))
                .andExpect(jsonPath("$.status").value(502));
    }

    private Employee employee(String id, String name) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setEmployeeName(name);
        return employee;
    }
}
