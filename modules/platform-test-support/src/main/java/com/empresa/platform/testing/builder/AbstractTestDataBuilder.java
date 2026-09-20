package com.empresa.platform.testing.builder;

public abstract class AbstractTestDataBuilder<
        T,
        B extends AbstractTestDataBuilder<T, B>>
        implements TestDataBuilder<T> {

    @SuppressWarnings("unchecked")
    protected final B self() {
        return (B) this;
    }
}
