package br.com.portalmanager.platform.library.testing.context;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

import java.lang.annotation.Annotation;
import java.util.List;

/** Creates cache keys from a feature annotation without coupling core to that feature. */
public abstract class AnnotationContextCustomizerFactory implements ContextCustomizerFactory {

    private final Class<? extends Annotation> annotationType;

    protected AnnotationContextCustomizerFactory(Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public final ContextCustomizer createContextCustomizer(
            Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        Annotation annotation = AnnotatedElementUtils.findMergedAnnotation(testClass, annotationType);
        return annotation == null ? null : new AnnotationContextCustomizer(annotation);
    }

    private record AnnotationContextCustomizer(Annotation annotation) implements ContextCustomizer {
        @Override
        public void customizeContext(
                ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfig) {
            // The annotation value is part of equals/hashCode, keeping distinct fixtures out of one cache entry.
        }
    }
}
