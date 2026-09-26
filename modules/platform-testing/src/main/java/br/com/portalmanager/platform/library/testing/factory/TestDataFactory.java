package br.com.portalmanager.platform.library.testing.factory;

@FunctionalInterface
public interface TestDataFactory<T> {

    T valid();
}
