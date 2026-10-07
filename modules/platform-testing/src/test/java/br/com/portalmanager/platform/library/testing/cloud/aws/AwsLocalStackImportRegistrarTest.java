package br.com.portalmanager.platform.library.testing.cloud.aws;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.testing.cloud.aws.s3.annotation.AwsS3;
import br.com.portalmanager.platform.library.testing.cloud.aws.s3notificationsqs.annotation.AwsS3SqsNotification;
import br.com.portalmanager.platform.library.testing.cloud.aws.sqs.annotation.AwsSqs;
import br.com.portalmanager.platform.library.testing.cloud.aws.annotation.WithAwsLocalStack;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.type.AnnotationMetadata;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwsLocalStackImportRegistrarTest {

    private final AwsLocalStackImportRegistrar registrar = new AwsLocalStackImportRegistrar();

    @Test
    void shouldRegisterOneSharedConnectionTypedClientsAndQueueRedriveFixture() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(AwsCloudTest.class), registry);

        assertTrue(registry.containsBeanDefinition("awsLocalStackContainer"));
        assertTrue(registry.containsBeanDefinition("awsLocalStackConnection"));
        assertTrue(registry.containsBeanDefinition("awsLocalStackSqsClient"));
        assertTrue(registry.containsBeanDefinition("awsLocalStackS3Client"));
        assertEquals(AwsLocalStackConnection.class.getName(),
                registry.getBeanDefinition("awsLocalStackConnection").getBeanClassName());

        ConstructorArgumentValues arguments = registry.getBeanDefinition("awsLocalStackContainer")
                .getConstructorArgumentValues();
        assertArrayEquals(new String[]{"audit-events", "order-events", "order-events-dlq"},
                (String[]) arguments.getIndexedArgumentValue(1, String[].class).getValue());
        assertArrayEquals(new String[][]{{"order-events", "order-events-dlq", "3"}},
                (String[][]) arguments.getIndexedArgumentValue(3, String[][].class).getValue());
        assertArrayEquals(new String[][]{{"documents", "audit-events"}},
                (String[][]) arguments.getIndexedArgumentValue(4, String[][].class).getValue());
    }

    @Test
    void shouldRejectNotificationToUndeclaredQueueWithPlatformConfigurationError() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        PlatformConfigurationException exception = assertThrows(PlatformConfigurationException.class,
                () -> registrar.registerBeanDefinitions(
                        AnnotationMetadata.introspect(InvalidAwsCloudTest.class), registry));

        assertEquals("PLT-TST-001", exception.getErrorResponse().code());
        assertTrue(exception.getErrorResponse().message().contains("must be declared in AwsSqs.queues"));
        assertTrue(exception.getErrorResponse().solution().contains("@WithAwsLocalStack"));
    }

    @WithAwsLocalStack(
            sqs = @AwsSqs(queues = {
                    @AwsSqs.Queue(name = "audit-events"),
                    @AwsSqs.Queue(name = "order-events", deadLetterEnabled = true, maxReceiveCount = 3)
            }),
            s3 = @AwsS3(buckets = "documents"),
            s3SqsNotifications = @AwsS3SqsNotification(bucket = "documents", queue = "audit-events")
    )
    private static final class AwsCloudTest {
    }

    @WithAwsLocalStack(
            sqs = @AwsSqs(queues = @AwsSqs.Queue(name = "audit-events")),
            s3 = @AwsS3(buckets = "documents"),
            s3SqsNotifications = @AwsS3SqsNotification(bucket = "documents", queue = "missing-queue")
    )
    private static final class InvalidAwsCloudTest {
    }
}
