package br.com.portalmanager.platform.library.messagequeue.contract;

import br.com.portalmanager.platform.library.messagequeue.exception.MessagePublishException;
import br.com.portalmanager.platform.library.messagequeue.message.MessageQueueMessageKeys;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageQueuePublisherTest {

    @Test
    void shouldUseLibraryExceptionWhenPublisherDoesNotSupportOrderedOptions() {
        MessageQueuePublisher publisher = new MessageQueuePublisher() {
            @Override
            public void publish(String destination, Object payload) {
            }

            @Override
            public void publish(
                    String destination,
                    Object payload,
                    String correlationId,
                    Map<String, String> headers) {
            }
        };

        var options = new MessageQueuePublishOptions(null, Map.of(), "orders", null);

        assertThatThrownBy(() -> publisher.publish("orders", new Object(), options))
                .isInstanceOfSatisfying(
                        MessagePublishException.class,
                        exception -> assertThat(exception.getMessageKey())
                                .isEqualTo(MessageQueueMessageKeys.PUBLISH_OPTIONS_UNSUPPORTED));
    }
}
