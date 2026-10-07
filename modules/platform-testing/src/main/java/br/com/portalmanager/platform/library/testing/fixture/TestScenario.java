package br.com.portalmanager.platform.library.testing.fixture;

@FunctionalInterface
public interface TestScenario<R> {

    R setup();
}
