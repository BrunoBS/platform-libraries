package br.com.portalmanager.core.testing.scenario;

@FunctionalInterface
public interface TestScenario<R> {

    R setup();
}
