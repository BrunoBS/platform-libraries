package br.com.portalmanager.platform.testing.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestContextTest {

    @AfterEach
    void cleanUp() {
        TestContext.reset();
    }

    @Test
    void shouldKeepCorrelationIdUntilReset() {
        String first = TestContext.correlationId();
        String second = TestContext.correlationId();

        assertThat(second).isEqualTo(first);
    }

    @Test
    void shouldCreateNewCorrelationIdAfterReset() {
        String first = TestContext.correlationId();
        TestContext.reset();

        assertThat(TestContext.correlationId()).isNotEqualTo(first);
    }
}
