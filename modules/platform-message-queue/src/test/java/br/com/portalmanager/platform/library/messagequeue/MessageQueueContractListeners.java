package br.com.portalmanager.platform.library.messagequeue;

import br.com.portalmanager.platform.library.messagequeue.annotation.MessageQueueDeadLetterListener;
import br.com.portalmanager.platform.library.messagequeue.annotation.MessageQueueListener;
import br.com.portalmanager.platform.library.messagequeue.contract.DeadLetterMessage;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueueMessage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageQueueContractListeners {

    public static final BlockingQueue<MessageQueueMessage<ContractPayload>> RECEIVED = new LinkedBlockingQueue<>();
    public static final BlockingQueue<DeadLetterMessage<ContractPayload>> DEAD_LETTERED = new LinkedBlockingQueue<>();
    public static final AtomicInteger FAILED_DELIVERIES = new AtomicInteger();

    @MessageQueueListener("consumer-contract")
    public void consume(MessageQueueMessage<ContractPayload> message) {
        RECEIVED.offer(message);
    }

    @MessageQueueListener("failing-contract")
    public void fail(MessageQueueMessage<ContractPayload> message) {
        FAILED_DELIVERIES.incrementAndGet();
        throw new IllegalStateException("contract listener failure");
    }

    @MessageQueueDeadLetterListener("failing-contract")
    public void consumeDeadLetter(DeadLetterMessage<ContractPayload> message) {
        DEAD_LETTERED.offer(message);
    }

    public static void reset() {
        RECEIVED.clear();
        DEAD_LETTERED.clear();
        FAILED_DELIVERIES.set(0);
    }

    public record ContractPayload(String id) {
    }
}
