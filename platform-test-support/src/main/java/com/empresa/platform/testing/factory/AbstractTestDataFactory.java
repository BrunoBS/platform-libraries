package com.empresa.platform.testing.factory;

import com.empresa.platform.testing.builder.TestDataBuilder;

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
