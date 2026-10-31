package br.com.portalmanager.platform.library.objectstorage.provider.aws.configuration;

import br.com.portalmanager.platform.library.objectstorage.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectStorageClient;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.objectstorage.provider.aws.S3ObjectStorageClient;
import br.com.portalmanager.platform.library.objectstorage.message.ObjectStorageTechnicalErrors;
import java.net.URI;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@AutoConfiguration
@ConditionalOnProperty(prefix = "platform.object-storage", name = "provider", havingValue = "AWS")
public class AwsObjectStorageAutoConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(S3Client.class)
    S3Client objectStorageS3Client(ObjectStorageProperties properties) {
        try {
            S3ClientBuilder builder = S3Client.builder().region(Region.of(properties.getRegion()));
            if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
                builder.endpointOverride(URI.create(properties.getEndpoint()));
            }
            return builder.build();
        } catch (RuntimeException exception) {
            throw new PlatformConfigurationException(
                    ObjectStorageTechnicalErrors.invalidConfiguration("AWS region or endpoint is invalid"), exception);
        }
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(S3Presigner.class)
    S3Presigner objectStorageS3Presigner(ObjectStorageProperties properties) {
        try {
            S3Presigner.Builder builder = S3Presigner.builder().region(Region.of(properties.getRegion()));
            if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
                builder.endpointOverride(URI.create(properties.getEndpoint()));
            }
            return builder.build();
        } catch (RuntimeException exception) {
            throw new PlatformConfigurationException(
                    ObjectStorageTechnicalErrors.invalidConfiguration("AWS object storage signing configuration is invalid"), exception);
        }
    }

    @Bean
    @ConditionalOnMissingBean(ObjectStorageClient.class)
    ObjectStorageClient awsObjectStorageClient(S3Client client, S3Presigner presigner, ObjectStorageProperties properties) {
        return new S3ObjectStorageClient(client, presigner, properties);
    }
}
