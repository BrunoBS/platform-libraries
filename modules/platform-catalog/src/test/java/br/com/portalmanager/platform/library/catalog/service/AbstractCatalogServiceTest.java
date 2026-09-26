package br.com.portalmanager.platform.library.catalog.service;

import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractCatalogServiceTest {

    @Test
    void shouldReturnTrueWhenActiveCodeExists() {
        TestCatalogRepository repository = mock(TestCatalogRepository.class);
        TestCatalogService service = new TestCatalogService(repository);

        when(repository.existsByCodeAndActiveTrue("ACTIVE")).thenReturn(true);

        assertThat(service.existsActive("ACTIVE")).isTrue();
        verify(repository).existsByCodeAndActiveTrue("ACTIVE");
    }

    @Test
    void shouldReturnFalseWhenActiveCodeDoesNotExist() {
        TestCatalogRepository repository = mock(TestCatalogRepository.class);
        TestCatalogService service = new TestCatalogService(repository);

        when(repository.existsByCodeAndActiveTrue("UNKNOWN")).thenReturn(false);

        assertThat(service.existsActive("UNKNOWN")).isFalse();
        verify(repository).existsByCodeAndActiveTrue("UNKNOWN");
    }

    @Test
    void shouldReturnFalseWithoutQueryForNullOrBlankCode() {
        TestCatalogRepository repository = mock(TestCatalogRepository.class);
        TestCatalogService service = new TestCatalogService(repository);

        assertThat(service.existsActive(null)).isFalse();
        assertThat(service.existsActive("")).isFalse();
        assertThat(service.existsActive("   ")).isFalse();

        verify(repository, never()).existsByCodeAndActiveTrue(org.mockito.ArgumentMatchers.anyString());
    }

    private static final class TestCatalogService extends DynamicCatalogService<TestCatalogEntity> {

        private TestCatalogService(TestCatalogRepository repository) {
            super(repository, new ObjectMapper(), TestCatalogEntity.class);
        }
    }

    private static final class TestCatalogEntity extends CatalogEntity {
    }

    private interface TestCatalogRepository extends CatalogRepository<TestCatalogEntity> {
    }
}
