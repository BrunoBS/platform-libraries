package br.com.portalmanager.platform.messaging.exception;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ExceptionCoverageTest {

    @Test
    void testExceptionsConstructors() {
        Throwable cause = new RuntimeException("Causa original");
        Map<String, Object> params = Map.of("id", 123);

        // Testa ValidationException
        assertNotNull(new ValidationException("KEY"));
        assertNotNull(new ValidationException("KEY", cause));
        assertNotNull(new ValidationException("KEY", params));
        assertNotNull(new ValidationException("KEY", params, cause));

        // Testa ConflictException
        assertNotNull(new ConflictException("KEY"));
        assertNotNull(new ConflictException("KEY", cause));
        assertNotNull(new ConflictException("KEY", params));
        assertNotNull(new ConflictException("KEY", params, cause));

        // Testa NotFoundException
        assertNotNull(new NotFoundException("KEY"));
        assertNotNull(new NotFoundException("KEY", cause));
        assertNotNull(new NotFoundException("KEY", params));
        assertNotNull(new NotFoundException("KEY", params, cause));

        // Testa UnauthorizedException
        assertNotNull(new UnauthorizedException("KEY"));
        assertNotNull(new UnauthorizedException("KEY", cause));
        assertNotNull(new UnauthorizedException("KEY", params));
        assertNotNull(new UnauthorizedException("KEY", params, cause));

        // Testa ForbiddenException
        assertNotNull(new ForbiddenException("KEY"));
        assertNotNull(new ForbiddenException("KEY", cause));
        assertNotNull(new ForbiddenException("KEY", params));
        assertNotNull(new ForbiddenException("KEY", params, cause));
    }
}
