package com.empresa.platform.crud.version;

import com.empresa.platform.messaging.exception.ResourceVersionConflictException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptimisticLockSupportTest {

    @Test
    void shouldAcceptCreateWithoutVersion() {
        assertThatCode(() -> OptimisticLockSupport.validateCreate(new TestResource(null)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectCreateWithVersion() {
        assertThatThrownBy(() -> OptimisticLockSupport.validateCreate(new TestResource(1L)))
                .isInstanceOf(ResourceVersionConflictException.class);
    }

    @Test
    void shouldIgnoreNonVersionedCreate() {
        assertThatCode(() -> OptimisticLockSupport.validateCreate(new Object()))
                .doesNotThrowAnyException();
    }

    private record TestResource(Long version) implements VersionedResource {
    }
}
