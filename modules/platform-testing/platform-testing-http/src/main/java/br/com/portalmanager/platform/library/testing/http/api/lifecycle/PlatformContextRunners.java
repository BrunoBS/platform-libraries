package br.com.portalmanager.platform.library.testing.http.api.lifecycle;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Creates isolated Spring contexts for configuration tests without starting the application. */
public final class PlatformContextRunners {

    private PlatformContextRunners() {}

    public static ApplicationContextRunner forUserConfiguration(Class<?>... configurations) {
        return new ApplicationContextRunner().withUserConfiguration(configurations);
    }
}
