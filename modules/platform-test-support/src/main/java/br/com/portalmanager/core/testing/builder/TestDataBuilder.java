package br.com.portalmanager.core.testing.builder;

@FunctionalInterface
public interface TestDataBuilder<T> {

    T build();
}
