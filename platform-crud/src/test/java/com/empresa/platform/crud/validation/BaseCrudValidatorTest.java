package com.empresa.platform.crud.validation;

import com.empresa.platform.crud.dto.BaseCrudDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseCrudValidatorTest {


    @Test
    void shouldUseDefaultCrudValidationException() {
        DefaultTestValidator validator = new DefaultTestValidator();

        assertThatThrownBy(() -> validator.validateForCreate(null))
                .isInstanceOf(CrudValidationException.class)
                .satisfies(exception -> {
                    CrudValidationException validationException =
                            (CrudValidationException) exception;

                    assertThat(validationException.getDetails())
                            .singleElement()
                            .satisfies(detail -> {
                                assertThat(detail.field()).isEqualTo("default-test");
                                assertThat(detail.messageKey())
                                        .isEqualTo(CrudMessageKeys.REQUIRED);
                            });
                });
    }

    @Test
    void shouldExecuteCreateValidationFlowDirectly() {
        TestValidator validator = new TestValidator();
        TestDTO dto = new TestDTO(null, "name");

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
        TestDTO dto = new TestDTO(1L, "name");

        validator.validateForUpdate(dto);

        assertThat(validator.attributesCalled).isTrue();
        assertThat(validator.updateIntegrityCalled).isTrue();
        assertThat(validator.additionalUpdateCalled).isTrue();
        assertThat(validator.createIntegrityCalled).isFalse();
        assertThat(validator.additionalCreateCalled).isFalse();
    }

    @Test
    void shouldRejectIdentifierOnCreate() {
        TestValidator validator = new TestValidator();

        assertThatThrownBy(() ->
                validator.validateForCreate(new TestDTO(1L, "name")))
                .isInstanceOf(TestValidationException.class)
                .satisfies(exception -> {
                    TestValidationException validationException =
                            (TestValidationException) exception;
                    assertThat(validationException.result().getDetails())
                            .anySatisfy(detail -> {
                                assertThat(detail.field()).isEqualTo("id");
                                assertThat(detail.messageKey())
                                        .isEqualTo(CrudMessageKeys.ID_MUST_BE_ABSENT);
                            });
                });
    }

    @Test
    void shouldRequireIdentifierForFindUpdateAndDelete() {
        TestValidator validator = new TestValidator();
        TestDTO dto = new TestDTO(null, "name");

        assertMissingId(() -> validator.validateForFind(dto));
        assertMissingId(() -> validator.validateForUpdate(dto));
        assertMissingId(() -> validator.validateForDelete(dto));

        assertThat(validator.attributesCalled).isFalse();
        assertThat(validator.updateIntegrityCalled).isFalse();
        assertThat(validator.additionalUpdateCalled).isFalse();
    }

    @Test
    void shouldDelegateValidationExceptionToConsumer() {
        TestValidator validator = new TestValidator();

        assertThatThrownBy(() -> validator.validateForCreate(null))
                .isInstanceOf(TestValidationException.class)
                .satisfies(exception -> {
                    TestValidationException validationException =
                            (TestValidationException) exception;
                    assertThat(validationException.result().getDetails()).hasSize(1);
                    assertThat(validationException.result().getDetails().getFirst().field())
                            .isEqualTo("test");
                });

        assertThat(validator.attributesCalled).isFalse();
        assertThat(validator.createIntegrityCalled).isFalse();
        assertThat(validator.additionalCreateCalled).isFalse();
    }

    private static void assertMissingId(Runnable validation) {
        assertThatThrownBy(validation::run)
                .isInstanceOf(TestValidationException.class)
                .satisfies(exception -> {
                    TestValidationException validationException =
                            (TestValidationException) exception;
                    assertThat(validationException.result().getDetails())
                            .anySatisfy(detail -> {
                                assertThat(detail.field()).isEqualTo("id");
                                assertThat(detail.messageKey())
                                        .isEqualTo(CrudMessageKeys.ID_REQUIRED);
                            });
                });
    }

    record TestDTO(Long id, String name) implements BaseCrudDTO<Long> {
    }

    static class DefaultTestValidator extends BaseCrudValidator<TestDTO> {
        @Override
        public String entityName() {
            return "default-test";
        }
    }

    static class TestValidator extends BaseCrudValidator<TestDTO> {
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
