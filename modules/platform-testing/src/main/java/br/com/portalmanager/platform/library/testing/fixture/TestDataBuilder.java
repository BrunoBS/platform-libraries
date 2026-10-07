package br.com.portalmanager.platform.library.testing.fixture;

@FunctionalInterface
public interface TestDataBuilder<T> {

    T build();
}
