package br.com.portalmanager.platform.library.testing.messagequeue.aws;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class LocalStackSqsTestConfiguration {

    static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:4.14.0");

    @Bean(destroyMethod = "stop")
    LocalStackContainer localStackSqsContainer() {
        return new LocalStackContainer(LOCALSTACK_IMAGE)
                .withServices("sqs");
    }
}
