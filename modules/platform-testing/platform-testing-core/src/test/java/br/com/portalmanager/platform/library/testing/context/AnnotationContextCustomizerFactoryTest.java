package br.com.portalmanager.platform.library.testing.context;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationContextCustomizerFactoryTest {

    private final ContextCustomizerFactory factory = new ExampleFactory();

    @Test
    void annotationValuesParticipateInTheSpringContextCacheKey() {
        var first = factory.createContextCustomizer(FirstTest.class, List.of());
        var same = factory.createContextCustomizer(SameTest.class, List.of());
        var different = factory.createContextCustomizer(DifferentTest.class, List.of());
        var absent = factory.createContextCustomizer(UnannotatedTest.class, List.of());

        assertThat(first).isEqualTo(same);
        assertThat(first).isNotEqualTo(different);
        assertThat(absent).isNull();
    }

    private static final class ExampleFactory extends AnnotationContextCustomizerFactory {
        private ExampleFactory() {
            super(Example.class);
        }
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Example {
        String value();
    }

    @Example("one")
    private static final class FirstTest {
    }

    @Example("one")
    private static final class SameTest {
    }

    @Example("two")
    private static final class DifferentTest {
    }

    private static final class UnannotatedTest {
    }
}
