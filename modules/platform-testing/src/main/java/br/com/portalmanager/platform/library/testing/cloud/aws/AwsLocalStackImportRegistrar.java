package br.com.portalmanager.platform.library.testing.cloud.aws;

import br.com.portalmanager.platform.library.testing.annotation.WithAwsLocalStack;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

public final class AwsLocalStackImportRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String BEAN_NAME = "awsLocalStackContainer";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(
                WithAwsLocalStack.class.getName(), false);
        if (attributes == null) {
            return;
        }

        AwsService[] services = (AwsService[]) attributes.get("services");
        String[] queues = (String[]) attributes.get("queues");
        String[] buckets = (String[]) attributes.get("buckets");

        RootBeanDefinition definition = new RootBeanDefinition(AwsLocalStackContainer.class);
        definition.setInstanceSupplier(() -> new AwsLocalStackContainer(services, queues, buckets));
        definition.setInitMethodName("start");
        definition.setDestroyMethodName("stop");
        registry.registerBeanDefinition(BEAN_NAME, definition);
    }
}
