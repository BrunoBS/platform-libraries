package br.com.portalmanager.platform.library.testing.fixture.scenario;

@FunctionalInterface
public interface TestScenario<R> {

    R setup();
}
