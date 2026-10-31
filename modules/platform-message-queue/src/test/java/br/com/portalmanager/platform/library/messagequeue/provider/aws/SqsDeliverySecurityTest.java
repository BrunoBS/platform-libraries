package br.com.portalmanager.platform.library.messagequeue.provider.aws;

import br.com.portalmanager.platform.library.messagequeue.capability.QueueCapabilitiesRegistry;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry.ListenerDefinition;
import br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueueMessage;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.serialization.MessageQueueSerializer;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.slf4j.MDC;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SqsDeliverySecurityTest {
    final SqsClient client=mock(SqsClient.class);
    final MessageQueueSerializer serializer=mock(MessageQueueSerializer.class);
    final SqsMessageQueueConsumer consumer=new SqsMessageQueueConsumer(client,mock(MessageQueueListenerRegistry.class),
        mock(DestinationResolver.class),serializer,new MessageQueueProperties(),mock(QueueCapabilitiesRegistry.class));
    public static class Listener {
        Runnable effect;
        public void receive(MessageQueueMessage<String> value) { effect.run(); }
    }
    final Listener listener=new Listener();
    @AfterEach void cleanup() { MDC.clear(); assertThat(SqsMessageQueueConsumer.currentDelivery()).isEmpty(); }
    void deliver() throws Exception {
        MessageQueueMessage<String> envelope=new MessageQueueMessage<>("event-id","existing","1",Instant.now(),"correlation",Map.of("destination","forged"),"payload");
        when(serializer.deserialize("body",String.class)).thenReturn(envelope);
        ListenerDefinition definition=new ListenerDefinition("configured",listener,Listener.class.getMethod("receive",MessageQueueMessage.class),String.class);
        Method process=SqsMessageQueueConsumer.class.getDeclaredMethod("process",ListenerDefinition.class,String.class,Message.class);
        process.setAccessible(true);
        process.invoke(consumer,definition,"https://sqs.example/account/queue",Message.builder().messageId("sqs-id").body("body").receiptHandle("receipt").build());
    }
    @Test void proofComesFromConsumerAndAckFollowsEffect() throws Exception {
        MDC.put("correlationId","before"); MDC.put("accountId","previous-account");
        listener.effect=()->{
            SqsMessageQueueConsumer.Delivery proof=SqsMessageQueueConsumer.currentDelivery().orElseThrow();
            assertThat(proof.destination()).isEqualTo("configured");
            assertThat(proof.queueUrl()).isEqualTo("https://sqs.example/account/queue");
            assertThat(proof.transportMessageId()).isEqualTo("sqs-id");
            assertThat(proof.correlationId()).isEqualTo("correlation");
            assertThat(MDC.get("correlationId")).isEqualTo("correlation");
            assertThat(MDC.get("accountId")).isNull();
            verify(client,never()).deleteMessage(any(DeleteMessageRequest.class));
        };
        deliver();
        verify(client).deleteMessage(any(DeleteMessageRequest.class));
        assertThat(MDC.get("correlationId")).isEqualTo("before");
        assertThat(MDC.get("accountId")).isEqualTo("previous-account");
    }
    @Test void failedHandlerClearsProofAndDoesNotAck() throws Exception {
        listener.effect=()->{ throw new IllegalStateException("transient-test-failure"); };
        deliver();
        assertThat(SqsMessageQueueConsumer.currentDelivery()).isEmpty();
        verify(client,never()).deleteMessage(any(DeleteMessageRequest.class));
    }
    @Test void proofIsNotInheritedByAsyncWork() throws Exception {
        listener.effect=()->{
            try (ExecutorService executor=Executors.newVirtualThreadPerTaskExecutor()) {
                try { assertThat(executor.submit(()->SqsMessageQueueConsumer.currentDelivery().isPresent()).get(3,TimeUnit.SECONDS)).isFalse(); }
                catch(Exception failure) { throw new IllegalStateException(failure); }
            }
        };
        deliver();
        verify(client).deleteMessage(any(DeleteMessageRequest.class));
    }
    @Test void ackFailureAllowsRedeliveryWithoutResidualContext() throws Exception {
        java.util.concurrent.atomic.AtomicInteger commits=new java.util.concurrent.atomic.AtomicInteger();
        listener.effect=()->{ assertThat(SqsMessageQueueConsumer.currentDelivery()).isPresent(); commits.incrementAndGet(); };
        when(client.deleteMessage(any(DeleteMessageRequest.class))).thenThrow(new IllegalStateException("network-test-failure"));
        deliver();
        assertThat(SqsMessageQueueConsumer.currentDelivery()).isEmpty();
        deliver();
        assertThat(commits).hasValue(2); // Business deduplication belongs to the transactional use case, not the queue.
        verify(client,times(2)).deleteMessage(any(DeleteMessageRequest.class));
    }
}
