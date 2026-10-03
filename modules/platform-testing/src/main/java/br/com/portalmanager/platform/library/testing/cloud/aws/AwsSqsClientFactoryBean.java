package br.com.portalmanager.platform.library.testing.cloud.aws;

import org.springframework.beans.factory.FactoryBean;
import software.amazon.awssdk.services.sqs.SqsClient;

public final class AwsSqsClientFactoryBean implements FactoryBean<SqsClient> {

    private final AwsLocalStackConnection connection;
    private SqsClient client;

    public AwsSqsClientFactoryBean(AwsLocalStackConnection connection) {
        this.connection = connection;
    }

    @Override
    public SqsClient getObject() {
        if (client == null) {
            client = SqsClient.builder()
                    .endpointOverride(connection.endpoint())
                    .region(connection.region())
                    .credentialsProvider(connection.credentialsProvider())
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
