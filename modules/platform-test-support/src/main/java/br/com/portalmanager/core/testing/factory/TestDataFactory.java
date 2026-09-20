package br.com.portalmanager.core.testing.factory;

@FunctionalInterface
public interface TestDataFactory<T> {

    T valid();
}
