package br.com.portalmanager.platform.library.testing.cloud.aws;

import br.com.portalmanager.platform.library.testing.cloud.aws.annotation.AwsS3;
import br.com.portalmanager.platform.library.testing.cloud.aws.annotation.AwsS3SqsNotification;
import br.com.portalmanager.platform.library.testing.cloud.aws.annotation.AwsSqs;
import br.com.portalmanager.platform.library.testing.cloud.aws.annotation.WithAwsLocalStack;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class AwsLocalStackImportRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String CONTAINER_BEAN = "awsLocalStackContainer";
    private static final String CONNECTION_BEAN = "awsLocalStackConnection";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(
                WithAwsLocalStack.class.getName(), false);
        if (attributes == null) {
            return;
        }

        AnnotationAttributes[] sqs = annotations(attributes.get("sqs"));
        AnnotationAttributes[] s3 = annotations(attributes.get("s3"));
        AnnotationAttributes[] s3SqsNotifications = annotations(attributes.get("s3SqsNotifications"));
        requireSingle("AwsSqs", sqs);
        requireSingle("AwsS3", s3);

        List<AwsService> services = new ArrayList<>();
        String[] queues = new String[0];
        String[] buckets = new String[0];
        String[][] redrivePolicies = new String[0][];
        String[][] bucketNotifications = new String[0][];

        if (sqs.length == 1) {
            services.add(AwsService.SQS);
            var configuration = sqsConfiguration(sqs[0]);
            queues = configuration.queues();
            redrivePolicies = configuration.redrivePolicies();
        }
        if (s3.length == 1) {
            services.add(AwsService.S3);
            buckets = s3[0].getStringArray("buckets");
        }
        if (s3SqsNotifications.length > 0) {
            bucketNotifications = s3Notifications(s3SqsNotifications, buckets, queues);
        }
        if (services.isEmpty()) {
            throw new IllegalArgumentException("At least one AWS service annotation must be configured");
        }

        registerContainer(registry, services, queues, buckets, redrivePolicies, bucketNotifications);
        registerConnection(registry);
        registerServiceClients(registry, sqs.length == 1, s3.length == 1);
    }

    private SqsConfiguration sqsConfiguration(AnnotationAttributes sqs) {
        AnnotationAttributes[] queueDefinitions = annotations(sqs.get("queues"));
        List<String> queues = new ArrayList<>();
        List<String[]> redrivePolicies = new ArrayList<>();

        for (AnnotationAttributes queue : queueDefinitions) {
            String name = queue.getString("name");
            requireName(name, "SQS queue");
            addUnique(queues, name, "SQS queue");

            boolean deadLetterEnabled = queue.getBoolean("deadLetterEnabled");
            String deadLetterQueue = queue.getString("deadLetterQueue");
            if (!deadLetterEnabled && (deadLetterQueue == null || deadLetterQueue.isBlank())) {
                continue;
            }
            if (deadLetterQueue == null || deadLetterQueue.isBlank()) {
                deadLetterQueue = name.endsWith(".fifo")
                    ? name.substring(0, name.length() - ".fifo".length()) + "-dlq.fifo"
                    : name + "-dlq";
            }
            requireName(deadLetterQueue, "SQS dead-letter queue");
            if (name.equals(deadLetterQueue)) {
                throw new IllegalArgumentException("SQS source and dead-letter queues must be different");
            }
            int maxReceiveCount = queue.getNumber("maxReceiveCount").intValue();
            if (maxReceiveCount < 1) {
                throw new IllegalArgumentException("SQS maxReceiveCount must be a positive integer");
            }
            addUnique(queues, deadLetterQueue, "SQS queue");
            redrivePolicies.add(new String[]{name, deadLetterQueue, Integer.toString(maxReceiveCount)});
        }

        return new SqsConfiguration(queues.toArray(String[]::new), redrivePolicies.toArray(String[][]::new));
    }

    private String[][] s3Notifications(AnnotationAttributes[] definitions, String[] buckets, String[] queues) {
        List<String[]> notifications = new ArrayList<>();
        for (AnnotationAttributes notification : definitions) {
            String bucket = notification.getString("bucket");
            String queue = notification.getString("queue");
            requireName(bucket, "S3 notification bucket");
            requireName(queue, "S3 notification queue");
            if (!Arrays.asList(buckets).contains(bucket)) {
                throw new IllegalArgumentException("S3 notification bucket must be declared in AwsS3.buckets: " + bucket);
            }
            if (!Arrays.asList(queues).contains(queue)) {
                throw new IllegalArgumentException("S3 notification queue must be declared in AwsSqs.queues: " + queue);
            }
            if (queue.endsWith(".fifo")) {
                throw new IllegalArgumentException("S3 notifications cannot target FIFO queues: " + queue);
            }
            notifications.add(new String[]{bucket, queue});
        }
        return notifications.toArray(String[][]::new);
    }

    private void requireName(String name, String type) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(type + " name must not be blank");
        }
    }

    private void addUnique(List<String> values, String value, String type) {
        if (values.contains(value)) {
            throw new IllegalArgumentException("Duplicate " + type + " name: " + value);
        }
        values.add(value);
    }

    private void registerContainer(
            BeanDefinitionRegistry registry,
            List<AwsService> services,
            String[] queues,
            String[] buckets,
            String[][] redrivePolicies,
            String[][] bucketNotifications) {
        AwsService[] enabledServices = services.toArray(AwsService[]::new);
        RootBeanDefinition definition = new RootBeanDefinition(AwsLocalStackContainer.class);
        definition.getConstructorArgumentValues().addIndexedArgumentValue(0, enabledServices);
        definition.getConstructorArgumentValues().addIndexedArgumentValue(1, queues.clone());
        definition.getConstructorArgumentValues().addIndexedArgumentValue(2, buckets.clone());
        definition.getConstructorArgumentValues().addIndexedArgumentValue(3, redrivePolicies.clone());
        definition.getConstructorArgumentValues().addIndexedArgumentValue(4, bucketNotifications.clone());
        definition.setInitMethodName("start");
        definition.setDestroyMethodName("stop");
        registry.registerBeanDefinition(CONTAINER_BEAN, definition);
    }

    private void registerConnection(BeanDefinitionRegistry registry) {
        RootBeanDefinition definition = new RootBeanDefinition(AwsLocalStackConnection.class);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        registry.registerBeanDefinition(CONNECTION_BEAN, definition);
    }

    private void registerServiceClients(BeanDefinitionRegistry registry, boolean sqsEnabled, boolean s3Enabled) {
        if (sqsEnabled) {
            AwsSqsTestSupport.register(registry);
        }
        if (s3Enabled) {
            AwsS3TestSupport.register(registry);
        }
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

    private record SqsConfiguration(String[] queues, String[][] redrivePolicies) {
    }
}
