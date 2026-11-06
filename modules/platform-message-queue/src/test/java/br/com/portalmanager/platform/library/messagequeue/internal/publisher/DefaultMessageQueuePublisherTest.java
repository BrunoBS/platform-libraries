package br.com.portalmanager.platform.library.messagequeue.internal.publisher;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messagequeue.internal.message.MessageQueueMessageKeys;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultMessageQueuePublisherTest {

    @Test
    void rejectsNullPayloadAsValidationFailure() {
        var publisher = new DefaultMessageQueuePublisher(null, null, null, null);

        assertThatThrownBy(() -> publisher.publish("orders", null))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.getMessageKey())
                                .isEqualTo(MessageQueueMessageKeys.PUBLISH_PAYLOAD_INVALID));
    }
}
