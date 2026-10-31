package br.com.portalmanager.platform.library.messagequeue.serialization;

import br.com.portalmanager.platform.library.messagequeue.annotation.QueueMessage;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueueMessage;
import br.com.portalmanager.platform.library.messagequeue.exception.MessageSerializationException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class MessageQueueIncomingContractTest {
    @QueueMessage(type="contract-test",version="1")
    public record Payload(String id) { }
    final MessageQueueSerializer serializer=new MessageQueueSerializer(JsonMapper.builder().build());
    String message(String type,String version,Payload payload) {
        return serializer.serialize(new MessageQueueMessage<>("id",type,version,Instant.now(),"correlation",Map.of(),payload));
    }
    @Test void declaredTypeAndVersionAreAccepted() {
        assertThat(serializer.deserialize(message("contract-test","1",new Payload("test")),Payload.class).payload().id()).isEqualTo("test");
    }
    @Test void unsupportedVersionIsRejected() {
        assertThatThrownBy(()->serializer.deserialize(message("contract-test","2",new Payload("test")),Payload.class)).isInstanceOf(MessageSerializationException.class);
    }
    @Test void differentTypeIsRejected() {
        assertThatThrownBy(()->serializer.deserialize(message("other","1",new Payload("test")),Payload.class)).isInstanceOf(MessageSerializationException.class);
    }
    @Test void missingPayloadIsRejected() {
        assertThatThrownBy(()->serializer.deserialize(message("contract-test","1",null),Payload.class)).isInstanceOf(MessageSerializationException.class);
    }
    @Test void nativeS3NotificationIsNotMistakenForPlatformEnvelope() {
        assertThatThrownBy(()->serializer.deserialize("{\"Records\":[{\"eventSource\":\"aws:s3\"}]}",Payload.class))
            .isInstanceOf(MessageSerializationException.class);
    }
}
