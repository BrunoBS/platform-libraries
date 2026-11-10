package br.com.portalmanager.platform.library.testing.core.api.fixture.scenario;

@FunctionalInterface
public interface TestScenario<R> {

    R setup();
}
