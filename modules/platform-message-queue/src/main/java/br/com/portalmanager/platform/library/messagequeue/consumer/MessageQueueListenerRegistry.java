package br.com.portalmanager.platform.library.messagequeue.consumer;

import br.com.portalmanager.platform.library.messagequeue.annotation.MessageQueueListener;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueueMessage;
import br.com.portalmanager.platform.library.messagequeue.exception.MessageQueueConfigurationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class MessageQueueListenerRegistry implements BeanPostProcessor {

    private final Map<String, ListenerDefinition> listeners = new LinkedHashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        for (Method method : bean.getClass().getMethods()) {
            MessageQueueListener annotation = AnnotatedElementUtils.findMergedAnnotation(method, MessageQueueListener.class);
            if (annotation != null) {
                register(annotation.value(), bean, method);
            }
        }
        return bean;
    }

    public Collection<ListenerDefinition> listeners() {
        return listeners.values();
    }

    private void register(String destination, Object bean, Method method) {
        if (destination == null || destination.isBlank()) {
            throw new MessageQueueConfigurationException("Message queue listener destination is required");
        }
        if (listeners.containsKey(destination)) {
            throw new MessageQueueConfigurationException("Duplicate message queue listener for destination: " + destination);
        }
        Class<?> payloadType = resolvePayloadType(method, destination);
        listeners.put(destination, new ListenerDefinition(destination, bean, method, payloadType));
    }

    private Class<?> resolvePayloadType(Method method, String destination) {
        if (method.getParameterCount() != 1) {
            throw invalidListener(destination, "listener must declare exactly one parameter");
        }
        Type parameterType = method.getGenericParameterTypes()[0];
        if (!(parameterType instanceof ParameterizedType parameterizedType)
                || parameterizedType.getRawType() != MessageQueueMessage.class) {
            throw invalidListener(destination, "listener parameter must be MessageQueueMessage<T>");
        }
        Type payloadType = parameterizedType.getActualTypeArguments()[0];
        if (!(payloadType instanceof Class<?> payloadClass)) {
            throw invalidListener(destination, "listener payload type must be a concrete class");
        }
        if (method.getReturnType() != Void.TYPE) {
            throw invalidListener(destination, "listener return type must be void");
        }
        return payloadClass;
    }

    private MessageQueueConfigurationException invalidListener(String destination, String reason) {
        return new MessageQueueConfigurationException("Invalid message queue listener for destination '" + destination + "': " + reason);
    }

    public record ListenerDefinition(String destination, Object bean, Method method, Class<?> payloadType) {
        public void invoke(MessageQueueMessage<?> message) throws ReflectiveOperationException {
            method.invoke(bean, message);
        }
    }
}
