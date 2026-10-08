package br.com.portalmanager.platform.library.testing.cloud.aws;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.util.Objects;

public final class AwsLocalStackConnection {

    private final URI endpoint;
    private final Region region;
    private final AwsCredentialsProvider credentialsProvider;

    public AwsLocalStackConnection(AwsLocalStackContainer container) {
        Objects.requireNonNull(container, "container must not be null");
        this.endpoint = container.getEndpoint();
        this.region = Region.of(container.getRegion());
        this.credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(container.getAccessKey(), container.getSecretKey()));
    }

    public URI endpoint() {
        return endpoint;
    }

    public Region region() {
        return region;
    }

    public AwsCredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }
}
