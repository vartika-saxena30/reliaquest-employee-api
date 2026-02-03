package com.reliaquest.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmployeeExceptionTest {

    @Test
    void employeeNotFoundExceptionStoresMessage() {
        EmployeeNotFoundException ex = new EmployeeNotFoundException("missing");

        assertThat(ex.getMessage()).isEqualTo("missing");
    }

    @Test
    void employeeApiExceptionStoresMessageAndCause() {
        RuntimeException cause = new RuntimeException("boom");
        EmployeeApiException ex = new EmployeeApiException("upstream", cause);

        assertThat(ex.getMessage()).isEqualTo("upstream");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
