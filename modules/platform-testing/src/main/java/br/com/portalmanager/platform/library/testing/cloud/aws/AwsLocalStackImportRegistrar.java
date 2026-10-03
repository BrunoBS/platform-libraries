package br.com.portalmanager.platform.library.testing.cloud.aws;

import br.com.portalmanager.platform.library.testing.annotation.WithAwsLocalStack;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AwsLocalStackImportRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String CONTAINER_BEAN = "awsLocalStackContainer";
    private static final String SQS_CLIENT_BEAN = "awsLocalStackSqsClient";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(
                WithAwsLocalStack.class.getName(), false);
        if (attributes == null) {
            return;
        }

        AnnotationAttributes[] sqs = annotations(attributes.get("sqs"));
        AnnotationAttributes[] s3 = annotations(attributes.get("s3"));
        requireSingle("AwsSqs", sqs);
        requireSingle("AwsS3", s3);

        List<AwsService> services = new ArrayList<>();
        String[] queues = new String[0];
        String[] buckets = new String[0];

        if (sqs.length == 1) {
            services.add(AwsService.SQS);
            queues = sqs[0].getStringArray("queues");
        }
        if (s3.length == 1) {
            services.add(AwsService.S3);
            buckets = s3[0].getStringArray("buckets");
        }
        if (services.isEmpty()) {
            throw new IllegalArgumentException("At least one AWS service annotation must be configured");
        }

        registerContainer(registry, services, queues, buckets);
        if (sqs.length == 1) {
            registerSqsClient(registry);
        }
    }

    private void registerContainer(
            BeanDefinitionRegistry registry,
            List<AwsService> services,
            String[] queues,
            String[] buckets) {
        AwsService[] enabledServices = services.toArray(AwsService[]::new);
        String[] configuredQueues = queues.clone();
        String[] configuredBuckets = buckets.clone();

        RootBeanDefinition definition = new RootBeanDefinition(AwsLocalStackContainer.class);
        definition.setInstanceSupplier(() -> new AwsLocalStackContainer(
                enabledServices,
                configuredQueues,
                configuredBuckets));
        definition.setInitMethodName("start");
        definition.setDestroyMethodName("stop");
        registry.registerBeanDefinition(CONTAINER_BEAN, definition);
    }

    private void registerSqsClient(BeanDefinitionRegistry registry) {
        RootBeanDefinition definition = new RootBeanDefinition(AwsSqsClientFactoryBean.class);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        definition.setDestroyMethodName("close");
        definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, SqsClient.class);
        registry.registerBeanDefinition(SQS_CLIENT_BEAN, definition);
    }

    @SuppressWarnings("unchecked")
    private AnnotationAttributes[] annotations(Object value) {
        if (value == null) {
            return new AnnotationAttributes[0];
        }
        if (value instanceof AnnotationAttributes[] annotationAttributes) {
            return annotationAttributes;
        }
        if (value instanceof Map<?, ?>[] maps) {
            AnnotationAttributes[] result = new AnnotationAttributes[maps.length];
            for (int index = 0; index < maps.length; index++) {
                result[index] = AnnotationAttributes.fromMap((Map<String, Object>) maps[index]);
            }
            return result;
        }
        throw new IllegalArgumentException("Unsupported nested AWS service annotation metadata");
    }

    private void requireSingle(String annotationName, AnnotationAttributes[] annotations) {
        if (annotations.length > 1) {
            throw new IllegalArgumentException(annotationName + " may be declared only once");
        }
    }
}
