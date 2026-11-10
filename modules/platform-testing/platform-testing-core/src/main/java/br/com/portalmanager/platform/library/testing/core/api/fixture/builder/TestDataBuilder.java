package br.com.portalmanager.platform.library.testing.core.api.fixture.builder;

@FunctionalInterface
public interface TestDataBuilder<T> {

    T build();
}
