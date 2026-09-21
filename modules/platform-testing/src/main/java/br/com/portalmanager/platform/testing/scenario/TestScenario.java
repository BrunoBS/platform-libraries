package br.com.portalmanager.platform.testing.scenario;

@FunctionalInterface
public interface TestScenario<R> {

    R setup();
}
