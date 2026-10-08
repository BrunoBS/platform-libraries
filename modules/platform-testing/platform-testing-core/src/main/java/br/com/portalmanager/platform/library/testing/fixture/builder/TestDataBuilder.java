package br.com.portalmanager.platform.library.testing.fixture.builder;

@FunctionalInterface
public interface TestDataBuilder<T> {

    T build();
}
