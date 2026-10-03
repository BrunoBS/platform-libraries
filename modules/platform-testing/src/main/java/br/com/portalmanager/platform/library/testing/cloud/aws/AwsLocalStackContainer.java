package br.com.portalmanager.platform.library.testing.cloud.aws;

import br.com.portalmanager.platform.library.testing.cloud.CloudTestContainer;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class AwsLocalStackContainer extends LocalStackContainer implements CloudTestContainer {

    public static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("localstack/localstack:4.14.0");

    public AwsLocalStackContainer(AwsService... services) {
        this(DEFAULT_IMAGE, services);
    }

    public AwsLocalStackContainer(DockerImageName image, AwsService... services) {
        super(image);
        if (services == null || services.length == 0) {
            throw new IllegalArgumentException("At least one AWS service must be configured");
        }
        withServices(Arrays.stream(services)
                .map(AwsService::localStackName)
                .toArray(String[]::new));
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
