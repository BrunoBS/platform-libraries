package com.empresa.platform.testing.factory;

import com.empresa.platform.testing.builder.TestDataBuilder;

public abstract class AbstractTestDataFactory<
        T,
        B extends TestDataBuilder<T>> {

    protected abstract B builder();

    public T valid() {
        return builder().build();
    }
}
