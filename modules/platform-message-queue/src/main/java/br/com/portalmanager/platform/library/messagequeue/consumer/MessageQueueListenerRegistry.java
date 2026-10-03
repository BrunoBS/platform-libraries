package br.com.portalmanager.platform.library.messagequeue.consumer;

import br.com.portalmanager.platform.library.messagequeue.annotation.MessageQueueDeadLetterListener;
import br.com.portalmanager.platform.library.messagequeue.annotation.MessageQueueListener;
import br.com.portalmanager.platform.library.messagequeue.contract.DeadLetterMessage;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueueMessage;
import br.com.portalmanager.platform.library.messagequeue.exception.MessageConsumeException;
import br.com.portalmanager.platform.library.messagequeue.exception.MessageQueueConfigurationException;
import org.springframework.beans.BeansException;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class MessageQueueListenerRegistry implements BeanPostProcessor {

    private final Map<String, ListenerDefinition> listeners = new LinkedHashMap<>();
    private final Map<String, DeadLetterListenerDefinition> deadLetterListeners = new LinkedHashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        for (Method method : targetClass.getMethods()) {
            MessageQueueListener listener = AnnotatedElementUtils.findMergedAnnotation(method, MessageQueueListener.class);
            MessageQueueDeadLetterListener deadLetterListener =
                    AnnotatedElementUtils.findMergedAnnotation(method, MessageQueueDeadLetterListener.class);

            if (listener != null && deadLetterListener != null) {
                throw new MessageQueueConfigurationException(
                        "Message queue method cannot be both listener and dead-letter listener: " + method.getName());
            }
            if (listener != null) {
                registerListener(listener.value(), bean, method);
            }
            if (deadLetterListener != null) {
                registerDeadLetterListener(deadLetterListener.value(), bean, method);
            }
        }
        return bean;
    }

    public Collection<ListenerDefinition> listeners() {
        return listeners.values();
    }

    public Collection<DeadLetterListenerDefinition> deadLetterListeners() {
        return deadLetterListeners.values();
    }

    private void registerListener(String destination, Object bean, Method method) {
        validateDestination(destination, "listener");
        if (listeners.containsKey(destination)) {
            throw new MessageQueueConfigurationException(
                    "Duplicate message queue listener for destination: " + destination);
        }
        Class<?> payloadType = resolvePayloadType(method, destination, MessageQueueMessage.class, "MessageQueueMessage<T>");
        Method invocableMethod = selectInvocableMethod(method, bean, destination);
        listeners.put(destination, new ListenerDefinition(destination, bean, invocableMethod, payloadType));
    }

    private void registerDeadLetterListener(String destination, Object bean, Method method) {
        validateDestination(destination, "dead-letter listener");
        if (deadLetterListeners.containsKey(destination)) {
            throw new MessageQueueConfigurationException(
                    "Duplicate message queue dead-letter listener for destination: " + destination);
        }
        Class<?> payloadType = resolvePayloadType(method, destination, DeadLetterMessage.class, "DeadLetterMessage<T>");
        Method invocableMethod = selectInvocableMethod(method, bean, destination);
        deadLetterListeners.put(destination, new DeadLetterListenerDefinition(destination, bean, invocableMethod, payloadType));
    }

    private Method selectInvocableMethod(Method method, Object bean, String destination) {
        try {
            return AopUtils.selectInvocableMethod(method, bean.getClass());
        } catch (IllegalStateException exception) {
            throw new MessageQueueConfigurationException(
                    "Message queue listener for destination '" + destination
                            + "' must be exposed by the Spring proxy: " + method.getName(),
                    exception);
        }
    }

    private void validateDestination(String destination, String listenerType) {
        if (destination == null || destination.isBlank()) {
            throw new MessageQueueConfigurationException(
                    "Message queue " + listenerType + " destination is required");
        }
    }

    private Class<?> resolvePayloadType(
            Method method,
            String destination,
            Class<?> expectedRawType,
            String expectedTypeDescription) {
        if (method.getParameterCount() != 1) {
            throw invalidListener(destination, "listener must declare exactly one parameter");
        }
        Type parameterType = method.getGenericParameterTypes()[0];
        if (!(parameterType instanceof ParameterizedType parameterizedType)
                || parameterizedType.getRawType() != expectedRawType) {
            throw invalidListener(destination, "listener parameter must be " + expectedTypeDescription);
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
        return new MessageQueueConfigurationException(
                "Invalid message queue listener for destination '" + destination + "': " + reason);
    }

    private static void invoke(Object bean, Method method, Object message, String destination) {
        try {
            method.invoke(bean, message);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new MessageConsumeException(
                    "Listener invocation failed for destination: " + destination,
                    cause == null ? exception : cause);
        } catch (IllegalAccessException exception) {
            throw new MessageConsumeException(
                    "Listener invocation is not accessible for destination: " + destination,
                    exception);
        }
    }

    public record ListenerDefinition(String destination, Object bean, Method method, Class<?> payloadType) {
        public void invoke(MessageQueueMessage<?> message) {
            MessageQueueListenerRegistry.invoke(bean, method, message, destination);
        }
    }

    public record DeadLetterListenerDefinition(String destination, Object bean, Method method, Class<?> payloadType) {
        public void invoke(DeadLetterMessage<?> message) {
            MessageQueueListenerRegistry.invoke(bean, method, message, destination);
        }
    }
}
