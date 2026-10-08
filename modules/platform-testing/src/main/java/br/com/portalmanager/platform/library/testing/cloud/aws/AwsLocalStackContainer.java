package br.com.portalmanager.platform.library.testing.cloud.aws;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.testing.message.PlatformTestingTechnicalErrors;
import br.com.portalmanager.platform.library.testing.container.PlatformTestingContainerImages;

import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AwsLocalStackContainer extends LocalStackContainer {

    public static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse(
            PlatformTestingContainerImages.AWS_LOCALSTACK
    );

    private final String[] queues;
    private final String[] buckets;
    private final String[][] redrivePolicies;
    private final String[][] bucketNotifications;

    public AwsLocalStackContainer(AwsService[] services, String[] queues, String[] buckets) {
        this(DEFAULT_IMAGE, services, queues, buckets, new String[0][], new String[0][]);
    }

    public AwsLocalStackContainer(
            AwsService[] services,
            String[] queues,
            String[] buckets,
            String[][] redrivePolicies) {
        this(DEFAULT_IMAGE, services, queues, buckets, redrivePolicies, new String[0][]);
    }

    public AwsLocalStackContainer(
            AwsService[] services,
            String[] queues,
            String[] buckets,
            String[][] redrivePolicies,
            String[][] bucketNotifications) {
        this(DEFAULT_IMAGE, services, queues, buckets, redrivePolicies, bucketNotifications);
    }

    public AwsLocalStackContainer(
            DockerImageName image,
            AwsService[] services,
            String[] queues,
            String[] buckets) {
        this(image, services, queues, buckets, new String[0][], new String[0][]);
    }

    public AwsLocalStackContainer(
            DockerImageName image,
            AwsService[] services,
            String[] queues,
            String[] buckets,
            String[][] redrivePolicies) {
        this(image, services, queues, buckets, redrivePolicies, new String[0][]);
    }

    public AwsLocalStackContainer(
            DockerImageName image,
            AwsService[] services,
            String[] queues,
            String[] buckets,
            String[][] redrivePolicies,
            String[][] bucketNotifications) {
        super(image);
        requireServices(services);
        this.queues = copyAndValidate(queues, "AWS queue");
        this.buckets = copyAndValidate(buckets, "AWS bucket");
        this.redrivePolicies = copyPolicies(redrivePolicies, this.queues);
        this.bucketNotifications = copyNotifications(bucketNotifications, this.queues, this.buckets);
        withServices(Arrays.stream(services)
                .map(AwsService::localStackName)
                .toArray(String[]::new));
    }

    @Override
    public void start() {
        super.start();
        try {
            provisionQueues();
            provisionRedrivePolicies();
            provisionBuckets();
            provisionBucketNotifications();
        } catch (RuntimeException exception) {
            super.stop();
            throw exception;
        }
    }

    private void provisionQueues() {
        for (String queue : queues) {
            if (queue.endsWith(".fifo")) {
                exec("sqs", "create-queue", "--queue-name", queue,
                        "--attributes", "{\"FifoQueue\":\"true\"}");
            } else {
                exec("sqs", "create-queue", "--queue-name", queue);
            }
        }
    }

    private void provisionRedrivePolicies() {
        for (String[] policy : redrivePolicies) {
            String sourceQueueUrl = queueUrl(policy[0]);
            String deadLetterQueueArn = queueArn(policy[1]);
            String redrivePolicy = "{\"deadLetterTargetArn\":\"%s\",\"maxReceiveCount\":\"%s\"}"
                    .formatted(deadLetterQueueArn, policy[2]);
            String escapedPolicy = escapeJson(redrivePolicy);
            String attributes = "{\"RedrivePolicy\":\"%s\"}".formatted(escapedPolicy);
            exec("sqs", "set-queue-attributes",
                    "--queue-url", sourceQueueUrl,
                    "--attributes", attributes);
        }
    }

    private void provisionBuckets() {
        for (String bucket : buckets) {
            exec("s3api", "create-bucket", "--bucket", bucket);
        }
    }

    private void provisionBucketNotifications() {
        if (bucketNotifications.length == 0) {
            return;
        }

        provisionNotificationQueuePolicies();
        for (String bucket : buckets) {
            List<String> configurations = new ArrayList<>();
            int notificationIndex = 0;
            for (String[] notification : bucketNotifications) {
                if (!bucket.equals(notification[0])) {
                    continue;
                }
                String queueArn = queueArn(notification[1]);
                configurations.add("{\"Id\":\"notification-%d\",\"QueueArn\":\"%s\","
                        .formatted(notificationIndex++, queueArn)
                        + "\"Events\":[\"s3:ObjectCreated:*\"]}");
            }
            if (!configurations.isEmpty()) {
                String configuration = "{\"QueueConfigurations\":["
                        + String.join(",", configurations) + "]}";
                exec("s3api", "put-bucket-notification-configuration",
                        "--bucket", bucket,
                        "--notification-configuration", configuration);
            }
        }
    }

    private void provisionNotificationQueuePolicies() {
        Set<String> configuredQueues = new HashSet<>();
        for (String[] notification : bucketNotifications) {
            String queue = notification[1];
            if (!configuredQueues.add(queue)) {
                continue;
            }

            String queueArn = queueArn(queue);
            String accountId = queueArn.split(":")[4];
            List<String> statements = new ArrayList<>();
            for (String[] target : bucketNotifications) {
                if (!queue.equals(target[1])) {
                    continue;
                }
                statements.add("{\"Effect\":\"Allow\",\"Principal\":{\"Service\":\"s3.amazonaws.com\"},"
                        + "\"Action\":\"sqs:SendMessage\",\"Resource\":\"" + queueArn + "\","
                        + "\"Condition\":{\"ArnLike\":{\"aws:SourceArn\":\"arn:aws:s3:::" + target[0]
                        + "\"},\"StringEquals\":{\"aws:SourceAccount\":\"" + accountId + "\"}}}");
            }

            String policy = "{\"Version\":\"2012-10-17\",\"Statement\":["
                    + String.join(",", statements) + "]}";
            String attributes = "{\"Policy\":\"" + escapeJson(policy) + "\"}";
            exec("sqs", "set-queue-attributes",
                    "--queue-url", queueUrl(queue),
                    "--attributes", attributes);
        }
    }

    private String queueUrl(String queue) {
        return exec("sqs", "get-queue-url", "--queue-name", queue, "--query", "QueueUrl", "--output", "text")
                .trim();
    }

    private String queueArn(String queue) {
        String arn = exec("sqs", "get-queue-attributes",
                "--queue-url", queueUrl(queue),
                "--attribute-names", "QueueArn",
                "--query", "Attributes.QueueArn",
                "--output", "text").trim();
        if (arn.isBlank() || "None".equals(arn)) {
            throw new IllegalStateException("Failed to resolve ARN for SQS queue: " + queue);
        }
        return arn;
    }

    private String exec(String... command) {
        String[] arguments = new String[command.length + 1];
        arguments[0] = "awslocal";
        System.arraycopy(command, 0, arguments, 1, command.length);
        try {
            var result = execInContainer(arguments);
            if (result.getExitCode() != 0) {
                throw new IllegalStateException("Failed to provision LocalStack resource: " + result.getStderr());
            }
            return result.getStdout();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LocalStack resource provisioning was interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to provision LocalStack resource", exception);
        }
    }

    private void requireServices(AwsService[] services) {
        if (services == null || services.length == 0) {
            throw configurationException("At least one AWS service must be configured");
        }
    }

    private String[] copyAndValidate(String[] values, String resource) {
        if (values == null) {
            return new String[0];
        }
        String[] copy = values.clone();
        Set<String> unique = new HashSet<>();
        for (String value : copy) {
            if (value == null || value.isBlank()) {
                throw configurationException(resource + " name must not be blank");
            }
            if (!unique.add(value)) {
                throw configurationException("Duplicate " + resource + " name: " + value);
            }
        }
        return copy;
    }

    private String[][] copyPolicies(String[][] policies, String[] configuredQueues) {
        if (policies == null) {
            return new String[0][];
        }
        Set<String> queueNames = new HashSet<>(Arrays.asList(configuredQueues));
        Set<String> sources = new HashSet<>();
        String[][] copy = new String[policies.length][];
        for (int index = 0; index < policies.length; index++) {
            String[] policy = policies[index];
            if (policy == null || policy.length != 3) {
                throw configurationException("Each SQS redrive policy must define source, DLQ, and max receive count");
            }
            String source = requireQueueName(policy[0], "SQS source queue");
            String deadLetter = requireQueueName(policy[1], "SQS dead-letter queue");
            int maxReceiveCount;
            try {
                maxReceiveCount = Integer.parseInt(policy[2]);
            } catch (NumberFormatException exception) {
                throw configurationException("SQS max receive count must be a positive integer", exception);
            }
            if (maxReceiveCount < 1) {
                throw configurationException("SQS max receive count must be a positive integer");
            }
            if (source.equals(deadLetter)) {
                throw configurationException("SQS source and dead-letter queues must be different");
            }
            if (source.endsWith(".fifo") != deadLetter.endsWith(".fifo")) {
                throw configurationException("SQS source and dead-letter queues must use the same queue type");
            }
            if (!queueNames.contains(source) || !queueNames.contains(deadLetter)) {
                throw configurationException("SQS source and dead-letter queues must both be provisioned");
            }
            if (!sources.add(source)) {
                throw configurationException("Only one SQS redrive policy can be configured per source queue: " + source);
            }
            copy[index] = new String[]{source, deadLetter, Integer.toString(maxReceiveCount)};
        }
        return copy;
    }

    private String[][] copyNotifications(String[][] notifications, String[] configuredQueues, String[] configuredBuckets) {
        if (notifications == null) {
            return new String[0][];
        }
        Set<String> queueNames = new HashSet<>(Arrays.asList(configuredQueues));
        Set<String> bucketNames = new HashSet<>(Arrays.asList(configuredBuckets));
        String[][] copy = new String[notifications.length][];
        for (int index = 0; index < notifications.length; index++) {
            String[] notification = notifications[index];
            if (notification == null || notification.length != 2) {
                throw configurationException("Each S3 notification must define a bucket and an SQS queue");
            }
            String bucket = notification[0];
            String queue = requireQueueName(notification[1], "SQS notification queue");
            if (bucket == null || bucket.isBlank()) {
                throw configurationException("S3 notification bucket name must not be blank");
            }
            if (!bucketNames.contains(bucket)) {
                throw configurationException("S3 notification bucket must be provisioned: " + bucket);
            }
            if (!queueNames.contains(queue)) {
                throw configurationException("S3 notification queue must be provisioned: " + queue);
            }
            if (queue.endsWith(".fifo")) {
                throw configurationException("S3 notifications cannot target FIFO queues: " + queue);
            }
            copy[index] = new String[]{bucket, queue};
        }
        return copy;
    }

    private String requireQueueName(String queue, String resource) {
        if (queue == null || queue.isBlank()) {
            throw configurationException(resource + " name must not be blank");
        }
        return queue;
    }

    private PlatformConfigurationException configurationException(String detail) {
        return new PlatformConfigurationException(PlatformTestingTechnicalErrors.invalidAwsConfiguration(detail));
    }

    private PlatformConfigurationException configurationException(String detail, Throwable cause) {
        return new PlatformConfigurationException(PlatformTestingTechnicalErrors.invalidAwsConfiguration(detail), cause);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
