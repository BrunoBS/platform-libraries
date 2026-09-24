package br.com.portalmanager.platform.catalog.model;

import br.com.portalmanager.platform.messaging.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractCatalogCodeTest {

    @Test
    void shouldCreateCodeFromEnum() {
        LifecycleCode code = LifecycleCode.of(Lifecycle.ACTIVE);

        assertThat(code.value()).isEqualTo("ACTIVE");
        assertThat(code.toString()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldCreateDynamicSemanticCode() {
        ServiceCode code = ServiceCode.of("workspace-service");

        assertThat(code.value()).isEqualTo("workspace-service");
    }

    @Test
    void shouldRejectInvalidCode() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> ServiceCode.of("workspace service")
                )
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldUseConcreteTypeInEquality() {
        assertThat(LifecycleCode.of(Lifecycle.ACTIVE))
                .isEqualTo(LifecycleCode.of(Lifecycle.ACTIVE))
                .isNotEqualTo(OtherCode.of("ACTIVE"));
    }

    private enum Lifecycle {
        ACTIVE
    }

    private static final class LifecycleCode extends AbstractCatalogCode {
        private LifecycleCode(Lifecycle value) {
            super(value);
        }

        static LifecycleCode of(Lifecycle value) {
            return new LifecycleCode(value);
        }
    }

    private static final class ServiceCode extends AbstractCatalogCode {
        private ServiceCode(String value) {
            super(value);
        }

        static ServiceCode of(String value) {
            return new ServiceCode(value);
        }
    }

    private static final class OtherCode extends AbstractCatalogCode {
        private OtherCode(String value) {
            super(value);
        }

        static OtherCode of(String value) {
            return new OtherCode(value);
        }
    }
}
