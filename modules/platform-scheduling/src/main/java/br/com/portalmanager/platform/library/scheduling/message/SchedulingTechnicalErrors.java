package br.com.portalmanager.platform.library.scheduling.message;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class SchedulingTechnicalErrors {
    public static final PlatformErrorDefinition LOCK_DISABLED = new PlatformErrorDefinition(
            "PLT-SCHED-001",
            "Distributed scheduling lock is disabled",
            "Enable golden.scheduling.distributed-lock.enabled to coordinate service instances.",
            200);

    public static final PlatformErrorDefinition REDIS_UNAVAILABLE = new PlatformErrorDefinition(
            "PLT-SCHED-002",
            "Distributed scheduling lock Redis is unavailable",
            "Check the Redis connection and retry the service startup.",
            500);

    public static final PlatformErrorDefinition REACTIVE_TASK_RETURNED_NULL = new PlatformErrorDefinition(
            "PLT-SCHED-005",
            "A reactive @Scheduled method returned null",
            "Return a non-null reactive Publisher from every reactive @Scheduled method.",
            500);

    public static final PlatformErrorDefinition SCHEDULER_NOT_INITIALIZED = new PlatformErrorDefinition(
            "PLT-SCHED-006",
            "The platform task scheduler is not initialized",
            "Use PlatformTaskScheduler from an application context with platform scheduling enabled.",
            500);

    private SchedulingTechnicalErrors() {}

    public static PlatformErrorDefinition invalidConfiguration(String reason) {
        return new PlatformErrorDefinition(
                "PLT-SCHED-003",
                "Invalid distributed scheduling lock configuration: " + reason,
                "Review golden.scheduling.distributed-lock and spring.application.name.",
                500);
    }

    public static PlatformErrorDefinition taskIdentityUnavailable(String reason) {
        return new PlatformErrorDefinition(
                "PLT-SCHED-004",
                "A stable distributed-lock identity could not be determined for the scheduled task: " + reason,
                "Use PlatformTaskScheduler with a stable task name, or a Spring @Scheduled method with a stable class and method name.",
                500);
    }
}
