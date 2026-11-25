package com.smartpantry.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartpantry.item.ItemNotFoundException;
import com.smartpantry.item.ItemValidationException;

/**
 * Error mapping (T11). The probe controller exists only in this test; it throws
 * each failure type on purpose.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiErrorHandlerTest.ProbeController.class)
class ApiErrorHandlerTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        String dbFile = tempDir.toAbsolutePath().toString().replace('\\', '/') + "/smartpantry";
        registry.add("spring.datasource.url", () -> "jdbc:h2:file:" + dbFile);
    }

    @Autowired
    private MockMvc mvc;

    @RestController
    static class ProbeController {

        @GetMapping("/test-errors/validation")
        String validation() {
            throw new ItemValidationException(Map.of("name", "Name is required."));
        }

        @GetMapping("/test-errors/data")
        String data() {
            throw new DataAccessResourceFailureException(
                    "SELECT * FROM grocery_item failed for jdbc:h2:file:/secret/path");
        }

        @GetMapping("/test-errors/not-found")
        String notFound() {
            throw new ItemNotFoundException(42L);
        }

        @GetMapping("/test-errors/runtime")
        String runtime() {
            throw new IllegalStateException("internal detail that must not leak");
        }
    }

    @Test
    void validationFailureReturns400WithFieldErrorsAndCorrelationId() throws Exception {
        mvc.perform(get("/test-errors/validation").header(CorrelationIdFilter.HEADER, "abc-123"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(CorrelationIdFilter.HEADER, "abc-123"))
                .andExpect(jsonPath("$.correlationId").value("abc-123"))
                .andExpect(jsonPath("$.message").value(ApiErrorHandler.VALIDATION_MESSAGE))
                .andExpect(jsonPath("$.fieldErrors.name").value("Name is required."));
    }

    @Test
    void notFoundFailureReturns404WithStandardBodyAndEmptyFieldErrors() throws Exception {
        mvc.perform(get("/test-errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.correlationId").isString())
                .andExpect(jsonPath("$.message").value(ApiErrorHandler.NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(result -> assertNoInternals(result.getResponse().getContentAsString()));
    }

    @Test
    void dataAccessFailureReturnsSanitized503() throws Exception {
        mvc.perform(get("/test-errors/data"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(ApiErrorHandler.UNAVAILABLE_MESSAGE))
                .andExpect(jsonPath("$.correlationId").isString())
                .andExpect(result -> assertNoInternals(result.getResponse().getContentAsString()));
    }

    @Test
    void unexpectedFailureReturnsSanitized500() throws Exception {
        mvc.perform(get("/test-errors/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(ApiErrorHandler.INTERNAL_MESSAGE))
                .andExpect(result -> assertNoInternals(result.getResponse().getContentAsString()));
    }

    @Test
    void unknownApiPathReturnsJson404NotAStackTrace() throws Exception {
        mvc.perform(get("/api/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(result -> assertNoInternals(result.getResponse().getContentAsString()));
    }

    private static void assertNoInternals(String body) {
        assertFalse(body.contains("Exception"), body);
        assertFalse(body.contains("SELECT"), body);
        assertFalse(body.contains("jdbc:"), body);
        assertFalse(body.contains("secret"), body);
        assertFalse(body.contains("at com."), body);
        assertEquals(true, body.contains("correlationId"), body);
    }
}
