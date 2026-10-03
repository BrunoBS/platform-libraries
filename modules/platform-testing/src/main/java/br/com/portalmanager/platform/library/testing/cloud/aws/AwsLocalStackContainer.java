package br.com.portalmanager.platform.library.testing.cloud.aws;

import br.com.portalmanager.platform.library.testing.cloud.CloudTestContainer;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class AwsLocalStackContainer extends LocalStackContainer implements CloudTestContainer {

    public static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("localstack/localstack:4.14.0");

    private final String[] queues;
    private final String[] buckets;

    public AwsLocalStackContainer(AwsService[] services, String[] queues, String[] buckets) {
        this(DEFAULT_IMAGE, services, queues, buckets);
    }

    public AwsLocalStackContainer(
            DockerImageName image,
            AwsService[] services,
            String[] queues,
            String[] buckets) {
        super(image);
        if (services == null || services.length == 0) {
            throw new IllegalArgumentException("At least one AWS service must be configured");
        }
        this.queues = queues == null ? new String[0] : queues.clone();
        this.buckets = buckets == null ? new String[0] : buckets.clone();
        withServices(Arrays.stream(services)
                .map(AwsService::localStackName)
                .toArray(String[]::new));
    }

    @Override
    public void start() {
        super.start();
        try {
            provisionQueues();
            provisionBuckets();
        } catch (RuntimeException exception) {
            super.stop();
            throw exception;
        }
    }

    private void provisionQueues() {
        for (String queue : queues) {
            requireName(queue, "AWS queue");
            exec("sqs", "create-queue", "--queue-name", queue);
        }
    }

    private void provisionBuckets() {
        for (String bucket : buckets) {
            requireName(bucket, "AWS bucket");
            exec("s3api", "create-bucket", "--bucket", bucket);
        }
    }

    private void exec(String... command) {
        String[] arguments = new String[command.length + 1];
        arguments[0] = "awslocal";
        System.arraycopy(command, 0, arguments, 1, command.length);
        try {
            var result = execInContainer(arguments);
            if (result.getExitCode() != 0) {
                throw new IllegalStateException("Failed to provision LocalStack resource: " + result.getStderr());
            }
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to provision LocalStack resource", exception);
        }
    }

    private void requireName(String value, String resource) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(resource + " name must not be blank");
        }
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
