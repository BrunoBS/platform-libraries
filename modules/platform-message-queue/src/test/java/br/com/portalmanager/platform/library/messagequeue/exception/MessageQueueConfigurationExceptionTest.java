package br.com.portalmanager.platform.library.messagequeue.exception;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageQueueConfigurationExceptionTest {

    @Test
    void shouldUseThePlatformConfigurationErrorContractAndKeepTheCause() {
        var cause = new IllegalStateException("invalid listener definition");
        var exception = new MessageQueueConfigurationException("listener configuration is invalid", cause);

        assertThat(exception).isInstanceOf(PlatformConfigurationException.class);
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getErrorResponse().code()).isEqualTo("PLT-MQ-001");
        assertThat(exception.getErrorResponse().message()).contains("listener configuration is invalid");
        assertThat(exception.getErrorResponse().solution()).isNotBlank();
    }
}
