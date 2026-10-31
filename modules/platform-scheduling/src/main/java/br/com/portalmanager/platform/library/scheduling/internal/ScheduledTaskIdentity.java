package br.com.portalmanager.platform.library.scheduling.internal;

import br.com.portalmanager.platform.library.scheduling.task.PlatformScheduledTask;
import org.springframework.aop.support.AopUtils;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.core.ReactiveAdapterRegistry;

final class ScheduledTaskIdentity {
    private ScheduledTaskIdentity() {}

    static String from(Runnable task, String serviceName, String scheduleDiscriminator) {
        if (task instanceof PlatformScheduledTask programmaticTask) {
            String taskName = programmaticTask.lockName();
            if (taskName == null || taskName.isBlank() || !taskName.equals(taskName.trim())) {
                throw new IllegalStateException("Programmatic scheduled task must provide a non-blank stable lock name");
            }
            return serviceName + ":programmatic:" + taskName + ":" + scheduleDiscriminator;
        }
        if (!(task instanceof ScheduledMethodRunnable scheduledMethod)) {
            if (task instanceof SchedulingAwareRunnable) {
                return fromSpringDisplayName(task.toString(), serviceName, scheduleDiscriminator);
            }
            throw unsupportedTask(task);
        }
        Class<?> targetClass = AopUtils.getTargetClass(scheduledMethod.getTarget());
        java.lang.reflect.Method method = AopUtils.getMostSpecificMethod(
                scheduledMethod.getMethod(), targetClass);
        assertSynchronous(method);
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

        try {
            java.lang.reflect.Method method = Class.forName(className).getDeclaredMethod(methodName);
            assertSynchronous(method);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            // Spring's public display name remains the identity source for wrappers.
        }

        return serviceName + ":" + className + "#" + methodName + "():" + scheduleDiscriminator;
    }

    private static void assertSynchronous(java.lang.reflect.Method method) {
        if (ReactiveAdapterRegistry.getSharedInstance().getAdapter(method.getReturnType()) != null) {
            throw new IllegalStateException("Reactive @Scheduled methods are unsupported by distributed locking: "
                    + method.getDeclaringClass().getName() + "#" + method.getName());
        }
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
