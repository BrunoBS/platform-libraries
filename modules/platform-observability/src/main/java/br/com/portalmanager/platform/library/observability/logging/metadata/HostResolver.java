package br.com.portalmanager.platform.library.observability.logging.metadata;

import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class HostResolver {

    private static final String HOSTNAME_PROPERTY = "HOSTNAME";
    private static final String UNKNOWN_HOST = "unknown-host";

    private HostResolver() {}

    public static String resolve(Environment environment) {
        String configuredHost = normalize(environment.getProperty(HOSTNAME_PROPERTY));
        if (configuredHost != null) {
            return configuredHost;
        }

        try {
            return resolve(environment, InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException ignored) {
            return UNKNOWN_HOST;
        }
    }

    static String resolve(Environment environment, String localHostName) {
        String configuredHost = normalize(environment.getProperty(HOSTNAME_PROPERTY));
        if (configuredHost != null) {
            return configuredHost;
        }

        String resolvedLocalHost = normalize(localHostName);
        return resolvedLocalHost != null ? resolvedLocalHost : UNKNOWN_HOST;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return value.trim();
    }
}
