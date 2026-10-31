package br.com.portalmanager.platform.library.scheduling.internal;

import org.springframework.aop.support.AopUtils;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.util.ClassUtils;

final class ScheduledTaskIdentity {
    private ScheduledTaskIdentity() {}

    static String from(Runnable task, String serviceName, String scheduleDiscriminator) {
        if (!(task instanceof ScheduledMethodRunnable scheduledMethod)) {
            if (task instanceof SchedulingAwareRunnable) {
                return fromSpringDisplayName(task.toString(), serviceName, scheduleDiscriminator);
            }
            throw unsupportedTask(task);
        }
        Class<?> targetClass = ClassUtils.getUserClass(scheduledMethod.getTarget());
        java.lang.reflect.Method method = AopUtils.getMostSpecificMethod(
                scheduledMethod.getMethod(), targetClass);
        return serviceName + ":" + method.getDeclaringClass().getName() + "#"
                + method.getName() + "("
                + java.util.Arrays.stream(method.getParameterTypes())
                        .map(Class::getName)
                        .collect(java.util.stream.Collectors.joining(","))
                + "):" + scheduleDiscriminator;
    }

    private static String fromSpringDisplayName(
            String displayName, String serviceName, String scheduleDiscriminator) {
        int methodSeparator = displayName.lastIndexOf('.');
        if (methodSeparator <= 0 || methodSeparator == displayName.length() - 1) {
            throw new IllegalStateException("Spring scheduled task has no stable class and method display name");
        }

        String className = displayName.substring(0, methodSeparator);
        String methodName = displayName.substring(methodSeparator + 1);
        if (className.contains("$$") || className.matches(".*\\$Proxy[0-9]+.*")
                || !isJavaIdentifier(methodName)) {
            throw new IllegalStateException("Spring scheduled task display name is unstable: " + displayName);
        }

        return serviceName + ":" + className + "#" + methodName + "():" + scheduleDiscriminator;
    }

    private static boolean isJavaIdentifier(String value) {
        if (value.isEmpty() || !Character.isJavaIdentifierStart(value.charAt(0))) {
            return false;
        }
        for (int index = 1; index < value.length(); index++) {
            if (!Character.isJavaIdentifierPart(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static IllegalStateException unsupportedTask(Runnable task) {
        return new IllegalStateException("Cannot create a stable distributed-lock identity for scheduled task type "
                + task.getClass().getName());
    }
}
