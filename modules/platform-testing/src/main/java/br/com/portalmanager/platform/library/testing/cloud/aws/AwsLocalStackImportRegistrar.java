package br.com.portalmanager.platform.library.testing.cloud.aws;

import br.com.portalmanager.platform.library.testing.annotation.WithAwsLocalStack;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

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

        validateTopology(services, queues, buckets);

        RootBeanDefinition definition = new RootBeanDefinition(AwsLocalStackContainer.class);
        definition.setInstanceSupplier(() -> new AwsLocalStackContainer(services, queues, buckets));
        definition.setInitMethodName("start");
        definition.setDestroyMethodName("stop");
        registry.registerBeanDefinition(BEAN_NAME, definition);
    }

    private void validateTopology(AwsService[] services, String[] queues, String[] buckets) {
        if (services == null || services.length == 0) {
            throw new IllegalArgumentException("At least one AWS service must be configured");
        }

        Set<AwsService> enabled = Set.copyOf(Arrays.asList(services));
        if (queues != null && queues.length > 0 && !enabled.contains(AwsService.SQS)) {
            throw new IllegalArgumentException("AWS queues require AwsService.SQS to be enabled");
        }
        if (buckets != null && buckets.length > 0 && !enabled.contains(AwsService.S3)) {
            throw new IllegalArgumentException("AWS buckets require AwsService.S3 to be enabled");
        }
    }
}
