package br.com.portalmanager.platform.library.testing.scenario;

@FunctionalInterface
public interface TestScenario<R> {

    R setup();
}
