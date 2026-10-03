package br.com.portalmanager.platform.library.testing.cloud.aws;

import br.com.portalmanager.platform.library.testing.annotation.AwsS3;
import br.com.portalmanager.platform.library.testing.annotation.AwsSqs;
import br.com.portalmanager.platform.library.testing.annotation.WithAwsLocalStack;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.type.AnnotationMetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwsLocalStackImportRegistrarTest {

    private final AwsLocalStackImportRegistrar registrar = new AwsLocalStackImportRegistrar();

    @Test
    void shouldRegisterOneSharedConnectionAndTypedServiceClients() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(AwsCloudTest.class), registry);

        assertTrue(registry.containsBeanDefinition("awsLocalStackContainer"));
        assertTrue(registry.containsBeanDefinition("awsLocalStackConnection"));
        assertTrue(registry.containsBeanDefinition("awsLocalStackSqsClient"));
        assertTrue(registry.containsBeanDefinition("awsLocalStackS3Client"));
        assertEquals(AwsLocalStackConnection.class.getName(),
                registry.getBeanDefinition("awsLocalStackConnection").getBeanClassName());
    }

    @WithAwsLocalStack(
            sqs = @AwsSqs(queues = "audit-events"),
            s3 = @AwsS3(buckets = "documents")
    )
    private static final class AwsCloudTest {
    }
}
