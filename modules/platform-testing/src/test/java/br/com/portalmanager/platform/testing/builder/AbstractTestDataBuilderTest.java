package br.com.portalmanager.platform.testing.builder;

import br.com.portalmanager.platform.testing.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.testing.factory.TestDataFactory;
import br.com.portalmanager.platform.testing.scenario.TestScenario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractTestDataBuilderTest {

    record ExampleRequest(String name, boolean active) {
    }

    static final class ExampleBuilder
            extends AbstractTestDataBuilder<ExampleRequest, ExampleBuilder> {

        private String name = "valid-name";
        private boolean active = true;

        ExampleBuilder withName(String name) {
            this.name = name;
            return self();
        }

        ExampleBuilder inactive() {
            this.active = false;
            return self();
        }

        @Override
        public ExampleRequest build() {
            return new ExampleRequest(name, active);
        }
    }

    static final class ExampleFactory
            extends AbstractTestDataFactory<ExampleRequest, ExampleBuilder> {

        @Override
        protected ExampleBuilder builder() {
            return new ExampleBuilder();
        }

        ExampleRequest withoutName() {
            return builder().withName(null).build();
        }
    }

    @Test
    void shouldAllowProjectSpecificBuilderFields() {
        ExampleRequest request = new ExampleBuilder()
                .withName("custom-name")
                .inactive()
                .build();

        assertThat(request.name()).isEqualTo("custom-name");
        assertThat(request.active()).isFalse();
    }

    @Test
    void shouldCreateValidDataFromAbstractFactory() {
        ExampleFactory factory = new ExampleFactory();

        assertThat(factory.valid()).isEqualTo(new ExampleRequest("valid-name", true));
        assertThat(factory.withoutName().name()).isNull();
    }

    @Test
    void shouldAllowScenarioToReturnPreparedContext() {
        TestScenario<Long> scenario = () -> 42L;

        assertThat(scenario.setup()).isEqualTo(42L);
    }

    @Test
    void shouldAllowFactoryCompositionWithoutInheritance() {
        TestDataFactory<ExampleRequest> factory = () -> new ExampleBuilder().build();

        assertThat(factory.valid()).isEqualTo(new ExampleRequest("valid-name", true));
    }
}
