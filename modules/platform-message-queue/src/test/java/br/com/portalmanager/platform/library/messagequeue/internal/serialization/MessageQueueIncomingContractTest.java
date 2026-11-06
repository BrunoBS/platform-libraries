package br.com.portalmanager.platform.library.messagequeue.internal.serialization;

import br.com.portalmanager.platform.library.messagequeue.api.annotation.QueueMessage;
import br.com.portalmanager.platform.library.messagequeue.api.contract.MessageQueueMessage;
import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class MessageQueueIncomingContractTest {
    @QueueMessage(type="contract-test",version="1")
    public record Payload(String id) { }
    @QueueMessage(type="",version="")
    public record DefaultMetadataPayload(String id) { }
    public record PlainPayload(String id) { }
    final MessageQueueSerializer serializer=new MessageQueueSerializer(JsonMapper.builder().build());
    String message(String type,String version,Payload payload) {
        return serializer.serialize(new MessageQueueMessage<>("id",type,version,Instant.now(),"correlation",Map.of(),payload));
    }
    @Test void declaredTypeAndVersionAreAccepted() {
        assertThat(serializer.deserialize(message("contract-test","1",new Payload("test")),Payload.class,"orders").payload().id()).isEqualTo("test");
    }
    @Test void unsupportedVersionIsRejected() {
        assertThatThrownBy(()->serializer.deserialize(message("contract-test","2",new Payload("test")),Payload.class,"orders")).isInstanceOf(ApiException.class);
    }
    @Test void differentTypeIsRejected() {
        assertThatThrownBy(()->serializer.deserialize(message("other","1",new Payload("test")),Payload.class,"orders")).isInstanceOf(ApiException.class);
    }
    @Test void missingPayloadIsRejected() {
        assertThatThrownBy(()->serializer.deserialize(message("contract-test","1",null),Payload.class,"orders")).isInstanceOf(ApiException.class);
    }
    @Test void nullEnvelopeIsRejectedEvenWithoutQueueMessageAnnotation() {
        assertThatThrownBy(()->serializer.deserialize("null",PlainPayload.class,"orders")).isInstanceOf(ApiException.class);
    }
    @Test void nullPayloadIsRejectedEvenWithoutQueueMessageAnnotation() {
        assertThatThrownBy(()->serializer.deserialize("{\"payload\":null}",PlainPayload.class,"orders")).isInstanceOf(ApiException.class);
    }
    @Test void blankAnnotationMetadataUsesDestinationAndVersionOne() {
        var envelope = new MessageQueueMessage<>("id", "orders", "1", Instant.now(), "correlation", Map.of(), new DefaultMetadataPayload("test"));
        assertThat(serializer.deserialize(serializer.serialize(envelope), DefaultMetadataPayload.class, "orders").payload().id()).isEqualTo("test");
    }
    @Test void nativeS3NotificationIsNotMistakenForPlatformEnvelope() {
        assertThatThrownBy(()->serializer.deserialize("{\"Records\":[{\"eventSource\":\"aws:s3\"}]}",Payload.class,"orders"))
            .isInstanceOf(ApiException.class);
    }
}
