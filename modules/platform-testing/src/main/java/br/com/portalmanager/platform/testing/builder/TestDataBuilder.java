package br.com.portalmanager.platform.testing.builder;

@FunctionalInterface
public interface TestDataBuilder<T> {

    T build();
}
