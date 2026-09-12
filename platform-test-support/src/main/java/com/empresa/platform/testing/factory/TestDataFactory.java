package com.empresa.platform.testing.factory;

@FunctionalInterface
public interface TestDataFactory<T> {

    T valid();
}
