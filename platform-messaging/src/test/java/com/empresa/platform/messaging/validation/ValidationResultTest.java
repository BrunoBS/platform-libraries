package com.empresa.platform.messaging.validation;

import com.empresa.platform.messaging.model.ValidationDetail;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidationResultTest {

    @Test
    void shouldAccumulateMessageKeysAndParameters() {
        ValidationResult result = new ValidationResult();

        result.addError("name", "account.name.required");
        result.addError(
                "type",
                "account.type.invalid",
                Map.of("0", "ADMIN, MANAGER")
        );

        assertTrue(result.hasErrors());
        assertEquals(2, result.getDetails().size());

        ValidationDetail first = result.getDetails().getFirst();
        assertEquals("name", first.field());
        assertEquals("account.name.required", first.messageKey());

        ValidationDetail second = result.getDetails().get(1);
        assertEquals("type", second.field());
        assertEquals("account.type.invalid", second.messageKey());
        assertEquals("ADMIN, MANAGER", second.parameters().get("0"));
    }

    @Test
    void shouldTreatNonKeyTextAsLiteralMessage() {
        ValidationResult result = new ValidationResult(
                "sharing",
                "Participante não pertence ao compartilhamento informado"
        );

        ValidationDetail detail = result.getDetails().getFirst();

        assertNull(detail.messageKey());
        assertEquals(
                "Participante não pertence ao compartilhamento informado",
                detail.defaultMessage()
        );
    }

    @Test
    void shouldMergeDetails() {
        ValidationResult result = new ValidationResult("name", "account.name.required");

        result.mergeDetails(List.of(
                new ValidationDetail("email", "account.email.invalid")
        ));

        assertEquals(2, result.getDetails().size());
    }
}
