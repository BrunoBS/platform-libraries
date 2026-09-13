package com.empresa.platform.crud.validation;

import com.empresa.platform.messaging.exception.ValidationException;
import com.empresa.platform.messaging.validation.ValidationResult;
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

        validator.validateForUpdate(10L, dto);

        assertThat(validator.attributesCalled).isTrue();
        assertThat(validator.updateIntegrityCalled).isTrue();
        assertThat(validator.additionalUpdateCalled).isTrue();
        assertThat(validator.createIntegrityCalled).isFalse();
        assertThat(validator.additionalCreateCalled).isFalse();
    }

    @Test
    void shouldStopValidationWhenDtoIsRequired() {
        TestValidator validator = new TestValidator();

        assertThatThrownBy(() -> validator.validateForCreate(null))
                .isInstanceOf(ValidationException.class);

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
        protected void validateAttributes(TestDTO dto, ValidationResult result) {
            attributesCalled = true;
        }

        @Override
        protected void validateCreateIntegrity(TestDTO dto, ValidationResult result) {
            createIntegrityCalled = true;
        }

        @Override
        protected void validateUpdateIntegrity(Long id, TestDTO dto, ValidationResult result) {
            updateIntegrityCalled = true;
        }

        @Override
        protected void validateAdditionalCreate(TestDTO dto, ValidationResult result) {
            additionalCreateCalled = true;
        }

        @Override
        protected void validateAdditionalUpdate(Long id, TestDTO dto, ValidationResult result) {
            additionalUpdateCalled = true;
        }

        @Override
        public String entityName() {
            return "test";
        }
    }
}
