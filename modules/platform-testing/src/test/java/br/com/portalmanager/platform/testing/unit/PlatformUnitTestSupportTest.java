package br.com.portalmanager.platform.testing.unit;

import br.com.portalmanager.platform.testing.annotation.PlatformUnitTest;
import br.com.portalmanager.platform.testing.context.TestContext;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@PlatformUnitTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlatformUnitTestSupportTest {

    private static String previousCorrelationId;

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService service;

    @Test
    @Order(1)
    void shouldInitializeMockitoAndPopulateContexts() {
        when(repository.findName()).thenReturn("Produto de teste");

        assertThat(service.findName()).isEqualTo("Produto de teste");

        previousCorrelationId = TestContext.correlationId();
        MDC.put("test-key", "test-value");
    }

    @Test
    @Order(2)
    void shouldClearTestAndLoggingContextsBetweenTests() {
        assertThat(MDC.get("test-key")).isNull();
        assertThat(TestContext.correlationId()).isNotEqualTo(previousCorrelationId);
    }

    interface ProductRepository {

        String findName();
    }

    static final class ProductService {

        private final ProductRepository repository;

        ProductService(ProductRepository repository) {
            this.repository = repository;
        }

        String findName() {
            return repository.findName();
        }
    }
}
