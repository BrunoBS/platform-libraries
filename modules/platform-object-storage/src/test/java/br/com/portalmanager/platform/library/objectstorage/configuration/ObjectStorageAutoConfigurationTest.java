package br.com.portalmanager.platform.library.objectstorage.configuration;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectStorageClient;
import br.com.portalmanager.platform.library.objectstorage.provider.aws.configuration.AwsObjectStorageAutoConfiguration;
import br.com.portalmanager.platform.library.objectstorage.provider.azure.configuration.AzureObjectStorageAutoConfiguration;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectStorageAutoConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ObjectStorageAutoConfiguration.class,
                    AwsObjectStorageAutoConfiguration.class,
                    AzureObjectStorageAutoConfiguration.class));

    @Test
    void shouldRegisterOnlyAwsProviderBeans() {
        context.withPropertyValues(
                        "platform.object-storage.provider=AWS",
                        "platform.object-storage.region=us-east-1",
                        "platform.object-storage.destinations.audit.bucket=audit-bucket")
                .run(application -> assertThat(application).hasNotFailed()
                        .hasSingleBean(S3Client.class)
                        .hasSingleBean(S3Presigner.class)
                        .hasSingleBean(ObjectStorageClient.class)
                        .doesNotHaveBean(BlobServiceClient.class));
    }

    @Test
    void shouldRegisterOnlyAzureProviderBeans() {
        context.withPropertyValues(
                        "platform.object-storage.provider=AZURE",
                        "platform.object-storage.endpoint=https://account.blob.core.windows.net",
                        "platform.object-storage.destinations.audit.container=audit-container")
                .run(application -> assertThat(application).hasNotFailed()
                        .hasSingleBean(BlobServiceClient.class)
                        .hasSingleBean(ObjectStorageClient.class)
                        .doesNotHaveBean(S3Client.class));
    }

    @Test
    void shouldFailStartupWhenProviderIsMissing() {
        context.withPropertyValues("platform.object-storage.destinations.audit.bucket=audit-bucket")
                .run(application -> assertThat(application.getStartupFailure())
                        .hasRootCauseInstanceOf(PlatformConfigurationException.class));
    }
}
