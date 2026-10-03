package br.com.portalmanager.platform.library.messagequeue.serialization;

import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueueMessage;
import br.com.portalmanager.platform.library.messagequeue.exception.MessageSerializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MessageQueueSerializer {

    private final ObjectMapper objectMapper;

    public MessageQueueSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(MessageQueueMessage<?> message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new MessageSerializationException("Failed to serialize message queue payload", exception);
        }
    }

    public <T> MessageQueueMessage<T> deserialize(String content, Class<T> payloadType) {
        try {
            JavaType type = objectMapper.getTypeFactory()
                    .constructParametricType(MessageQueueMessage.class, payloadType);
            return objectMapper.readValue(content, type);
        } catch (JsonProcessingException exception) {
            throw new MessageSerializationException("Failed to deserialize message queue payload", exception);
        }
    }
}
