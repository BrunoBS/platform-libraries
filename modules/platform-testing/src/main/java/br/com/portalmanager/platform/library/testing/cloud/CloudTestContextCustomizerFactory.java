package br.com.portalmanager.platform.library.testing.cloud;

import br.com.portalmanager.platform.library.testing.cloud.aws.annotation.WithAwsLocalStack;
import br.com.portalmanager.platform.library.testing.cloud.azure.annotation.WithAzureEmulator;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

import java.util.List;

public final class CloudTestContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        WithAwsLocalStack aws = AnnotatedElementUtils.findMergedAnnotation(testClass, WithAwsLocalStack.class);
        WithAzureEmulator azure = AnnotatedElementUtils.findMergedAnnotation(testClass, WithAzureEmulator.class);
        if (aws == null && azure == null) {
            return null;
        }
        return new CloudTestContextCustomizer(aws, azure);
    }

    private record CloudTestContextCustomizer(
            WithAwsLocalStack aws,
            WithAzureEmulator azure
    ) implements ContextCustomizer {

        @Override
        public void customizeContext(
                ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfig) {
            // Annotation values are intentionally retained in this customizer's equality
            // so Spring does not reuse a context configured with different cloud resources.
        }
    }
}
