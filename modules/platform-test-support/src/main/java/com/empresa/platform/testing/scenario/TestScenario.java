package com.empresa.platform.testing.scenario;

@FunctionalInterface
public interface TestScenario<R> {

    R setup();
}
