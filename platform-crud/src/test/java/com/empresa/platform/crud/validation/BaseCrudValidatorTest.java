package com.empresa.platform.crud.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseCrudValidatorTest {

    @Test
    void shouldExecuteCreateValidationFlowDirectly() {
        TestValidator validator = new TestValidator();
        TestDTO dto = new TestDTO("name");

        validator.validateForCreate(dto);

        assertThat(validator.attributesCalled).isTrue();
        assertThat(validator.createIntegrityCalled).isTrue();
        assertThat(validator.additionalCreateCalled).isTrue();
        assertThat(validator.updateIntegrityCalled).isFalse();
        assertThat(validator.additionalUpdateCalled).isFalse();
    }

    @Test
    void shouldExecuteUpdateValidationFlowDirectly() {
        TestValidator validator = new TestValidator();
        TestDTO dto = new TestDTO("name");

        validator.validateForUpdate(dto);

        assertThat(validator.attributesCalled).isTrue();
        assertThat(validator.updateIntegrityCalled).isTrue();
        assertThat(validator.additionalUpdateCalled).isTrue();
        assertThat(validator.createIntegrityCalled).isFalse();
        assertThat(validator.additionalCreateCalled).isFalse();
    }

    @Test
    void shouldDelegateValidationExceptionToConsumer() {
        TestValidator validator = new TestValidator();

        assertThatThrownBy(() -> validator.validateForCreate(null))
                .isInstanceOf(TestValidationException.class)
                .satisfies(exception -> {
                    TestValidationException validationException = (TestValidationException) exception;
                    assertThat(validationException.result().getDetails()).hasSize(1);
                    assertThat(validationException.result().getDetails().getFirst().field()).isEqualTo("test");
                });

        assertThat(validator.attributesCalled).isFalse();
        assertThat(validator.createIntegrityCalled).isFalse();
        assertThat(validator.additionalCreateCalled).isFalse();
    }

    record TestDTO(String name) {
    }

    static class TestValidator extends BaseCrudValidator<TestDTO, Long> {
        private boolean attributesCalled;
        private boolean createIntegrityCalled;
        private boolean updateIntegrityCalled;
        private boolean additionalCreateCalled;
        private boolean additionalUpdateCalled;

        @Override
        protected void validateAttributes(TestDTO dto, CrudValidationResult result) {
            attributesCalled = true;
        }

        @Override
        protected void validateCreateIntegrity(TestDTO dto, CrudValidationResult result) {
            createIntegrityCalled = true;
        }

        @Override
        protected void validateUpdateIntegrity(TestDTO dto, CrudValidationResult result) {
            updateIntegrityCalled = true;
        }

        @Override
        protected void validateAdditionalCreate(TestDTO dto, CrudValidationResult result) {
            additionalCreateCalled = true;
        }

        @Override
        protected void validateAdditionalUpdate(TestDTO dto, CrudValidationResult result) {
            additionalUpdateCalled = true;
        }

        @Override
        protected RuntimeException validationException(CrudValidationResult result) {
            return new TestValidationException(result);
        }

        @Override
        public String entityName() {
            return "test";
        }
    }

    static class TestValidationException extends RuntimeException {
        private final CrudValidationResult result;

        TestValidationException(CrudValidationResult result) {
            this.result = result;
        }

        CrudValidationResult result() {
            return result;
        }
    }
}
