package br.com.portalmanager.platform.library.testing.fixture;

@FunctionalInterface
public interface TestDataFactory<T> {

    T valid();
}
