package br.com.portalmanager.platform.library.testing.core.api.fixture.factory;

@FunctionalInterface
public interface TestDataFactory<T> {

    T valid();
}
