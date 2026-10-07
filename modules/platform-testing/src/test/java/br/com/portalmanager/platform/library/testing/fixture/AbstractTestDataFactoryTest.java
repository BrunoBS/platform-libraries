package br.com.portalmanager.platform.library.testing.fixture;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractTestDataFactoryTest {

    @Test
    void shouldBuildValidDataByDefault() {
        SampleFactory factory = new SampleFactory();

        assertThat(factory.valid())
                .isEqualTo(new Sample("valid"));
    }

    @Test
    void shouldBuildSemanticVariantFromFreshBuilder() {
        SampleFactory factory = new SampleFactory();

        assertThat(factory.withoutName())
                .isEqualTo(new Sample(null));
        assertThat(factory.valid())
                .isEqualTo(new Sample("valid"));
    }

    private record Sample(String name) {
    }

    private static final class SampleBuilder
            extends AbstractTestDataBuilder<Sample, SampleBuilder> {

        private String name = "valid";

        private SampleBuilder withName(String name) {
            this.name = name;
            return self();
        }

        @Override
        public Sample build() {
            return new Sample(name);
        }
    }

    private static final class SampleFactory
            extends AbstractTestDataFactory<Sample, SampleBuilder> {

        @Override
        protected SampleBuilder builder() {
            return new SampleBuilder();
        }

        private Sample withoutName() {
            return create(builder -> builder.withName(null));
        }
    }
}
