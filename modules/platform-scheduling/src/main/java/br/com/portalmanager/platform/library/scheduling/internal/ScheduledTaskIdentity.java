package br.com.portalmanager.platform.library.scheduling.internal;

import br.com.portalmanager.platform.library.scheduling.annotation.DisableScheduledLock;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.scheduling.message.SchedulingTechnicalErrors;
import br.com.portalmanager.platform.library.scheduling.task.PlatformScheduledTask;
import java.lang.reflect.Method;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

final class ScheduledTaskIdentity {
    private ScheduledTaskIdentity() {}

    static String from(Runnable task, String scheduleDiscriminator) {
        if (task instanceof PlatformScheduledTask programmaticTask) {
            String taskName = programmaticTask.lockName();
            if (taskName == null || taskName.isBlank() || !taskName.equals(taskName.trim())) {
                throw taskIdentityFailure("programmatic tasks require a non-blank stable lock name");
            }
            return "programmatic:" + taskName + ":" + scheduleDiscriminator;
        }
        Method method = methodOf(task);
        if (method == null) {
            if (task instanceof SchedulingAwareRunnable) {
                return fromSpringDisplayName(task.toString(), scheduleDiscriminator);
            }
            throw unsupportedTask(task);
        }
        return method.getDeclaringClass().getName() + "#"
                + method.getName() + "("
                + java.util.Arrays.stream(method.getParameterTypes())
                        .map(Class::getName)
                        .collect(java.util.stream.Collectors.joining(","))
                + "):" + scheduleDiscriminator;
    }

    static boolean disablesLock(Runnable task) {
        Method method = methodOf(task);
        return method != null && AnnotatedElementUtils.hasAnnotation(method, DisableScheduledLock.class);
    }

    private static Method methodOf(Runnable task) {
        if (task instanceof ScheduledMethodRunnable scheduledMethod) {
            Class<?> targetClass = AopUtils.getTargetClass(scheduledMethod.getTarget());
            return AopUtils.getMostSpecificMethod(scheduledMethod.getMethod(), targetClass);
        }
        String displayName = task.toString();
        int methodSeparator = displayName.lastIndexOf('.');
        if (methodSeparator <= 0 || methodSeparator == displayName.length() - 1) {
            return null;
        }
        String className = displayName.substring(0, methodSeparator);
        String methodName = displayName.substring(methodSeparator + 1);
        try {
            Class<?> targetClass = Class.forName(className);
            return java.util.Arrays.stream(targetClass.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(methodName)
                            && candidate.getParameterCount() == 0)
                    .findFirst()
                    .orElse(null);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static String fromSpringDisplayName(
            String displayName, String scheduleDiscriminator) {
        int methodSeparator = displayName.lastIndexOf('.');
        if (methodSeparator <= 0 || methodSeparator == displayName.length() - 1) {
            throw taskIdentityFailure("Spring did not provide a stable class and method display name");
        }

        String className = displayName.substring(0, methodSeparator);
        String methodName = displayName.substring(methodSeparator + 1);
        if (className.contains("$$") || className.matches(".*\\$Proxy[0-9]+.*")
                || !isJavaIdentifier(methodName)) {
            throw taskIdentityFailure("Spring provided an unstable task display name: " + displayName);
        }

        Method method = methodOfDisplayName(className, methodName);
        if (method != null) {
            return method.getDeclaringClass().getName() + "#"
                    + method.getName() + "():" + scheduleDiscriminator;
        }

        return className + "#" + methodName + "():" + scheduleDiscriminator;
    }

    private static Method methodOfDisplayName(String className, String methodName) {
        try {
            Class<?> targetClass = Class.forName(className);
            return java.util.Arrays.stream(targetClass.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(methodName)
                            && candidate.getParameterCount() == 0)
                    .findFirst()
                    .orElse(null);
        } catch (ClassNotFoundException ignored) {
            return null;
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

    private static PlatformConfigurationException unsupportedTask(Runnable task) {
        return taskIdentityFailure("unsupported scheduled task type " + task.getClass().getName());
    }

    private static PlatformConfigurationException taskIdentityFailure(String reason) {
        return new PlatformConfigurationException(SchedulingTechnicalErrors.taskIdentityUnavailable(reason));
    }
}
