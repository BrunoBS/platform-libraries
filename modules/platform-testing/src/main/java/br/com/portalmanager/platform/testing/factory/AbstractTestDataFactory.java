package br.com.portalmanager.platform.testing.factory;

import br.com.portalmanager.platform.testing.builder.TestDataBuilder;

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
