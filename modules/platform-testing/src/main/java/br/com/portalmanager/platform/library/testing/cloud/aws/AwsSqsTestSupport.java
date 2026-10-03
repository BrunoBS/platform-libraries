package br.com.portalmanager.platform.library.testing.cloud.aws;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import software.amazon.awssdk.services.sqs.SqsClient;

final class AwsSqsTestSupport {

    private static final String SQS_CLIENT_BEAN = "awsLocalStackSqsClient";

    private AwsSqsTestSupport() {
    }

    static void register(BeanDefinitionRegistry registry) {
        RootBeanDefinition definition = new RootBeanDefinition(AwsSqsClientFactoryBean.class);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        definition.setDestroyMethodName("close");
        definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, SqsClient.class);
        registry.registerBeanDefinition(SQS_CLIENT_BEAN, definition);
    }
}
