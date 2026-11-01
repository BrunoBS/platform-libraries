package br.com.portalmanager.platform.library.scheduling.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;

class BlockingReactiveScheduledMethodPostProcessorTest {
    @Test
    void rejectsReactiveScheduledMethodThatReturnsNullWithPlatformError() {
        NullPublisherScheduledTask task = (NullPublisherScheduledTask)
                new BlockingReactiveScheduledMethodPostProcessor()
                        .postProcessAfterInitialization(new NullPublisherScheduledTask(), "task");

        assertThatThrownBy(task::run)
                .isInstanceOf(PlatformConfigurationException.class)
                .satisfies(failure -> assertThat(
                                ((PlatformConfigurationException) failure).getErrorResponse().code())
                        .isEqualTo("PLT-SCHED-005"));
    }

    static class NullPublisherScheduledTask {
        @Scheduled(fixedRate = 1_000)
        public Mono<Void> run() {
            return null;
        }
    }
}
