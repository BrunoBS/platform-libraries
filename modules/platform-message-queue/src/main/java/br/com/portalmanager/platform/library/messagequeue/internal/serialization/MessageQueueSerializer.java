package br.com.portalmanager.platform.library.messagequeue.internal.serialization;

import br.com.portalmanager.platform.library.messagequeue.api.contract.MessageQueueMessage;
import br.com.portalmanager.platform.library.messaging.api.exception.ApiException;
import br.com.portalmanager.platform.library.messagequeue.internal.message.MessageQueueMessageKeys;
import br.com.portalmanager.platform.library.messagequeue.internal.message.MessageMetadataResolver;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

public class MessageQueueSerializer {

    private final ObjectMapper objectMapper;

    public MessageQueueSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(MessageQueueMessage<?> message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JacksonException exception) {
            throw new ApiException(MessageQueueMessageKeys.SERIALIZATION_FAILED, exception);
        }
    }

    public <T> MessageQueueMessage<T> deserialize(String content, Class<T> payloadType, String destination) {
        try {
            JavaType type = objectMapper.getTypeFactory()
                    .constructParametricType(MessageQueueMessage.class, payloadType);
            MessageQueueMessage<T> message = objectMapper.readValue(content, type);
            if (message == null || message.payload() == null) {
                throw new ApiException(MessageQueueMessageKeys.DESERIALIZATION_FAILED);
            }
            if (payloadType.isAnnotationPresent(
                    br.com.portalmanager.platform.library.messagequeue.api.annotation.QueueMessage.class)) {
                var expected = MessageMetadataResolver.resolve(destination, payloadType);
                if (!expected.type().equals(message.messageType())
                        || !expected.version().equals(message.messageVersion())) {
                    throw new ApiException(MessageQueueMessageKeys.DESERIALIZATION_FAILED);
                }
            }
            return message;
        } catch (JacksonException exception) {
            throw new ApiException(MessageQueueMessageKeys.DESERIALIZATION_FAILED, exception);
        }
    }
}
