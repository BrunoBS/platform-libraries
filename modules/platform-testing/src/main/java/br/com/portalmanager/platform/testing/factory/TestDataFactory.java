package br.com.portalmanager.platform.testing.factory;

@FunctionalInterface
public interface TestDataFactory<T> {

    T valid();
}
