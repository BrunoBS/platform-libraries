package br.com.portalmanager.platform.library.testing.cloud.aws.s3;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import software.amazon.awssdk.services.s3.S3Client;

public final class AwsS3TestSupport {

    private static final String S3_CLIENT_BEAN = "awsLocalStackS3Client";

    private AwsS3TestSupport() {
    }

    public static void register(BeanDefinitionRegistry registry) {
        RootBeanDefinition definition = new RootBeanDefinition(AwsS3ClientFactoryBean.class);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        definition.setDestroyMethodName("close");
        definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, S3Client.class);
        registry.registerBeanDefinition(S3_CLIENT_BEAN, definition);
    }
}
