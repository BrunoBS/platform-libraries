package br.com.portalmanager.platform.observability.logging.metadata;

import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class BuildVersionResolver {

    private static final String INFO_BUILD_VERSION = "info.build.version";
    private static final String BUILD_INFO_RESOURCE = "META-INF/build-info.properties";
    private static final String BUILD_VERSION = "build.version";
    private static final String UNKNOWN_VERSION = "unknown";

    private BuildVersionResolver() {}

    public static String resolve(Environment environment) {
        return resolve(environment, new ClassPathResource(BUILD_INFO_RESOURCE));
    }

    static String resolve(Environment environment, Resource buildInfoResource) {
        String configuredVersion = normalize(environment.getProperty(INFO_BUILD_VERSION));
        if (configuredVersion != null) {
            return configuredVersion;
        }

        String buildInfoVersion = readBuildInfoVersion(buildInfoResource);
        return buildInfoVersion != null ? buildInfoVersion : UNKNOWN_VERSION;
    }

    private static String readBuildInfoVersion(Resource resource) {
        if (resource == null || !resource.exists()) {
            return null;
        }

        Properties properties = new Properties();
        try (InputStream inputStream = resource.getInputStream()) {
            properties.load(inputStream);
            return normalize(properties.getProperty(BUILD_VERSION));
        } catch (IOException ignored) {
            return null;
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
