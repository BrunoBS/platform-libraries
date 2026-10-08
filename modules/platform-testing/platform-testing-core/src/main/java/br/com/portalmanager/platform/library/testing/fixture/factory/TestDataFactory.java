package br.com.portalmanager.platform.library.testing.fixture.factory;

@FunctionalInterface
public interface TestDataFactory<T> {

    T valid();
}
