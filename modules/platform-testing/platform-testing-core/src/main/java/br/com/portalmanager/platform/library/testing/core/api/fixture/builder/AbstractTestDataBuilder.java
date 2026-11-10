package br.com.portalmanager.platform.library.testing.core.api.fixture.builder;

import br.com.portalmanager.platform.library.testing.core.api.fixture.builder.TestDataBuilder;

public abstract class AbstractTestDataBuilder<
        T,
        B extends AbstractTestDataBuilder<T, B>>
        implements TestDataBuilder<T> {

    @SuppressWarnings("unchecked")
    protected final B self() {
        return (B) this;
    }
}
