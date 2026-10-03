package br.com.portalmanager.platform.library.testing.cloud.aws;

import br.com.portalmanager.platform.library.testing.cloud.CloudTestContainer;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AwsLocalStackContainer extends LocalStackContainer implements CloudTestContainer {

    public static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("localstack/localstack:4.14.0");

    private final String[] queues;
    private final String[] buckets;
    private final String[][] redrivePolicies;

    public AwsLocalStackContainer(AwsService[] services, String[] queues, String[] buckets) {
        this(DEFAULT_IMAGE, services, queues, buckets, new String[0][]);
    }

    public AwsLocalStackContainer(
            AwsService[] services,
            String[] queues,
            String[] buckets,
            String[][] redrivePolicies) {
        this(DEFAULT_IMAGE, services, queues, buckets, redrivePolicies);
    }

    public AwsLocalStackContainer(
            DockerImageName image,
            AwsService[] services,
            String[] queues,
            String[] buckets) {
        this(image, services, queues, buckets, new String[0][]);
    }

    public AwsLocalStackContainer(
            DockerImageName image,
            AwsService[] services,
            String[] queues,
            String[] buckets,
            String[][] redrivePolicies) {
        super(image);
        requireServices(services);
        this.queues = copyAndValidate(queues, "AWS queue");
        this.buckets = copyAndValidate(buckets, "AWS bucket");
        this.redrivePolicies = copyPolicies(redrivePolicies, this.queues);
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
        } catch (RuntimeException exception) {
            super.stop();
            throw exception;
        }
    }

    private void provisionQueues() {
        for (String queue : queues) {
            if (queue.endsWith(".fifo")) {
                exec("sqs", "create-queue", "--queue-name", queue,
                        "--attributes", "{\\"FifoQueue\\":\\"true\\",\\"ContentBasedDeduplication\\":\\"true\\"}");
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
            String escapedPolicy = redrivePolicy.replace("\\", "\\\\").replace("\"", "\\\"");
            String attributes = "{\"RedrivePolicy\":\"%s\"}".formatted(escapedPolicy);
            exec("sqs", "set-queue-attributes",
                    "--queue-url", sourceQueueUrl,
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
            throw new IllegalStateException("Failed to resolve ARN for dead-letter queue: " + queue);
        }
        return arn;
    }

    private void provisionBuckets() {
        for (String bucket : buckets) {
            exec("s3api", "create-bucket", "--bucket", bucket);
        }
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
            throw new IllegalArgumentException("At least one AWS service must be configured");
        }
    }

    private String[] copyAndValidate(String[] values, String resource) {
        if (values == null) {
            return new String[0];
        }
        String[] copy = values.clone();
        for (String value : copy) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(resource + " name must not be blank");
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
                throw new IllegalArgumentException("Each SQS redrive policy must define source, DLQ, and max receive count");
            }
            String source = requireQueueName(policy[0], "SQS source queue");
            String deadLetter = requireQueueName(policy[1], "SQS dead-letter queue");
            int maxReceiveCount;
            try {
                maxReceiveCount = Integer.parseInt(policy[2]);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("SQS max receive count must be a positive integer", exception);
            }
            if (maxReceiveCount < 1) {
                throw new IllegalArgumentException("SQS max receive count must be a positive integer");
            }
            if (source.equals(deadLetter)) {
                throw new IllegalArgumentException("SQS source and dead-letter queues must be different");
            }
            if (source.endsWith(".fifo") != deadLetter.endsWith(".fifo")) {
                throw new IllegalArgumentException("SQS source and dead-letter queues must use the same queue type");
            }
            if (!queueNames.contains(source) || !queueNames.contains(deadLetter)) {
                throw new IllegalArgumentException("SQS source and dead-letter queues must both be provisioned");
            }
            if (!sources.add(source)) {
                throw new IllegalArgumentException("Only one SQS redrive policy can be configured per source queue: " + source);
            }
            copy[index] = new String[]{source, deadLetter, Integer.toString(maxReceiveCount)};
        }
        return copy;
    }

    private String requireQueueName(String queue, String resource) {
        if (queue == null || queue.isBlank()) {
            throw new IllegalArgumentException(resource + " name must not be blank");
        }
        return queue;
    }

    @Override
    public Map<String, String> connectionProperties() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("endpoint", getEndpoint().toString());
        properties.put("region", getRegion());
        properties.put("access-key", getAccessKey());
        properties.put("secret-key", getSecretKey());
        return Map.copyOf(properties);
    }
}
