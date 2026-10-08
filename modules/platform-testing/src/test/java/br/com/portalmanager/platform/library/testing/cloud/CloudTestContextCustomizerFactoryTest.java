package br.com.portalmanager.platform.library.testing.cloud;

import br.com.portalmanager.platform.library.testing.cloud.aws.annotation.WithAwsLocalStack;
import br.com.portalmanager.platform.library.testing.cloud.aws.sqs.annotation.AwsSqs;
import br.com.portalmanager.platform.library.testing.cloud.azure.annotation.WithAzureEmulator;
import br.com.portalmanager.platform.library.testing.cloud.azure.servicebus.annotation.AzureServiceBus;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CloudTestContextCustomizerFactoryTest {

    private final CloudTestContextCustomizerFactory factory = new CloudTestContextCustomizerFactory();

    @Test
    void shouldIncludeCloudAnnotationConfigurationInContextCacheKey() {
        var firstAws = factory.createContextCustomizer(AwsTest.class, List.of());
        var secondAws = factory.createContextCustomizer(DifferentAwsTest.class, List.of());
        var azure = factory.createContextCustomizer(AzureTest.class, List.of());

        assertThat(firstAws).isNotEqualTo(secondAws);
        assertThat(firstAws).isNotEqualTo(azure);
    }

    @WithAwsLocalStack(sqs = @AwsSqs(queues = @AwsSqs.Queue(name = "orders")))
    private static final class AwsTest {
    }

    @WithAwsLocalStack(sqs = @AwsSqs(queues = @AwsSqs.Queue(name = "events")))
    private static final class DifferentAwsTest {
    }

    @WithAzureEmulator(serviceBus = @AzureServiceBus(
            image = "mcr.microsoft.com/azure-messaging/servicebus-emulator:1.1.3"))
    private static final class AzureTest {
    }
}
