package com.empresa.platform.testing.builder;

@FunctionalInterface
public interface TestDataBuilder<T> {

    T build();
}
