package br.com.portalmanager.core.testing.factory;

import br.com.portalmanager.core.testing.builder.TestDataBuilder;

public abstract class AbstractTestDataFactory<
        T,
        B extends TestDataBuilder<T>>
        implements TestDataFactory<T> {

    protected abstract B builder();

    @Override
    public T valid() {
        return builder().build();
    }
}
