package br.com.portalmanager.platform.library.catalog.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogEnumTest {

    @Test
    void shouldResolveEnumByExactCode() {
        assertThat(CatalogEnum.from(TestCatalog.class, "ONE")).isEqualTo(TestCatalog.ONE);
        assertThat(CatalogEnum.from(TestCatalog.class, "UNKNOWN")).isNull();
        assertThat(CatalogEnum.from(TestCatalog.class, null)).isNull();
    }

    @Test
    void shouldListValidOptions() {
        assertThat(CatalogEnum.getOptionsValid(TestCatalog.class))
                .isEqualTo("ONE, TWO");
    }

    private enum TestCatalog implements CatalogEnum<TestCatalog> {
        ONE,
        TWO
    }
}
