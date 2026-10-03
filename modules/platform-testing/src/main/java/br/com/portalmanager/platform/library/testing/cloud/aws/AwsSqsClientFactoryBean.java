package br.com.portalmanager.platform.library.testing.cloud.aws;

import org.springframework.beans.factory.FactoryBean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

public final class AwsSqsClientFactoryBean implements FactoryBean<SqsClient> {

    private final AwsLocalStackContainer container;
    private SqsClient client;

    public AwsSqsClientFactoryBean(AwsLocalStackContainer container) {
        this.container = container;
    }

    @Override
    public SqsClient getObject() {
        if (client == null) {
            client = SqsClient.builder()
                    .endpointOverride(container.getEndpoint())
                    .region(Region.of(container.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(container.getAccessKey(), container.getSecretKey())))
                    .build();
        }
        return client;
    }

    @Override
    public Class<?> getObjectType() {
        return SqsClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
