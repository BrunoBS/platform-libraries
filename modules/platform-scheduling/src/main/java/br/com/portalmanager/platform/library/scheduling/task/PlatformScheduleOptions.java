package br.com.portalmanager.platform.library.scheduling.task;

import java.util.Objects;

/** Options that control whether a programmatic scheduled task uses a distributed lock. */
public final class PlatformScheduleOptions {
    private static final PlatformScheduleOptions WITHOUT_LOCK = new PlatformScheduleOptions(null, false);

    private final String lockName;
    private final boolean lockEnabled;

    private PlatformScheduleOptions(String lockName, boolean lockEnabled) {
        this.lockName = lockName;
        this.lockEnabled = lockEnabled;
    }

    /** Creates options for a task protected by a distributed lock with the supplied stable name. */
    public static PlatformScheduleOptions withLock(String lockName) {
        Objects.requireNonNull(lockName, "lockName");
        if (lockName.isBlank() || !lockName.equals(lockName.trim())) {
            throw new IllegalArgumentException("lockName must be non-blank and must not contain surrounding whitespace");
        }
        return new PlatformScheduleOptions(lockName, true);
    }

    /** Creates options for a task that runs without acquiring a distributed lock. */
    public static PlatformScheduleOptions withoutLock() {
        return WITHOUT_LOCK;
    }

    String lockName() {
        return lockName;
    }

    boolean lockEnabled() {
        return lockEnabled;
    }
}
