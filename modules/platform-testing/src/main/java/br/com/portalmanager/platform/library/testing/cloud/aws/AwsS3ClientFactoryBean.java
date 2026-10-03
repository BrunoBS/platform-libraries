package br.com.portalmanager.platform.library.testing.cloud.aws;

import org.springframework.beans.factory.FactoryBean;
import software.amazon.awssdk.services.s3.S3Client;

public final class AwsS3ClientFactoryBean implements FactoryBean<S3Client> {

    private final AwsLocalStackConnection connection;
    private S3Client client;

    public AwsS3ClientFactoryBean(AwsLocalStackConnection connection) {
        this.connection = connection;
    }

    @Override
    public S3Client getObject() {
        if (client == null) {
            client = S3Client.builder()
                    .endpointOverride(connection.endpoint())
                    .region(connection.region())
                    .credentialsProvider(connection.credentialsProvider())
                    .forcePathStyle(true)
                    .build();
        }
        return client;
    }

    @Override
    public Class<?> getObjectType() {
        return S3Client.class;
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
