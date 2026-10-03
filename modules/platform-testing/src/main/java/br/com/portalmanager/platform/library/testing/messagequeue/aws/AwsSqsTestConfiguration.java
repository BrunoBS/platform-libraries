package br.com.portalmanager.platform.library.testing.messagequeue.aws;

import br.com.portalmanager.platform.library.testing.cloud.aws.AwsLocalStackContainer;
import br.com.portalmanager.platform.library.testing.cloud.aws.AwsService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class AwsSqsTestConfiguration {

    @Bean(destroyMethod = "stop")
    AwsLocalStackContainer awsLocalStackContainer() {
        return new AwsLocalStackContainer(AwsService.SQS);
    }
}
