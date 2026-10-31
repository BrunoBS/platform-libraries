package br.com.portalmanager.platform.library.scheduling.task;

/**
 * Programmatic scheduled task with an explicit, stable distributed-lock identity.
 *
 * <p>The identity must remain the same across every application instance that can run
 * the same logical task.
 */
public interface PlatformScheduledTask extends Runnable {
    String lockName();
}
