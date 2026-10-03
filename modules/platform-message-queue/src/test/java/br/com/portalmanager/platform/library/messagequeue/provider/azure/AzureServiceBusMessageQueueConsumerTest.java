package br.com.portalmanager.platform.library.messagequeue.provider.azure;

import com.azure.core.amqp.exception.AmqpErrorCondition;
import com.azure.core.amqp.exception.AmqpException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AzureServiceBusMessageQueueConsumerTest {

    @Test
    void shouldRecognizeSessionAcquisitionTimeoutAsAnIdlePoll() {
        var timeout = new AmqpException(
                true,
                AmqpErrorCondition.TIMEOUT_ERROR,
                "No session became available before the operation timed out",
                null);

        assertThat(AzureServiceBusMessageQueueConsumer.isSessionAcquisitionTimeout(timeout)).isTrue();
    }

    @Test
    void shouldKeepOtherAmqpErrorsOnTheFailurePath() {
        var failure = new AmqpException(
                false,
                AmqpErrorCondition.UNAUTHORIZED_ACCESS,
                "The client is not authorized",
                null);

        assertThat(AzureServiceBusMessageQueueConsumer.isSessionAcquisitionTimeout(failure)).isFalse();
    }
}
