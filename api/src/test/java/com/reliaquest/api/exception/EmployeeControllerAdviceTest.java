package com.reliaquest.api.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.reliaquest.api.controller.EmployeeController;
import com.reliaquest.api.model.CreateEmployeeInput;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class EmployeeControllerAdviceTest {

    private final EmployeeControllerAdvice advice = new EmployeeControllerAdvice();

    @Test
    void handleNotFoundReturns404() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/employee/1");

        ResponseEntity<ErrorResponse> response =
                advice.handleNotFound(new EmployeeNotFoundException("missing"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("missing");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }

    @Test
    void handleEmployeeApiReturnsBadGateway() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/employee");

        ResponseEntity<ErrorResponse> response = advice.handleEmployeeApi(new EmployeeApiException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().getMessage()).isEqualTo("boom");
        assertThat(response.getBody().getStatus()).isEqualTo(502);
    }

    @Test
    void handleValidationReturnsBadRequest() throws Exception {
        CreateEmployeeInput input = new CreateEmployeeInput();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(input, "input");
        bindingResult.addError(new FieldError("input", "name", "must not be blank"));

        Method method = EmployeeController.class.getMethod("createEmployee", CreateEmployeeInput.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/employee");

        ResponseEntity<ErrorResponse> response = advice.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("name must not be blank");
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    void handleConstraintViolationReturnsBadRequest() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("must not be blank");
        ConstraintViolationException ex = new ConstraintViolationException("invalid", Set.of(violation));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/employee/1");

        ResponseEntity<ErrorResponse> response = advice.handleConstraintViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("must not be blank");
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    void handleUnexpectedReturnsInternalServerError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/employee");

        ResponseEntity<ErrorResponse> response = advice.handleUnexpected(new IllegalStateException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Unexpected error occurred");
        assertThat(response.getBody().getStatus()).isEqualTo(500);
    }
}
