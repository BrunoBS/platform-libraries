package br.com.portalmanager.platform.library.messagequeue.exception;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messagequeue.message.MessageQueueMessageKeys;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessageQueueExceptionTest {

    @Test
    void shouldPreserveMessageKeyParametersAndCause() {
        var cause = new IllegalStateException("broker unavailable");
        var exception = new MessagePublishException(
                MessageQueueMessageKeys.PUBLISH_FAILED,
                Map.of("0", "orders"),
                cause);

        assertThat(exception).isInstanceOf(ApiException.class);
        assertThat(exception.getMessageKey()).isEqualTo(MessageQueueMessageKeys.PUBLISH_FAILED);
        assertThat(exception.getParameters()).containsEntry("0", "orders");
        assertThat(exception.getCause()).isSameAs(cause);
    }
}
