package br.com.portalmanager.platform.messaging.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiMessageNotFoundExceptionTest {

    @Test
    void shouldInitializeWithMessageKey() {
        // Arrange
        String expectedKey = "user.not.found";

        // Act
        ApiMessageNotFoundException exception = new ApiMessageNotFoundException(expectedKey);

        // Assert
        assertEquals(expectedKey, exception.getMessageKey());
        assertTrue(exception.getMessage().contains(expectedKey));
        assertNull(exception.getCause());
    }

    @Test
    void shouldInitializeWithKeyAndCause() {
        // Arrange
        String expectedKey = "order.failed";
        Throwable cause = new RuntimeException("Erro original de infraestrutura");

        // Act
        ApiMessageNotFoundException exception = new ApiMessageNotFoundException(expectedKey, cause);

        // Assert
        assertEquals(expectedKey, exception.getMessageKey());
        assertTrue(exception.getMessage().contains(expectedKey));
        assertNotNull(exception.getCause());
        assertEquals(cause, exception.getCause());
    }
}
