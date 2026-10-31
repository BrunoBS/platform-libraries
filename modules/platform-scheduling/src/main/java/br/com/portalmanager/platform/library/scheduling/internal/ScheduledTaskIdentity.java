package br.com.portalmanager.platform.library.scheduling.internal;

import org.springframework.aop.support.AopUtils;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.util.ClassUtils;

final class ScheduledTaskIdentity {
    private ScheduledTaskIdentity() {}

    static String from(Runnable task, String serviceName, String scheduleDiscriminator) {
        if (!(task instanceof ScheduledMethodRunnable scheduledMethod)) {
            throw new IllegalStateException("Cannot create a stable distributed-lock identity for scheduled task type "
                    + task.getClass().getName()
                    + "; automatic locking is limited to Spring ScheduledMethodRunnable in this POC");
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
}
