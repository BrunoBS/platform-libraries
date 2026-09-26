package br.com.portalmanager.platform.library.testing.builder;

@FunctionalInterface
public interface TestDataBuilder<T> {

    T build();
}
