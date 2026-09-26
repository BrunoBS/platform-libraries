package br.com.portalmanager.platform.library.testing.factory;

import br.com.portalmanager.platform.library.testing.builder.TestDataBuilder;

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
