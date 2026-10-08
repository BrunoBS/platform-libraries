package br.com.portalmanager.platform.library.testing.container;

import org.testcontainers.utility.DockerImageName;

/**
 * Default Docker images exercised by the platform-testing fixtures.
 * Consumers can override these defaults in the corresponding annotations.
 */

public final class PlatformTestingContainerImages {

    public static final String KAFKA = "confluentinc/cp-kafka:7.8.0";
    public static final String AWS_LOCALSTACK = "localstack/localstack:4.14.0";
    public static final String AZURITE = "mcr.microsoft.com/azure-storage/azurite:3.37.0";
    public static final String AZURE_SERVICE_BUS = "mcr.microsoft.com/azure-messaging/servicebus-emulator:1.1.2";
    public static final String AZURE_SQL_SERVER = "mcr.microsoft.com/mssql/server:2022-CU14-ubuntu-22.04";

    /**
     * Parses a version-pinned image reference. Floating tags such as {@code latest}
     * are rejected so test behavior does not change without a source change.
     */
    public static DockerImageName parse(String image) {
        if (image == null || image.isBlank()) {
            throw new IllegalArgumentException("Docker image must not be blank");
        }

        String value = image.trim();
        int lastSlash = value.lastIndexOf('/');
        int lastColon = value.lastIndexOf(':');
        boolean hasTag = lastColon > lastSlash;
        boolean hasDigest = value.contains("@sha256:");
        if (!hasTag && !hasDigest) {
            throw new IllegalArgumentException("Docker image must include an explicit tag or digest");
        }
        if (hasTag && "latest".equalsIgnoreCase(value.substring(lastColon + 1))) {
            throw new IllegalArgumentException("Floating Docker image tag 'latest' is not supported");
        }
        return DockerImageName.parse(value);
    }

    private PlatformTestingContainerImages() {
    }
}
